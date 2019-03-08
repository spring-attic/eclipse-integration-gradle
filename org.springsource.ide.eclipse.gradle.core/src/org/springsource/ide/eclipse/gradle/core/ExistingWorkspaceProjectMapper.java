/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * Maps external projects to existing workspace projects, based on the project locations.
 * <p>
 * This is a partial mapping, it can return null if there is no corresponding project
 * in the workspace.
 * 
 * @author Kris De Volder
 */
public class ExistingWorkspaceProjectMapper implements IProjectMapper {

	public IProject get(HierarchicalEclipseProject target) {
		//TODO: add a (prefilled) cache?
		try {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			String lookingFor = target.getProjectDirectory().getCanonicalPath();
			for (IProject wsProject : projects) {
				try {
					if (wsProject.getLocation().toFile().getCanonicalPath().equals(lookingFor)) {
						return wsProject;
					}
				} catch (IOException e) {
					GradleCore.log(e);
				}
			}
			return null;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
