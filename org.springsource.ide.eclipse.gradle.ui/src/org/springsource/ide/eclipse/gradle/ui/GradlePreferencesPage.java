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


import java.util.ArrayList;
import java.util.List;

/**
 * @author Kris De Volder
 */
public class GradlePreferencesPage extends PreferencePageWithSections {

	public GradlePreferencesPage() {
	}
	public static final String ID = "org.springsource.ide.eclipse.gradle.PreferencesPage";
	
	@Override
	protected List<PrefsPageSection> createSections() {
		List<PrefsPageSection> sections = new ArrayList<PrefsPageSection>();
		sections.add(new EnableUnderliningSection(this));
		sections.add(new DistributionSection(this));
		sections.add(new GradleUserHomeSection(this));
		sections.add(new DependencyManagementSection(this));
		//JavaHomeSection moved to arguments page.
		//sections.add(new JavaHomeSection(this, GradleCore.getInstance().getPreferences()));
		return sections;
	}

//	public GradlePreferencesPage(String title) {
//		super(title);
//	}
//
//	public GradlePreferencesPage(String title, ImageDescriptor image) {
//		super(title, image);
//	}


}
