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
package org.springsource.ide.eclipse.gradle.core.util;

import org.eclipse.core.runtime.Assert;

/**
 * A class for tracking whether something is busy. Allows clients to register 'start' and
 * 'stop' events. The status will be considered 'busy' as long as more starts have been
 * received than stops. Clients should take extreme care to ensure they release their
 * busy lock. I.e. any call to 'start' must be balanced by a call to stop, even if
 * the client is terminated by some exception. 
 * <p>
 * Note: java.util.concurrent has ReadWriteLock which does something similar and *could* be
 * used here, except that it has some constraints built-in that locks must be acquired and
 * released by the same thread. Our use case acquires a lock when a Job is created and
 * releases it when this Job terminated. Thus by definition it doesn't meet the requirement
 * (The thread creating the Job is different from the thread the Job itself runs on).
 * 
 * @author Kris De Volder
 */
public class BusyStatus {
	
	private String description;
	private int busyCount;

	public BusyStatus(String forToString) {
		this.description = forToString;
	}
	
	public synchronized void start() {
		busyCount++;
	}
	
	public synchronized void stop() {
		Assert.isLegal(busyCount>0);
		busyCount--;
		notify();
	}
	
	/**
	 * This method blocks until busy counter is 0. Note: this doesn't provide a real guarantee that busy counter
	 * may then not be immediately increased again by another thread.
	 * <p>
	 * This is intended mostly for use cases where a client is calling some method that may do some of its work
	 * in an asynchronous way. And the client needs some way to wait for all this work to complete.
	 * Each bit of work is increasing the busy count. Clients (e.g. testing code) may need a way to
	 * wait for all these bits of work to complete. This method provides a mechanism to do that. 
	 */
	public synchronized void waitNotBusy() {
		while (busyCount>0) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	@Override
	public String toString() {
		return "BusyStatus("+description +" = "+busyCount+")";
	}

}
