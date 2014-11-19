/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.taskview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.modelmanager.IGradleModelListener;

/**
 * Content provider for displaying tasks tree
 * 
 * @author Kris De Volder
 * @author Alex Boyko
 * 
 */
public class TaskTreeContentProvider implements ITreeContentProvider {
	private static final Object[] NO_ELEMENTS = new Object[0];
	
	private TreeViewer viewer;
	private GradleProject currentProject;
	private boolean isLocalTasks;
	private boolean isHideInternalTasks;
	
	public TaskTreeContentProvider(TreeViewer viewer) {
		this.viewer = viewer;
	}
	
	public void dispose() {
		try {
			if (currentProject!=null) {
				if (modelListener!=null) {
					currentProject.removeModelListener(modelListener);
				}
			}
		} finally {
			currentProject=null;
			modelListener=null;
		}
	}
	
	private IGradleModelListener modelListener = new IGradleModelListener() {
		@Override
		public <T> void modelChanged(GradleProject p, Class<T> type,
				T model) {
			if (currentProject==p && (model instanceof BuildInvocations || model instanceof EclipseProject)) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (viewer!=null) {
							viewer.refresh();
							Tree tree = viewer.getTree();
							for (TreeColumn col : tree.getColumns()) {
								col.pack();
							}
						}
					}
				});
			}
		}
	};

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Assert.isTrue(this.viewer == viewer);
		Assert.isTrue(newInput==null || newInput instanceof GradleProject);
		setProject((GradleProject)newInput);
	}

	private void setProject(GradleProject project) {
		if (currentProject != project) {
			if (currentProject != null) {
				currentProject.removeModelListener(modelListener);
			}
			currentProject = project;
			if (currentProject != null) {
				currentProject.addModelListener(modelListener);
			}
		}
	}
	
	public Object[] getElements(Object inputElement) {
		GradleProject root = (GradleProject) inputElement;
		if (root==null) {
			return NO_ELEMENTS;
		} else {
			try {
				boolean modelNotAvailable = false;
				/*
				 * Request EclipseProject and BuildInvocations models in
				 * parallel. Swallow the exception and proceed to the next model
				 * request if previous throws FastOperationException
				 */
				try {
					root.getModelOfType(BuildInvocations.class);
				} catch (FastOperationFailedException e) {
					modelNotAvailable = true;
				}
				try {
					root.requestGradleModel();
				} catch (FastOperationFailedException e) {
					modelNotAvailable = true;
				}
				GradleTask[] gradleTasks = getGradleTasks(root);
				return modelNotAvailable ? new Object[] {"model not yet available"} : gradleTasks;
			} catch (CoreException e) {
				GradleCore.log(e);
				return new Object[] {"ERROR: "+e.getMessage()+"", "See error log for details"};
			}
		}
	}
	
	private GradleTask[] getGradleTasks(GradleProject project) {
		try {
			EclipseProject eclipseProjectModel = project.getGradleModel();
			BuildInvocations buildInvocationsModel = project.getModelOfType(BuildInvocations.class);
			ProjectTasksVisibility tasksVisibility = null;
			try {
				tasksVisibility = new ProjectTasksVisibility(buildInvocationsModel);
			} catch (UnsupportedMethodException e) {
				/*
				 * Make all tasks public if Gradle runtime does for the project does not support visibility feature
				 */
				GradleTasksViewPlugin
						.getDefault()
						.getLog()
						.log(new Status(
								IStatus.WARNING,
								GradleTasksViewPlugin.PLUGIN_ID,
								"All tasks for project '"
										+ project.getName()
										+ "' will be shown as public because it uses old version of Gradle",
								e));
			}
			List<GradleTask> tasksCollection = new ArrayList<GradleTask>(Math.max(buildInvocationsModel.getTasks().size(), buildInvocationsModel.getTaskSelectors().size()));
			for (final GradleTask task : isLocalTasks
					? GradleProject.getTasks(eclipseProjectModel)
					: GradleProject.getAggregateTasks(eclipseProjectModel).values()) {
				final boolean isPublic = tasksVisibility == null || (isLocalTasks ? tasksVisibility.isTaskPublic(task.getName()) : tasksVisibility.isTaskSelectorPublic(task.getName()));
				if (!isHideInternalTasks || isPublic) {
					tasksCollection.add(
						new GradleTask() {
			
							@Override
							public String getPath() {
								return task.getPath();
							}
			
							@Override
							public String getName() {
								return task.getName();
							}
			
							@Override
							public String getDescription() {
								return task.getDescription();
							}
			
							@Override
							public String getDisplayName() {
								return task.getDisplayName();
							}
			
							@Override
							public boolean isPublic() {
								return isPublic;
							}
			
							@Override
							public org.gradle.tooling.model.GradleProject getProject() {
								return task.getProject();
							}
							
						}
					);
				}
			}
			return tasksCollection.toArray(new GradleTask[tasksCollection.size()]);
		} catch (FastOperationFailedException e) {
			return new GradleTask[0];
		} catch (CoreException e) {
			return new GradleTask[0];
		}
	}
	
	public Object[] getChildren(Object parentElement) {
		return NO_ELEMENTS;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}

	public void setLocalTasks(boolean isLocalTasks) {
		this.isLocalTasks = isLocalTasks;
	}
	
	public void setHideInternalTasks(boolean isHideInternalTasks) {
		this.isHideInternalTasks = isHideInternalTasks;
	}

}
