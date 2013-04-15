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

import java.net.URI;

import org.springsource.ide.eclipse.gradle.core.test.util.GitProject;
import org.springsource.ide.eclipse.gradle.core.test.util.GroovySanityTest;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;

/**
 * This test suite has only one purpose, to import GrailsCore and build it. 
 * This is more a test of GrailsCore greclipse than it is a test of the
 * Gradle Tooling. This why we separate it out from the other
 * tests. This test does not get run in the regular Gradle tooling test
 * builds.
 * 
 * @author Kris De Volder
 */
public class GrailsCoreBuildTest extends GradleTest {
	
	public void testGroovySanity() throws Exception {
		GroovySanityTest.checkSanity(1); // 1 means 2.1 (it is the 'minor' version number.
	}
	
	public void testImportGrailsCore() throws Exception {
		//This test disabled, needs Groovy 2.0 but other tests need Groovy 1.8. Cannot run both in the same
		// test build / runtime workbench.
		final GradleImportOperation importOp = importGitProjectOperation(new GitProject("grails-core", 
				new URI("git://github.com/grails/grails-core.git"), "master"), true);

		importOp.setEnableDSLD(false); // cause some compilation errors in this project so turn off
		importOp.setEnableDependencyManagement(false);
		importOp.setDoBeforeTasks(true);

		performImport(importOp);
		
		//TODO: instead of disabling the check maybe determine *why* there's a problem with two compilers 
		// on the classpath of this project?
		TestUtils.disableCompilerLevelCheck(getProject("grails-docs")); 

		buildProjects();

		boolean build = false; //No need to build again, already built all projects.
		assertNoErrors(build); 
	}



}
