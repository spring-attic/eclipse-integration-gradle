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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.Launchable;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;

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
		public void modelChanged(final GradleProject p, Object model) {
			if (currentProject==p && model instanceof BuildInvocations) {
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
		GradleProject project = (GradleProject) inputElement;
		if (project == null) {
			return NO_ELEMENTS;
		} else {
			try {
				BuildInvocations model = project.getSpecificModel(BuildInvocations.class);
				if (model != null) {
					DomainObjectSet<? extends Launchable> tasks = isLocalTasks
							? model.getTasks()
							: model.getTaskSelectors();
					if (isHideInternalTasks) {
						List<Launchable> result = new ArrayList<Launchable>(
								tasks.size());
						for (Launchable task : tasks) {
							if (task.isPublic()) {
								result.add(task);
							}
						}
						return result.toArray(new Launchable[result.size()]);
					} else {
						return tasks.toArray(new Launchable[tasks.size()]);
					}
				}
			} catch (Exception e) {
				GradleTasksViewPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR,
								GradleTasksViewPlugin.PLUGIN_ID,
								"Cannot fetch BuildInvocationsModel", e));
			}
			return new Object[0];
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
