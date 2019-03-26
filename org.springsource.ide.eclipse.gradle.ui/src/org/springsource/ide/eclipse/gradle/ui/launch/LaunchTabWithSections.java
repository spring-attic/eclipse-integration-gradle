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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.ui.IPageWithSections;


/**
 * Abstract superclass that provides most of what is needed to easily create a 
 * 'launch Tab' composed of PageSections (which can also be used on preferences pages).
 * 
 * @author Kris De Volder
 */
public abstract class LaunchTabWithSections extends AbstractLaunchConfigurationTab 
implements IPageWithSections, ValueListener<ValidationResult> {

	private List<LaunchTabSection> sections = null;
	private CompositeValidator validator;
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
//        layout.marginHeight = 1;
//        layout.marginWidth = 1;
        page.setLayout(layout);
        validator = new CompositeValidator();
        for (LaunchTabSection section : getSections()) {
			section.createControl(page);
			validator.addChild(section.getValidator());
		}
        validator.addListener(this);
		setControl(page);
	}
	
	private synchronized List<LaunchTabSection> getSections() {
		if (sections==null) {
			sections = createSections();
		}
		return sections;
	}

	protected abstract List<LaunchTabSection> createSections();
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		if (validator!=null) {
			return validator.getValue().isOk();
		}
		return true;
	}

	/**
	 * Defaults are empty.
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		for (LaunchTabSection section : getSections()) {
			section.setDefaults(config);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		for (LaunchTabSection section : getSections()) {
			section.initializeFrom(configuration);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		for (LaunchTabSection section : getSections()) {
			section.performApply(configuration);
		}
	}
	
//	/**
//	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
//	 */
//	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
//		super.setLaunchConfigurationDialog(dialog);
//		for (LaunchTabSection section : getSections()) {
//			section.setLaunchConfigurationDialog(dialog);
//		}
//	}	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		String m = super.getErrorMessage();
		return m;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		String m = super.getMessage();
		return m;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		initializeFrom(workingCopy);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		performApply(workingCopy);
	}
	
	////////// IPageWithSections methods //////////////////////////////////////////
	
	@Override
	public Shell getShell() {
		return super.getShell();
	}
	
	//////// Live expression value listener ////////////////////////////////////////

	/**
	 * This method receives status updates obtained by aggregating all the validators on this page. 
	 */
	public void gotValue(LiveExpression<ValidationResult> exp, ValidationResult status) {
		setErrorMessage(null);
		setMessage(null);
		if (status.isOk()) {
		} else if (status.status == IStatus.ERROR) {
			setErrorMessage(status.msg);
		} else {
			//Superclass only seems to have support for two kinds of message 'error' and 'other'. We ignore distinctions
			// so we ignore different status codes other than error.
			setMessage(status.msg);
		}
//		if (status.status == IStatus.WARNING) {
//			setMessage(status.msg, IMessageProvider.WARNING);
//		} else if (status.status == IStatus.INFO) {
//			setMessage(status.msg, IMessageProvider.INFORMATION);
//		} else {
//			setMessage(status.msg, IMessageProvider.NONE);
//		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#scheduleUpdateJob()
	 */
	@Override
	public void scheduleUpdateJob() {
		super.scheduleUpdateJob();
	}

}
