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

import org.eclipse.core.resources.IProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * Maps external project instances to internal ones. This is necessary when creating project dependencies.
 * 
 * @author Kris De Volder
 */
public interface IProjectMapper {

	/**
	 * Returns an IProject instance associated with a given Gradle project model, according to some lookup mechanism or
	 * naming scheme. Some concrete implementations of this may return null if there is no suitable mapping. Others may
	 * return projects that do not actually exist in the workspace.
	 */
	IProject get(HierarchicalEclipseProject target);

}
