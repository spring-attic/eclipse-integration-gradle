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

import org.eclipse.jdt.launching.IVMInstall;
import org.springsource.ide.eclipse.gradle.core.util.JavaRuntimeUtils;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;


/**
 * @author Kris De Volder
 */
public class JavaHomeValidator extends LiveExpression<ValidationResult> {

	private JavaHomeValidatorContext context;

	public JavaHomeValidator(JavaHomeValidatorContext javaHomeSection) {
		super(ValidationResult.OK);
		this.context = javaHomeSection;
	}

	@Override
	protected ValidationResult compute() {
		String jreName = context.getJavaHomeJRENameInPage();
		if (jreName!=null) {
			JavaRuntimeUtils jres = context.getJREUtils();
			IVMInstall install = jres.getInstall(jreName);
			if (install==null) {
				return ValidationResult.error("Unkwown JRE name: "+jreName);
			} else if (JavaRuntimeUtils.hasTheJREProblem(install)) {
				return ValidationResult.error(jreName+" is not a JDK. Gradle requires a JDK.");
			}
		}
		
		String execEnvName = context.getExecutionEnvNameInPage();
		if (execEnvName!=null) {
			JavaRuntimeUtils jres = context.getJREUtils();
			IVMInstall install = jres.getInstallForEE(execEnvName);
			if (install==null) {
				return ValidationResult.error("No default JRE defined for "+execEnvName);
			} else if (JavaRuntimeUtils.hasTheJREProblem(install)) {
				return ValidationResult.error(install.getName()+" is not a JDK. Gradle requires a JDK.");
			}
		}
		
		return ValidationResult.OK;
	}

}
