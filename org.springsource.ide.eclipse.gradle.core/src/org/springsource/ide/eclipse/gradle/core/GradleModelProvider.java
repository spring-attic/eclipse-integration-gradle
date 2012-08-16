/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import java.io.File;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil.Console;


/**
 * An abstraction to shield us from the precise way in which we obtain a gradle model 
 * for a given project. The model may be obtained in different ways depending on ...
 * 
 * TODO: find a way to avoid multiple concurrent requests for the same or related models
 * from rebuilding the models unnecessary. Complication: a request for a model
 * of some project may actually also satisfy future requests for models of related projects.
 * Idea: keep time stamps with models and requests.
 * Return existing model immediately if its time stamp is more recent than the request stamp.
 * (time stamps on model's get set when the model is set).
 * 
 * @author Kris De Volder
 */
public abstract class GradleModelProvider {
	
	private static final boolean DEBUG = false;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	/**
	 * This indicates the type of models currently contained in the model cache. Set to null
	 * if the cache is uninitialised.
	 */
	protected Class<? extends HierarchicalEclipseProject> type;

	/**
	 * Requests that the model provider lazy initialises its cache with models that satisfy the requested level of detail.
	 */
	public abstract <T extends HierarchicalEclipseProject> void ensureModels(Class<T> type, IProgressMonitor mon) throws CoreException, OperationCanceledException;
	
	/**
	 * Retrieves a model from the cache. Never returns null. 
	 * @throws InconsistenProjectHierarchyException if the cache was initialised but has no model for this project.
	 * @throws FastOperationFailedException if the cache has not been initialised to requested level of detail.
	 */
	public abstract <T extends HierarchicalEclipseProject> T getCachedModel(GradleProject project, Class<T> type) 
			throws FastOperationFailedException, InconsistenProjectHierarchyException;
	
	/**
	 * Clears the cache of this model provider
	 */
	public abstract void invalidate();
	
	private static URI getDistributionPref() {
		return GradleCore.getInstance().getPreferences().getDistribution();
	}
	
