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

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import org.eclipse.core.resources.IProject;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurationRequest;

/**
 * Implementation of a common project configuration request.
 * 
 * @author Alex Boyko
 *
 */
public class ProjectConfigurationRequest implements
		IProjectConfigurationRequest {
	
	private StsEclipseProject gradleProject;
	private IProject project;
	
	public ProjectConfigurationRequest(StsEclipseProject gradleProject, IProject project) {
		super();
		this.gradleProject = gradleProject;
		this.project = project;
	}

	public StsEclipseProject getGradleModel() {
		return gradleProject;
	}

	public IProject getProject() {
		return project;
	}

}
