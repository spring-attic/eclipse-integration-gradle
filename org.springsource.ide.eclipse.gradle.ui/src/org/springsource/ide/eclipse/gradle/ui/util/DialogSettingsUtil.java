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
package org.springsource.ide.eclipse.gradle.ui.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * Method that I wish would exist on IDialogSettings but don't.
 * 
 * @author Kris De Volder
 */
public class DialogSettingsUtil {

	public static boolean getBoolean(IDialogSettings dialogSettings, String key, boolean defaultValue) {
		if (dialogSettings.get(key)==null) {
			return defaultValue;
		} else {
			return dialogSettings.getBoolean(key);
		}
	}

	/**
	 * Store a GradleProject reference in a dialogSettings. This method only works if the project exists in the
	 * Eclipse workspace since it stores the Eclipse project name. If the project is null or has no eclipse name
	 * this method silently fails.
	 */
	public static void put(IDialogSettings dialogSettings, String key, GradleProject project) {
		if (project!=null) {
			String name = project.getName();
			if (name!=null) {
				dialogSettings.put(key,name);
			}
		}
	}

	/**
	 * Retrieves a GradleProject from  dialogSettings. This may return null if no project was stored, or if the
	 * project no longer exists or doesn't look like a Gradle project.
	 */
	public static GradleProject getGradleProject(IDialogSettings dialogSettings, String key) {
		String name = dialogSettings.get(key);
		if (name!=null) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (GradleNature.hasNature(project)) {
				return GradleCore.create(project);
			}
		}
		return null;
	}

}
