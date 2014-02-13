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
 * A 'joinable' is some representative of an asynch computation that can be waited for by
 * calling the 'join' method.
 * 
 * @author Kris De Volder
 */
public interface Joinable<T> {

	/**
	 * Block current thread until some asynchronous computation is finished. 
	 * If the asynch computation ended by returning a value then this value is returned by
	 * join. If the computation ended by throwing an exception than this exception is
	 * rethrown in the current thread by join.
	 */
	T join() throws Exception;
	
}
