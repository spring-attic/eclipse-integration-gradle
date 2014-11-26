/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.resources.IProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurationRequest;

/**
 * Implementation of a common project configuration request.
 * 
 * @author Alex Boyko
 *
 */
public class ProjectConfigurationRequest implements
		IProjectConfigurationRequest {
	
	private EclipseProject gradleProject;
	private IProject project;
	
	public ProjectConfigurationRequest(EclipseProject gradleProject, IProject project) {
		super();
		this.gradleProject = gradleProject;
		this.project = project;
	}

	public EclipseProject getGradleModel() {
		return gradleProject;
	}

	public IProject getProject() {
		return project;
	}

}
