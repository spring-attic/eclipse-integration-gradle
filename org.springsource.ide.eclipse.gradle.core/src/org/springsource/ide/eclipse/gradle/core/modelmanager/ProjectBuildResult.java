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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * GradleProject paired with a BuildResult.
 */
public class ProjectBuildResult<T> {
	
	final private BuildResult<T> result;
	final private GradleProject project;

	public ProjectBuildResult(GradleProject project, BuildResult<T> result) {
		this.project = project;
		this.result = result;
	}

	@Override
	public String toString() {
		return "ProjectBuildResult [result=" + result + ", project=" + project
				+ "]";
	}

	public BuildResult<T> getResult() {
		return result;
	}

	public GradleProject getProject() {
		return project;
	}

	public boolean isCancelation() {
		return result.isFailed() && ExceptionUtil.isCancelation(result.getError());
	}

	public boolean isFailure() {
		return result.isFailed();
	}
	
	

}
