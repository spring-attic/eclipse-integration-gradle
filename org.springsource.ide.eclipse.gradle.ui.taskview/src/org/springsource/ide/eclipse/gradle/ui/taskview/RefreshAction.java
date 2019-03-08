/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class RefreshAction extends Action {
	
	private GradleTasksView owner;
	
	/**
	 * Constructs a new action.
	 */
	public RefreshAction(GradleTasksView owner) {
		super("Refresh");
		setDescription("Refresh Tasks");
		setToolTipText("Request a rebuild of the gradle model and refresh the list of tasks");
		JavaPluginImages.setLocalImageDescriptors(this, "refresh.gif"); //$NON-NLS-1$
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);
//		setChecked(owner.isLinkingEnabled());
		this.owner = owner;
		setToolTipText("Refresh task list");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		try {
			owner.requestRefreshTasks();
		} catch (CoreException e) {
			GradleCore.log(e);
		}
	}
	

}
