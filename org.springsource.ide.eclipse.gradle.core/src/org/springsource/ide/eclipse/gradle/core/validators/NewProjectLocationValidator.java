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

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;


import static org.springsource.ide.eclipse.gradle.core.validators.ValidationResult.error;

/**
 * Validation logic for functionality that expects to a new directory. 
 * The validator accepts paths that either do not exist or that point to an
 * empty directory.
 * 
 * @author Kris De Volder
 */
public class NewProjectLocationValidator extends Validator implements ValueListener<String> {
	
	private static final boolean DEBUG = (""+Platform.getLocation()).equals("/tmp/testws");
	
	private String elementName;
	private LiveExpression<String> pathExp;
	private LiveExpression<String> projectNameExp;

	public NewProjectLocationValidator(String elementName, LiveExpression<String> path, LiveExpression<String> projectName) {
		Assert.isNotNull(path);
		Assert.isNotNull(projectName);
		this.elementName = elementName;
		this.pathExp = path;
		this.projectNameExp = projectName;
		path.addListener(this);
		projectName.addListener(this);
	}

	@Override
	protected ValidationResult compute() {
		String path = pathExp.getValue();
		if (path==null || "".equals(path)) {
			return error(elementName+" should be defined");
		}
		String lastSegment = new Path(path).lastSegment();
		String projectName = projectNameExp.getValue();
		if (projectName!=null && lastSegment!=null && !lastSegment.equals(projectName)) {
			return error(elementName+": last segment of path should be '"+projectName+"'");
		}
		File file = new File(path);
		if (file.exists()) {
			if (file.isDirectory()) {
				if (!isEmptyDirectory(file)) {
					return error("'"+file+"' is not empty (contains '"+file.listFiles()[0]+"')");
				}
			} else {
				return error("'"+file+"' exists but is not a directory");
			}
		}
		return ValidationResult.OK;
	}

	private boolean isEmptyDirectory(File file) {
		File[] files = file.listFiles();
		if (files!=null) {
			return files.length==0;
		}
		return false;
	}

	/**
	 * Called when the path being validated changes.
	 */
	public void gotValue(LiveExpression<String> exp, String path) {
//		if (DEBUG) {
//			System.out.println(elementName + " = " + "'"+path+"'");
//		}
		refresh();
	}

}
