/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.ui.wizards.GradleProjectTreeContentProvider;


/**
 * @author Kris De Volder
 */
public class GradleTaskTreeContentProvider implements ITreeContentProvider {
	
	private static final Object[] NO_ELEMENTS = new Object[0];
	
	private GradleProjectTreeContentProvider projectProv = null; 
	
	private TreeViewer viewer;
	
	public GradleTaskTreeContentProvider(TreeViewer viewer, boolean showProjects) {
		if (showProjects) {
			this.projectProv = new GradleProjectTreeContentProvider();
		}
		this.viewer = viewer;
	}
	
	public void dispose() {
		try {
			projectProv.dispose();
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

	private GradleProject currentProject;

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Assert.isTrue(this.viewer == viewer);
		Assert.isTrue(newInput==null || newInput instanceof GradleProject);
		setProject((GradleProject)newInput);
		if (projectProv!=null) {
			projectProv.inputChanged(viewer, oldInput, newInput);
		}
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
				EclipseProject model = root.requestGradleModel();
				return getChildren(model);
			} catch (FastOperationFailedException e) {
				return new Object[] {"model not yet available"};
			} catch (CoreException e) {
				GradleCore.log(e);
				return new Object[] {"ERROR: "+e.getMessage()+"", "See error log for details"};
			}
		}
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof EclipseProject) {
			EclipseProject parentProj = (EclipseProject) parentElement;
			DomainObjectSet<? extends Task> tasks = GradleProject.getTasks(parentProj);
			if (projectProv!=null) {
				Object[] projects = projectProv.getChildren(parentElement);
				Object[] children = new Object[projects.length+tasks.size()];
				int i = 0;
				for (Task task : tasks) {
					children[i++] = task;
				}
				System.arraycopy(projects, 0, children, i, projects.length);
				return children;
			} else {
				return tasks.toArray();
			}
		} 
		return NO_ELEMENTS;
	}

	public Object getParent(Object element) {
		if (element instanceof EclipseProject && projectProv!=null) {
			return projectProv.getParent(element);
		} else if (element instanceof Task) {
			return ((Task)element).getProject();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		Object[] c = getChildren(element);
		return c!=null && c.length>0;
	}

}
