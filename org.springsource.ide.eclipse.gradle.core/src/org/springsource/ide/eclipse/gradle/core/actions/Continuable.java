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
package org.springsource.ide.eclipse.gradle.core.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.util.Continuation;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;


/**
 * This is a GradleRunnable that is associated with a continuation. When using continuations, 
 * use this style of runable. It makes sure thrown execptions are automatically passed on
 * to the continuation.
 * 
 * @author Kris De Volder
 */
public abstract class Continuable extends GradleRunnable {
	
	Continuation<Void> cont;
	private int ticks;

	public Continuable(String jobName, int ticks, Continuation<Void> cont) {
		super(jobName);
		this.ticks = ticks;
		this.cont = cont;
	}

	/**
	 * Implementation of GradleRunable's doit.
	 * This method is final because you are supposed to implement a doit method that takes a continuation instead.
	 */
	@Override
	public final void doit(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(jobName, ticks);
		try {
			doit(cont, monitor);
		} catch (Throwable e) {
			cont.error(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * This method is called by the Continuable when it is run. The call will be wrapped with an appropriate 
	 * try catch that catches any uncaught exceptions and propagates them to the continuation. If no exceptions
	 * are raised thrown out of this method, then it is the responsibility of the implementer to 
	 * ensure the continuation is invoked either with an error or value. 
	 * <p>
	 * The calling context also takes care of calling the progress monitor's beginTask and done methods before and
	 * after this method is called.
	 */ 
	public abstract void doit(Continuation<Void> cont, IProgressMonitor monitor) throws Exception;

}
