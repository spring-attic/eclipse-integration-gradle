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
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * @author Kris De Volder
 */
public abstract class LaunchTabSection {

//	protected ILaunchConfigurationDialog dialog;
	public abstract void createControl(Composite page);
	public LiveExpression<ValidationResult> getValidator() {
		return LiveExpression.constant(ValidationResult.OK);
	}
	public abstract void performApply(ILaunchConfigurationWorkingCopy configuration);
	public abstract void setDefaults(ILaunchConfigurationWorkingCopy config);
	public abstract void initializeFrom(ILaunchConfiguration configuration);
//	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
//		this.dialog = dialog;
//	}
	
}
