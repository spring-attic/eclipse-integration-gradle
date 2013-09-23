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
package org.springsource.ide.eclipse.gradle.core.launch;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.springsource.ide.eclipse.gradle.core.launch.GradleProcess;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;


/**
 * A DebugEvent listener that is used for testing purposes. The listener provides a way to
 * Grab a hold of and then wait for the termination of a GradleProcess that is expected to 
 * be started soon after the listener is added to the Eclipse {@link DebugPlugin}.
 * <p>
 * Typically, you don't use this class directly but rather call a method in {@link LaunchUtil}
 * to launch a Gradle process.
 * 
 * @author Kris De Volder
 */
class LaunchTerminationListener implements ILaunchesListener, ILaunchesListener2 {

	private ILaunch launch;

	public LaunchTerminationListener(ILaunch launch) {
		this.launch = launch;
	}

	private synchronized boolean isTerminated() {
		return launch.isTerminated();
	}
	
	public synchronized void waitForProcessTermination() {
		//TODO: need a timeout limit?
		while (!isTerminated()) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
	}
	
	
	
	public void launchesRemoved(ILaunch[] launches) {
		notifyMaybe(launches);
	}

	private void notifyMaybe(ILaunch[] launches) {
		for (ILaunch iLaunch : launches) {
			if (iLaunch==this.launch) {
				synchronized (this) {
					notifyAll();
				}
			}
		}
	}

	public void launchesAdded(ILaunch[] launches) {
		notifyMaybe(launches);
	}

	public void launchesChanged(ILaunch[] launches) {
		notifyMaybe(launches);
	}

	public void launchesTerminated(ILaunch[] launches) {
		notifyMaybe(launches);
	}

	public GradleProcess waitForProcess() {
		waitForProcessTermination();
		IProcess[] processes = launch.getProcesses();
		if (processes!=null && processes.length>0) {
			Assert.isTrue(processes.length==1);
			return (GradleProcess) processes[0];
		}
		return null;
	}

}
