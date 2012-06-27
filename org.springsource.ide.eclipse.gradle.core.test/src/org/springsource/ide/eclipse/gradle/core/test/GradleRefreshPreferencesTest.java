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

import static org.junit.Assert.assertArrayEquals;

import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.GradleRefreshPreferences;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * Some unit tests for GradleRefreshPreferences API.
 * 
 * @author Kris De Volder
 */
public class GradleRefreshPreferencesTest extends GradleTest {
	
	public void testCopyFrom() throws Exception {
		String projectName = "quickstart";
		GradleImportOperation importOp = importTestProjectOperation(extractJavaSample(projectName));
		performImport(importOp);

		GradleProject project = getGradleProject(projectName);
		GradleRefreshPreferences refreshPrefs = project.getRefreshPreferences();
		copyAndTest(importOp, refreshPrefs);

		importOp.setDoBeforeTasks(!importOp.getDoAfterTasks());
		copyAndTest(importOp, refreshPrefs);
		
		importOp.setBeforeTasks("b_foo", "b_bar");
		copyAndTest(importOp, refreshPrefs);
		
		importOp.setAfterTasks("a_foo", "a_bar");
		copyAndTest(importOp, refreshPrefs);
		
		importOp.setDoAfterTasks(importOp.getDoAfterTasks());
		copyAndTest(importOp, refreshPrefs);

		importOp.setAddResourceFilters(!importOp.getAddResourceFilters());
		copyAndTest(importOp, refreshPrefs);
		
		importOp.setUseHierachicalNames(!importOp.getUseHierarchicalNames());
		copyAndTest(importOp, refreshPrefs);
		
		importOp.setEnableDSLD(!importOp.getEnableDSLD());
		copyAndTest(importOp, refreshPrefs);
	}
	
	private void copyAndTest(GradleImportOperation importOp,
			GradleRefreshPreferences refreshPrefs) {
		refreshPrefs.copyFrom(importOp);
		assertAttributesSame(importOp, refreshPrefs);
	}
	/**
	 * Check that all relevant preferences foind in importPrefs match exactly the values
	 * in refreshPrefs
	 */
	private void assertAttributesSame(GradleImportOperation importOp, GradleRefreshPreferences refreshPrefs) {
		assertEquals(importOp.getDoAfterTasks(), refreshPrefs.getDoAfterTasks());
		assertArrayEquals(importOp.getAfterTasks(), refreshPrefs.getAfterTasks());
		assertEquals(importOp.getDoBeforeTasks(), refreshPrefs.getDoBeforeTasks());
		assertArrayEquals(importOp.getBeforeTasks(), refreshPrefs.getBeforeTasks());
		assertEquals(importOp.getAddResourceFilters(), refreshPrefs.getAddResourceFilters());
		assertEquals(importOp.getUseHierarchicalNames(), refreshPrefs.getUseHierarchicalNames());
		assertEquals(importOp.getEnableDSLD(), refreshPrefs.getEnableDSLD());
	}

}
