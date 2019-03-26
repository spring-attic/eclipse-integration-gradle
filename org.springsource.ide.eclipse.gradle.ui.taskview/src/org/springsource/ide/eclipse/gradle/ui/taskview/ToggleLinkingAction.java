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

import org.eclipse.jdt.internal.ui.actions.AbstractToggleLinkingAction;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class ToggleLinkingAction extends AbstractToggleLinkingAction {

	private GradleTasksView owner;

	/**
	 * Constructs a new action.
	 * @param explorer the package explorer
	 */
	public ToggleLinkingAction(GradleTasksView owner) {
		setChecked(owner.isLinkingEnabled());
		this.owner= owner;
		setToolTipText("Link with selection");
	}

	/**
	 * Runs the action.
	 */
	@Override
	public void run() {
		owner.setLinkingEnabled(isChecked());
	}

}
