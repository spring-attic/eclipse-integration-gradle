/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui;

import org.springsource.ide.eclipse.gradle.core.GradleCore;

/**
 * @author Kris De Volder
 */
public class ProgramArgumentsSection extends ArgumentsSection {

	public ProgramArgumentsSection(IPageWithSections owner) {
		super(owner);
	}

	@Override
	protected String getLabelText() {
		return "Program Arguments (requires Gradle 1.0-rc-2)";
	}

	@Override
	protected String getLabelTooltipText() {
		return "Command line arguments passed to gradle operations (i.e. model " +
				"builds and task executions)." +
				"These are the same kinds of arguments as might be passed to gradle or " +
				"gradlew on the command line. Note however that not all types of arguments " +
				"are supported. For details see the Gradle Tooling API documentation for " +
				"method LongRunningOperation.withArguments.";
	}

	@Override
	protected String getCustomArguments() {
		return GradleCore.getInstance().getPreferences().getProgramArguments();
	}

	@Override
	protected void setCustomArguments(String args) {
		GradleCore.getInstance().getPreferences().setProgramArguments(args);
	}

}
