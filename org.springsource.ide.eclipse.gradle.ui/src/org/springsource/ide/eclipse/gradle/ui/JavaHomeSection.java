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

import java.net.URISyntaxException;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.util.JavaRuntimeUtils;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.JavaHomeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.JavaHomeValidatorContext;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * Section on a preferences page that allows user to pick a specific Gradle distribution.
 * 
 * @author Kris De Volder
 */
public class JavaHomeSection extends PrefsPageSection implements JavaHomeValidatorContext {
	
	protected static final String JRE_PREF_PAGE_ID = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage";

	private LiveExpression<ValidationResult> validator;
	
	//////// alternative 1: Use wrapper default ///////////////////////
	private Button defaultButton;

	//////// alternative 2: Local JVM install folder ///////////////////////
	private Button customHomeButton;
	private Combo customJRECombo;
	private Button configureJREsButton;
	
	/////// alternative 3: Execution environment ///////////////////////////
	private Button customExecutionEnvButton;
	private Combo customExecutionEnvCombo;
	private Button configureExecEnvsButton;
	
	////////////
	
	private JavaRuntimeUtils jres = new JavaRuntimeUtils();

	
	public JavaHomeSection(PreferencePageWithSections owner) {
		super(owner);
		validator = new JavaHomeValidator(this);
	}
	
	public void createContents(Composite page) {
        GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
		Label label = new Label(page, SWT.NONE);
		label.setText("Java Home (requires Gradle 1.0.RC1 or later)");

		//Alternative 1
		
        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);
        grabHor.applyTo(composite);
        
		defaultButton = new Button(composite, SWT.RADIO); 
		defaultButton.setText("Use Gradle wrapper's default");
		GridDataFactory span = GridDataFactory.fillDefaults().span(3, 1);
		span.applyTo(defaultButton);

        //Alternative 2: choose a workspace JRE
        
		customHomeButton = new Button(composite, SWT.RADIO);
		customHomeButton.setText("Workspace JRE: ");
		customHomeButton.setToolTipText("Use a specific Java installation configured in this workspace");
        customJRECombo = new Combo(composite, SWT.DROP_DOWN+SWT.READ_ONLY);
		configureJREsButton = new Button(composite, SWT.PUSH);
		configureJREsButton.setText("Configure JREs");
		
		grabHor.applyTo(configureJREsButton);
		grabHor.applyTo(customJRECombo);
		
		//Alternative 3: choose an execution environment
		customExecutionEnvButton = new Button(composite, SWT.RADIO);
		customExecutionEnvButton.setText("Execution Environment");
		customExecutionEnvButton.setToolTipText("Specify a JRE indirectly via an execution environment");
		customExecutionEnvCombo = new Combo(composite, SWT.DROP_DOWN+SWT.READ_ONLY);
		configureExecEnvsButton = new Button(composite, SWT.PUSH);
		configureExecEnvsButton.setText("Configure EEs");

		grabHor.applyTo(configureExecEnvsButton);
		grabHor.applyTo(customExecutionEnvCombo);
		
        refreshJREs();

        customHomeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets(customHomeButton, customJRECombo, configureJREsButton);
				validator.refresh();
			}
		});
        customExecutionEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets(customExecutionEnvButton, 
						customExecutionEnvCombo, configureExecEnvsButton);
				validator.refresh();
			}
		});
                
        configureJREsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				@SuppressWarnings("restriction")
				IPreferencePage page = new org.eclipse.jdt.internal.debug.ui.jres.JREsPreferencePage();
				PreferenceManager mgr = new PreferenceManager();
				IPreferenceNode node = new PreferenceNode("1", page);
				mgr.addToRoot(node);
				PreferenceDialog dialog = new PreferenceDialog(owner.getShell(), mgr);
				dialog.create();
				dialog.setMessage(page.getTitle());
				dialog.open();
				
				refreshJREs();
			}
		});
        
        configureExecEnvsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				@SuppressWarnings("restriction")
				IPreferencePage page = new org.eclipse.jdt.internal.debug.ui.jres.ExecutionEnvironmentsPreferencePage();
				PreferenceManager mgr = new PreferenceManager();
				IPreferenceNode node = new PreferenceNode("1", page);
				mgr.addToRoot(node);
				PreferenceDialog dialog = new PreferenceDialog(owner.getShell(), mgr);
				dialog.create();
				dialog.setMessage(page.getTitle());
				dialog.open();
				
				refreshJREs();
			}

		});
        
        customJRECombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
        
        customExecutionEnvCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
        
        grabHor.applyTo(customJRECombo);

        copyFrom(getPreferences());
        
        enableDisableWidgets();
	}

	/**
	 * Fill the widget contents based on GradlePreferences
	 */
	private void copyFrom(GradlePreferences preferences) {
		Button enable = defaultButton;
		String jreName = preferences.getJavaHomeJREName();
		if (jreName!=null) {
			customJRECombo.setText(jreName);
			if (customJRECombo.getText().equals(jreName)) {
				enable = customHomeButton;
			}
		}
		String eeName = preferences.getJavaHomeEEName();
		if (eeName!=null) {
			customExecutionEnvCombo.setText(eeName);
			if (customExecutionEnvCombo.getText().equals(eeName)) {
				enable = customExecutionEnvButton;
			}
		}
		for (Button b : new Button[] {defaultButton, customHomeButton, customExecutionEnvButton}) {
			b.setSelection(b==enable);
		}
	}

	private void enableDisableWidgets() {
		enableDisableWidgets(customHomeButton, customJRECombo, configureJREsButton);
		enableDisableWidgets(customExecutionEnvButton, customExecutionEnvCombo, configureExecEnvsButton);
	}

	private void refreshJREs() {
		jres = new JavaRuntimeUtils();
		setItems(customJRECombo, jres.getWorkspaceJVMNames());
        setItems(customExecutionEnvCombo, jres.getExecutionEnvNames());
	}
	
	
	private static void setItems(Combo combo, String[] items) {
		String oldSelection = combo.getText();
		combo.setItems(items);
		combo.setText(oldSelection); //Otherwise the selection gets cleared which is annoying.
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
		GradlePreferences prefs = getPreferences();
		prefs.setJavaHomeJREName(getJavaHomeJRENameInPage());
		prefs.setJavaHomeEEName(getExecutionEnvNameInPage());
		return true;
	}

	private GradlePreferences getPreferences() {
		return GradleCore.getInstance().getPreferences();
	}

	public void performDefaults() {
		defaultButton.setSelection(true);
		customExecutionEnvButton.setSelection(false);
		customHomeButton.setSelection(false);
	}

	public String getJavaHomeJRENameInPage() {
		if (customHomeButton.getSelection()) {
			String name = customJRECombo.getText();
			if (!"".equals(name)) {
				return name;
			}
		}
		return null;
	}
	
	public String getExecutionEnvNameInPage() {
		if (customExecutionEnvButton.getSelection()) {
			String name = customExecutionEnvCombo.getText();
			if (!"".equals(name)) {
				return name;
			}
		}
		return null;
	}
	
	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	public JavaRuntimeUtils getJREUtils() {
		return jres;
	}

}