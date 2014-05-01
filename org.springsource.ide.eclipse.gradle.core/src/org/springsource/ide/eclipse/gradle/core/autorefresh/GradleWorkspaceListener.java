/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.autorefresh;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;

/**
 * Listens for workspace changes and marks Gradle projects as dirty when
 * any .gradle file is changed.
 * 
 * @author Kris De Volder
 */
public class GradleWorkspaceListener {
	
	/**
	 * Get a set of root GradleProjects that are affected by changes to .gradle files. A root project is considered
	 * affected if a .gradle file is changed in the project itself or any of its nested projects.
	 */
	private Set<GradleProject> getAffectedRootProjects(IResourceChangeEvent event) {
		final Set<GradleProject> affectedRootProjects = new HashSet<GradleProject>();
		IResourceDelta delta = event.getDelta();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource rsrc = delta.getResource();
					int type = rsrc.getType();
					switch (type) {
					case IResource.PROJECT:
						//only interested in Gradle projects
						return GradleNature.hasNature((IProject)rsrc);
					case IResource.FILE: 
						//only interested in *.gradle files
		               if ("gradle".equals(rsrc.getFileExtension())) {	
		            	   GradleProject gp = GradleCore.create(rsrc.getProject());
		            	   try {
		            		   GradleProject root = gp.getRootProject();
		            		   affectedRootProjects.add(root);
		            	   } catch (FastOperationFailedException e) {
		            		   //If we can't do it without building a gradle model then don't bother.
		            		   //We really don't want this to automatically start building gradle models.
		            	   }
		               }
		               break;
					default:
						break;
					}
					return true;
				}
				
			});
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return affectedRootProjects;
	}
	
	public GradleWorkspaceListener(final IDirtyProjectListener dirtyProjectListener) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResourceChangeListener listener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getType() != IResourceChangeEvent.POST_CHANGE)
					return;
				
				final Set<GradleProject> affectedRootProjects = getAffectedRootProjects(event);
				if (!affectedRootProjects.isEmpty()) {
					for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
						if (GradleNature.hasNature(p)) {
							GradleProject gp = GradleCore.create(p);
							if (GradleClassPathContainer.isOnClassPath(gp.getJavaProject())) {
								//Auto refresh is limited to projects that have dependency management
								//enabled. It may be possible to broaden this.
								try {
									if (affectedRootProjects.contains(gp.getRootProject())) {
										dirtyProjectListener.addDirty(gp);
									}
								} catch (FastOperationFailedException e) {
									//ignore
								}
							}
						}
					}
				}
			}

		};
		workspace.addResourceChangeListener(listener);
	}


}
