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

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * An instance of BuildStrategy provides a way to build one or more Gradle models in response
 * to a request to obtain a model for a given 'focus' project and model type.
 * <p>
 * The simplest strategy will only build one model as per the request. More sophisticated 
 * strategies may produce additional models to be placed into the model manager's cache.
 * <p>
 * BuildStrategy is only responsible for building models caching the build results is handled
 * elsewhere.
 *  
 * @author Kris De Volder
 */
public abstract class BuildStrategy {

	protected final ModelBuilder builder;

	public BuildStrategy(ModelBuilder builder) {
		this.builder = builder;
	}
	
	public interface BuildRequestor {
		void addResult(GradleProject project, BuildResult<?> result);
	}
	
	/**
	 * Request to build a model for a given focusProject and type. It is the responsibility of the
	 * be incorrect, the implementor should return a List of at least one element with the BuildResult for the 
	 * focus project in element at position 0.
	 * <p>
	 * If the family prediction was inaccurate, then it is possible the build result does not contain a model
	 * for the focus project. 
	 * <p>
	 * Additional results may be provided as elements at position 1 and up.
	 */
	public abstract <T> List<ProjectBuildResult<T>> buildModels(GradleProject focusProject, Class<T> type, IProgressMonitor mon) throws CoreException;
	
	/**
	 * BuildStrategy should make a 'best effort' attempt to predict what projects it will build models for
	 * for a given request. This information is used by the model manager to efficiently schedule build
	 * requests to the model builder so that concurrent requests for related projects in a build family
	 * do not result in multiple concurrent build requests to the model builder.
	 * <p>
	 * A predictor may signal that it cannot make a reasonable prediction (typically this is because
	 * not enough cached information from prior build is available) by returning null. Correctness of
	 * the model manager does not rely on the absolute correctness of the predictor. Incorrect predictions
	 * 'merely' lead to inefeccient build scheduling. It is therefor ok to return a prediction that is not
	 * 100% guaranteed to be correct. All that is really expected/required as a high probability that 
	 * the prediction is accurate. 
	 */
	public abstract <T> Set<GradleProject> predictBuildFamily(GradleProject focusProject, Class<T> type);
		
}
