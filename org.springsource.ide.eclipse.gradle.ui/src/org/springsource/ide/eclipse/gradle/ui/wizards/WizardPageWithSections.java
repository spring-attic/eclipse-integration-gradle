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
package org.springsource.ide.eclipse.gradle.ui.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.ui.IPageWithSections;
import org.springsource.ide.eclipse.gradle.ui.PrefsPageSection;
import org.springsource.ide.eclipse.gradle.ui.util.PageSection;


/**
 * @author Kris De Volder
 */
public abstract class WizardPageWithSections extends WizardPage implements IPageWithSections, ValueListener<ValidationResult> {

	protected WizardPageWithSections(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	private List<WizardPageSection> sections = null;
	private CompositeValidator validator;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
        validator = new CompositeValidator();
        for (PageSection section : getSections()) {
			section.createContents(page);
			validator.addChild(section.getValidator());
		}
        validator.addListener(this);
        setControl(page);
        getContainer().updateButtons();
        getContainer().updateMessage();
	}
	
	protected synchronized List<WizardPageSection> getSections() {
		if (sections==null) {
			sections = createSections();
		}
		return sections;
	}
	
//	@Override
//	public boolean isPageComplete() {
//		if (validator!=null) {
//			return validator.getValue().isOk();
//		}
//		return false;
//	}
	
	/**
	 * This method should be implemented to generate the contents of the page.
	 */
	protected abstract List<WizardPageSection> createSections();
	
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
		setPageComplete(status.isOk());
	}

}
