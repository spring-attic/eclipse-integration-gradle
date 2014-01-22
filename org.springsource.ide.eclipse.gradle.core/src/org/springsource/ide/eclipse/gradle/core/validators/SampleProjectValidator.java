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
package org.springsource.ide.eclipse.gradle.core.validators;

import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;

/**
 * @author Kris De Volder
 */
public class SampleProjectValidator extends Validator implements ValueListener<SampleProject> {
	
	private LiveExpression<SampleProject> sampleProjectField;
	private String elementLabel;
	
	public SampleProjectValidator(String elementLabel, LiveExpression<SampleProject> sampleProjectField) {
		this.elementLabel = elementLabel;
		this.sampleProjectField = sampleProjectField;
		this.sampleProjectField.addListener(this);
	}

	@Override
	protected ValidationResult compute() {
		SampleProject sampleProject = sampleProjectField.getValue();
		if (sampleProject==null) {
			return ValidationResult.error("'"+elementLabel+"' is undefined. Please select one.");
		}
		return ValidationResult.OK;
	}

	public void gotValue(LiveExpression<SampleProject> exp, SampleProject value) {
		refresh();
	}

}
