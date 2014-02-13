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
 * A NullJoinable represents something that waits for an 'empty' amount of work.
 * As such join simply returns immediately as there is never anything to wait for.
 * <p>
 * Return an instance of this class wherever a Joinable is expected but there is 
 * no work to do.
 * 
 * @author Kris De Volder
 */
public class NullJoinable<T> implements Joinable<T> {

	private T value = null;

	public NullJoinable() {
	}
	
	public NullJoinable(T value) {
		this.value = value;
	}
	
	public T join() throws Exception {
		return value;
	}

}
