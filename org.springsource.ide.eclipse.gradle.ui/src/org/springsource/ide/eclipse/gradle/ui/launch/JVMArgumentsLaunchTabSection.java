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
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;


/**
 * @author Kris De Volder
 */
public class JVMArgumentsLaunchTabSection extends ArgumentsLaunchTabSection {

	public JVMArgumentsLaunchTabSection(LaunchTabWithSections owner) {
		super(owner);
	}

	@Override
	protected String getLabelText() {
		return "JVM Args";
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.launch.ArgumentsLaunchTabSection#getLabelTooltipText()
	 */
	@Override
	protected String getLabelTooltipText() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.launch.ArgumentsLaunchTabSection#setCustomArguments(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy, java.lang.String)
	 */
	@Override
	protected void setCustomArguments(ILaunchConfigurationWorkingCopy conf, String args) {
		GradleLaunchConfigurationDelegate.setJVMArguments(conf, args);
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.launch.ArgumentsLaunchTabSection#getCustomArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	protected String getCustomArguments(ILaunchConfiguration conf) {
		return GradleLaunchConfigurationDelegate.getJVMArguments(conf);
	}
}
