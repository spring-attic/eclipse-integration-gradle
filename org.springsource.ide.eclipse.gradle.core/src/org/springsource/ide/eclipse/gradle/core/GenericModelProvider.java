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
package org.springsource.ide.eclipse.gradle.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.CancellationTokenInternal;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleModelProvider.GroupedModelProvider;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil.Console;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleOpearionProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

/**
 * An object that is responsible of obtaining a 'model' from Gradle for a given project. 
 * It is generic in the sense that it can be used to build models of different types.
 * <p>
 * Use this provider to build models of types other than {@link HierarchicalEclipseProject} and subtypes.
 * For those model types use the old {@link GradleModelProvider} and {@link GroupedModelProvider} classes which
 * have been optimized to build models more efficiently for groups of related projects.
 */
public class GenericModelProvider<T> {
	
	//TODO: remove this, should be replaced by SingleModelProvider
	
	private Class<T> type;
	private GradleProject project;
	
	private JoinableContinuation<T> model = null;

	public GenericModelProvider(GradleProject project, Class<T> type) {
		this.project = project;
		this.type = type;
	}
	
	public T get(IProgressMonitor mon) throws Exception {
		try {
			return ensureModel(mon).join();
		} finally {
			mon.done();
		}
	}

	private synchronized Joinable<T> ensureModel(IProgressMonitor mon) throws Exception {
		mon.beginTask(jobName(project, type), 10);
		try {
			synchronized (this) {
				if (model!=null) {
					return model;
				} 
				model = new JoinableContinuation<T>();
			}
			//We only get here if we created the 'model promise' so its up to us to build it.
			try {
				model.apply(buildModel(project, type, new SubProgressMonitor(mon, 9)));
			} catch (Throwable e) {
				model.error(e);
			}
			return model;
		} finally {
			mon.done();
		}
	}

	public static <T> T buildModel(GradleProject project, Class<T> requiredType, final IProgressMonitor monitor) throws CoreException {
		final String jobName = jobName(project, requiredType);
		SystemPropertyCleaner.clean();
		File projectLoc = project.getLocation();
		final int totalWork = 10000;
		monitor.beginTask(jobName, totalWork+100);
		ProjectConnection connection = null;
		final Console console = ConsoleUtil.getConsole("Building Gradle Model '"+projectLoc+"'");
		try {
			connection = GradleModelProvider.getGradleConnector(project, new SubProgressMonitor(monitor, 100));

			// Load the Eclipse model for the project
			monitor.subTask("Loading model");
			
			ModelBuilder<T> builder = connection.model(requiredType);
			project.configureOperation(builder, null);
			builder.setStandardOutput(console.out);
			builder.setStandardError(console.err);
			CancellationToken cancellationToken = GradleOpearionProgressMonitor
					.findCancellationToken(monitor);
			if (cancellationToken != null) {
				builder.withCancellationToken(cancellationToken);
				/*
				 * Hack to print something in the console right away to give
				 * user a heads up that cancel is pending
				 */
				if (cancellationToken instanceof CancellationTokenInternal) {
					((CancellationTokenInternal) cancellationToken).getToken()
							.addCallback(new Runnable() {
								@Override
								public void run() {
									try {
										console.out
												.write("Cancellation request posted...\n"
														.getBytes());
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							});
				}
			}
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
		} catch (GradleConnectionException e) {
			throw e;
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

	private static <T> String jobName(GradleProject project,
			Class<T> requiredType) {
		return "Build '"+requiredType.getSimpleName()+"' model for '"+project.getDisplayName();
	}
	
	private static final boolean DEBUG = false;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
	
	
}
