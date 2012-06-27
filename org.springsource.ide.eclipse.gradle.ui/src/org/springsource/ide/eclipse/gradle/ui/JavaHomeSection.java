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
	private Button browseHomeButton;
	
	private JavaRuntimeUtils jres = new JavaRuntimeUtils();
	
	public JavaHomeSection(PreferencePageWithSections owner) {
		super(owner);
		validator = new JavaHomeValidator(this);
	}
	
	public void createContents(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		Label label = new Label(page, SWT.NONE);
		label.setText("Java Home (requires Gradle 1.0.RC1 or later)");

		//Alternative 1
		
        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);
        grabHorizontal.applyTo(composite);
        
		defaultButton = new Button(composite, SWT.RADIO); 
		defaultButton.setText("Use Gradle wrapper's default");
		GridDataFactory span = GridDataFactory.fillDefaults().span(3, 1);
		span.applyTo(defaultButton);

        //Alternative 2: choose a workspace JRE
        
		customHomeButton = new Button(composite, SWT.RADIO);
		customHomeButton.setText("Workspace JRE: ");
		customHomeButton.setToolTipText("Use a specific Java installation configured in this workspace");
        
        customJRECombo = new Combo(composite, SWT.DROP_DOWN+SWT.READ_ONLY);
        refreshJREs();
        
		browseHomeButton = new Button(composite, SWT.PUSH);
		browseHomeButton.setText("Configure JREs");
		
        customHomeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
                
        browseHomeButton.addSelectionListener(new SelectionAdapter() {
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
				
//				System.out.println("kdvolder");
				
//				PreferenceDialog w = PreferencesUtil.createPreferenceDialogOn(owner.getShell(), JRE_PREF_PAGE_ID, new String[] {JRE_PREF_PAGE_ID, GradlePreferencesPage.ID}, null);
//				w.setBlockOnOpen(true);
//				w.open();
//				//Eclipse only allows one preferences dialog to be open at the same time
//				//We only get here after user closed dialog, so we must reopen it on the Gradle preferences page.
//				PreferencesUtil.createPreferenceDialogOn(null, GradlePreferencesPage.ID, new String[] {JRE_PREF_PAGE_ID, GradlePreferencesPage.ID}, null).open();
			}

		});
        
        customJRECombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
        
        grabHorizontal.applyTo(customJRECombo);

        setJavaHomeInPage(getJavaHome());
        enableDisableWidgets();
	}
	
	private void setJavaHome(IVMInstall distro) {
		GradleCore.getInstance().getPreferences().setJavaHomeJRE(distro);
	}

	private void enableDisableWidgets() {
		enableDisableWidgets(customHomeButton, customJRECombo, browseHomeButton);
	}

	private void refreshJREs() {
		jres = new JavaRuntimeUtils();
        customJRECombo.setItems(jres.getWorkspaceJVMNames());
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
	
	private void setJavaHomeInPage(IVMInstall install) {
		if (install==null) {
			defaultButton.setSelection(true);
			customHomeButton.setSelection(false);
		} else {
			String name = install.getName();
			customHomeButton.setSelection(true);
			defaultButton.setSelection(false);
			customJRECombo.setText(name);
		}
	}

	private IVMInstall getJavaHome() {
		return GradleCore.getInstance().getPreferences().getJavaHomeJRE();
	}

	public boolean performOK() {
		setJavaHome(getJavaHomeInPage());
		return true;
	}

	public void performDefaults() {
		setJavaHomeInPage(null);
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
	
	private IVMInstall getJavaHomeInPage() {
		String name = getJavaHomeJRENameInPage();
		if (name!=null) {
			return jres.getInstall(name);
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