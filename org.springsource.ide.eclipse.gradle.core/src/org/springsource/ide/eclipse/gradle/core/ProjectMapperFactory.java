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
 * @author Kris De Volder
 */
public class ProjectMapperFactory {

	public static IProjectMapper compose(final IProjectMapper first, final IProjectMapper fallback) {
		return new IProjectMapper() {
			public IProject get(HierarchicalEclipseProject target) {
				IProject result = first.get(target);
				if (result!=null) {
					return result;
				}
				return fallback.get(target);
			}
			
			@Override
			public String toString() {
				return first + "=>" + fallback;
			}
		};
	}

	/**
	 * Create a project mapper suitable for mapping projects existing in the workspace and their dependencies.
	 * <p>
	 * This mapping never returns null, but falls back on the default project mapper if no project exists in the
	 * workspace.
	 */
	public static IProjectMapper workspaceMapper() {
		return compose(
					new ExistingWorkspaceProjectMapper(),
					new DefaultProjectMapper()
				);
	}
	
}
