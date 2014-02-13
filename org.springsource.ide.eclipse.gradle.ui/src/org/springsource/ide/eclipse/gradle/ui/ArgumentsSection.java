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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * This abstract class factors out the common functionality between "JVMArgumentsSection" and "ProgramArgumentsSection".
 * Both of these are rather similar except for where they store the arguments.
 * 
 * @author Kris De Volder
 */
public abstract class ArgumentsSection extends PrefsPageSection implements ArgumentsValidatorContext {

	public ArgumentsSection(IPageWithSections owner) {
		super(owner);
	}

	private ArgumentsValidator validator = new ArgumentsValidator(this);
	
	//////// alternative 1: Use wrapper default ///////////////////////
	private Button defaultButton;

	//////// alternative 2: Specify a list of arguments ///////////////////////
	private Button customArgsButton;
	private Text customArgsText; //It might be nicer to make this some kind of list or table

	///////////////////////////////////////////////////////////////////////////////////
	//Stuff that subclasses must implement

	protected abstract String getLabelText();
	protected abstract String getLabelTooltipText();
	
	/**
	 * Subclass implements this method to read the arguments from the correct preference.
	 * @return An 'arguments' String or null. Null means 'use default'.
	 */
	protected abstract String getCustomArguments();
	
	/**
	 * Subclass implements this method to set the arguments into the correct preference.
	 * @return An 'arguments' String or null. Null means 'use default'.
	 */
	protected abstract void setCustomArguments(String args);
	
	/////////////////////////////////////////////////////////////////////////////////
	// the stuff that is shared between the subclasses 
	
	public void createContents(Composite page) {
        GridDataFactory grabBoth = GridDataFactory.fillDefaults().grab(true, true);
		Label label = new Label(page, SWT.NONE);
		label.setText(getLabelText());
		label.setToolTipText(getLabelTooltipText());

		//Alternative 1
		
        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);
        grabBoth.applyTo(composite);
        
		defaultButton = new Button(composite, SWT.RADIO); 
		defaultButton.setText("Use Gradle wrapper's default");
		GridDataFactory gdf = GridDataFactory.fillDefaults().span(3, 1);
		gdf.applyTo(defaultButton);

        //Alternative 2: choose a workspace JRE
        
		customArgsButton = new Button(composite, SWT.RADIO);
		customArgsButton.setText("Use: ");
		gdf = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING);
		gdf.applyTo(customArgsButton);
        
		customArgsText = new Text(composite,  SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
		gdf = GridDataFactory.fillDefaults().span(2, 1).grab(true, true);
		gdf.applyTo(customArgsText);
		customArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
		
        customArgsButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
				validator.refresh();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
        setCustomArgumentsInPage(getCustomArguments());
        enableDisableWidgets();
	}
	
	
	private void enableDisableWidgets() {
		enableDisableWidgets(customArgsButton, customArgsText);
	}
	
	/**
	 * Enable/disable a number of controls depending on whether given radio button is enabled/disabled.
	 */
	public void enableDisableWidgets(Button radio, Control... controlls) {
		boolean enable = radio.getSelection();
		for (Control control : controlls) {
			control.setEnabled(enable);
		}
	}
	
	public boolean performOK() {
		setCustomArguments(getCustomArgumentsInPage());
		return true;
	}

	private void setCustomArgumentsInPage(String customArguments) {
		boolean isDefault = customArguments==null;
		customArgsButton.setSelection(!isDefault);
		defaultButton.setSelection(isDefault);
		if (customArguments!=null) {
			customArgsText.setText(customArguments);
		}
	}

	public String getCustomArgumentsInPage() {
		if (customArgsButton.getSelection()) {
			return customArgsText.getText();
		} else {
			return null;
		}
	}
	
	public void performDefaults() {
		setCustomArgumentsInPage(null);
	}
	
	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}
}
