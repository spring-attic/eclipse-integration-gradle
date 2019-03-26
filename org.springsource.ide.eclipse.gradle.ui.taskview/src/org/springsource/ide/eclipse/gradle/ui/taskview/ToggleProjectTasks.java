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
package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.jface.action.Action;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;

/**
 * Action responsible for switching the view mode from project local tasks to
 * aggregate tasks
 * 
 * @author Alex Boyko
 * 
 */
public class ToggleProjectTasks extends Action {
	
	private GradleTasksView owner;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleProjectTasks(GradleTasksView owner, boolean on) {
		super(null, AS_CHECK_BOX);
		this.owner = owner;
		setChecked(on);
		setDescription("Display Local Tasks");
		setToolTipText("Displays tasks only defined in this project excluding subprojects");
		setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_PROJECT_FOLDER));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		owner.setDisplayProjectLocalTasks(isChecked());
	}
	

}
