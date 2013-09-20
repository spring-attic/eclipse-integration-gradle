/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.wtp.DeploymentExclusions;
import org.springsource.ide.eclipse.gradle.core.wtp.RegexpListDeploymentExclusions;
import org.springsource.ide.eclipse.gradle.core.wtp.WTPUtil;


/**
 * @author Kris De Volder
 */
public class WTPPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private Text deploymentExclusionsText;
	private Object newline;

	public WTPPreferencesPage() {
	}

	public WTPPreferencesPage(String title) {
		super(title);
	}

	public WTPPreferencesPage(String title, ImageDescriptor image) {
		super(title, image);
		
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
        GridDataFactory grabBoth = GridDataFactory.fillDefaults().grab(true, true);
		
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
       
        grabBoth.applyTo(page);
        
        if (!WTPUtil.isInstalled()) { 
        	Label label = new Label(page, SWT.NONE);
        	label.setText("WTP is not installed");

        } else {
        	Label label = new Label(page, SWT.NONE);
        	label.setText("Gradle Dependencies Deployment Exclusions");
        	label.setToolTipText("Define a set of specific jars that should be excluded from deployment.");

        	deploymentExclusionsText = new Text(page, SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
        	deploymentExclusionsText.setToolTipText("Enter regular expressions, one on each line. Empty lines are ignored");
        	grabBoth.applyTo(deploymentExclusionsText);
        	newline = deploymentExclusionsText.getLineDelimiter();
        	setDeploymentExclusionsInPage(getDeploymentExclusions());
        }
        return page;
	}
	
	private void setDeploymentExclusionsInPage(RegexpListDeploymentExclusions deploymentExclusions) {
		String[] sourceExps = deploymentExclusions.getSourceExps();
		StringBuilder text = new StringBuilder();
		boolean first = true;
		for (String exp : sourceExps) {
			if (!first) {
				text.append(newline);
			}
			text.append(exp);
			first = false;
		}
		deploymentExclusionsText.setText(text.toString());
	}

	private RegexpListDeploymentExclusions getDeploymentExclusions() {
		return GradleCore.getInstance().getPreferences().getDeploymentExclusions();
	}

	@Override
	public boolean performOk() {
		if (WTPUtil.isInstalled()) {
			try {
				setDeploymentExclusions(getDeploymentExclusionsInPage());
				return true;
			} catch (PatternSyntaxException e) {
				setErrorMessage(e.getMessage());
				GradleCore.log(e);
				return false;
			}
		} else {
			return true;
		}
	}
	
	private void setDeploymentExclusions(RegexpListDeploymentExclusions exclusions) {
		GradleCore.getInstance().getPreferences().setDeploymentExclusions(exclusions);
	}

	private RegexpListDeploymentExclusions getDeploymentExclusionsInPage() {
		String text = deploymentExclusionsText.getText();
		String[] regexps = text.split("\n");
		List<String> nonEmptyRegexps = new ArrayList<String>(regexps.length);
		for (String exp : regexps) {
			exp = exp.trim();
			if (!"".equals(exp)) {
				nonEmptyRegexps.add(exp);
			}
		}
		return new RegexpListDeploymentExclusions(nonEmptyRegexps);
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		setDeploymentExclusionsInPage(DeploymentExclusions.getDefault());
	}

}
