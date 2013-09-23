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
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.Collection;
import java.util.HashSet;

import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * Utility methods that operate on project model objects from the Gradle API.
 * 
 * @author Kris De Volder
 */
public class GradleProjectUtil {

	/**
	 * Starting from a given project, retrieve a list of all projects including the project itself
	 * and all its nested children, grandchildren etc.
	 */
	public static Collection<HierarchicalEclipseProject> getAllProjects(HierarchicalEclipseProject rootModel) {
		HashSet<HierarchicalEclipseProject> all = new HashSet<HierarchicalEclipseProject>();
		collectChildren(rootModel, all);
		return all;
	}

	private static void collectChildren(HierarchicalEclipseProject rootModel, HashSet<HierarchicalEclipseProject> all) {
		all.add(rootModel);
		for (HierarchicalEclipseProject c : rootModel.getChildren()) {
			collectChildren(c, all);
		}
	}
}
