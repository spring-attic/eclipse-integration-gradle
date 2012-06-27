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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.ProjectMapperFactory;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;


/**
 * @author Kris De Volder
 */
public class ConvertToGradleProjectActionDelegate extends GradleProjectActionDelegate {

	public ConvertToGradleProjectActionDelegate() {
	}

	/**
	 * Relax enabling condition from superclass because we are adding the nature (so checking whether is
	 * there is a bit too strict here :-)
	 */
	@Override
	protected boolean isEnabled(IProject project) {
		try {
			return project.isAccessible() && !project.hasNature(GradleNature.NATURE_ID);
		} catch (CoreException e) {
			GradleCore.log(e);
			return false;
		}
	}
	
	public void run(IAction action) {
		final GradleProject project = GradleCore.create(getProject());
		JobUtil.schedule(new GradleRunnable("Convert to Gradle project") {
			@Override
			public void doit(IProgressMonitor monitor) throws CoreException {
				ErrorHandler eh = new ErrorHandler.KeepFirst();
				project.convertToGradleProject(ProjectMapperFactory.workspaceMapper(), eh, monitor);
				eh.rethrowAsCore();
			}
		}); 
	}

}
