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
package org.springsource.ide.eclipse.gradle.core.validators;

import org.springsource.ide.eclipse.gradle.core.util.JavaRuntimeUtils;

/**
 * Represents the 'context' of the JavaHomeValidator. Typically this is a preferences
 * page, and the validator is meant to validate the contents of some widgets on the page.
 * 
 * @author Kris De Volder
 */
public interface JavaHomeValidatorContext {

	/**
	 * Should return the name of the JRE install selected by the user, or null if the
	 * user has not selected a JRE. (Note: if a execution environment is selected, this
	 * returns null, it only returns a non-null value if a JRE is selected directly.
	 */
	String getJavaHomeJRENameInPage();

	/**
	 * Should return the name of the Execution environment selected by the suer of null if
	 * has not selected a EE.
	 */
	String getExecutionEnvNameInPage();
	
	
	JavaRuntimeUtils getJREUtils();


}
