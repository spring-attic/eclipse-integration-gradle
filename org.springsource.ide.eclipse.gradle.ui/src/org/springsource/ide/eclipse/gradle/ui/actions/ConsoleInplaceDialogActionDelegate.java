/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.ui.cli.inplace.ConsoleInplaceDialog;


/**
 * Action delegate able to show the tasks quick launcher dialog
 * 
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Kris De Volder
 * @author Alex Boyko
 * @since 2.2.0
 */
public class ConsoleInplaceDialogActionDelegate extends GradleProjectActionDelegate implements IWorkbenchWindowActionDelegate {
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		// nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		Shell parent = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		ConsoleInplaceDialog dialog = ConsoleInplaceDialog.getInstance(parent);
		dialog.setSelectedProject(GradleNature.hasNature(getProject()) ? getProject() : null);
		dialog.open();
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		action.setEnabled(true);

		// Have selected something in the editor - therefore
		// want to close the inplace view if haven't already done so
		if (selection != null && !(selection instanceof TreeSelection)) {
			ConsoleInplaceDialog.closeIfNotPinned();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
		// nothing
	}

}