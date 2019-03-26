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

import org.springsource.ide.eclipse.gradle.ui.util.PageSection;


/**
 * Abstract base class for implementing 'sections' containing a group of widgets that can be added to the Gradle preferences page.
 * 
 * @author Kris De Volder
 */
public abstract class PrefsPageSection extends PageSection {
	
	public PrefsPageSection(IPageWithSections owner) {
		super(owner);
	}

	public abstract boolean performOK();
	public abstract void performDefaults();
	
}
