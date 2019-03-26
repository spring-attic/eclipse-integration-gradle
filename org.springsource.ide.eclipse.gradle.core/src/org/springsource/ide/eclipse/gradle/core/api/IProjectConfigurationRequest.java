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

import org.eclipse.core.resources.IProject;
import org.gradle.tooling.model.eclipse.EclipseProject;

/**
 * Request for configuring a project. Allows client s to access Gradle and Eclipse model of the project 
 * 
 * @author Alex Boyko
 *
 */
public interface IProjectConfigurationRequest {
	
	EclipseProject getGradleModel();
	
	IProject getProject();

}
