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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;


/**
 * @author Kris De Volder
 */
public class EnableDisableDSLSupportDelegate extends GradleProjectActionDelegate {
	
	/**
	 * This may be null if Greclipse isn't installed.
	 */
	DSLDSupport dslSupport = DSLDSupport.getInstance();

	public EnableDisableDSLSupportDelegate() {
	}

	public void run(IAction action) {
		final GradleProject project = getGradleProject();
		runInUi(new GradleRunnable("Toggle DSLD support for "+project.getDisplayName()) {
			@Override
			public void doit(IProgressMonitor monitor) throws Exception {
				boolean enabled = dslSupport.isEnabled(project);
				if (!enabled && !dslSupport.haveGreclipse()) {
					//If greclipse is not installed, only allow disabling DSLD and issue an error otherwise.
					if (!dslSupport.haveGreclipse()) {
						MessageDialog.openError(null, "Greclipse is Required", 
								"DSLD support only works if you have Greclipse installed.");
						return;
					}
				}
				dslSupport.enableFor(project, !enabled, monitor);
			}
		});
	} 
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (action.isEnabled()) {
			if (dslSupport.isEnabled(getGradleProject())) {
				action.setText("Disable DSL Support");
			} else {
				action.setText("Enable DSL Support");
			}
		}
	}

}
