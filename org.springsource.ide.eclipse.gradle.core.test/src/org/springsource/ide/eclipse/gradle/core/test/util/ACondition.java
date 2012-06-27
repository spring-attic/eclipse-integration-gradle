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
package org.springsource.ide.eclipse.gradle.core.test.util;

import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Abstract class to create a "waitable" condition. This is to be used in implementing
 * tests where we want some condition to be true eventually, but cannot be sure it will be
 * true immediately.
 * <p>
 * Typical use would like so:
 * <code>
 * new ACondition() {
 * public boolean test() throws Exception {
 * ... some asserts ...
 * return ...something to test...;
 * }
 * }.waitFor(4000); // wait for 4 seconds or until test passes.
 * </code>
 * An exception will be thrown if the test method does not return true before the
 * timeout limit.
 * <p>
 * If the test method throws an exception this is treated the same as returning false.
 * If condition fails, we will try to rethrow a pertinent exception (typically the
 * exception thrown by the test method, the last time we tried to run it.
 * 
 * @author Kris De Volder
 */
public abstract class ACondition {
	
	private String description = null;
	
	public ACondition() {
	}
	
	public ACondition(String description) {
		this.description = description;
	}

	Throwable e = null;
	
	public void waitFor(long timeout) throws Exception {
		long startTime = System.currentTimeMillis();
		long endTime = startTime + timeout;
		boolean result = false;
		while (!(result = doTest()) && System.currentTimeMillis() < endTime) {
			TestUtils.waitForDisplay(); // Avoids UI deadlock by allowing UI to process events.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		if (!result) {
			//Try our best to create a 'nice' exception reflecting the reason for the test failure
			if (e!=null)
				if (e instanceof Exception) {
					throw (Exception)e;
				} else {
					throw new Error(e);
				}
			else {
				throw new Error(getMessage());
			}
		}
		if (description!=null) {
			System.out.println(description + " succeeded after: " + (System.currentTimeMillis() - startTime));
		}
	}
	
	private boolean doTest() {
		boolean result = false;
		try {
			e = null;
			result = test();
		} catch (Throwable e) {
			this.e = e;
		}
		return result;
	}
	
	/**
	 * Test something. If the method returns true, the test passes.
	 * If it returns false or throws an exception the test fails (and will be
	 * retried until it passes or timeout is reached).
	 */
	public abstract boolean test() throws Exception;

	/**
	 * Message used when time out reached without an exception
	 */
	public String getMessage() {
		return "timed out";
	}

	public static void assertJobManagerIdle() {
		final IJobManager jm = Job.getJobManager();
		if (jm.isIdle()) {
			return; //OK!
		}
		//Make a nice message listing all the jobs and their present state.
		Job[] allJobs = jm.find(null);
		StringBuffer msg = new StringBuffer("JobManager not idle: \n");
		for (Job job : allJobs) {
			msg.append("   Job: "+job.getName() + " State: " + stateString(job) +"\n");
		}
		throw new AssertionFailedError(msg.toString());
	}
	
	public static String stateString(Job job) {
		int state = job.getState();
		switch (state) {
		case Job.RUNNING:
			return "RUNNING";
		case Job.SLEEPING:
			return "SLEEPING";
		case Job.WAITING:
			return "WAITING";
		case Job.NONE:
			return "NONE";
		default:
			return ""+state;
		}
	}
	
}
