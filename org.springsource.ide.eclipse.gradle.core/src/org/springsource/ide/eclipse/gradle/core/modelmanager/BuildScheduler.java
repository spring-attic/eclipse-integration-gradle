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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GenericModelProvider;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

/**
 * Build scheduler is responsible for forwarding build requests to the builder in 
 * a efficient way. Mainly, its goal is to allow multiple incoming requests 
 * for the same  model to only result in a single build request being forwarded to the
 * model builder. 
 * 
 * 
 * @author Kris De Volder
 */
public class BuildScheduler {

	private ModelBuilder builder;
	
	/**
	 * Keeps track of builds that are currently in progress. Each in progress build is stored as a
	 * "Promise" of the eventual build result.
	 */
	private Map<String, ModelPromise<?>> builds;

	public BuildScheduler(ModelBuilder builder) {
		this.builder = builder;
	}
	
	public <T> BuildResult<T> buildModel(GradleProject project, Class<T> type, final IProgressMonitor mon) {
		mon.beginTask(GenericModelProvider.jobName(project, type), 10);
		ModelPromise<T> promise;
		boolean isNew = false;
		String key = key(project, type);
		try {
			synchronized (this) {
				//cast is safe because key contains type name:
				promise = (ModelPromise<T>) get(key);
				if (promise==null) {
					promise = new ModelPromise<T>();
					isNew = true;
					put(key, promise);
				}
			}
			mon.worked(1);
			if (isNew) {
				try {
					builder.buildModel(project, type, new SubProgressMonitor(mon, 8)).sendTo(promise);
				} catch (Throwable e) {
					promise.error(e);
				}
			}
			return BuildResult.fromPromise(type, promise);
		} finally {
			if (isNew) {
				synchronized (this) {
					remove(key);
				}
			}
			mon.done();
		}
	}

	private void remove(String key) {
		if (builds!=null) {
			builds.remove(key);
		}
	}

	private void put(String key, ModelPromise<?> value) {
		if (builds==null) {
			builds = new HashMap<String, ModelPromise<?>>();
		}
		builds.put(key, value);
	}
	
	/**
	 * Checks if any in progress builds could satisfy a given request, and if so return the promise 
	 * of this build's value. 
	 */
	private JoinableContinuation<?> get(String key) {
		if (builds!=null) {
			//This unchecked cast is safe because the type name is part of the key. 
			return builds.get(key);
		}
		return null;
	}

	private static String key(GradleProject project, Class<?> type) {
		return project.getLocation()+"::"+type.getName();
	}

	public synchronized void invalidate() {
		if (builds!=null) {
			for (ModelPromise<?> promise : builds.values()) {
				promise.cancel();
			}
			builds = null;
		}
	}
	
	/**
	 * Schedule an asynchronous model build and return a 'promise' of the model.
	 */
	private <T> ModelPromise<T> schedule(final GradleProject project, final Class<T> type) {
		final ModelPromise<T> promise = new ModelPromise<T>();
		GradleRunnable modelRequest = new GradleRunnable("Build model ["+type.getSimpleName()+"] for "+project.getDisplayName()) {
			@Override
			public void doit(IProgressMonitor mon) throws Exception {
				try {
					promise.setMonitor(mon);
					builder.buildModel(project, type, mon).sendTo(promise);
				} catch (Throwable e) {
					promise.error(e);
				}
			}
		};
//		promise.setJob(JobUtil.schedule(JobUtil.NO_RULE, modelRequest));
		return promise;
	}
	
	
}
