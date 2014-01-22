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
package org.springsource.ide.eclipse.gradle.ui.actions;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;


/**
 * Action that causes the select project's source folders and dependencies to be refreshed.
 * 
 * @author Kris De Volder
 */
public class RefreshAllAction extends RefreshAction {

	public RefreshAllAction() {
	}

	public void run(IAction action) {
		final List<IProject> projects = getProjects();
		try {
			RefreshAllActionCore.callOn(projects);
		} catch (CoreException e) {
			GradleUI.log(e);
			String msg = e.getMessage();
			MessageDialog.openError(null, "Problem refreshing projects", ""+msg);
		}
	}
	
	@Override
	protected boolean isEnabled(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(GradleNature.NATURE_ID);
		} catch (CoreException e) {
			GradleUI.log(e);
		}
		return false;
	}

}
