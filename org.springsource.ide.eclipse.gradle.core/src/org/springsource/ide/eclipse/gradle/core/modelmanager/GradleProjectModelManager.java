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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.InconsistenProjectHierarchyException;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * Model cache manager for a single GradleProject
 */
public class GradleProjectModelManager {
	
	private GradleModelManager mgr;
	private GradleProject project;
	
	private Map<Class<?>, BuildResult<?>> cache;
	
	public GradleProjectModelManager(GradleModelManager mgr, GradleProject project) {
		this.mgr = mgr;
		this.project = project;
	}

	public <T> T getModel(Class<T> type) throws FastOperationFailedException, CoreException {
		T model = getModelMaybe(type);
		if (model!=null) {
			return model;
		}
		Throwable error = getFailureExplanation(type);
		if (error!=null) {
			throw ExceptionUtil.coreException(error);
		}
		throw new FastOperationFailedException();
	}

	/**
	 * Gets model from cache if available and returns null otherwise.
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> T getModelMaybe(Class<T> type) {
		if (cache!=null) {
			for (BuildResult<?> buildResult : cache.values()) {
				if (buildResult.isSucceeded()) {
					Class<?> modelType = buildResult.getType();
					if (type.isAssignableFrom(modelType)) {
						return (T)buildResult.getModel();
					}
				}
			}
		}
		//Not found 
		return null;
	}
	
	/**
	 * Tries to find explanation for a failed model build. May return null if 
	 * model build has not failed (either a build result has not yet been
	 * stored or the stored build result is a succesful one).
	 */
	public synchronized Throwable getFailureExplanation(Class<?> type) {
		//In case of failure explanation we only match the requested type exactly
		// because there's a chance that building model of subtype fails if 
		// the one for supertype can succeed.
		if (cache!=null) {
			BuildResult<?> buildResult = cache.get(type);
			if (buildResult!=null && buildResult.isFailed()) {
				return buildResult.getError();
			}
		}
		return null;
	}

	public <T> T getModel(Class<T> type, IProgressMonitor mon) throws CoreException {
		mon.beginTask("Fetch model of type "+type.getSimpleName()+" for project "+project.getDisplayName(), 10);
		try {
			try {
				return getModelInternal(type, new SubProgressMonitor(mon, 9));
			} catch (InconsistenProjectHierarchyException e) {
				//This exception indicates a failure to produce model for the focus project caused
				//by inaccurate build family info.
				//The build strategy is supposed to update its family prediction info when this happens,
				//so grant one retry attempt to allow builder strategy to recover from this misprediction.
				if (mgr.SLEEP_BETWEEN_RETRIES>0) {
					try {
						Thread.sleep(mgr.SLEEP_BETWEEN_RETRIES);
					} catch (InterruptedException e1) {
					}
				}
				return getModelInternal(type, mon);
			}
		} finally {
			mon.done();
		}
	}
	
	private <T> T getModelInternal(Class<T> type, IProgressMonitor mon) throws CoreException {
		BuildStrategy buildStrategy = mgr.getBuildStrategy(project, type);
		Collection<GradleProject> predictedFamily = buildStrategy.predictBuildFamily(project, type);
		Lock lock = predictedFamily==null?mgr.lockAll(type):mgr.lockFamily(type, predictedFamily);
		mon.beginTask("Fetch model of type "+type.getSimpleName()+" for project "+project.getDisplayName(), 10);
		try {
			synchronized (this) {
				//All that goes in here is deciding if we should do a build, this should be fast
				// the actual model build might be long and should be outside synch block!
				T fromCache = getModelMaybe(type);
				mon.worked(1);
				if (fromCache!=null) {
					return fromCache;
				}
				//If there's a failure explanation don't do a build because it will likely
				// just fail again for the same reason (and take a long time).
				Throwable failureExplanation = getFailureExplanation(type);
				if (failureExplanation!=null) {
					throw ExceptionUtil.coreException(failureExplanation);
				}
			}
			//If we get here we need to attempt to build the model. 
			//Take care to keep build outside of any synchronized blocks!
			List<ProjectBuildResult<T>> buildResults = buildStrategy.buildModels(project, type, new SubProgressMonitor(mon, 8));
			mgr.addToCache(buildResults);
			ProjectBuildResult<T> primaryResult = getFirst(buildResults);
			if (primaryResult!=null && primaryResult.getProject().equals(project)) {
				return (T) primaryResult.getResult().get();
			} else {
				throw ExceptionUtil.inconsistentProjectHierachy(project);
			}
		} finally {
			mon.done();
			lock.release();
		}
	}
	
	private static <T> T getFirst(List<T> elements) {
		if (elements!=null && !elements.isEmpty()) {
			return elements.get(0);
		}
		return null;
	}

	public synchronized void addToCache(BuildResult<?> result) {
		if (cache==null) {
			cache = new HashMap<Class<?>, BuildResult<?>>();
		}
		cache.put(result.getType(), result);
	}
	
}
