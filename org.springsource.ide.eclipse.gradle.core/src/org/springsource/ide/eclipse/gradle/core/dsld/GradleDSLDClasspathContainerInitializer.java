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
package org.springsource.ide.eclipse.gradle.core.dsld;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class GradleDSLDClasspathContainerInitializer extends
		ClasspathContainerInitializer {

	/**
	 * The one and only instance of the Gradle DSLD container. (Currently there's only one, but in the future 
	 * there may be different containers depending on the Gradle version.
	 */
	private static GradleDSLDClasspathContainer theContainer = null;
	
	public GradleDSLDClasspathContainerInitializer() {
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		Assert.isTrue(containerPath.equals(new Path(GradleDSLDClasspathContainer.ID)));
		if (theContainer==null) {
			theContainer = new GradleDSLDClasspathContainer();
		}
		JavaCore.setClasspathContainer(containerPath, 
				new IJavaProject[] {project}, new IClasspathContainer[] {theContainer}, new NullProgressMonitor());
	}
	
	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return super.getComparisonID(containerPath, project);
	}

}
