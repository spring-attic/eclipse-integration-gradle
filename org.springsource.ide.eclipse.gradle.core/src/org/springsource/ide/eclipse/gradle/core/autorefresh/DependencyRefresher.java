/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.autorefresh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.actions.IProjectProvider;

/**
 * An instance of this class is responsible for tracking a set of 'dirty' Gradle projects
 * and refreshing their dependencies after some delay. The delay serves as a kind of buffer
 * to avoid bursts of large number of small change events to potentially trigger multiple
 * executions of the refresh job.
 * 
 * @author Kris De Volder
 */
public class DependencyRefresher implements IDirtyProjectListener {
	
	private static DependencyRefresher instance = null;

	/**
	 * Unless you really want to enable the DependencyRefresher it is advisable not 
	 * to call this method to avoid instantiating it and creating a permanent
	 * workspace listener to support it.
	 */
	public static synchronized DependencyRefresher getInstance() {
		if (instance==null) {
			instance = new DependencyRefresher();
			new GradleWorkspaceListener(instance);
		}
		return instance;
	}

	/**
	 * Like getInstance, but it will not force the creation of an instance if one doesn't
	 * already exist.
	 */
	public static synchronized DependencyRefresher getInstanceGently() {
		return instance;
	}
	
	private long delay = GradleCore.getInstance().getPreferences().getAutoRefreshDelay();
	private boolean isEnabled = false;
	
	private Set<GradleProject> dirtyProjects = new HashSet<GradleProject>();
	private long lastDirtied = System.currentTimeMillis();
	
	/**
	 * Returns a copy of the current list of dirty projects and empties the 
	 * dirty projects. This is 
	 */
	private synchronized List<IProject> getAndClearDirties() {
		List<IProject> result = new ArrayList<IProject>();
		for (GradleProject gp : dirtyProjects) {
			IProject p = gp.getProject();
			if (p!=null) {
				result.add(p);
			}
		}
		dirtyProjects.clear();
		return result;
	}
	
	Job checkDirty = new Job("Check for dirty Gradle projects") {
		
		@Override
		protected IStatus run(IProgressMonitor mon) {
			mon.beginTask("Check for dirty Gradle projects", 1);
			long now = System.currentTimeMillis();
			try {
				if (now - lastDirtied < delay) {
					//too soon try again later
					//this can happen if change events keep triggering more
					//projects getting dirtied after the first one.
					this.schedule(delay);
				} else {
					//Beware RefreshDependenciesActionCore also does its work in a job.
					//We want to make sure it fetches the list of projects only when it actually runs
					//This is to avoid that some additional dirty projects got added since then.
					//That would cause additional refreshes being triggered.
					RefreshDependenciesActionCore.callOn(new IProjectProvider() {
						@Override
						public List<IProject> get() {
							return getAndClearDirties();
						}
					});
				}
			} finally {
				mon.done();
			}
			return ExceptionUtil.OK_STATUS;
		}
	};

	
	/**
	 * Called when a workspace change dirties a project.
	 */
	public synchronized void addDirty(GradleProject gp) {
		if (isEnabled) {
			this.dirtyProjects.add(gp);
			checkDirty.schedule(delay);
		}
	}
	
	/**
	 * May be called when a manual refresh 'cleans' a project. This can potentially 
	 * avoid unnecessary automatic refreshes later.
	 */
	public synchronized void removeDirty(GradleProject gp) {
		if (isEnabled) {
			this.dirtyProjects.remove(gp);
		}
	}
	
	
	public void enable(boolean enable) {
		this.isEnabled = enable;
	}

	public static void init() {
		if (GradleCore.getInstance().getPreferences().isAutoRefreshDependencies()) {
			//Don't request the instance unless it is actually enabled. This avoids
			//creating all that workspace listener infrastructure.
			getInstance().enable(true);
		}
	}

	/**
	 * Refresh the instance when prefs have been changed.
	 */
	public static void refresh() {
		boolean enable = GradleCore.getInstance().getPreferences().isAutoRefreshDependencies();
		if (enable) {
			getInstance().enable(enable);
			getInstance().setDelay(GradleCore.getInstance().getPreferences().getAutoRefreshDelay());
		} else {
			if (getInstanceGently()!=null) {
				//Avoid creating an instance if not necessary
				//If there's no instance then its not enabled for sure and nothing else matters
				getInstance().enable(enable);
			}
		}
	}

	public void setDelay(int autoRefreshDelay) {
		this.delay = autoRefreshDelay;
	}

}
