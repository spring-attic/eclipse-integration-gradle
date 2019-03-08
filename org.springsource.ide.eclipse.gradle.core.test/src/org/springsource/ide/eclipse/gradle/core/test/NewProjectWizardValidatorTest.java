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
package org.springsource.ide.eclipse.gradle.core.test;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProjectRegistry;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * @author Kris De Volder
 */
public class NewProjectWizardValidatorTest extends GradleTest {

	private MockNewProjectWizardUI wizard = new MockNewProjectWizardUI();
	
	/////////////// project name /////////////////////////////////////////////
	
	/**
	 * Appropriate error message when the project name is null?
	 */
	public void testNoProjectName() {
		wizard.name.setValue(null);
		assertError("Project name is undefined");
	}
	
	/**
	 * Appropriate error message when the project name is empty?
	 */
	public void testProjectNameEmpty() {
		wizard.name.setValue("");
		assertError("Project name is empty");
	}
	
	/**
	 * Appropriate error message if project name has spaces.
	 */
	public void testProjectNameWithSpaces() {
		wizard.name.setValue("some spaces");
		assertError("Project name contains spaces");
	}
	
	/**
	 * ProjectName validator accepts names with dashes, underscores, dots and numbers
	 */
	public void testProjectNameAcceptsCertainNonAlphaChars() {
		wizard.name.setValue("some-of_this.that-and-others-0123456789");
		ValidationResult status = wizard.newProjectOp.getProjectNameValidator().getValue();
		assertOk(status);
	}
	
	/**
	 * Appropriate error message if a project with name already exists in workspace?
	 */
	public void testProjectExistsInWorkspace() throws Exception {
		//Note: validator is assuming projects don't get created during its life time. So we must
		//create the project beforehand.
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("bork");
		project.create(new NullProgressMonitor());
		
		wizard.name.setValue("bork");
		assertError("A project with name 'bork' already exists in the workspace");
	}
	
	public void testBadCharsInName() throws Exception {
		wizard.name.setValue("some#bad");
		assertError("Project name contains forbidden character '#'");
		
		wizard.name.setValue("some/bad");
		assertError("Project name contains forbidden character '/'");
		
		wizard.name.setValue("some:bad");
		assertError("Project name contains forbidden character ':'");
		
		wizard.name.setValue("some$bad");
		assertError("Project name contains forbidden character '$'");
	}

	//////////// project location ////////////////////////
	
	public void testLocationIsNull() throws Exception {
		wizard.name.setValue("bork"); // must be a valid value or location errors will
									  // not show up in the agregated status
		wizard.location.setValue(null);
		assertError("Location should be defined");
	}
	
	public void testLocationIsEmptyString() throws Exception {
		wizard.name.setValue("bork"); // must be a valid value or location errors will
									  // not show up in the agregated status
		wizard.location.setValue("");
		assertError("Location should be defined");
	}
	
	public void testLastSegmentIsNotProjectName() throws Exception {
		File dir = TestUtils.createTempDirectory("foo");
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		
		assertError("Location: last segment of path should be 'bork'");
		
	}

	public void testLocationsWithOnlyGitFolderCountsAsEmpty() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		File gitDir = new File(dir, ".git");
		assertTrue(gitDir.mkdirs());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		assertOk(wizard.newProjectOp.getLocationValidator().getValue());
	}
	
	public void testLocationIsNotAnEmptyDir() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		assertTrue(dir.mkdirs());
		File badFile;
		assertTrue((badFile = new File(dir, "something.txt")).createNewFile());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		
		assertError("'"+dir.getAbsolutePath()+"' is not empty (contains '"+badFile+"')");
	}
	
	public void testLocationIsNotADirectory() throws Exception {
		File file = File.createTempFile("boohoo", "txt");
		wizard.name.setValue(file.getName());
		wizard.location.setValue(file.getAbsolutePath());
		assertError("'"+file+"' exists but is not a directory");
	}
	
	public void testLocationOKExists() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		assertTrue(dir.mkdirs());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		
		assertOk(wizard.newProjectOp.getLocationValidator().getValue());
	}
	
	public void testLocationOKNotExists() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		//assertTrue(dir.mkdirs());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		
		assertOk(wizard.newProjectOp.getLocationValidator().getValue());
	}
	
	/////////// sample project ///////////////////////////
	
	public void testSampleProjectNotSelected() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		//assertTrue(dir.mkdirs());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		wizard.sampleProject.setValue(null);
		
		assertError("'Sample project' is undefined. Please select one.");
	}

	public void testSampleProjectSelected() throws Exception {
		File dir = TestUtils.createTempDirectory();
		dir = new File(dir, "bork");
		//assertTrue(dir.mkdirs());
		
		wizard.name.setValue("bork");
		wizard.location.setValue(dir.getAbsolutePath());
		wizard.sampleProject.setValue(SampleProjectRegistry.getInstance().getAll().get(0));
		
		assertOk(wizard.validationStatus.getValue());
	}
	
	/////////// Helper ///////////////////////////////////
	
	private void assertOk(ValidationResult status) {
		if (!status.isOk()) {
			fail(status.toString());
		}
	}

	private void assertError(String expectedMsg) {
		ValidationResult result = wizard.validationStatus.getValue();
		assertError(expectedMsg, result);
	}

	private void assertError(String expectedMsg, ValidationResult result) {
		assertEquals("Error status expected", IStatus.ERROR, result.status);
		assertContains(expectedMsg, result.msg);
	}
	
}
