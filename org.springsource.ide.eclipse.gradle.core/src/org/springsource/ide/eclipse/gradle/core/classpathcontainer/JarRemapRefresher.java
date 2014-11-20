/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.classpathcontainer;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;

/**
 * Manages a Job that refreshes classpath containers for all projects in 
 * workspace, ensuring that models needed for proper Jar remapping are
 * present in the model cache before actually requesting classpath 
 * container updates.
 * <p>
 * This refresh is like a 'second' tier update that gets scheduled by a 
 * first tier update. First tier updates will populate containers with
 * dependencies but only do jar remmaping if the publications models
 * are already present in the cache. Since creating these models
 * can be very slow (if large numbers of them are involved) the first tier
 * refresh does not wait for these models. Instead, first tier refresh
 * will take notice when these models are missing and proceed to populate
 * the classpath. It will then request this second tier refresh to be
 * scheduled.
 * <p>
 * This refresh is also triggered directly from a project open/close
 * listener to perform 'quick' recomputation of classpath with updated 
 * remappings, but without refreshing model caches.
 * 
 * @author Kris De Volder
 */
public class JarRemapRefresher {
	
	public static final boolean DEBUG = (""+Platform.getLocation()).contains("kdvolder");

	private static final void debug(String msg) {
		if (DEBUG) {
			System.out.println("JarRemapRefresher: "+msg);
		}
	}
	

	private static JarRemapRefresher instance;
	
	/**
	 * This class is a singleton. Don't call this, rather use
	 */
	private JarRemapRefresher() {
	}
	
	private Job qrJob = null;
	
	public static void request() {
		instance().handleRequest();
	}
	
	/**
	 * Refresh classpath entries in all containers in the workspace quickly (i.e. without invalidating
	 * the cached gradle models and rebuilding them). This is useful when the entries need to be
	 * recomputed because a project was opened / closed and so jar -> gradle or 
	 */
	private synchronized void handleRequest() {
		debug("handleRequest called");
		Collection<GradleProject> projects = GradleCore.getGradleProjects();
		if (!projects.isEmpty()) {
			if (qrJob==null) {
				qrJob = new GradleRunnable("Remap Gradle Dependencies") {
					@Override
					public void doit(IProgressMonitor mon) throws Exception {
						//Important: must re-fetch current list of projects each time job runs.
						final Collection<GradleProject> projects = GradleCore.getGradleProjects();
						
						debug("Job started");
						mon.beginTask("Remap Gradle Dependencies", 2*projects.size()+1);
						
						mon.subTask("Computing project publications");
						//Force publications models into the model cache.
						// This is done before aquiring workspace lock.
						for (GradleProject p : projects) {
							try {
								p.getPublications(new SubProgressMonitor(mon, 1));
							} catch (Throwable e) {
								GradleCore.log(e);
							}
						}
						
						mon.subTask("Updating classpaths");
						JobUtil.withRule(JobUtil.buildRule(), mon, 1, new GradleRunnable("Refresh Gradle Classpath Containers") {
							public void doit(IProgressMonitor mon) throws Exception {
								for (GradleProject p : projects) {
									GradleClassPathContainer classpath = p.getClassPathcontainer();
									if (classpath!=null) {
										mon.subTask("Refresh "+p.getName());
										classpath.clearPersistedEntries();
										classpath.notifyJDT();
									}
									mon.worked(1);
								}
							}
						});
					}
				}.asJob();
			}
			qrJob.schedule(100); //Slight delay for 'bursty' sets of change events.
		}
	}

	private synchronized static JarRemapRefresher instance() {
		if (instance==null) {
			instance = new JarRemapRefresher();
		}
		return instance;
	}
	
}
