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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * @author Kris De Volder
 */
public class EnableUnderliningSection extends PrefsPageSection {

	public EnableUnderliningSection(PreferencePageWithSections owner) {
		super(owner);
	}

    private Button disableUnderlining;
	
	@Override
	public void createContents(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);

		Label sectionTitle = new Label(page, SWT.NONE);
		sectionTitle.setText("Groovy Editor");
		Composite section = new Composite(page, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		section.setLayout(layout);
		grabHorizontal.applyTo(section);
		
        disableUnderlining = new Button(section, SWT.CHECK);
        disableUnderlining.setText("Disable underlining for .gradle files");
        disableUnderlining.setSelection(getDisableUnderlining());
 	}

	private boolean getDisableUnderlining() {
		return GradleCore.getInstance().getPreferences().getGroovyEditorDisableUnderlining();
	}
	
	private void setDisableUnderlining(boolean disable) {
		GradleCore.getInstance().getPreferences().setGroovyEditorDisableUnderlining(disable);
	}

	@Override
	public boolean performOK() {
		setDisableUnderlining(getDisableUnderliningInPage());
		return true;
	}

	@Override
	public void performDefaults() {
		setDisableUnderliningInPage(GradlePreferences.DEFAULT_DISABLE_UNDERLINING);
	}

	private void setDisableUnderliningInPage(boolean disable) {
		disableUnderlining.setSelection(disable);
	}

	private boolean getDisableUnderliningInPage() {
		return disableUnderlining.getSelection();
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return OK_VALIDATOR;
	}

}
