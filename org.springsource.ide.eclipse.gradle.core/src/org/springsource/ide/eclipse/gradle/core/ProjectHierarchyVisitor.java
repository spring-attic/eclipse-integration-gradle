/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.runtime.CoreException;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * An abstract visitor that walks projects in a Gradle project hierarchy calling its abstrack visit
 * method for each visited projects. 
 * 
 * @author Kris De Volder
 */
public abstract class ProjectHierarchyVisitor {

	/**
	 * Visit method that will be called on each project in the hierarchy. For convenience any Exception may be thrown
	 * Thrown exceptions will be caught and rethrown as CoreExcpetions. If a visit method throws the visit will
	 * be aborted.
	 */
	protected abstract void visit(HierarchicalEclipseProject project) throws Exception;
	
	public void accept(HierarchicalEclipseProject project) throws CoreException {
		try {
			visit(project);
		} catch (Exception e) {
			throw ExceptionUtil.coreException(e);
		}
		DomainObjectSet<? extends HierarchicalEclipseProject> children = project.getChildren();
		for (HierarchicalEclipseProject child : children) {
			accept(child);
		}
	}
	
}
