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
package org.springsource.ide.eclipse.gradle.core.launch;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ProjectConnection;
import org.springsource.ide.eclipse.gradle.core.GradleModelProvider;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.TaskUtil;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.TimeUtils;


import org.eclipse.debug.ui.RefreshTab;

/**
 * Wrapper class that adapts something that executes gradle tasks to an {@link IProcess} that can
 * be manipulated through the Eclipse debug UI.
 * 
 * @author Kris De Volder
 */
public class GradleProcess extends PlatformObject implements IProcess {
	
	@Override
	public String toString() {
		return "GradleProcess("+GradleLaunchConfigurationDelegate.getProject(conf)+")";
	}
	
	boolean isRunning = true;
	private ILaunchConfiguration conf;
	private ILaunch launch;
	private Map<String,String> attribs = new HashMap<String, String>();
	
	private GradleStreamsProxy streams;
	
	@SuppressWarnings("unused")
	private InputStream in;
	private PrintStream out;
	private PrintStream err;
	
	private GradleConnectionException exception = null; // If process terminated with an error this will be set to the returned exception.

	public GradleProcess(ILaunchConfiguration configuration, ILaunch launch) throws CoreException {
		this.launch = launch;
		this.conf = configuration;
		this.streams = new GradleStreamsProxy(this); // This will init input, out and err streams in this instance!
		run();
	}

	public List<String> getTasks() {
		return GradleLaunchConfigurationDelegate.getTasks(conf);
	}
	
	protected void run() throws CoreException {
		final GradleProject project = getProject();
		if (project!=null) {
			final List<String> taskList = GradleLaunchConfigurationDelegate.getTasks(conf);
			if (!taskList.isEmpty()) {
				JobUtil.schedule(JobUtil.NO_RULE, new GradleRunnable(getLabel()) {
					@Override
					public void doit(IProgressMonitor mon) throws Exception {
						mon.beginTask("Executing tasks", 10);
						try {
							fireCreateEvent();
							try {
								TaskUtil.execute(project, conf, taskList, new SubProgressMonitor(mon, 8), out, err);
								ISchedulingRule rule = JobUtil.buildRule();
								Job.getJobManager().beginRule(rule, new SubProgressMonitor(mon, 1));
								try {
									RefreshTab.refreshResources(conf, new SubProgressMonitor(mon, 1));
								} finally {
									Job.getJobManager().endRule(rule);
								}
							} finally {
								fireTerminateEvent();
							}
						} finally {
							mon.done();
						}
					}

				});
			} else {
				throw ExceptionUtil.coreException("Couldn't launch "+conf.getName()+" because no tasks have been selected");
			}

		} else {
			//Shouldn't happen in normal circumstance. Maybe this could happen if the launch conf was deleted.
			throw ExceptionUtil.coreException("Couldn't run Gradle build: can't determine GradleProject");
		}
	}
	
//	/**
//	 * Dummy implementation of "run". Just connects to console UI in Eclipse and sends some dummy output
//	 * to it with some delay time in between each line.
//	 */
//	protected void run() {
//		Thread thread = new Thread(getLabel()) {
//			public void run() {
//				try {
//					out.println("Starting gradle process : "+getLabel());
//					pause(3000);
//					for (String task : getTasks()) {
//						out.println(task);
//						pause(3000);
//					}
//					out.println("Finished gradle process : "+getLabel());
//				} finally {
//					fireTerminateEvent();
//				}
//			}
//
//			private void pause(long millis) {
//				try {
//					sleep(millis);
//				} catch (InterruptedException e) {
//				}
//			}
//		};
//		thread.start();
//	}

	public boolean canTerminate() {
		return false;
	}

	public boolean isTerminated() {
		return !isRunning;
	}

	public void terminate() throws DebugException {
		if (!isTerminated()) {
			throw new DebugException(ExceptionUtil.status("Forced termination of GradleProcess isn't supported by the Tooling API"));
		}
	}

	public String getLabel() {
		GradleProject proj = getProject();
		if (proj!=null) {
			return "Gradle Build on "+proj.getDisplayName();
		} else {
			return "Gradle Build";
		}
	}


	protected GradleProject getProject() {
		return GradleLaunchConfigurationDelegate.getProject(conf);
	}

	public ILaunch getLaunch() {
		return launch;
	}

	public IStreamsProxy getStreamsProxy() {
		return streams;
	}

	public void setAttribute(String key, String value) {
		attribs.put(key, value);
	}

	public String getAttribute(String key) {
		return attribs.get(key);
	}

	public int getExitValue() throws DebugException {
		if (!isTerminated()) {
			throw new DebugException(ExceptionUtil.status("can't get exitvalue unless process was terminated"));
		}
		if (exception==null) {
			return 0; 
		} else {
			return 999;
		}
	}

	/**
	 * Fires the given debug event.
	 * 
	 * @param event debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{event});
		}
	}

	/**
	 * Fires a terminate event.
	 */
	protected void fireTerminateEvent() {
		isRunning = false;
		if (exception!=null) {
			exception.printStackTrace(err);
		}
		streams.close();
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}
	
	private void fireCreateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	public void setInput(InputStream in) {
		this.in = in;
	}

	public void setOutput(OutputStream out) {
		this.out = new PrintStream(out);
	}
	
	public void setError(OutputStream out) {
		this.err = new PrintStream(out);
	}
	
}
