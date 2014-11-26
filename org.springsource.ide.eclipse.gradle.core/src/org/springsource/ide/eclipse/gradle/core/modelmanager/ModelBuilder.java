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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Interface that hides the mechanics of how models are being built via Gradle's tooling API (or whatever way
 * models are being built). To implement a ModelBuilder create a subclass of AbstractModelBuilder
 */
public interface ModelBuilder {
	
	public <T> BuildResult<T> buildModel(GradleProject project, Class<T> type, final IProgressMonitor mon);
	
}