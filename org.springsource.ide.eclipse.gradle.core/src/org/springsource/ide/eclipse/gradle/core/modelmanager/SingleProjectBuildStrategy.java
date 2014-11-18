/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Simple model build strategy that just calls the model builder to produce 
 * exactly the model that was asked for and nothing more.
 * 
 * @author Kris De Volder
 */
public class SingleProjectBuildStrategy extends BuildStrategy {

	public SingleProjectBuildStrategy(ModelBuilder builder) {
		super(builder);
	}

	@Override
	public <T> List<ProjectBuildResult<T>> buildModels(
			GradleProject focusProject, Class<T> type, IProgressMonitor mon)
			throws CoreException {
		
		BuildResult<T> result = builder.buildModel(focusProject, type, mon);
		return Collections.singletonList(new ProjectBuildResult<T>(focusProject, result));
	}

	@Override
	public <T> Collection<GradleProject> predictBuildFamily(GradleProject focusProject, Class<T> type) {
		return Collections.singleton(focusProject);
	}

//	@Override
//	public <T> List<BuildResult<T>> buildModels(GradleProject focusProject, Class<T> type, IProgressMonitor mon) throws CoreException {
//		return Arrays.asList(builder.buildModel(focusProject, type, mon));
//	}
	
}
