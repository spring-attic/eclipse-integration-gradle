/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.classpathcontainer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.springsource.ide.eclipse.gradle.core.Debug;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * @author Kris De Volder
 */
public class GradleClasspathContainerInitializer extends ClasspathContainerInitializer {
	
	private static final boolean DEBUG = Debug.FALSE;

	public static void debug(String msg) {
		if (DEBUG) {
			System.out.println("GradleClasspathContainerInitializer:" +msg);
		}
	}
	
	public static GradleClasspathContainerInitializer instance = null;

	public GradleClasspathContainerInitializer() {
		instance = this;
		debug("Creating instance");
	}
	
	private synchronized GradleClassPathContainer get(IJavaProject jProject, IPath path) {
		GradleProject gradleProject = GradleCore.create(jProject.getProject());
		GradleClassPathContainer it;
		synchronized (gradleProject) {
			it = gradleProject.getClassPathcontainer();
			if (it==null) {
				it = new GradleClassPathContainer(path, GradleCore.create(jProject));
				gradleProject.setClassPathContainer(it);
			}
		}
		return it;
	}
	
	/**
	 * A 'gentler' version of 'get'. It will return 'null' if the container wasn't created yet, but
	 * will not force the creation of a container.
	 */
	private synchronized GradleClassPathContainer getMaybe(IProject project) {
		GradleProject gProject = GradleCore.getGradleProject(project);
		if (gProject!=null) {
			return gProject.getClassPathcontainer();
		}
		return null;
	}

	@Override
	public void initialize(final IPath path, final IJavaProject project) throws CoreException {
		debug("initialize called");
		GradleClassPathContainer it = get(project, path);
//		if (it.isInitialized()) {
//			//Set it now
//			debug("is already initialized");
			GradleClassPathContainer.setJDTClassPathContainer(project, path, it);
//		} else {
//			//Use persisted container from last session, or schedule an update
//			debug("is not yet initialized");
//			IClasspathContainer persistedContainer = JavaModelManager.getJavaModelManager().getPreviousSessionContainer(path, project);
//			if (persistedContainer!=null) {
//				IClasspathEntry[] persistedEntries = persistedContainer.getClasspathEntries();
//				debug("use "+persistedEntries.length+ " persisted entries");
//				it.setPersistedEntries(persistedEntries);
//				GradleClassPathContainer.setJDTClassPathContainer(project, path, it);
//			} else {
//				debug("request async update");
//				it.requestUpdate(false);
//			}
//		}
	}

//	@Override
//	public IClasspathContainer getFailureContainer(IPath containerPath, IJavaProject project) {
//		return null;
//	}
	
//	@Override
//	public boolean canUpdateClasspathContainer(IPath containerPath,
//			IJavaProject project) {
//		return true;
//	}
	
//	@Override
//	public void requestClasspathContainerUpdate(IPath path,
//			IJavaProject project, IClasspathContainer containerSuggestion)
//			throws CoreException {
//		GradleClassPathContainer it = get(project, path);
//		it.requestUpdate();
//	}
//
	public static Job requestUpdateFor(IProject project, boolean popupProgress) {
		if (instance!=null) {
			GradleClassPathContainer container = instance.getMaybe(project);
			if (container!=null) {
				return container.requestUpdate(popupProgress);
			}
		}
		return null;
	}
	
	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return containerPath.append(project.getElementName());
	}

}
