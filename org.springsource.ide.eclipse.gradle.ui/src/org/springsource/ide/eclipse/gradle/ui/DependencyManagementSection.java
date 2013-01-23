/*******************************************************************************
 * Copyright (c) 2013 VMWare, Inc.
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;

/**
 * @author Kris De Volder
 */
public class DependencyManagementSection extends PrefsPageSection {
	
	LiveExpression<ValidationResult> validator = new CompositeValidator();
	
	Text autoRefreshDelayText;
	Button enableAutoRefreshButton;
	
	public DependencyManagementSection(GradlePreferencesPage owner) {
		super(owner);
	}

	@Override
	public boolean performOK() {
		setEnableAutoRefresh(getEnableAutoRefreshInPage());
		setAutoRefreshDelay(getAutoRefreshDelayInPage());
		return true;
	}

	@Override
	public void performDefaults() {
		setEnableAutoRefreshInPage(GradlePreferences.DEFAULT_AUTO_REFRESH_DEPENDENCIES);
		setAutoRefreshDelayInPage(GradlePreferences.DEFAULT_AUTO_REFRESH_DELAY);
		// TODO Auto-generated method stub

	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	@Override
	public void createContents(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		Label label = new Label(page, SWT.NONE);
		label.setText("Dependency Management");

        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);
		
		//Enable auto refresh checkbox
        
        enableAutoRefreshButton = new Button(composite, SWT.CHECK);
        enableAutoRefreshButton.setText("Enable automatic refresh. Delay (ms) : ");
        enableAutoRefreshButton.setToolTipText("Automatically refresh 'Gradle Depencies' when any .gradle file is changed");
//		GridDataFactory span2 = GridDataFactory.fillDefaults().span(2, 1);
//		span2.applyTo(enableAutoRefreshButton);
        enableAutoRefreshButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}
        });

		setEnableAutoRefreshInPage(getEnableAutoRefresh());

		//Refresh delay text widget
		autoRefreshDelayText = new Text(composite, SWT.BORDER);
		autoRefreshDelayText.setToolTipText("Delay between change event and triggered auto refresh.");
		grabHorizontal.applyTo(composite);
		grabHorizontal.applyTo(autoRefreshDelayText);
		
		setAutoRefreshDelayInPage(getAutoRefreshDelay());
		enableDisableWidgets();
	}

	private void enableDisableWidgets() {
		enableDisableWidgets(enableAutoRefreshButton, autoRefreshDelayText);
	}

	public void enableDisableWidgets(Button radio, Control... others) {
		boolean enable = radio.getSelection();
		for (Control widget : others) {
			widget.setEnabled(enable);
		}
	}
	
	////////////////// 'in page' getters and setters //////////////////////////
	
	public boolean getEnableAutoRefreshInPage() {
		if (enableAutoRefreshButton!=null) {
			return enableAutoRefreshButton.getSelection();
		}
		return GradlePreferences.DEFAULT_AUTO_REFRESH_DEPENDENCIES;
	}

	public void setEnableAutoRefreshInPage(boolean e) {
		enableAutoRefreshButton.setSelection(e);
	}

	private void setAutoRefreshDelayInPage(int v) {
		autoRefreshDelayText.setText(""+v);
	}
	
	private int getAutoRefreshDelayInPage() {
		if (autoRefreshDelayText!=null) {
			try {
				return Integer.parseInt(autoRefreshDelayText.getText());
			} catch (NumberFormatException e) {
			}
		}
		return GradlePreferences.DEFAULT_AUTO_REFRESH_DELAY;
	}

	///////////// preferences getters and setters /////////////////////////////////

	private boolean getEnableAutoRefresh() {
		return GradleCore.getInstance().getPreferences().isAutoRefreshDependencies();
	}
	
	private int getAutoRefreshDelay() {
		return GradleCore.getInstance().getPreferences().getAutoRefreshDelay();
	}
	
	private void setEnableAutoRefresh(boolean e) {
		GradleCore.getInstance().getPreferences().setAutoRefreshDependencies(e);
	}

	private void setAutoRefreshDelay(int v) {
		GradleCore.getInstance().getPreferences().setAutoRefreshDelay(v);
	}



}
