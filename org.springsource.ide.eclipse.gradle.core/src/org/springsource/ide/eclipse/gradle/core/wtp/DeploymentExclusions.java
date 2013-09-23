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
package org.springsource.ide.eclipse.gradle.core.wtp;

/**
 * An instance of this class encapsulates a mechanism/list to decide whether to exclude a certain jar dependency 
 * occurring in the gradle class path container from the WTP deployment assembly.
 * <p>
 * The current implementation is retrieved from the GradlePreferences. 
 * <p>
 * A future implementation should use the type of dependency to exclude dependency types
 * that shouldn't be deployed such as 'test' and 'provided' dependencies. 
 * <p>
 * See https://issuetracker.springsource.com/browse/STS-2063 for information on what dependencies
 * shouldn't be deployed.
 * 
 * @author Kris De Volder
 */
public abstract class DeploymentExclusions {
	
	private static RegexpListDeploymentExclusions defaultInstance;

	public static RegexpListDeploymentExclusions getDefault() {
		if (defaultInstance==null) {
			RegexpListDeploymentExclusions result = new RegexpListDeploymentExclusions(
					"servlet-api-.*\\.jar", 
					"javax.servlet-api-.*\\.jar", 
					"jsp-api-.*\\.jar",
					"javax.servlet.*\\.jar"
					);
			result.verify(); // Make sure that default exclusions are valid
			defaultInstance = result;
		}
		return defaultInstance;
	}
	
	public abstract boolean shouldExclude(String jarFileName);

}
