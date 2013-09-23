/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 */
package org.springsource.ide.eclipse.gradle.core.autorefresh;

import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * @author Kris De Volder
 */
public interface IDirtyProjectListener {

	/**
	 * Called when a workspace change dirties a project.
	 */
	void addDirty(GradleProject gp);
	
	/**
	 * May be called when a manual refresh 'cleans' a project. This can potentially 
	 * avoid unnecessary automatic refreshes later.
	 */
	void removeDirty(GradleProject gp);

}
