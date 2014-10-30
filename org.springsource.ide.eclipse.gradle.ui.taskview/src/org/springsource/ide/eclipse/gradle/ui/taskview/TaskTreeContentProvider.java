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

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.gradle.tooling.model.GradleTask;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;

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
		public void modelChanged(final GradleProject p) {
			if (currentProject==p) {
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
		if (currentProject!=project) {
			GradleProject oldProject = currentProject;
			currentProject = project;
			if (oldProject!=null) {
				oldProject.removeModelListener(modelListener);
			}
			if (project!=null) {
				project.addModelListener(modelListener);
			}
		}
	}

	public Object[] getElements(Object inputElement) {
		GradleProject root = (GradleProject) inputElement;
		if (root==null) {
			return NO_ELEMENTS;
		} else {
			try {
				GradleTask[] gradleTasks = getGradleTasks(root.requestGradleModel());
				return gradleTasks;
			} catch (FastOperationFailedException e) {
				return new Object[] {"model not yet available"};
			} catch (CoreException e) {
				GradleCore.log(e);
				return new Object[] {"ERROR: "+e.getMessage()+"", "See error log for details"};
			}
		}
	}
	
	private GradleTask[] getGradleTasks(StsEclipseProject project) {
		Collection<? extends GradleTask> tasksCollection = isLocalTasks
				? GradleProject.getTasks(project)
				: GradleProject.getAggregateTasks(project).values();
		return tasksCollection.toArray(new GradleTask[tasksCollection.size()]);
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

}
