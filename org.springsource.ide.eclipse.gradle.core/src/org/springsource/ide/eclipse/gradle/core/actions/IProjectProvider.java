/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.List;

import org.eclipse.core.resources.IProject;

/**
 * Abstraction of something that can provide a list of IProject instances.
 * This is used to decopule the consumer of the list from the provider.
 * 
 * @author Kris De Volder
 */
public abstract class IProjectProvider {

	/**
	 * Retrieve the current list of projects. Note that some implementations of this interface
	 * may immediately clear the source of the project list. It is assumed clients will only
	 * ask for the list once.
	 */
	public abstract List<IProject> get();
	
	/**
	 * Convenience method to convert a List into a provider that simply provides the contents of the list.
	 */
	public static IProjectProvider from(final List<IProject> list) {
		return new IProjectProvider() {
			@Override
			public List<IProject> get() {
				return list;
			}
		};
	}

}
