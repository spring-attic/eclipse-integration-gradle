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

/**
 * A continuation that implements the Joinable interface. The purpose of this is to support
 * clients that want to make their thread wait for a computation that calls the continuation
 * rather than create a callback to handle the result.
 * <p>
 * We make it appear, as much as possible, as if the computation that is being joined
 * was run by the join method (i.e. return values and exceptions are propagated as if
 * returned / thrown by the join method itself).
 * 
 * @author Kris De Volder
 */
public class JoinableContinuation<T> extends Continuation<T> implements Joinable<T> {
	
	private boolean isValue = false;
	private boolean isThrow = false;
	private T value = null;
	private Throwable thrw = null;

	public synchronized T join() throws Exception {
		while(!(isValue || isThrow)) {
			try {
				wait(3000);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		if (isValue) {
			return value;
		} else {
			//isThrow
			if (thrw instanceof Exception) {
				throw (Exception)thrw;
			} else if (thrw instanceof Error) {
				throw (Error)thrw; 
			} else {
				//not sure what it is but throw it anyway
				throw new Error(thrw);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.core.util.Continuation#apply(java.lang.Object)
	 */
	@Override
	public void apply(T value) {
		setValue(value);
	}
	
	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.core.util.Continuation#error(java.lang.Throwable)
	 */
	@Override
	public void error(Throwable e) {
		super.error(e);
		setError(e);
	}
	
	public boolean isError() {
		return isThrow;
	}

	private synchronized void setError(Throwable e) {
		if (isDone()) {
			return;
		}
		isThrow = true;
		thrw = e;
		notifyAll();
	}

	private boolean isDone() {
		return isValue || isThrow; 
	}

	private synchronized void setValue(T value) {
		if (isDone()) {
			return;
		}
		isValue = true;
		this.value = value;
		notifyAll();
	}

}
