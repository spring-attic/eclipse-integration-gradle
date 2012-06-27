/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.validators;

import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;

/**
 * @author Kris De Volder
 */
public abstract class Validator extends LiveExpression<ValidationResult> {

	public Validator() {
		super(ValidationResult.OK);
	}

}
