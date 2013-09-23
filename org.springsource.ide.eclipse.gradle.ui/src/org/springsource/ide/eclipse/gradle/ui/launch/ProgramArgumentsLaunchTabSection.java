/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;


/**
 * @author Kris De Volder
 */
public class ProgramArgumentsLaunchTabSection extends ArgumentsLaunchTabSection {

	public ProgramArgumentsLaunchTabSection(LaunchTabWithSections owner) {
		super(owner);
	}
	
	@Override
	protected String getLabelText() {
		return "Program Args";
	}

	@Override
	protected String getLabelTooltipText() {
		return null;
	}

	@Override
	protected void setCustomArguments(ILaunchConfigurationWorkingCopy conf, String args) {
		GradleLaunchConfigurationDelegate.setProgramArguments(conf, args);
	}

	@Override
	protected String getCustomArguments(ILaunchConfiguration conf) {
		return GradleLaunchConfigurationDelegate.getProgramArguments(conf);
	}
}
