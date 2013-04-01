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
package org.springsource.ide.eclipse.gradle.core.test;

import java.io.File;

import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.springsource.ide.eclipse.gradle.core.test.util.GroovySanityTest;
import org.springsource.ide.eclipse.gradle.core.test.util.JavaUtils;
import org.springsource.ide.eclipse.gradle.core.test.util.JavaXXRuntime;
import org.springsource.ide.eclipse.gradle.core.test.util.ManagedTestSuite;
import org.springsource.ide.eclipse.gradle.core.test.util.RefreshAllActionCoreTests;


/**
 * @author Kris De Volder
 */
public class AllGradleCoreTests extends ManagedTestSuite {
	
	public static final String PLUGIN_ID = "org.springsource.ide.eclipse.gradle.core.test";
	public static String GRADLE_FOLDER = "/home/bamboo/.gradle";
	
	/**
	 * Change this constant to a new value to force .gradle folder on the build server
	 * to be deleted on the next build.
	 */
	public static final String VALIDITY_STAMP = "STS_2";
	
	public static TestSuite suite() throws Exception {
		deleteInvalidGradleFolder();
		
		TestSuite suite = new ManagedTestSuite(AllGradleCoreTests.class.getName());
		suite.addTestSuite(GroovySanityTest.class);
		suite.addTestSuite(GradleRefreshPreferencesTest.class);
		suite.addTestSuite(ArrayEncoderTest.class);
		suite.addTestSuite(TopoSortTest.class);
		suite.addTestSuite(GradleProjectTest.class);
		suite.addTestSuite(GradleImportTests.class);
		suite.addTestSuite(GradleTaskRunTest.class);
		suite.addTestSuite(GradleMenuEnablementTest.class);
		suite.addTestSuite(GradleDSLDTests.class);
		suite.addTestSuite(RefreshAllActionCoreTests.class);
		suite.addTestSuite(ClasspathContainerErrorMarkersTests.class);
		suite.addTestSuite(DistributionValidatorTest.class);
		suite.addTestSuite(ArgumentsParserTests.class);
		suite.addTestSuite(GradleSampleProjectTest.class);
		suite.addTestSuite(NewProjectWizardValidatorTest.class);
		return suite;
	}

	/**
	 * This method deletes the .gradle folder on the build server. This is used when there's something corrupted
	 * in the folder. 
	 * <p>
	 * To avoid deleting the folder each time, we use a 'validity' stamp file that is placed at the root
	 * of the folder. Only if the validity stamp can not be found do we delete the folder.
	 * <p>
	 * Once the folder is deleted, it is recreated as an empty folder and the validity stamp file is created.
	 * <p>
	 * To force a corrupted .gradle folder to be deleted, simply change the VALIDITY_STAMP constant. This will
	 * automatically invalidate any existing .gradle folder on the build server.
	 */
	private static void deleteInvalidGradleFolder() throws Exception {
		File folder = new File(GRADLE_FOLDER);
		if (folder.exists()) {
			System.out.println("Found .gradle folder: "+folder);
			File stamp = new File(folder, VALIDITY_STAMP);
			if (stamp.exists()) {
				System.out.println("Validity stamp found: "+stamp);
				return;
			}
			System.out.println("Validity stamp NOT found: "+stamp);
			System.out.println("Deleting "+folder+" ");
			FileUtils.deleteDirectory(folder);
			System.out.println("Recreated "+folder+" "+ folder.mkdirs());
			System.out.println("Create stamp: "+stamp.createNewFile());
		}
	}

}
