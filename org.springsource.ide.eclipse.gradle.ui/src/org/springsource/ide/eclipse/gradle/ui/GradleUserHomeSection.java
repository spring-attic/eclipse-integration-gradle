/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.core.validators.Validator;

/**
 * @author Kris De Volder
 */
public class GradleUserHomeSection extends PrefsPageSection {
	
	private Button defaultButton;
	private Button customButton;
	private Text customDirectoryText;
	private Button browseButton;
	
	//TODO: implement real validation logic.
	private LiveExpression<ValidationResult> validator = Validator.constant(ValidationResult.OK);

	public GradleUserHomeSection(PreferencePageWithSections owner) {
		super(owner);
	}

	@Override
	public boolean performOK() {
		setGradleUserHome(getGradleUserHomeInPage());
		return true;
	}

	@Override
	public void performDefaults() {
		setGradleUserHomeInPage(null);
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	@Override
	public void createContents(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		Label label = new Label(page, SWT.NONE);
		label.setText("Gradle User Home (.gradle folder location)");

        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);
		
		//Alternative 1
		
		defaultButton = new Button(composite, SWT.RADIO); 
		defaultButton.setText("Use Gradle wrapper's default");

		//Alternative 2
		
		customButton = new Button(composite, SWT.RADIO);
		customButton.setText("Directory: ");
		//customButton.setToolTipText("Yada yada yada");
		
		GridDataFactory span = GridDataFactory.fillDefaults().span(3, 1);
		span.applyTo(defaultButton);
				
        customDirectoryText = new Text(composite, SWT.BORDER);
        browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse");
		
        customButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
        browseButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				//Button clicked
				File file = openFolderDialog();
				if (file!=null) {
					customDirectoryText.setText(file.toString());
				}
				
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
		customDirectoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
        
        grabHorizontal.applyTo(customDirectoryText);
        grabHorizontal.applyTo(composite);

        setGradleUserHomeInPage(getGradleUserHome());
        enableDisableWidgets();
	}
	
	//////////////////////////////////////////
	
	private File getGradleUserHomeInPage() {
		if (customButton.getSelection()) {
			String str = customDirectoryText.getText().trim();
			if (!"".equals(str)) {
				return new File(str);
			}
		} 
		return null;
	}

	private void setGradleUserHomeInPage(File dir) {
		if (dir==null) {
			defaultButton.setSelection(true);
			customButton.setSelection(false);
		} else {
			defaultButton.setSelection(false);
			customButton.setSelection(true);
			customDirectoryText.setText(dir.toString());
		}
	}
	
	private void setGradleUserHome(File loc) {
		GradleCore.getInstance().getPreferences().setGradleUserHome(loc);
	}

	private File getGradleUserHome() {
		return GradleCore.getInstance().getPreferences().getGradleUserHome();
	}
	
	private void enableDisableWidgets() {
		enableDisableWidgets(customButton, customDirectoryText, browseButton);
		validator.refresh();
	}

	public void enableDisableWidgets(Button radio, Text text, Button browse) {
		boolean enable = radio.getSelection();
		text.setEnabled(enable);
		browse.setEnabled(enable);
	}
	
	private File openFolderDialog() {
		DirectoryDialog fileDialog = new DirectoryDialog(owner.getShell(),  SWT.OPEN);
		String file = fileDialog.open();
		if (file!=null) {
			return new File(file);
		}
		return null;
	}
	
}
