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
package org.springsource.ide.eclipse.gradle.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;


/**
 * @author Kris De Volder
 */
public abstract class RefreshAction extends GradleProjectActionDelegate {

	@Override
	protected boolean isEnabled(IProject project) {
		try {
			if (project != null 
			&& project.isAccessible() 
			&& project.hasNature(JavaCore.NATURE_ID) 
			&& project.hasNature(GradleNature.NATURE_ID)) {
				return GradleClassPathContainer.isOnClassPath(getJavaProject());
			}
		} catch (CoreException e) {
			GradleUI.log(e);
		}
		return false;
	}

}
