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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Default implementation of ModelBuilder. Delegates to Gradle tooling API.
 * 
 * @author Kris De Volder
 */
public class DefaultModelBuilder extends AbstractModelBuilder {
	
	@Override
	public <T> T doBuild(GradleProject project, Class<T> type, IProgressMonitor mon) throws CoreException {
		return ToolinApiUtils.buildModel(project, type, mon);
	}
	
	public static <T> String jobName(GradleProject project, Class<T> requiredType) {
		return "Build '"+requiredType.getSimpleName()+"' model for '"+project.getDisplayName();
	}

}
