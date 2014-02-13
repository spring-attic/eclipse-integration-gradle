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

import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * An exception thrown when creating a IClassPathEntry is somehow illegal or results in
 * an error.
 * 
 * @author Kris De Volder
 */
public class IllegalClassPathEntryException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public IllegalClassPathEntryException(String msg, GradleProject gradleProject, 
			EclipseSourceDirectory gradleSourceDir) {
		this(msg, gradleProject, gradleSourceDir, null);
	}

	public IllegalClassPathEntryException(String msg,
			GradleProject gradleProject,
			EclipseSourceDirectory gradleSourceDir, Throwable e) {
		super("Ignored Classpath Entry "+gradleSourceDir+" for project "+gradleProject.getDisplayName()+"\nreason: "+msg, e);
	}

}
