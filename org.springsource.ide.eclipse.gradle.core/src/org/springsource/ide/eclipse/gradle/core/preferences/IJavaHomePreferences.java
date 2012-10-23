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
package org.springsource.ide.eclipse.gradle.core.preferences;

import org.eclipse.core.runtime.Assert;


/**
 * An instance of IJavaHomePreferences represents some place where we can
 * retrieve and store the configration of a Java home for Gradle.
 * <p>
 * This allows us to reuse the UI section for configuring Java home across
 * different kinds of UI's such as a preferences page or a launch configuration
 * Tab.
 * <p>
 * The Java HOME can be configured in tow different ways, either via 
 * a workspace JRE or an Execution environment. Thus a IJavaHomePreferences
 * provides methods for getting/setting either one.
 * <p>
 * It is not legal for both an EE and JRE to be specified at the same time,
 * only one or the other should be specified.
 * <p>
 * It is legal for both to be unspecified (i.e. set to null). This means the
 * tooling API's default JAVA_HOME will be used.
 * 
 * @author Kris De Volder
 */
public interface IJavaHomePreferences {

//	/**
//	 * A read only IJavaHomePreferences instance containing the default settings.
//	 */
//	IJavaHomePreferences DEFAULT = new IJavaHomePreferences() {
//		@Override
//		public void unsetJavaHome() {
//		}
//		
//		@Override
//		public void setJavaHomeJREName(String name) {
//			Assert.isLegal(name==null);
//		}
//		
//		@Override
//		public void setJavaHomeEEName(String name) {
//			Assert.isLegal(name==null);
//		}
//		
//		@Override
//		public String getJavaHomeJREName() {
//			return null;
//		}
//		
//		@Override
//		public String getJavaHomeEEName() {
//			return null;
//		}
//	};
	
	/**
	 * @return a workspace JRE name or null. 
	 */
	String getJavaHomeJREName();

	/**
	 * @return The name of an execution environment or null. 
	 */
	String getJavaHomeEEName();

	/**
	 * Set JAVA_HOME via an execution environment. Setting this to
	 * Setting this to a non-null value should implicitly
	 * clear the JREName.
	 */
	void setJavaHomeJREName(String name);

	/**
	 * Set JAVA_HOME via an execution environment. Setting this to
	 * Setting this to a non-null value should implicitly
	 * clear the JREName.
	 */
	void setJavaHomeEEName(String name);

	/**
	 * Clear any Java home related settings so that we will simply use the tooling APIs 
	 * default. (This is equivalent to setting both  EEName and JREName to null.)
	 */
	void unsetJavaHome();
	
}
