/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.CancellationTokenInternal;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.SystemPropertyCleaner;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil.Console;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleOpearionProgressMonitor;

public class ToolinApiUtils {
	
	private static final boolean DEBUG = false;

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
	
	public static <T> T buildModel(GradleProject project, Class<T> requiredType, final IProgressMonitor monitor) throws CoreException {
		final String jobName = jobName(project, requiredType);
		SystemPropertyCleaner.clean();
		File projectLoc = project.getLocation();
		final int totalWork = 10000;
		monitor.beginTask(jobName, totalWork+100);
		ProjectConnection connection = null;
		final Console console = ConsoleUtil.getConsole("Building "+requiredType.getSimpleName()+" Gradle Model '"+projectLoc+"'");
		try {
			connection = getGradleConnector(project, new SubProgressMonitor(monitor, 100));

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
	
	private static URI getDistributionPref() {
		return GradleCore.getInstance().getPreferences().getDistribution();
	}
	
	private static ProjectConnection getGradleConnector(File projectLoc, URI distributionPref, File gradleUserHomePref, IProgressMonitor monitor) {
		monitor.beginTask("Connection to Gradle", 1);
		try {
			GradleConnector connector = GradleConnector.newConnector();
			if (gradleUserHomePref!=null) {
				connector.useGradleUserHomeDir(gradleUserHomePref);
			}
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
			File gradleUserHome = getGradleUserHomePref();
			try {
				connection = getGradleConnector(projectLoc, distribution, gradleUserHome, new SubProgressMonitor(monitor, 1));
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
	
	private static File getGradleUserHomePref() {
		return GradleCore.getInstance().getPreferences().getGradleUserHome();
	}
	
	public static <T> String jobName(GradleProject project, Class<T> requiredType) {
		return "Build '"+requiredType.getSimpleName()+"' model for '"+project.getDisplayName();
	}
	
}
