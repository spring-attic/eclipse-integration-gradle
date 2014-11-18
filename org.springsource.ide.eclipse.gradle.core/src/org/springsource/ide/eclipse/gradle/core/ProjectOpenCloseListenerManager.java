/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author John Schneider
 * @author Kris De Volder
 */
public class ProjectOpenCloseListenerManager implements IResourceChangeListener {

	private ListenerList listeners = new ListenerList();
	//No need to lazy init this list because the ProjectOpenCloseListenerManager only gets created if
	// at least one listener needs to be added to it.

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
				IResource res = event.getResource();
				IProject project;
				switch (res.getType()) {
				case IResource.FOLDER:
					project = ((IFolder) res).getProject();
					break;
				case IResource.FILE:
					project = ((IFile) res).getProject();
					break;
				case IResource.PROJECT:
					project = (IProject) res;
					break;
				default:
					return;
				}
				projectClosed(project);
			} else if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				// Find out if a project was opened.
				IResourceDelta delta = event.getDelta();
				if (delta == null) return;

				IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
				for (int i = 0; i < projDeltas.length; ++i) {
					IResourceDelta projDelta = projDeltas[i];
					if ((projDelta.getFlags() & IResourceDelta.OPEN) == 0)
						continue;
					IResource resource = projDeltas[i].getResource();
					if (!(resource instanceof IProject))
						continue;

					IProject project = (IProject) resource;
					projectOpened(project);
				}
			}
		} catch (OperationCanceledException oce) {
			// do nothing
		}
	}

	private void projectClosed(final IProject project) {
		if (GradleNature.hasNature(project)) {
			for (Object l : listeners.getListeners()) {
				((ProjectOpenCloseListener)l).projectClosed(project);
			}
		}
	}

	private void projectOpened(final IProject project) {
		if (GradleNature.hasNature(project)) {
			for (Object l : listeners.getListeners()) {
				((ProjectOpenCloseListener)l).projectOpened(project);
			}
		}
	}

	public void add(ProjectOpenCloseListener l) {
		listeners.add(l);
	}

	//	private void updateRelatedProjects(final IProject project, IProgressMonitor monitor) {
	//		GradleProject updatingProject = GradleCore.create(project);
	//		
	//		for (GradleProject possibleRelatedProject : GradleCore.getGradleProjects()) {
	//			if(possibleRelatedProject.getProject().equals(project))
	//				continue;
	//			try {
	//				if(possibleRelatedProject.dependsOn(updatingProject) || dependsOnThroughIvyDependency(possibleRelatedProject, updatingProject, monitor))
	//					GradleClasspathContainerGroup.requestUpdate(possibleRelatedProject.getProject(), false);
	//			} catch (FastOperationFailedException e) {
	//				GradleCore.log(e);
	//			} catch (CoreException e) {
	//				GradleCore.log(e);
	//			}
	//		}
	//	}

	//	private boolean dependsOnThroughIvyDependency(GradleProject depender, GradleProject dependee, IProgressMonitor monitor) {
	//		try {
	//			ExternalDependency library = IvyUtils.getLibrary(dependee.getGradleModel(monitor), monitor);
	//			IPath jarPath = library != null ? new Path(library.getFile().getAbsolutePath()) : null; 
	//			
	//			for (IClasspathEntry entry : depender.getDependencyComputer().getClassPath(monitor).toArray())
	//				if(entry.getPath().equals(jarPath))
	//					return true;
	//			for (IClasspathEntry entry : depender.getDependencyComputer().getProjectClassPath(monitor).toArray())
	//				if(entry.getPath().equals(jarPath) || entry.getPath().equals(dependee.getProject().getFullPath()))
	//					return true;
	//		} catch (CoreException e) {
	//			GradleCore.log(e);
	//		}
	//		return false;
	//	}
}
