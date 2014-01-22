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
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * @author Kris De Volder
 */
public class GradleImportWizard extends Wizard implements IImportWizard {

	private GradleImportWizardPageOne pageOne;
//	private IWorkbench workbench;

	public GradleImportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
//		this.workbench = workbench;
//		super.init(workbench, selection);
	}
	
	public void addPages() {
		super.addPages();
		addPage(getPageOne());
	}

	private GradleImportWizardPageOne getPageOne() {
		if (pageOne==null) {
			pageOne = new GradleImportWizardPageOne();
		}
		return pageOne;
	}
	
	public GradleImportOperation createOperation() {
		return getPageOne().createOperation();
	}

	@Override
	public boolean performFinish() {
		getPageOne().wizardAboutToFinish();
		final GradleImportOperation operation = createOperation();
		JobUtil.userJob(new GradleRunnable("Import Gradle projects") {
			@Override
			public void doit(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
				ErrorHandler eh = ErrorHandler.forImportWizard();
				operation.perform(eh, monitor);
				eh.rethrowAsCore();
			}
		});
		return true;
	}

}
