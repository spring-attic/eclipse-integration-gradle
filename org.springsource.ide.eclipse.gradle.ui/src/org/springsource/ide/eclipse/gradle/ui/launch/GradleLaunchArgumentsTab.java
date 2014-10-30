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
package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.debug.ui.JavaDebugImages;
import org.eclipse.swt.graphics.Image;

/**
 * A launch configuration tab that displays and edits program arguments,
 * and VM arguments for the Gradle task launch UI.
 */
@SuppressWarnings("restriction")
public class GradleLaunchArgumentsTab extends LaunchTabWithSections {
	
	protected List<LaunchTabSection> createSections() {
		ArrayList<LaunchTabSection> sections = new ArrayList<LaunchTabSection>();
		sections.add(new JavaHomeLaunchTabSection(this));
		sections.add(new JVMArgumentsLaunchTabSection(this));
		sections.add(new ProgramArgumentsLaunchTabSection(this));
		return sections;
	}
		
	public GradleLaunchArgumentsTab() {
		super();
	}

	public String getName() {
		return "Arguments"; 
	}	
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return JavaDebugImages.get(JavaDebugImages.IMG_VIEW_ARGUMENTS_TAB);
	}	
}

