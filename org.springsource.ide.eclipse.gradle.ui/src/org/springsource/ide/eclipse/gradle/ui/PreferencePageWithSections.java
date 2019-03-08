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
package org.springsource.ide.eclipse.gradle.ui;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


//TODO: copied to commons livexp. Remove this from gradle bundles.

/**
 * An abstract super class to more easily create preferences pages composed of modular sections.
 * Each section encapsulates its own widgetry and validation logic.
 * 
 * @author Kris De Volder
 */
public abstract class PreferencePageWithSections extends PreferencePage implements IWorkbenchPreferencePage, ValueListener<ValidationResult>, IPageWithSections {

	private List<PrefsPageSection> sections = null;

	public PreferencePageWithSections() {
	}

	public void init(IWorkbench workbench) {
	}

	/**
	 * This method should be implemented to generate the contents of the page.
	 */
	protected abstract List<PrefsPageSection> createSections();

	@Override
	protected Control createContents(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
        CompositeValidator validator = new CompositeValidator();
        for (PrefsPageSection section : getSections()) {
			section.createContents(page);
			validator.addChild(section.getValidator());
		}
        validator.addListener(this);
        return page;
	}
	
	@Override
	public boolean performOk() {
		for (PrefsPageSection section : getSections()) {
			boolean ok = section.performOK();
			if (!ok) {
				return false;
			}
		}
		//We reach here only when all sections performOK returned true 
		return true;
	}
	
	private synchronized List<PrefsPageSection> getSections() {
		if (sections==null) {
			sections = createSections();
		}
		return sections;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		for (PrefsPageSection section : getSections()) {
			section.performDefaults();
		}
	}

	public void gotValue(LiveExpression<ValidationResult> exp, ValidationResult status) {
		setErrorMessage(null);
		setMessage(null);
		if (status.isOk()) {
		} else if (status.status == IStatus.ERROR) {
			setErrorMessage(status.msg);
		} else if (status.status == IStatus.WARNING) {
			setMessage(status.msg, IMessageProvider.WARNING);
		} else if (status.status == IStatus.INFO) {
			setMessage(status.msg, IMessageProvider.INFORMATION);
		} else {
			setMessage(status.msg, IMessageProvider.NONE);
		}
	}
	
}
