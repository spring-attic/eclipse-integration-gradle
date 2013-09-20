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
package org.springsource.ide.eclipse.gradle.core.wizards;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * This is a Precomputed project mapper that maps each gradle model onto an 
 * eclipse project with a name consisting of the parent projects' names
 * and the child project's name, separated by dots. Thus the eclipse
 * project name reflects the position in the gradle project hierarchy for
 * a given gradle project.
 * 
 * @author Kris De Volder
 */
public class HierarchicalProjectMapper extends PrecomputedProjectMapper {

	public HierarchicalProjectMapper(Collection<HierarchicalEclipseProject> collection) throws CoreException, NameClashException {
		super(collection);
	}

	@Override
	protected IProject computeMapping(HierarchicalEclipseProject p) throws CoreException {
		String name = GradleProject.getHierarchicalName(p);
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

}
