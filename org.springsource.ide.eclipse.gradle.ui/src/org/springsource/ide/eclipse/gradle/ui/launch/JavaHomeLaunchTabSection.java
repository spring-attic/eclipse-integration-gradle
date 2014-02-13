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
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.ui.JavaHomeSectionImpl;
import org.springsource.ide.eclipse.gradle.ui.JavaHomeSectionImpl.IWidgetRefreshListener;

/**
 * Adapts a JavaHomeSectionImpl to be usable as a LaunchTabSection.
 * 
 * @author Kris De Volder
 */
public class JavaHomeLaunchTabSection extends LaunchTabSection implements IWidgetRefreshListener {

	private JavaHomeSectionImpl impl;
	private LaunchTabWithSections owner;
	
	public JavaHomeLaunchTabSection(LaunchTabWithSections owner) {
		super();
		this.owner = owner;
		this.impl = new JavaHomeSectionImpl(owner, this)
						.setBorder(true);
	}
	
	@Override
	public void createControl(Composite page) {
		impl.createWidgets(page);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy conf) {
		impl.copyTo(GradleLaunchConfigurationDelegate.asJavaHomePrefs(conf));
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy conf) {
		impl.setDefaults(GradleLaunchConfigurationDelegate.asJavaHomePrefs(conf));
	}

	@Override
	public void initializeFrom(ILaunchConfiguration conf) {
		impl.copyFrom(GradleLaunchConfigurationDelegate.asJavaHomePrefs(conf));
	}

	public void widgetsRefreshed() {
		owner.scheduleUpdateJob(); //Enable apply button etc.
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return impl.getValidator();
	}
	
}
