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
package org.springsource.ide.eclipse.gradle.core.classpathcontainer;

/**
 * For operations that may take a long time we will often provide two implementations. One version that is fast,
 * but is allowed to fail if the work can not be done fast at the moment, and one that is allowed to take
 * whatever time is needed.
 * <p>
 * The fast version of the operation will throw a {@link FastOperationFailedException} to indicate that the work
 * can not be done fast at this time. Callers of fast operations should be prepared to deal with the situation.
 * 
 * @author Kris De Volder
 */
public class FastOperationFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	public FastOperationFailedException() {
		super();
	}

	public FastOperationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public FastOperationFailedException(String message) {
		super(message);
	}

	public FastOperationFailedException(Throwable cause) {
		super(cause);
	}

}
