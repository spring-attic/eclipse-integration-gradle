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
package org.springsource.ide.eclipse.gradle.ui.actions;


import org.eclipse.jface.action.IAction;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;


/**
 * Action that causes the contents of the projects class path container to be recomputed.
 * 
 * @author Kris De Volder
 */
public class RefreshDependenciesAction extends RefreshAction {

	public RefreshDependenciesAction() {
	}

	public void run(IAction action) {
		RefreshDependenciesActionCore.callOn(getProjects());
	}

}
