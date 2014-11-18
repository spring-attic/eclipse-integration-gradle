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

import java.security.spec.MGF1ParameterSpec;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.InconsistenProjectHierarchyException;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;

/**
 * Manages GradleModels for all GradleProjects. Each GradleProject may be
 * associated with different model providers for different types of models.
 * 
 * @author Kris De Volder
 */
public class GradleModelManager {

	private ModelBuilder builder;
	private Map<GradleProject, GradleProjectModelManager> managers;
	private Map<Class<?>, LockManager> lockManagers = null; // lock managers, per model type.
	private Map<GradleProject,ListenerList> listeners;
	
	public GradleModelManager(ModelBuilder builder) {
		this.builder = builder;
	}
	
	public <T> T getModel(GradleProject project, Class<T> type) throws CoreException, FastOperationFailedException {
		return getManager(project).getModel(type);
	}

	/**
	 * Clears out all models in all the caches.
	 */
	public synchronized void invalidate() {
		managers = null;
	}
	
	/**
	 * Clears out models of all types for a given project.
	 */
	public synchronized void invalidate(GradleProject gradleProject) {
		managers.remove(gradleProject);
	}
	
	private synchronized GradleProjectModelManager getManager(GradleProject project) {
		if (managers==null) {
			managers = new HashMap<GradleProject, GradleProjectModelManager>();
		}
		GradleProjectModelManager existing = managers.get(project);
		if (existing==null) {
			managers.put(project, existing = new GradleProjectModelManager(this, project));
		}
		return existing;
	}

	public <T> T getModel(GradleProject project, Class<T> type, IProgressMonitor mon) throws CoreException {
		return getManager(project).getModel(type, mon);
	}
	
	/**
	 * Clients may overide this method to add / change build strategies for different types of
	 * model. The main use case we have in mind here is to allow for something similar to the old
	 * 'GroupedModelProvider' where a single build actually produces models for multiple projects 
	 * at once.
	 */
	public <T> BuildStrategy getBuildStrategy(GradleProject project, Class<T> type) {
		if (HierarchicalEclipseProject.class.isAssignableFrom(type)) {
			return new HierarchicalProjectBuildStrategy(builder);
		}
		return new SingleProjectBuildStrategy(builder);
	}
	
	/**
	 * Add new build results to the model cache, overwriting any buildresults that are
	 * already stored at the same coordinates.
	 */
	<T> void addToCache(List<ProjectBuildResult<T>> buildResults) {
		synchronized (this) {
			for (ProjectBuildResult<?> buildResult : buildResults) {
				if (!buildResult.isCancelation()) {
					getManager(buildResult.getProject()).addToCache(buildResult.getResult());
				}
			}
		}
		//We take care to notify listeners outside synch blocks for less chance of deadlocking
		// There's really no need for the notifications to be sent while holding locks on
		// the cache
		for (ProjectBuildResult<T> buildResult : buildResults) {
			if (!buildResult.isFailure()) {
				notifyListeners(buildResult.getProject(), buildResult.getResult().getType(), buildResult.getResult().getModel());
			}
		}
	}
	
	private synchronized ListenerList listeners(GradleProject project) {
		GradleProject key = project;
		if (listeners==null) {
			//Since keys are class objects can use IdentityHashMap
			listeners = new HashMap<GradleProject, ListenerList>();
		}
		ListenerList llist = listeners.get(key);
		if (llist==null) {
			listeners.put(key, llist = new ListenerList(ListenerList.IDENTITY));
		}
		return llist;
	}
	
	private synchronized ListenerList listenersMaybe(GradleProject project) {
		if (listeners!=null) {
			ListenerList llist = listeners.get(project);
			if (llist!=null) {
				return llist;
			}
		}
		return null;
	}
	
	private <T> void notifyListeners(GradleProject project, Class<T> type, T model) {
		ListenerList llist = listenersMaybe(project);
		if (llist!=null) {
			for (Object l : llist.getListeners()) {
				((IGradleModelListener)l).modelChanged(project, type, model);
			}
		}
	}
	
	public <T> void addListener(GradleProject project, IGradleModelListener listener) {
		listeners(project).add(listener);
	}
	
	public <T> void removeListener(GradleProject project, IGradleModelListener listener) {
		ListenerList llist = listenersMaybe(project);
		if (llist!=null) {
			llist.remove(listener);
		}
	}

	/**
	 * Synchronization helper to make requests for models in the same project family sequential.
	 */
	Lock lockFamily(Class<?> type, Collection<GradleProject> predictedFamily) {
		LockManager lockManager = getLockManager(type);
		Set<String> keys = new HashSet<String>();
		for (GradleProject project : predictedFamily) {
			keys.add(project.getLocation().toString()+"::"+type.getName());
		}
		return lockManager.lock(keys);
	}

	private synchronized LockManager getLockManager(Class<?> type) {
		LockManager manager = null;
		if (lockManagers==null) {
			lockManagers = new HashMap<Class<?>, LockManager>();
		} else {
			manager = lockManagers.get(type);
		}
		if (manager==null) {
			manager = new LockManager();
			lockManagers.put(type, manager);
		}
		return manager;
	}

	Lock lockAll(Class<?> type) {
		return getLockManager(type).lockAll();
	}
	
	
	///////////// test-only related code below ///////////////////////////////////////////////////

	/**
	 * When set to a positive value, this causes thread to sleep for some duration
	 * after {@link InconsistenProjectHierarchyException} is caught, before retrying.
	 * <p>
	 * This is meant for testing purposes to force certain outcomes of race conditions
	 * and make certain bugs reliably reproducible. 
	 * <p>
	 * DO NOT use this in non-test context, there is no good reason to add this delay.
	 */
	public void sleepBetweenRetries(int duration) {
		this.SLEEP_BETWEEN_RETRIES = duration;
	}
	
	protected int SLEEP_BETWEEN_RETRIES = 0;


}
