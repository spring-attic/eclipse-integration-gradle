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

import java.util.ArrayList;
import java.util.Collection;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * Topological sorter to sort Gradle projects to put them in the 'right order' (i.e. project 
 * dependencies before any project that depends on it.
 * 
 * @author Kris De Volder
 */
public class GradleProjectSorter extends TopoSort<HierarchicalEclipseProject> {

	public GradleProjectSorter(Collection<HierarchicalEclipseProject> elements) {
		super(elements, new DependencyOrder());
	}
	
	private static class DependencyOrder implements PartialOrder<HierarchicalEclipseProject> {

		public Collection<HierarchicalEclipseProject> getPredecessors(HierarchicalEclipseProject project) {
			DomainObjectSet<? extends EclipseProjectDependency> deps = project.getProjectDependencies();
			ArrayList<HierarchicalEclipseProject> result = new ArrayList<HierarchicalEclipseProject>(deps.size());
			for (EclipseProjectDependency d : deps) {
				result.add(d.getTargetProject());
			}
			return result;
		}

	}

}
