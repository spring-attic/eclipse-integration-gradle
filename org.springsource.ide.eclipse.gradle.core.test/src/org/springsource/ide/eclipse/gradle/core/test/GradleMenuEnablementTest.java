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
package org.springsource.ide.eclipse.gradle.core.test;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.util.NatureUtils;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;
import org.springsource.ide.eclipse.gradle.ui.actions.ConvertToGradleProjectActionDelegate;
import org.springsource.ide.eclipse.gradle.ui.actions.EnableDisableDSLSupportDelegate;
import org.springsource.ide.eclipse.gradle.ui.actions.EnableDisableDependencyManagementActionDelegate;
import org.springsource.ide.eclipse.gradle.ui.actions.RefreshDependenciesAction;


/**
 * @author Kris De Volder
 */
public class GradleMenuEnablementTest extends GradleTest {

	public void testRefreshDependenciesMenuEnablement() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		
		RefreshDependenciesAction actionDelegate = new RefreshDependenciesAction();
		
		select(actionDelegate, project);
		assertEnablement(true);
		
		select(actionDelegate, "Some random thing");
		assertEnablement(false);
		
		select(actionDelegate, project.getFile("build.gradle"));
		assertEnablement(true);
	}
	
	public void testRefreshDependenciesMenuEnablementNoNature() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		
		NatureUtils.remove(project, GradleNature.NATURE_ID, new NullProgressMonitor());
		RefreshDependenciesAction actionDelegate = new RefreshDependenciesAction();
		
		select(actionDelegate, project);
		assertEnablement(false);
		
		select(actionDelegate, "Some random thing");
		assertEnablement(false);
		
		select(actionDelegate, project.getFile("build.gradle"));
		assertEnablement(false);
	}

	public void testRefreshDependenciesMenuEnablementNoClasspathContainer() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		IJavaProject javaProject = getJavaProject(projectName);
		
		GradleClassPathContainer.removeFrom(javaProject, new NullProgressMonitor());
		RefreshDependenciesAction actionDelegate = new RefreshDependenciesAction();
		
		select(actionDelegate, project);
		assertEnablement(false);
		
		select(actionDelegate, "Some random thing");
		assertEnablement(false);
		
		select(actionDelegate, project.getFile("build.gradle"));
		assertEnablement(false);
	}
	
	public void testEnableDisableDependencyManagementMenuEnablement() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		IJavaProject javaProject = getJavaProject(projectName);
		
		IObjectActionDelegate actionDelegate = new EnableDisableDependencyManagementActionDelegate();
		
		assertTrue(GradleClassPathContainer.isOnClassPath(javaProject));
		select(actionDelegate, project);
		assertEnablement(true);
		assertText("Disable Dependency Management");
		
		select(actionDelegate, "Some random thing");
		assertEnablement(false);
		
		select(actionDelegate, project.getFile("build.gradle"));
		assertEnablement(true);
		
		GradleClassPathContainer.removeFrom(javaProject, new NullProgressMonitor());
		select(actionDelegate, project);
		assertEnablement(true);
		assertText("Enable Dependency Management");
	}
	
	public void testEnableDisableDependencyManagementMenuEnablementNoNature() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		
		NatureUtils.remove(project, GradleNature.NATURE_ID, new NullProgressMonitor());
		
		IObjectActionDelegate actionDelegate = new EnableDisableDependencyManagementActionDelegate();
		
		select(actionDelegate, project);
		assertEnablement(false);
		
		select(actionDelegate, project.getFile("build.gradle"));
		assertEnablement(false);
	}
	
	public void test_STS2450_IgnoreGroovyLibrariesInDSLDSupportEnablementTest() throws Exception {
		String projectName = "quickstart";
		GradleImportOperation op = importSampleProjectOperation(projectName);
		op.setEnableDSLD(true);
		op.perform(defaultTestErrorHandler(), new NullProgressMonitor());
		IProject project = getProject(projectName);
		IJavaProject jp = JavaCore.create(project);
		
		//DSLD should be enabled now, and we expect that the groovy libs are on the classpath
		assertTrue(ClassPath.isContainerOnClasspath(jp, DSLDSupport.GROOVY_LIBS_CONTAINER)); 
		IObjectActionDelegate actionDelegate = new EnableDisableDSLSupportDelegate();
		select(actionDelegate, project);
		assertEnablement(true);
		assertMenuTextContains("Disable");
		
		//Remove the Grooyv libs container... 
		GradleProject gp = GradleCore.create(project);
		ClassPath cp = gp.getClassPath();
		cp.removeContainer(DSLDSupport.GROOVY_LIBS_CONTAINER);
		cp.setOn(gp.getJavaProject(), new NullProgressMonitor());
		assertFalse(ClassPath.isContainerOnClasspath(jp, DSLDSupport.GROOVY_LIBS_CONTAINER)); 
		
		//the DSLD support should still be considered as enabled.
		select(actionDelegate, project);
		assertEnablement(true);
		assertMenuTextContains("Disable");
	}
	
	public void testConvertToGradleProjectMenuEnablement() throws Exception {
		String projectName = "quickstart";
		importSampleProject(projectName);
		IProject project = getProject(projectName);
		
		IObjectActionDelegate actionDelegate = new ConvertToGradleProjectActionDelegate();
		
		select(actionDelegate, project);
		assertEnablement(false);
		
		NatureUtils.remove(project, GradleNature.NATURE_ID, new NullProgressMonitor());
		select(actionDelegate, project);
		assertEnablement(true);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	// Infrastructure code below
	
	private void assertText(String expectText) {
		assertEquals(expectText, action.getText());
	}

	private class MockAction extends Action {
		boolean enablementSet = false; // To make sure it gets set! 
		                               // and doesn't accidentally have the right value!
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			enablementSet = true;
		}
	}

	private MockAction action;
	
	/**
	 * For testing the mocking
	 */
	public static class BadDelegate implements IObjectActionDelegate {
		private Boolean wrongValue = null;

		/**
		 * Create a bad delegate that doesn't set the enablement value of the action
		 */
		public BadDelegate() {
		}
		
		/**
		 * Create a bad delegate that sets a wrong enablement value
		 */
		public BadDelegate(boolean wrongValue) {
			this.wrongValue = wrongValue;
		}
		public void run(IAction action) {
			//Don't care
		}
		public void selectionChanged(IAction action, ISelection selection) {
			if (wrongValue!=null) {
				action.setEnabled(wrongValue);
			}
		}
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			//Don't care
		}
		
	}

	/**
	 * Test whether our mocking infrastructure behaves as expected.
	 */
	public void testMocking() {
		assertNull(action);
		select(new BadDelegate(), "something");
		try {
			assertEnablement(true);
			fail("Should have failed!");
		} catch (AssertionFailedError e) {
			//Good!
		}
		try {
			assertEnablement(false);
			fail("Should have failed!");
		} catch (AssertionFailedError e) {
			//Good!
		}
		
		select(new BadDelegate(true), "something");
		try {
			assertEnablement(false);
			fail("Should have failed!");
		} catch (AssertionFailedError e) {
			//Good!
		}
		
		
		action.enablementSet = true; //This should be reset when we try another selection!
		select(new BadDelegate(), "something");
		assertFalse(action.enablementSet); //Did it get reset?
		
		try {
			select(new BadDelegate(), null);
			fail("should have failed");
		} catch (AssertionFailedError e) {
			assertEquals("selection should not be null", e.getMessage());
		}
	}
		
	private void assertEnablement(boolean isEnabled) {
		assertTrue("The action wasn't explicitly disabled or enabled", action.enablementSet);
		assertEquals(isEnabled, action.isEnabled());
	}
	
	private void assertMenuTextContains(String snippet) {
		assertTrue("The action wasn't explicitly disabled or enabled", action.enablementSet);
		assertContains(snippet, action.getText());
	}

	private void select(IObjectActionDelegate actionDelegate, Object someThing) {
		assertNotNull("selection should not be null", someThing);
		action = new MockAction();
		actionDelegate.selectionChanged(action, mockSelection(someThing));
	}

	private ISelection mockSelection(Object obj) {
		return new StructuredSelection(obj);
	}

}
