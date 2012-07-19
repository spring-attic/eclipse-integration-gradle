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
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * @author Kris De Volder
 */
public class GradleNature implements IProjectNature {
	
	public static final boolean DEBUG = false;
	static {
		if (DEBUG) {
			System.out.println("Class is initialized: "+GradleNature.class.getName());
		}
	}
	
	public static final String NATURE_ID = "org.springsource.ide.eclipse.gradle.core.nature";
	public static final String OLD_NATURE_ID = "com.springsource.sts.gradle.core.nature";
	
	private IJavaProject project;
	
	public void configure() throws CoreException {
		if (DEBUG) {
			System.out.println("Configuring Gradle Nature for "+project.getElementName());
		}
		if (DEBUG) {
			System.out.println("Done configuring Gradle Nature for "+project.getElementName());
		}
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return project.getProject();
	}

	public void setProject(IProject project) {
		this.project = JavaCore.create(project);
	}

	public static boolean hasNature(IProject p) {
		try {
			return p!=null && p.isAccessible() && p.hasNature(NATURE_ID);
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return false;
	}

}
