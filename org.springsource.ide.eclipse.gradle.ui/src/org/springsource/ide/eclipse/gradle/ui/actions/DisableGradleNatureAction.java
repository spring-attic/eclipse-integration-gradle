/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.progress.UIJob;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.util.NatureUtils;

/**
 * Action for disabling Gradle nature
 * 
 * @author Alex Boyko
 *
 */
public class DisableGradleNatureAction extends GradleProjectActionDelegate {

	@Override
	public void run(IAction action) {
		new UIJob("Removing Gradle Nature") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				monitor.beginTask("Removing Gradle Natures", getProjects().size());
				try {
					for (IProject project : getProjects()) {
						NatureUtils.remove(project, GradleNature.NATURE_ID, monitor);
						monitor.worked(1);
					}
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
			
		}.schedule();
	}

}
