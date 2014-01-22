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
 * Exception that is thrown when we suspect that we have hit the bug
 * http://issues.gradle.org/browse/GRADLE-1792
 * 
 * @author Kris De Volder
 */
public class Gradle1792BugException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private static final String EXPLANATION = 
			"Most likely you are hitting bug\n" +
			"http://issues.gradle.org/browse/GRADLE-1792\n\n" +
			"To avoid the bug, please make sure that the Gradle Eclipse Plugin " +
			"is applied to all your projects, or that your are using a version " +
			"of Gradle M4 or later.";

	public Gradle1792BugException(Throwable e) {
		super(EXPLANATION, e);
	}
	
}
