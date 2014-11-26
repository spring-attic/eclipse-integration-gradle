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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

/**
 * 
 * @author Kris De Volder
 */
public class BuildResult<T> {

	final private Class<T> type;
	final private Throwable error;
	final private T model;
	
	public BuildResult(Class<T> type, T model) {
		this.type = type;
		this.model = model;
		this.error = null;
	}
	
	public BuildResult(Class<T> type, Throwable error) {
		this.type = type;
		this.model = null;
		this.error = error;
	}

	private BuildResult(Class<T> type, T model, Throwable error) {
		this.type = type;
		this.model = model;
		this.error = error;
	}

	public Class<T> getType() {
		return type;
	}
	
	public boolean isFailed() {
		return error!=null;
	}
	
	public boolean isSucceeded() {
		return error == null;
	}
	
	@Override
	public String toString() {
		if (isFailed()) {
			return error.toString();
		} else {
			return ""+model;
		}
	}
	
	public T getModel() {
		if (isSucceeded()) {
			return model;
		} else {
			throw new IllegalStateException("Only a succesful build result contains a model", error);
		}
	}

	public Throwable getError() {
		if (isFailed()) {
			return error;
		} else {
			throw new IllegalStateException("Only a failed build result contains an error");
		}
	}

	public T get() throws CoreException {
		if (isFailed()) {
			throw ExceptionUtil.coreException(getError());
		} else {
			return getModel();
		}
	}
	
	/**
	 * "Safe" typecast with runtime check.
	 */
	@SuppressWarnings("unchecked")
	public <S> BuildResult<S> cast(Class<S> type) {
		Assert.isLegal(type.isAssignableFrom(this.type));
		if (type.equals(this.type)) {
			//special case, don't make a new object that's exactly the same as
			//this one.
			return (BuildResult<S>)this;
		}
		return new BuildResult<S>(type, (S)model, error);
	}
	
	public void sendTo(JoinableContinuation<T> promise) {
		if (isFailed()) {
			promise.error(error);
		} else {
			promise.apply(model);
		}
	}
}
