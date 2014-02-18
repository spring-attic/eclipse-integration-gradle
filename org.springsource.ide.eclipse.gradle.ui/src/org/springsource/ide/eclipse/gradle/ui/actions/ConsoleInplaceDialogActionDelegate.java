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

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
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
public class ConsoleInplaceDialogActionDelegate implements IWorkbenchWindowActionDelegate {
	
	protected IProject selected = null;

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		Shell parent = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		ConsoleInplaceDialog dialog = ConsoleInplaceDialog.getInstance(parent);
		dialog.setSelectedProject(selected);
		dialog.open();
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		selected = null;
		
		if (selection instanceof IStructuredSelection && !(selection instanceof ITextSelection)) {
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (obj instanceof IJavaProject) {
					obj = ((IJavaProject) obj).getProject();
				}
				else if (obj instanceof IAdaptable) {
					obj = ((IAdaptable) obj).getAdapter(IResource.class);
				}
				if (obj instanceof IResource) {
					IResource project = (IResource) obj;
					selected = project.getProject();
				}
			}
		}
		else {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor != null) {
				if (editor.getEditorInput() instanceof IFileEditorInput) {
					selected = (((IFileEditorInput) editor).getFile()
							.getProject());
				}
			}
		}
		
		action.setEnabled(selected != null);

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