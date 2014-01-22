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
package org.springsource.ide.eclipse.gradle.core;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.gradle.api.internal.CompositeDomainObjectSet;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil;
import org.springsource.ide.eclipse.gradle.core.util.ConsoleUtil.Console;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.TimeUtils;


/**
 * Utility methods for executing Gradle tasks.
 * 
 * @author Kris De Volder
 */
public class TaskUtil {
	
	public static void execute(GradleProject project,  ILaunchConfiguration conf, Collection<String> taskList, IProgressMonitor mon) throws CoreException {
		Console console = ConsoleUtil.getConsole("Executing tasks on "+project.getDisplayName());
		execute(project, conf, taskList, mon, new PrintStream(console.out), new PrintStream(console.err));
	}

	public static void execute(GradleProject project, ILaunchConfiguration conf, Collection<String> taskList, IProgressMonitor mon, PrintStream out, PrintStream err) throws CoreException {
		mon.beginTask("Executing tasks", 90);
		try {
			BuildLauncher build;
			Job.getJobManager().beginRule(JobUtil.LIGHT_RULE, new SubProgressMonitor(mon, 5));
			//cumulative work: 5%
			try {
				project = project.getRootProject(); //Workaround for bug http://issues.gradle.org/browse/GRADLE-1765
				// is ok to go via root, since task path strings are 'absolute' anyway.
				ProjectConnection conn = GradleModelProvider.getGradleConnector(project, new SubProgressMonitor(mon, 5));
				//cumulative work: 10%

				build = conn.newBuild();
				project.configureOperation(build, conf);

				build.setStandardError(err);
				build.setStandardOutput(out);
				mon.worked(2);
				//cumulative work: 12%

				build.forTasks(taskList.toArray(new String[taskList.size()]));
				out.println("[sts] -----------------------------------------------------");
				out.println("[sts] Starting Gradle build for the following tasks: ");
				for (String task : taskList) {
					out.println("[sts]      "+task);
				}
				//			String javaHome = getJavaHome();
				//			if (javaHome!=null) {
				//				out.println("[sts] JAVA_HOME is set to '"+javaHome+"'");
				//			} else {
				//				out.println("[sts] JAVA_HOME is NOT SET");
				//			}
				out.println("[sts] -----------------------------------------------------");
				mon.worked(2);
				//cumulative work: 14%
			} finally {
				Job.getJobManager().endRule(JobUtil.LIGHT_RULE);
			}
			//No more scheduling rule in effect here.
			long startTime = System.currentTimeMillis();
			try {
				build.run(); 
				mon.worked(76);
				out.println("[sts] -----------------------------------------------------");
				out.println("[sts] Build finished succesfully!");
			} catch (Exception e) { 
				//only exceptions raised by the Gradle build itself are caught here.
				out.println("[sts] Build failed");
				e.printStackTrace(out);
				throw ExceptionUtil.coreException(e);
			} finally {
				String duration = TimeUtils.minutusAndSecondsFromMillis(System.currentTimeMillis()-startTime);
				out.println("[sts] Time taken: "+duration);
				out.println("[sts] -----------------------------------------------------");
				//cumulative work: 90%
			}
		} catch (FastOperationFailedException e) {
			throw ExceptionUtil.coreException(e);
		} finally {
			mon.done();
		}
	}

	private static String getJavaHome() {
		return System.getenv().get("JAVA_HOME");
	}

	/**
	 * Run a bunch of tasks 'in bulk'. It is possible that no tasks will be executed, if there are no
	 * tasks matching the provided list of names in the specified project list.
	 * 
	 * @return true if some tasks where actually found and executed.
	 */
	public static boolean bulkRunEclipseTasksOn(List<HierarchicalEclipseProject> sortedProjects, String[] taskNamesToRun, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		monitor.beginTask("Run eclipse tasks", sortedProjects.size()*3);
		try {
			if (sortedProjects.size()>0) {
				GradleProject rootProject = null; //Will be set on first occasion to determine
				CompositeDomainObjectSet<String> tasksToRun = new CompositeDomainObjectSet<String>(String.class);
				List<DefaultDomainObjectSet<String>> subCollections = new ArrayList<DefaultDomainObjectSet<String>>(sortedProjects.size());
				for (String taskName : taskNamesToRun) {
					DefaultDomainObjectSet<String> subCollection = new DefaultDomainObjectSet<String>(String.class);
					subCollections.add(subCollection);
					tasksToRun.addCollection(subCollection);
				}
	
				//Collection the tasks to run: Ticks: 2*sorted.size
				for (HierarchicalEclipseProject _project : sortedProjects) {
					GradleProject project = GradleCore.create(_project);
					if (rootProject==null) {
						try {
							rootProject = project.getRootProject();
						} catch (FastOperationFailedException e) {
							throw new IllegalStateException(e);
						}
					}
					for (Task task : project.getTasks(new SubProgressMonitor(monitor, 2))) {
						for (int i = 0; i < taskNamesToRun.length; i++) {
							String path = task.getPath();
							if (task.getName().equals(taskNamesToRun[i]) || path.equals(taskNamesToRun[i])) {
								subCollections.get(i).add(path);
							}
						}
					}
				}
				
				//Running the tasks: ticks: sorted.size
				if (!tasksToRun.isEmpty()) {
					execute(rootProject, null, tasksToRun, new SubProgressMonitor(monitor, sortedProjects.size()) );
					return true;
				} else {
					monitor.worked(sortedProjects.size());
				}
			}
		} finally {
			monitor.done();
		}
		return false;
	}
	
	
}
