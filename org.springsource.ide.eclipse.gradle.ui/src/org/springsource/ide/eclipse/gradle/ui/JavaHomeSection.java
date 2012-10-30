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

import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.IJavaHomePreferences;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;

/**
 * Section on a preferences page that allows user to pick a specific Gradle distribution.
 * 
 * This class is now implemented as a kind of 'adapter' around a JavaHomeSectionImpl object.
 * The impl object contains the widgets and validation logic, but non of the wiring
 * To hook it up to an actual IJavaHomePreferences store where the preferences get stored.
 * 
 * @author Kris De Volder
 */
public class JavaHomeSection extends PrefsPageSection {
	
	private JavaHomeSectionImpl impl; //contains all the widgets validation logic etc.
	
	public JavaHomeSection(IPageWithSections owner) {
		super(owner);
		impl = new JavaHomeSectionImpl(owner, null);
	}
	
	@Override
	public boolean performOK() {
		impl.copyTo(getPreferences());
		return true;
	}

	private IJavaHomePreferences getPreferences() {
		return GradleCore.getInstance().getPreferences();
	}

	@Override
	public void performDefaults() {
		impl.setDefaults(impl);
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return impl.getValidator();
	}

	@Override
	public void createContents(Composite page) {
		impl.createWidgets(page);
		impl.copyFrom(getPreferences());
	}

}