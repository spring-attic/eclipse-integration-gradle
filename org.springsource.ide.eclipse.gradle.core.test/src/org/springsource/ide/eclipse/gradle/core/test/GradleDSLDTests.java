/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.test;

import static org.eclipse.jdt.core.groovy.tests.search.AbstractInferencingTest.printTypeName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.AssertionFailedError;

import org.codehaus.groovy.eclipse.GroovyLogManager;
import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.eclipse.dsl.RefreshDSLDJob;
import org.codehaus.groovy.eclipse.dsl.tests.AbstractDSLInferencingTest;
import org.codehaus.groovy.eclipse.dsl.tests.InferencerWorkload;
import org.codehaus.groovy.eclipse.dsl.tests.InferencerWorkload.InferencerTask;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.groovy.tests.search.AbstractInferencingTest;
import org.eclipse.jdt.core.groovy.tests.search.AbstractInferencingTest.SearchRequestor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.dsld.GradleDSLDClasspathContainer;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.test.util.GradleInferencerWorkload;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * @author Kris De Volder
 */
public class GradleDSLDTests extends GradleTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("Active Groovy Version = "+CompilerUtils.getGroovyVersion());
		GroovyLogManager.manager.addLogger(new AbstractDSLInferencingTest.TestLogger());
	}
	
	public void testScaffolding() throws Exception {
		InferencerWorkload workload = new GradleInferencerWorkload( 
				"/*!V:P!*/apply/*!*/ plugin: 'java'\n");
		IJavaProject javaProject = simpleDSLDProject("scaffold", workload.getContents());
		IProject project = javaProject.getProject();
		TestUtils.assertNoErrors(project);
		new RefreshDSLDJob(project).run(new NullProgressMonitor()); //Needed to make DSLD stuff work.
		
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		assertClasspathContainer(rawClasspath, GradleDSLDClasspathContainer.ID);
		assertClasspathContainer(rawClasspath, GradleClassPathContainer.ID);
		assertTrue(project.hasNature(GradleNature.NATURE_ID));
		
		GroovyCompilationUnit unit = getUnit(project, "build.gradle");
		for (InferencerTask task : workload) {
			assertType(unit, task.start, task.end, task.expectedResultType, task.expectedDeclaringType);
		}

	}
	
	private IJavaProject simpleDSLDProject(String projName, String buildFileContents) throws Exception {
		GradleImportOperation importOp = simpleProjectImport(projName, buildFileContents);
		importOp.setEnableDSLD(true);
		importOp.perform(defaultTestErrorHandler(), new NullProgressMonitor(), null);
		return getJavaProject(projName);
	}

	public void testSuppressUnderlining() throws Exception {
		GradlePreferences prefs = GradleCore.getInstance().getPreferences();
		assertTrue(prefs.getGroovyEditorDisableUnderlining());
		
		InferencerWorkload workload = new GradleInferencerWorkload(
				"/*!V:P!*/apply/*!*/ plugin: 'base'\n" + 
				"\n" + 
				"garbageThatShouldBeUnderlined {\n" +
				"   moreGarbage()\n" +
				"}\n");
		
		GradleProject gp = emptyGradleJavaProject("scaffold");
		DSLDSupport.getInstance().enableFor(gp, true, new NullProgressMonitor());
		IProject project = gp.getProject();
		TestUtils.assertNoErrors(project);
		new RefreshDSLDJob(project).run(new NullProgressMonitor()); //Needed to make DSLD stuff work.

		IFile buildFile = project.getFile("build.gradle");
		InputStream source = new ByteArrayInputStream(workload.getContents().getBytes());
		buildFile.create(source, true, new NullProgressMonitor());
		
		GroovyCompilationUnit unit = getUnit(project, "build.gradle");
		workload.perform(unit, true);
		
		prefs.setGroovyEditorDisableUnderlining(false); // Now stuff *should* get underlined
		try {
			workload.perform(unit , true);
			fail("The above perform should have failed, otherwise this isn't a good test!");
		} catch (AssertionFailedError e) {
			assertContains("Inferencing failure", e.getMessage());
		}
	}

	public void testASimpleBuildScript() throws Exception {
		InferencerWorkload workload = new GradleInferencerWorkload(
				"apply plugin: 'base'\n" + 
				"\n" + 
				"description = 'Spring Security'\n" + 
				" \n" + 
				"/*!V:P!*/allprojects/*!*/ { \n" + 
				"    version = '3.1.1.CI-SNAPSHOT'\n" + 
				"    ext {\n" + 
				"    	releaseBuild = version.endsWith('RELEASE')\n" + 
				"    	snapshotBuild = version.endsWith('SNAPSHOT')\n" + 
				"    }\n" + 
				"\n" + 
				"    group = 'org.springframework.security'\n" + 
				"\n" + 
				"    /*!V:P!*/repositories/*!*/ {\n" + 
				"        /*!MAR:RH!*/mavenLocal/*!*/()\n" + 
				"        /*!MAR:RH!*/mavenCentral/*!*/()\n" + 
				"    }\n" + 
				"}\n" +
				"\n" +
				"task goodbye {\n" +
				"	/*!T:T!*/doLast/*!*/ {\n" +
				"		println 'Bye'\n"+
				"	}\n" +
				"}\n" +
				"/*!T:P!*/task/*!*/ hello {\n" +
				"   /*!T:T!*/dependsOn(goodbye)/*!*/\n"+
				"}\n");

		IJavaProject javaProject = simpleDSLDProject("scaffold", workload.getContents());
		IProject project = javaProject.getProject();
		TestUtils.assertNoErrors(project);
		new RefreshDSLDJob(project).run(new NullProgressMonitor()); //Needed to make DSLD stuff work.
		
		GroovyCompilationUnit unit = getUnit(project, "build.gradle");
		workload.perform(unit, false);
		for (InferencerTask task : workload) {
			assertType(unit, task.start, task.end, task.expectedResultType, task.expectedDeclaringType);
		}
	}

	public static void assertType(GroovyCompilationUnit unit, int start, int end, String expectedResultType, String expectedDeclaringType) {
        SearchRequestor requestor = AbstractInferencingTest.doVisit(start, end, unit, true);
        
        assertNotNull("Did not find expected ASTNode", requestor.node);
        if (! expectedResultType.equals(printTypeName(requestor.result.type)) || ! expectedDeclaringType.equals(printTypeName(requestor.result.declaringType))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected Result Type : " + expectedResultType + "\n");
            sb.append("Found    Result Type : " + printTypeName(requestor.result.type) + "\n");
            sb.append("Expected Declaring Type : " + expectedDeclaringType + "\n");
            sb.append("Found    Declaring type : " + printTypeName(requestor.result.declaringType) + "\n");
            sb.append("ASTNode: " + requestor.node + "\n");
            sb.append("Confidence: " + requestor.result.confidence + "\n");
            fail(sb.toString());
        }
        
//        // this is from https://issuetracker.springsource.com/browse/STS-1854
//        // make sure that the Type parameterization of Object has not been messed up
//        assertNull("Problem!!! Object type has type parameters now.  See STS-1854", VariableScope.OBJECT_CLASS_NODE.getGenericsTypes());
	}

	private GroovyCompilationUnit getUnit(IProject project, String path) {
		IFile file = project.getFile(new Path(path));
		return (GroovyCompilationUnit) JavaCore.create(file);
	}

}
