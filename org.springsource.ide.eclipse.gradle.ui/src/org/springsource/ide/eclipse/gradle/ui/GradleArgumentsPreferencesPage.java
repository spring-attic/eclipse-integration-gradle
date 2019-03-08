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
package org.springsource.ide.eclipse.gradle.ui;

import java.util.ArrayList;
import java.util.List;

public class GradleArgumentsPreferencesPage extends PreferencePageWithSections {

	public GradleArgumentsPreferencesPage() {
	}

	@Override
	protected List<PrefsPageSection> createSections() {
		List<PrefsPageSection> sections = new ArrayList<PrefsPageSection>();
		sections.add(new JavaHomeSection(this));
		sections.add(new JVMArgumentsSection(this));
		sections.add(new ProgramArgumentsSection(this));
		return sections;
	}

}
