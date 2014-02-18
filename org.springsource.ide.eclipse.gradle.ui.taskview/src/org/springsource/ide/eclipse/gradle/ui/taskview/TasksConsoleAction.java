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
package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.actions.ConsoleInplaceDialogActionDelegate;

/**
 * A proxy action to {@link ConsoleInplaceDialogActionDelegate}
 * 
 * @author Alex Boyko
 *
 */
public class TasksConsoleAction extends Action {
	
	private ConsoleInplaceDialogActionDelegate delegateAction;
	
	public TasksConsoleAction() {
		super(null);
		this.delegateAction = new ConsoleInplaceDialogActionDelegate();
		setDescription("Tasks Quick Launcher");
		setToolTipText("Displays Tasks Quick Launcher to launch multiple tasks");
		setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_LAUNCH));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		delegateAction.run(this);
	}
	
	public void selectChanged(ISelection selection) {
		delegateAction.selectionChanged(this, selection);
	}

}
