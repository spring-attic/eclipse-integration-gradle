package org.springsource.ide.eclipse.gradle.core.modelmanager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

public class ModelPromise<T> extends JoinableContinuation<T> {
	
	private IProgressMonitor mon = null;
	private boolean canceled = false;

	public ModelPromise() {
	}
	
	public synchronized void setMonitor(IProgressMonitor mon) {
		this.mon = mon;
		if (canceled) {
			mon.setCanceled(true);
		}
	}

	public void cancel() {
//		synchronized (this) {
//			if (!started) {
//				//If job wasn't started yet then it won't produce any result when canceled so
//				// we must raise produce the cancel exception ourselves.
//				error(ExceptionUtil.coreException(new OperationCanceledException("Build canceled")));
//			}
//		}
		this.canceled = true;
		if (mon!=null) {
			mon.setCanceled(true);
		}
	}

}