/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;


/**
 * Action that causes the projects source folders to be reconfigured.
 * 
 * @author Kris De Volder
 */
public class RefreshSourceFoldersAction extends RefreshAction {

	public RefreshSourceFoldersAction() {
	}
	
	public void run(IAction action) {
		final GradleProject project = getGradleProject();
		if (project!=null) {
			JobUtil.schedule(new GradleRunnable("Refresh "+project.getName()) {
				@Override
				public void doit(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
					ErrorHandler eh = ErrorHandler.forRefreshSourceFolders();
					project.invalidateGradleModel();
					project.refreshSourceFolders(eh, monitor, cancellationSource.token());
					eh.rethrowAsCore();
				}
			});
		}
	}
	
}
