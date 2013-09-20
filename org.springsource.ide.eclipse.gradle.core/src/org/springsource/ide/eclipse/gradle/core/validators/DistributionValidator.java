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
package org.springsource.ide.eclipse.gradle.core.validators;

import static org.springsource.ide.eclipse.gradle.core.validators.ValidationResult.OK;
import static org.springsource.ide.eclipse.gradle.core.validators.ValidationResult.error;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;


/**
 * Validator logic for the {@link DistributionSection}
 * 
 * @author Kris De Volder
 */
public class DistributionValidator extends LiveExpression<ValidationResult> {

	private DistributionValidatorContext context;

	public DistributionValidator(DistributionValidatorContext context) {
		super(ValidationResult.OK);
		this.context = context;
	}

	@Override
	protected ValidationResult compute() {
		try {
			URI distro = context.getDistroInPage();
			if (distro==null) {
				//means use the default.
				return OK;
			} else if ("file".equals(distro.getScheme())) {
				return validateFileDistro(distro);
			} else {
				//TODO: validate other kinds of urls.
				return OK;
			}
		} catch (URISyntaxException e) {
			return error("URI syntax error in Distribution URI");
		}
	}

	
	private static String[] expectedFiles = {
		"lib",
		"bin/gradle.bat",
		"bin/gradle"
	};
	
	private ValidationResult validateFileDistro(URI distro) {
		File rootDir = new File(distro);
		if (!rootDir.exists()) {
			return ValidationResult.error("'"+rootDir+"' does not exist");
		} else if (!rootDir.isDirectory()) {
			return ValidationResult.error("'"+rootDir+"' is not a directory");
		}
		for (String pathStr : expectedFiles) {
			File expect = new File(rootDir, pathStr);
			if (!expect.exists()) {
				return ValidationResult.error("'"+rootDir+"' doesn't look like a valid Gradle installation: " 
						+" Couldn't find '"+expect+"'");
			}
		}
		return OK;
	}
	
}
