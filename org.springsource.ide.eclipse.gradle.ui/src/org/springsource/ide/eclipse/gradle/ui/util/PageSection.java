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
package org.springsource.ide.eclipse.gradle.ui.util;

import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.ui.IPageWithSections;


/**
 * @author Kris De Volder
 */
public abstract class PageSection {
	
	protected final IPageWithSections owner;
	
	protected PageSection(IPageWithSections owner) {
		this.owner = owner;
	}

	public static final LiveExpression<ValidationResult> OK_VALIDATOR = LiveExpression.constant(ValidationResult.OK);
	public abstract LiveExpression<ValidationResult> getValidator();
	public abstract void createContents(Composite page);

}
