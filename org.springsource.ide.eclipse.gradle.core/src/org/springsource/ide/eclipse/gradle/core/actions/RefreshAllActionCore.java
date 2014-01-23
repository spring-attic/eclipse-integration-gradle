/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.Continuation;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
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
			List<GradleProject> gradleProjects = new ArrayList<GradleProject>(projects.size());
			for (IProject p : projects) {
				gradleProjects.add(GradleCore.create(p));
			}
			return refreshProjects(gradleProjects);
		}
		return new NullJoinable<Void>();
	}

	/**
	 * Refresh a list of projects. Executes re-import operation for batches of interdependent projects.
	 */
	private static Joinable<Void> refreshProjects(final List<GradleProject> projects) throws CoreException {
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
								Map<GradleProject, List<GradleProject>> batches = new HashMap<GradleProject, List<GradleProject>>();
								for (GradleProject project : projects) {
									GradleProject root = project.getRootProject();
									List<GradleProject> batch = batches.get(root);
									if (batch == null) {
										batch = new ArrayList<GradleProject>();
										batches.put(root, batch);
									}
									batch.add(project);
								}
								for (List<GradleProject> batch : batches.values()) {
									new ReimportOperation(batch).perform(eh, new SubProgressMonitor(monitor, batch.size()));
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
