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
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * This exception is thrown when Gradle tooling detects that projects in the workspace
 * no longer are associated with a root project. This may happen if a Gradle project
 * hierarchy is changed after the projects where imported.
 * 
 * @author Kris De Volder
 */
public class InconsistenProjectHierarchyException extends CoreException {

	public InconsistenProjectHierarchyException(IStatus status) {
		super(status);
	}

	private static final long serialVersionUID = 1L;

}
