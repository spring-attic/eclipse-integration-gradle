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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
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
	private LockManager lockManager = new LockManager();
	
	public GradleModelManager(ModelBuilder builder) {
		this.builder = builder;
	}
	
	/**
	 * Create default model manager configuration as used in the 'production' version of the
	 * tools. 
	 */
	public GradleModelManager() {
		this(
				new DefaultModelBuilder()
		);
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
	synchronized <T> void addToCache(List<ProjectBuildResult<T>> buildResults) {
		for (ProjectBuildResult<?> buildResult : buildResults) {
			getManager(buildResult.getProject()).addToCache(buildResult.getResult());
		}
	}

	public Lock lockFamily(Collection<GradleProject> predictedFamily) {
		Set<String> keys = new HashSet<String>();
		for (GradleProject project : predictedFamily) {
			keys.add(project.getLocation().toString());
		}
		return lockManager.lock(keys);
	}

	public Lock lockAll() {
		return lockManager.lockAll();
	}

}
