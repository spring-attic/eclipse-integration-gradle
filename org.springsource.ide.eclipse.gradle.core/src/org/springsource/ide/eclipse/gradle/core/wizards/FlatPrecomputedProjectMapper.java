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
package org.springsource.ide.eclipse.gradle.core.wizards;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * @author Kris De Volder
 */
public class FlatPrecomputedProjectMapper extends PrecomputedProjectMapper {

	public FlatPrecomputedProjectMapper(Collection<HierarchicalEclipseProject> collection)
			throws CoreException, NameClashException {
		super(collection);
	}
	
	@Override
	protected IProject computeMapping(HierarchicalEclipseProject p) throws CoreException {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(GradleProject.getName(p));
	}

}
