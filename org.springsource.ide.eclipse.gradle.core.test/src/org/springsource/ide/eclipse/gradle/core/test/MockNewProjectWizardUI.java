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
package org.springsource.ide.eclipse.gradle.core.test;

import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveVariable;
import org.springsource.ide.eclipse.gradle.core.validators.CompositeValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.core.wizards.NewGradleProjectOperation;

/**
 * Emulates the NewProjectWizard UI input fields for testing.
 * 
 * @author Kris De Volder
 */
public class MockNewProjectWizardUI {
	
	//We won't be bothering with defining getters/setters for all these fields. We just make em final so it is
	//pretty safe to access em directly.
	
	public final NewGradleProjectOperation newProjectOp = new NewGradleProjectOperation();
	public final LiveVariable<String> name = new LiveVariable<String>();
	public final LiveVariable<String> location = new LiveVariable<String>();
	public final LiveVariable<SampleProject> sampleProject = new LiveVariable<SampleProject>();
	public final LiveExpression<ValidationResult> validationStatus;

	/**
	 * Create a 'Mock' UI and connect its input fields to the operation.
	 */
	public MockNewProjectWizardUI() {
		newProjectOp.setProjectNameField(name);
		newProjectOp.setLocationField(location);
		newProjectOp.setSampleProjectField(sampleProject);
		newProjectOp.assertComplete();
		validationStatus = new CompositeValidator()
			.addChild(newProjectOp.getProjectNameValidator())
			.addChild(newProjectOp.getLocationValidator())
			.addChild(newProjectOp.getSampleProjectValidator());
	}

}
