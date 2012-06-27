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
package org.springsource.ide.eclipse.gradle.core.samples;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

/**
 * There may be different ways to create/obtain the contents of a sample project. This class is meant to
 * hide the details from client code that needs to allow users to select some sample project and use it.
 * 
 * @author Kris De Volder
 */
public abstract class SampleProject {
	
	public abstract String getName(); // A name that will be shown to the user.

	/**
	 * Generate the contents of the sample project at the given location.
	 * @throws CoreException 
	 */
	public abstract void createAt(File location) throws CoreException;

	public static String getDefaultProjectLocation(String projectName) {
		if (projectName!=null) {
			return Platform.getLocation().append(projectName).toOSString();
		} else {
			return Platform.getLocation().toOSString();
		}
	}

}
