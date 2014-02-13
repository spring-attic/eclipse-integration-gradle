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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.springsource.ide.eclipse.gradle.core.preferences.IJavaHomePreferences;
import org.springsource.ide.eclipse.gradle.core.util.JavaRuntimeUtils;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.JavaHomeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.JavaHomeValidatorContext;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;

/**
 * Section on a preferences page that allows user to pick a specific Gradle distribution.
 * <p>
 * Here we implement/extend neither PageWithSection nor LaunchTabSection because we 
 * need to be able to use it in the implementation of either of these two.
 * <p>
 * Essentially, this implementation provides the widgets, validation logic and
 * methods to copy preferences from/to the widgets to/from a IJavaHomePreferences object.
 * 
 * @author Kris De Volder
 */
public class JavaHomeSectionImpl implements JavaHomeValidatorContext, IJavaHomePreferences {
	
	private static final String TITLE = "Java Home (requires Gradle 1.0.RC1 or later)";

	public interface IWidgetRefreshListener {

		void widgetsRefreshed();

	}

	private IPageWithSections owner;
	private IWidgetRefreshListener widgetListener; // Clients that need to be able to respond to any changes in the widgets
	                               // to update some state can attach an instance via the constructor.
	
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
	
	private boolean border = false;

	public JavaHomeSectionImpl(IPageWithSections owner, IWidgetRefreshListener widgetListener) {
		this.owner = owner;
		this.validator = new JavaHomeValidator(this);
		this.widgetListener = widgetListener;
	}
	
	private void doRefresh() {
		validator.refresh();
		if (widgetListener!=null) {
			widgetListener.widgetsRefreshed();
		}
	}
	
	/**
	 * If set to true, a border will be create around the widgets.
	 * This method must be called before createWidgets.
	 */
	public JavaHomeSectionImpl setBorder(boolean enable) {
		this.border = enable;
		return this;
	}
	
	/**
	 * This does more or less what a PageSection implementation of createContents should do (i.e. 
	 * create the widgets on the page).
	 * <p>
	 * However, it does not fill the widget contents with the contents of the preferences because
	 * this implementation may not yet be connected to a preferences store yet when the widgets
	 * are being created. It is up to the caller/client to make sure a to call copyFrom method
	 * to (re)initialise the widget contents at an appropriate time. 
	 * 
	 * @param page
	 */
	public void createWidgets(Composite page) {
        GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
        Group group = null;
		if (!border) {
			Label label = new Label(page, SWT.NONE);
			label.setText(TITLE);
		} else {
			group = new Group(page, SWT.BORDER);
			group.setText(TITLE);
		}

		//Alternative 1
		
        Composite composite = border ? group : new Composite(page, SWT.NONE);
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
				doRefresh();
			}
		});
        customExecutionEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets(customExecutionEnvButton, 
						customExecutionEnvCombo, configureExecEnvsButton);
				doRefresh();
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
				doRefresh();
			}
		});
        
        customExecutionEnvCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doRefresh();
			}
		});
        
        grabHor.applyTo(customJRECombo);
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
	
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	public JavaRuntimeUtils getJREUtils() {
		return jres;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// The methods below are here so that JavaHomeSection instance can be more easily
	// adapted to be usable as a LaunchTabSection
	
	// Important: setter methods which directly put contents into widgets need to
	// call 'doRefresh' because the SWT widgets only seem to fire off listeners
	// when the changes are triggered by users, not when they are directly set by
	// calling methods on the widgets.
	
	/**
	 * Copy widget contents into some place where the preferences can be stored.
	 */
	public void copyTo(IJavaHomePreferences preferences) {
		if (defaultButton.getSelection()) {
			preferences.unsetJavaHome();
		} else if (customHomeButton.getSelection()) {
			preferences.setJavaHomeJREName(getText(customJRECombo));
		} else if (customExecutionEnvButton.getSelection()) {
			preferences.setJavaHomeEEName(getText(customExecutionEnvCombo));
		}
	}

	public void setDefaults(IJavaHomePreferences preferences) {
		preferences.unsetJavaHome();
	}

	/**
	 * Fill the widget contents based on GradlePreferences. This should only be called after
	 * the widgets have been created.
	 */
	public void copyFrom(IJavaHomePreferences preferences) {
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
		radioEnable(enable);
		doRefresh();
	}

	/**
	 * Enable one of the three radio buttons and also update all the corresponding other widgets
	 * enablement state accordingly.
	 */
	private void radioEnable(Button enable) {
		for (Button b : new Button[] {defaultButton, customHomeButton, customExecutionEnvButton}) {
			b.setSelection(b==enable);
		}
		enableDisableWidgets();
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	// implement IJavaHomePreferences /////////////////////////////////
	//
	// This implementation treats the widgets themselves as a place where we can 
	// store the preferences (this is used by 'setDefaults' in the context of a preferences page)

	public String getJavaHomeJREName() {
		return getText(customJRECombo);
	}

	private static String getText(Combo combo) {
		if (combo!=null) {
			String txt = combo.getText();
			if (!"".equals(txt)) {
				return txt;
			}
		}
		return null;
	}

	public String getJavaHomeEEName() {
		return getText(customExecutionEnvCombo);
	}

	public void setJavaHomeJREName(String name) {
		radioEnable(customHomeButton);
		customJRECombo.setText(name);
		doRefresh();
	}

	public void setJavaHomeEEName(String name) {
		radioEnable(customExecutionEnvButton);
		customExecutionEnvCombo.setText(name);
		doRefresh();
	}

	public void unsetJavaHome() {
		radioEnable(defaultButton);
		doRefresh();
	}

	private void enableDisableWidgets() {
		enableDisableWidgets(customHomeButton, customJRECombo, configureJREsButton);
		enableDisableWidgets(customExecutionEnvButton, customExecutionEnvCombo, configureExecEnvsButton);
	}

}
