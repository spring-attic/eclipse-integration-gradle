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
package org.springsource.ide.eclipse.gradle.core.util;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Utility methods related workspace and resources in the workspace.
 * 
 * @author Kris De Volder
 */
public class WorkspaceUtil {

	/**
	 * For a give File, attempt to find a project in the workspace that 'contains' this file. 
	 * The file must be an absolute file reference, but it may or may not exist in the underlying 
	 * file system and may or may not exist in the workspace (e.g. because it is hidden by
	 * a resource filter or because is not in synch with the workspace). This method only
	 * uses the path of the file to determine whether it is contained in a project by
	 * determining a project existin in the workspace who's file system location is a
	 * prefex of this file. 
	 * <p>
	 * If more than one project contains the file, then the most nested project is returned.
	 * <p>
	 * If no project is found, null is returned.
	 */
	public static IProject getContainingProject(File file) {
		IPath path = new Path(file.getAbsolutePath());
		int longest = 0;
		IProject found = null;
		for (IProject p : getProjects()) {
			IPath loc = p.getLocation();
			if (loc!=null && loc.isPrefixOf(path)) {
				//Found a match
				if (loc.segmentCount()>longest) {
					//Only keep longest match
					found = p;
					longest = loc.segmentCount();
				}
			}
		}
		return found;
	}

	public static IProject[] getProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

}
