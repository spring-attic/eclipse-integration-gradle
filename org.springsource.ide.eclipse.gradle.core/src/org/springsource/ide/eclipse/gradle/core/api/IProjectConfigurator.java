/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.api;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Configures a project. Hook subclasses to
 * <code>org.springsource.ide.eclipse.gradle.core.projectConfigurators</code> extension point
 * 
 * @author Alex Boyko
 * 
 */
public interface IProjectConfigurator {
	
	/**
	 * Configures a project
	 * 
	 * @param request request for configuring a project 
	 * @param monitor progress monitor
	 * @throws Exception
	 */
	void configure(IProjectConfigurationRequest request, IProgressMonitor monitor) throws Exception;

}
