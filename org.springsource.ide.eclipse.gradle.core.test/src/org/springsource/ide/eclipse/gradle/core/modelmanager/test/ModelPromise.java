/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.modelmanager.test;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.Job;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

public class ModelPromise<T> extends JoinableContinuation<T> {
	
	private Job job = null;
	private boolean canceled = false;

	public ModelPromise() {
	}

	/**
	 * Associate an Eclipse job with this ModelPromise. When the promise is 
	 * canceled the cancelation request will be passed on to cancel the Job.
	 * <p>
	 * TRICKY: you should only call 'setJob' when the code inside the
	 * job has started executing. So best to call this from the Job's code
	 * itself. Otherwise a race condition is introduced into
	 * your test code where Eclipse may already cancel the job before it did started...
	 * Whatever code is in the job  won't have chance to be 'canceled' and 
	 * reject the promise as 'canceled'. This likely to cause test to hang
	 * waiting for the promise forever.
	 */
	public synchronized void setJob(Job job) {
		Assert.isLegal(this.job==null || this.job==job,
				"ModelPromise.job should only be set once");
		this.job = job;
		if (canceled) {
			job.cancel();
		}
	}

	public void cancel() {
		this.canceled = true;
		if (job!=null) {
			job.cancel();
		}
	}

}