	private static ProjectConnection getGradleConnector(File projectLoc, URI distributionPref, IProgressMonitor monitor) {
		monitor.beginTask("Connection to Gradle", 1);
		try {
			GradleConnector connector = GradleConnector.newConnector();
			// Configure the connector and create the connection
			if (distributionPref!=null) {
				boolean distroSet = false;
				if ("file".equals(distributionPref.getScheme())) {
					File maybeFolder = new File(distributionPref);
					if (maybeFolder.isDirectory()) {
						connector.useInstallation(maybeFolder);
						distroSet = true;
					}
				}
				if (!distroSet) {
					connector.useDistribution(distributionPref);
				}
			}
			monitor.subTask("Creating connector"); 
			connector.forProjectDirectory(projectLoc);
			return connector.connect();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Tries to connect to gradle, using the distrubution set by the preferences page. If this fails and the prefs page wasn't
	 * actually set, then we try to fall back on the distribution zip that's packaged up into the core plugin.
	 */
	public static ProjectConnection getGradleConnector(GradleProject project, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Connecting to Gradle", 1);
		File projectLoc = project.getLocation();
		try {
			ProjectConnection connection;
			URI distribution = getDistributionPref();
			try {
				connection = getGradleConnector(projectLoc, distribution, new SubProgressMonitor(monitor, 1));
				return connection;
			} catch (Exception e) {
//				if (distribution==null) {
//					//Try find built-in distribution instead.
//					distribution = FallBackDistributionCore.getFallBackDistribution(projectLoc, e);
//					if (distribution!=null) {
//						connection = getGradleConnector(projectLoc, distribution, new SubProgressMonitor(monitor, 1));
//						return connection;
//					}
//				}
				throw e;
			}
		} catch (Exception e) {
			throw ExceptionUtil.coreException(e);
		} finally {
			monitor.done();
		}
	}

	/////////////////// Provider strategy building blocks /////////////////////////////////////////////////////////////////
	
//	/**
//	 * Fully fleshed out models are expensive to construct so we use this model provider to prefetch them as much as possible.
//	 */
//	private static class PrefetchingModelProvider<T extends HierarchicalEclipseProject> extends GradleModelProvider<T> {
//		
//		public PrefetchingModelProvider(Class<T> type) {
//			super(type);
//		}
//
//		@Override
//		public void requestModel(GradleProject project, final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
//			File projectLoc = project.getLocation();
//			final int totalWork = 10000;
//			monitor.beginTask("Creating Gradle model for "+projectLoc, totalWork+100);
//			ProjectConnection connection = null;
//			Console console = null;
//			try {
//				connection = getGradleConnector(project, new SubProgressMonitor(monitor, 100));
//
//				// Load the Eclipse model for the project
//				monitor.subTask("Loading model");
//				
//				ModelBuilder<T> builder = connection.model(type);
//				console = ConsoleUtil.getConsole("Building Gradle Model '"+projectLoc+"'");
//				builder.setStandardOutput(console.out);
//				builder.setStandardError(console.err);
//				builder.addProgressListener(new ProgressListener() {
//					
//					int remainingWork = totalWork;
//					
//					public void statusChanged(ProgressEvent evt) {
//						debug("progress = '"+evt.getDescription()+"'");
//						monitor.subTask(evt.getDescription());
//						int worked = remainingWork / 100;
//						if (worked>0) {
//							monitor.worked(worked);
//							remainingWork -= worked;
//						}
//					}
//
//				});
//				T model = builder.get();  // blocks until the model is available
//				project.setGradleModel(model, true);
//				walk(model, new HashSet<HierarchicalEclipseProject>());
//			} catch (GradleConnectionException e) {
//				if (project.getDistribution()==null) {
//					//Try to recover by setting a recent distribution overriding the default
//					URI distro = FallBackDistributionCore.getFallBackDistribution(projectLoc, e);
//					if (distro!=null) {
//						GradleCore.log(e);
//						project.setDistribution(distro);
//						requestModel(project, monitor);
//						return;
//					}
//				}
//				//We didn't return so recovery failed
//				throw ExceptionUtil.coreException(e);
//			} catch (Exception e) {
//				throw ExceptionUtil.coreException(e);
//			} finally {
//				monitor.done();
//				if (connection!=null) {
//					connection.close();
//				}
//				if (console!=null) {
//					console.close();
//				}
//			}
//		}
//
//		private void walk(HierarchicalEclipseProject model, Set<HierarchicalEclipseProject> hashSet) {
//			if (model!=null && !hashSet.contains(model)) {
//				hashSet.add(model);
//				GradleProject modelHolder = GradleCore.create(model);
//				modelHolder.setGradleModel(model, false);
//				
//				walk(model.getParent(), hashSet);
//				for (HierarchicalEclipseProject it : model.getChildren()) {
//					walk(it, hashSet);
//				}
//			}
//		}
//	}

	/**
	 * Model provider that provides models for a group of related projects by building the model for a 
	 * single representative project. This representative project is typically the root of the hierarchy
	 * but on rare occasions we may be forced to build the model via another project, if the root
	 * cannot be determined.
	 */
	public static class GroupedModelProvider extends GradleModelProvider {
		
		private GradleProject rootProject;
		private Map<GradleProject, HierarchicalEclipseProject> models = null;
		
		/** If a model build fails, we store the error as an explanation. */
		private CoreException error;
		
		/** To remember that we already failed to build a model of some specific type and shouldn't try again */
		private Class<? extends HierarchicalEclipseProject> failedType;
		
		public GroupedModelProvider(GradleProject project) {
			this.rootProject = project;
		}

		@Override
		public synchronized void invalidate() {
			models = null;
			type = null;
			failedType = null; //Set if a model build for this type failed
			error = null; // Set at the same time as failedType
		}

		private boolean satisfies(Class<? extends HierarchicalEclipseProject> requiredType) {
			return type!=null && requiredType.isAssignableFrom(type);
		}
		
		private boolean alreadyFailed(Class<? extends HierarchicalEclipseProject> requiredType) {
			return failedType!=null && failedType == requiredType;
		}

		@Override
		public <T extends HierarchicalEclipseProject> void ensureModels(Class<T> requiredType, IProgressMonitor mon) throws CoreException, OperationCanceledException {
			synchronized (this) {
				if (satisfies(requiredType)) {
					return;
				}
			}
			if (alreadyFailed(requiredType)) {
				Assert.isNotNull(error);
				throw new CoreException(ExceptionUtil.status(error));
			}
			//Building the model done outside synchronized block because it is a long operation. Just be careful to 
			//precompute what we need in local variables so concurrent threads can safely keep using the old provider state.
			T model = null;
			try {
				model = buildModel(rootProject, requiredType, mon);
				Map<GradleProject, HierarchicalEclipseProject> cache = new IdentityHashMap<GradleProject, HierarchicalEclipseProject>();
				walk(model, cache);
				synchronized (this) {
					this.models = cache; 
					this.type = requiredType;
				}
				//Now this provider is consistent and safe to use... make sure the relevant projects will be using it!
				for (GradleProject p : cache.keySet()) {
					p.setModelProvider(this);
				}
				for (Entry<GradleProject, HierarchicalEclipseProject> e : cache.entrySet()) {
					e.getKey().notifyModelListeners(e.getValue());
				}
			} catch (CoreException e) {
				this.failedType = requiredType;
				this.error = e;
			}
		}
		
		/** Walk the hierarchy and fill a given cache map */
		private void walk(HierarchicalEclipseProject model, Map<GradleProject, HierarchicalEclipseProject> cache) {
			if (model!=null) {
				if (model.getParent()==null) { // Make sure root is up-to-date
					rootProject = GradleCore.create(model);
				}
				GradleProject project = GradleCore.create(model);
				if (!cache.containsKey(project)) {
					cache.put(project, model);
					walk(model.getParent(), cache);
					for (HierarchicalEclipseProject it : model.getChildren()) {
						walk(it, cache);
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public synchronized <T extends HierarchicalEclipseProject> T getCachedModel(GradleProject project, Class<T> requiredType) 
				throws FastOperationFailedException, InconsistenProjectHierarchyException {
			if (satisfies(requiredType)) {
				T model = (T) models.get(project);
				if (model==null) {
					throw ExceptionUtil.inconsistentProjectHierachy(project);
				} else {
					return model;
				}
			}
			throw new FastOperationFailedException();
		}

		public GradleProject getRootProject() {
			return rootProject;
		}

	}
	
	public static GroupedModelProvider create(GradleProject rootProject) {
		return new GroupedModelProvider(rootProject);
	}

	
	public static <T extends HierarchicalEclipseProject> T buildModel(GradleProject rootProject, Class<T> requiredType, final IProgressMonitor monitor) throws CoreException {
		File projectLoc = rootProject.getLocation();
		final int totalWork = 10000;
		monitor.beginTask("Creating Gradle model for "+projectLoc, totalWork+100);
		ProjectConnection connection = null;
		Console console = null;
		try {
			connection = getGradleConnector(rootProject, new SubProgressMonitor(monitor, 100));

			// Load the Eclipse model for the project
			monitor.subTask("Loading model");
			
			ModelBuilder<T> builder = connection.model(requiredType);
			rootProject.configureOperation(builder, null);
			console = ConsoleUtil.getConsole("Building Gradle Model '"+projectLoc+"'");
			builder.setStandardOutput(console.out);
			builder.setStandardError(console.err);
			builder.addProgressListener(new ProgressListener() {
				
				int remainingWork = totalWork;
				
				public void statusChanged(ProgressEvent evt) {
					debug("progress = '"+evt.getDescription()+"'");
					monitor.subTask(evt.getDescription());
					int worked = remainingWork / 100;
					if (worked>0) {
						monitor.worked(worked);
						remainingWork -= worked;
					}
				}

			});
			T model = builder.get();  // blocks until the model is available
			return model;
		} catch (Exception e) {
			throw ExceptionUtil.coreException(e);
		} finally {
			monitor.done();
			if (connection!=null) {
				connection.close();
			}
			if (console!=null) {
				console.close();
			}
		}
	}

	
}
