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
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.wizards.NewGradleProjectOperation;


public class NewGradleProjectWizard extends Wizard implements INewWizard {

	private final NewGradleProjectOperation operation = new NewGradleProjectOperation();
	private NewGradleProjectWizardPage pageOne;

	public NewGradleProjectWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		super.addPages();
		pageOne = new NewGradleProjectWizardPage(operation);
		addPage(pageOne);
	}
	@Override
	public boolean performFinish() {
		JobUtil.userJob(new GradleRunnable("Create Gradle project(s)") {
			@Override
			public void doit(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
//				throw ExceptionUtil.coreException("It went horribly wrong!");
				operation.perform(monitor);
			}
		});
		return true;
	}

}
