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
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.ui.ArgumentsValidator;
import org.springsource.ide.eclipse.gradle.ui.ArgumentsValidatorContext;


/**
 * @author Kris De Volder
 */
public abstract class ArgumentsLaunchTabSection extends LaunchTabSection implements ArgumentsValidatorContext {

	//TODO: The code in here is awfully similar to that in ArgumentsSection. But unfortunately it is hard
	// to factor because the launch tabs allow switching between launch configs whereas a preferences
	// pages is fixed to a specific prefs store upon creation. Thus they follow very different protocols
	// re initialization and reading/storing values into the the 'backing store'.
	
	private LiveExpression<ValidationResult> validator = new ArgumentsValidator(this);
	protected final LaunchTabWithSections owner;
	
	public ArgumentsLaunchTabSection(LaunchTabWithSections owner) {
		this.owner = owner;
	}
	
	//////// alternative 1: Use wrapper default ///////////////////////
	private Button defaultButton;

	//////// alternative 2: Specify a list of arguments ///////////////////////
	private Button customArgsButton;
	private Text customArgsText; //It might be nicer to make this some kind of list or table
	
	///////////////////////////////////////////////////////////////////////////////////
	//Stuff that subclasses must implement

	protected abstract String getLabelText();
	protected abstract String getLabelTooltipText();
	protected abstract void setCustomArguments(ILaunchConfigurationWorkingCopy conf, String args);
	protected abstract String getCustomArguments(ILaunchConfiguration conf);

	/////////////////////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.launch.LaunchTabSection#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite page) {
        GridDataFactory grabBoth = GridDataFactory.fillDefaults().grab(true, true);
		Group group = new Group(page, SWT.BORDER);
		group.setText(getLabelText());
		group.setToolTipText(getLabelTooltipText());

		//Alternative 1
		
//        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        group.setLayout(layout);
        grabBoth.applyTo(group);
        
		defaultButton = new Button(group, SWT.RADIO); 
		defaultButton.setText("Use default");
		GridDataFactory gdf = GridDataFactory.fillDefaults().span(3, 1);
		gdf.applyTo(defaultButton);
		defaultButton.setToolTipText("The default is determined as follows: First the Global preference page is checked. " +
				"If a value is not set there then the Gradle Wrapper is left to choose its own default.");

        //Alternative 2: enter a string
        
		customArgsButton = new Button(group, SWT.RADIO);
		customArgsButton.setText("Use: ");
		gdf = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING);
		gdf.applyTo(customArgsButton);
        
		customArgsText = new Text(group,  SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
		gdf = GridDataFactory.fillDefaults().span(2, 1).grab(true, true);
		gdf.applyTo(customArgsText);
		customArgsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
				owner.scheduleUpdateJob();
			}
		});
		
        customArgsButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
				validator.refresh();
				owner.scheduleUpdateJob();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
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
	
	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.launch.LaunchTabSection#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		setCustomArguments(configuration, getCustomArgumentsInPage());
	}

	private void setCustomArgumentsInPage(String customArguments) {
		boolean isDefault = customArguments==null;
		customArgsButton.setSelection(!isDefault);
		defaultButton.setSelection(isDefault);
		if (customArguments!=null) {
			customArgsText.setText(customArguments);
		}
		validator.refresh();
		enableDisableWidgets();
	}

	public String getCustomArgumentsInPage() {
		if (customArgsButton.getSelection()) {
			return customArgsText.getText();
		} else {
			return null;
		}
	}
	
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration conf) {
		setCustomArgumentsInPage(getCustomArguments(conf));
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}
}
