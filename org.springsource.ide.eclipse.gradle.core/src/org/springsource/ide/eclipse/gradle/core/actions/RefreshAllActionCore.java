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
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.util.Continuation;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;
import org.springsource.ide.eclipse.gradle.core.util.NullJoinable;


/**
 * The 'core' counterpart for RefreshAllAction.
 * 
 * @author Kris De Volder
 */
public class RefreshAllActionCore {

	/**
	 * Exposes what this UI action does through an easy to call static method.
	 * @return The job that was scheduled, or null if no job was scheduled.
	 */
	public static Joinable<Void> callOn(final List<IProject> projects) throws CoreException {
		if (!projects.isEmpty()) {
			List<GradleProject> managed = new ArrayList<GradleProject>();
			List<GradleProject> unmanaged = new ArrayList<GradleProject>();
			for (IProject p : projects) {
				GradleProject gp = GradleCore.create(p);
				if (GradleClassPathContainer.isOnClassPath(gp.getJavaProject())) {
					managed.add(gp);
				} else {
					unmanaged.add(gp);
				}
			}
			
			if (managed.isEmpty()) {
				return refreshUnmanaged(unmanaged);
			} else if (unmanaged.isEmpty()) {
				return refreshManagedProjects(managed);
			} else {
				throw ExceptionUtil.coreException("The selection contains some projects with dependency management enabled and some with it disabled." +
						"All selected projects should either be managed or unmanaged.");
			}
		}
		return new NullJoinable<Void>();
	}

	/**
	 * Refresh a list of projects that do *not* have dependency management enabled.
	 */
	private static Joinable<Void> refreshUnmanaged(final List<GradleProject> projects) throws CoreException {
		final String jobName = getName(projects);
		final ErrorHandler eh = ErrorHandler.forRefreshAll();
		if (!projects.isEmpty()) {
			JoinableContinuation<Void> cont = new JoinableContinuation<Void>();
			JobUtil.schedule(JobUtil.LIGHT_RULE, new Continuable("Refresh Gradle models for "+jobName, projects.size()*2, cont) {
				@Override
				public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception {
					monitor.beginTask(jobName, 1+projects.size()*2);
					try {
						for (GradleProject p : projects) {
							p.invalidateGradleModel();
							monitor.worked(1);
						}
						for (GradleProject p : projects) {
							p.getGradleModel(new SubProgressMonitor(monitor, 1));
						}
						JobUtil.schedule(new Continuable("Reimporting "+jobName, projects.size(), cont) {
							@Override
							public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception {
								for (GradleProject p : projects) {
									new ReimportOperation(p).perform(eh, new SubProgressMonitor(monitor, 1));
								}
								eh.rethrowAsCore();
								cont.apply(null);
							}
						});
					} finally {
						monitor.done();
					}
				}

			});
			return cont;
		}
		return new NullJoinable<Void>();
	}
	
	/**
	 * Refresh a list of projects that *do* have dependency management enabled.
	 */
	private static Joinable<Void> refreshManagedProjects(final List<GradleProject> managed) {
		JoinableContinuation<Void> cont = new JoinableContinuation<Void>();
		final String jobName = "Refresh Gradle model for "+getName(managed);
		final ErrorHandler eh = ErrorHandler.forRefreshAll();
		JobUtil.schedule(JobUtil.LIGHT_RULE, new Continuable(jobName, managed.size()*2, cont) {
			@Override
			public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception {
				monitor.subTask("Invalidating old gradle models");
				for (GradleProject p : managed) {
					p.invalidateGradleModel();
				}
				monitor.worked(1);
				for (GradleProject p : managed) {
					p.getGradleModel(new SubProgressMonitor(monitor, 1));
				}
				JobUtil.schedule(new Continuable("Refresh All for "+getName(managed), managed.size(), cont) {
					@Override
					public void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception {
						for (GradleProject p : managed) {
							new ReimportOperation(p).perform(eh, new SubProgressMonitor(monitor, 1));
						}
						eh.rethrowAsCore();
						cont.apply(null);
					}
				});
			}
		}); 
		return cont;
	}

	private static String getName(List<GradleProject> projects) {
		if (projects.size()>1) {
			return "multiple projects";
		} else if (projects.size()==1){
			return projects.get(0).getName();
		} else {
			return "Nothing"; //Jobs that do nothing shouldn't really be scheduled but just in case they are.
		}
	}
	
}
