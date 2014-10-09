/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;

/**
 * Wrapper for long running operations, provides a way to turn the operation into
 * different types of "runnables" that exist within eclipse.
 * 
 * @author Kris De Volder
 */
public abstract class GradleRunnable implements IRunnableWithProgress {
	
	protected String jobName = "Gradle Job "+generateId();
	private int jobCtr = 0;
	private synchronized int generateId() {
		return jobCtr++;
	}
	
	public GradleRunnable(String jobName) {
		this.jobName = jobName;
	}

	public abstract void doit(IProgressMonitor mon) throws Exception;

	public Job asJob() {
		return new Job(jobName) {
			private CancellationTokenSource cancellationSource;
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				this.cancellationSource = GradleConnector.newCancellationTokenSource();
				try {
					doit(new GradleOpearionProgressMonitor(monitor, cancellationSource.token()));
					return Status.OK_STATUS;
				} catch (Throwable e) {
					return monitor.isCanceled() ? Status.CANCEL_STATUS : ExceptionUtil.status(e);
				}
			}
			
			@Override
			public Job yieldRule(IProgressMonitor monitor) { //Avoids a deadlocking problem when WTP tasks call yieldRule
				return null;
			}

			@Override
			protected void canceling() {
				super.canceling();
				if (cancellationSource != null) {
					cancellationSource.cancel();
				}
			}			
			
		};
	}

	public WorkspaceJob asWorkspaceJob() {
		return new WorkspaceJob(jobName) {
			private CancellationTokenSource cancellationSource;
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				this.cancellationSource = GradleConnector.newCancellationTokenSource();
				try {
					doit(new GradleOpearionProgressMonitor(monitor, cancellationSource.token()));
					return Status.OK_STATUS;
				} catch (Throwable e) {
					return ExceptionUtil.status(e);
				}
			}
			
			@Override
			protected void canceling() {
				super.canceling();
				cancellationSource.cancel();
			}
		};
	}
	
	/**
	 * This method is here so that GradleRunnable can be used as an {@link IRunnableWithProgress}. It is final
	 * as you are not supposed to implement it directly. Implement the doit method instead.
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			doit(monitor);
		} catch (InterruptedException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw new InterruptedException("Canceled by user");
		} catch (InvocationTargetException e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}
	
	@Override
	public String toString() {
		return jobName;
	}
	
}
