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

import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;

/**
 * This is mostly for testing purposes. It does some rather bogys check that the
 * arguments String doesn't contain the String 'kdvolder-test'.
 * 
 * @author Kris De Volder
 */
public class ArgumentsValidator extends LiveExpression<ValidationResult> {

	private static final CharSequence BAD_STRING = "kdvolder-test";
	private ArgumentsValidatorContext context;

	public ArgumentsValidator(ArgumentsValidatorContext argumentsSection) {
		super(ValidationResult.OK);
		this.context = argumentsSection;
	}

	@Override 
	protected ValidationResult compute() {
		String args = context.getCustomArgumentsInPage();
		if (args!=null && args.contains(BAD_STRING)) {
			return ValidationResult.error("arguments contains '"+BAD_STRING+"'");
		}
		return ValidationResult.OK;
	}

}
