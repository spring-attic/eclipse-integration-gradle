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

import static org.springsource.ide.eclipse.gradle.core.validators.ValidationResult.error;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;


/**
 * @author Kris De Volder
 *
 * TODO: remove: duplicated in eclipse-integration-commons (reusable component!).
 * Don't forget to move testing code as well.
 * 
 * See org.springsource.ide.eclipse.commons.livexp.core.validators.NewProjectNameValidator
 */
public class ProjectNameValidator extends Validator implements ValueListener<String> {
	
	private final LiveExpression<String> projectNameField;

	public ProjectNameValidator(LiveExpression<String> projectNameField) {
		this.projectNameField = projectNameField;
		projectNameField.addListener(this);
	}

	private boolean isAllowedChar(char c) {
		return Character.isLetterOrDigit(c) 
			|| "-_.".indexOf(c)>=0;
	}
	
	@Override
	protected ValidationResult compute() {
		String projectName = projectNameField.getValue();
		if (projectName==null) {
			return error("Project name is undefined");
		} else if ("".equals(projectName)) {
			return error("Project name is empty");
		} else if (projectName.indexOf(' ')>=0) {
			return error("Project name contains spaces");
		} else if (existsInWorkspace(projectName)) {
			return error("A project with name '"+projectName+"' already exists in the workspace.");
		} else {
			for (int i = 0; i < projectName.length(); i++) {
				char c = projectName.charAt(i);
				if (!isAllowedChar(c)) {
					return error("Project name contains forbidden character '"+c+"'");
				}
			}
		}
		return ValidationResult.OK;
	}
	
	private boolean existsInWorkspace(String projectName) {
		IProject[] eclipseProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : eclipseProjects) {
			if (p.getName().equals(projectName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Called when the projectName is changed.
	 */
	public void gotValue(LiveExpression<String> exp, String value) {
		Assert.isLegal(exp==projectNameField);
		refresh();
	}

}
