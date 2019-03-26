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
import org.eclipse.core.resources.ResourcesPlugin;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * Suitable base implementation for IProjectMapper, it returns a project instance based on the default
 * project naming scheme of {@link GradleImportOperation}
 * 
 * @author Kris De Volder
 */
public class DefaultProjectMapper implements IProjectMapper {

	public IProject get(HierarchicalEclipseProject target) {
		String name = GradleImportOperation.getDefaultEclipseName(target);
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

}
