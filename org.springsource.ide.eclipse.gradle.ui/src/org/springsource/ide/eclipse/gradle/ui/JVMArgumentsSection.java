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
package org.springsource.ide.eclipse.gradle.ui;

import org.springsource.ide.eclipse.gradle.core.GradleCore;

/**
 * Section on a preferences page (or launch config editor tab) that allows user to set JVM 
 * arguments.
 * 
 * @author Kris De Volder
 */
public class JVMArgumentsSection extends ArgumentsSection {
	
//	/**
//	 * JVMArgumentsSection needs some place to store and read the arguments from. This interface
//	 * represents that place.
//	 */
//	public interface IJVMArgumentsHolder {
//		String getJVMArguments();
//		void setJVMArguments(String args);
//	}

//	private IJVMArgumentsHolder store;

	public JVMArgumentsSection(IPageWithSections owner) {
		super(owner);
	}
	
	@Override
	protected String getLabelTooltipText() {
		return "Arguments to be passed to the JVM that runs the Gradle daemon.";
	}

	@Override
	protected String getLabelText() {
		return "JVM arguments (requires Gradle 1.0.M9 or later)";
	}
	
	@Override
	protected String getCustomArguments() {
		return GradleCore.getInstance().getPreferences().getJVMArguments();
	}

	@Override
	protected void setCustomArguments(String args) {
		GradleCore.getInstance().getPreferences().setJVMArguments(args);
	}

}