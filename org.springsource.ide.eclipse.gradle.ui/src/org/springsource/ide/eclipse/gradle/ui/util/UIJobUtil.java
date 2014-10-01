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
package org.springsource.ide.eclipse.gradle.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;


/**
 * @author Kris De Volder
 */
public class UIJobUtil {

	public static void busyCursorWhile(GradleRunnable runnable) throws InvocationTargetException, InterruptedException {
		IWorkbench wb = PlatformUI.getWorkbench();
		IProgressService ps = wb.getProgressService();
		ps.busyCursorWhile(runnable);
	}

	/**
	 * Execute a runable while showing a modal progress dialog. Note that it is preferred in general to
	 * use "busyCursorWhile". Use this method only in the context of modal
	 * dialog, where busyCursorwhile will not switch to showing a progress dialog (because the dialog is
	 * suppressed when another modal dialog is already open).
	 */
	public static void withProgressDialog(Shell shell, final GradleRunnable runnable) {
		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell) {
			@Override
			protected void cancelPressed() {
				super.cancelPressed();
				runnable.getCancellationSource().cancel();
			}
		};
		try {
			progressDialog.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			if (!progressDialog.getProgressMonitor().isCanceled()) {
				//TODO: probably, this should propagate error messages to the context, because this called from some UI
				// context, like a dialog, and this dialog may want to display the error in a dialog status line rather than popup message.
				MessageDialog.openError(shell, "Error in runnable '"+runnable+"'", ExceptionUtil.getMessage(e) 
						+ "\nSee error log for details");
				GradleCore.log(e);
			}
		} catch (InterruptedException e) {
			if (!progressDialog.getProgressMonitor().isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}
	
}
