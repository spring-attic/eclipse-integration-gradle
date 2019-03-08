/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
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
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProjectRegistry;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;


/**
 * @author Kris De Volder
 */
public class GradleSampleProjectTest extends GradleTest {

	/**
	 * Check whether sample project implementation correctly creates the contents of simple sample.
	 */
	public void testCreateSampleContents() throws Exception {
		File targetLocation = TestUtils.createTempDirectory("borker");
		SampleProject sample = SampleProjectRegistry.getInstance().get("Java Quickstart");
		sample.createAt(targetLocation);
		assertTrue(new File(targetLocation, "build.gradle").exists());
	} 
	
	/**
	 * Check that none of the sample projects is empty (could happen if there location is ill configured somehow).
	 */
	public void testNoEmptySamples() throws Exception {
		List<SampleProject> allSamples = SampleProjectRegistry.getInstance().getAll();
		for (SampleProject sampleProject : allSamples) {
			File targetLocation = TestUtils.createTempDirectory("sample");
			sampleProject.createAt(targetLocation);
			assertTrue("Sample project '"+ sampleProject.getName() +"' is empty!", new File(targetLocation, "build.gradle").exists());
		}
	} 
	
	/**
	 * Check whether all the sample projects import and build without errors. All projects imported to
	 * default location with a generated unique name.
	 */
	public void testImportSamples() throws Exception {
		List<SampleProject> allSamples = SampleProjectRegistry.getInstance().getAll();
		for (SampleProject sampleProject : allSamples) {
			doTestNewSample(sampleProject);
			deleteAllProjects();
		}
	}

	private void doTestNewSample(SampleProject sampleProject) throws Exception {
		MockNewProjectWizardUI gui = new MockNewProjectWizardUI();
		gui.name.setValue(nameGen("bork"));
		gui.location.setValue(SampleProject.getDefaultProjectLocation(gui.name.getValue()));
		gui.sampleProject.setValue(sampleProject);
		
		gui.newProjectOp.perform(new NullProgressMonitor());
		
		assertProjects(findProjectNames(gui.name.getValue()));
	}
	
	public void testNonDefaultLocationSample() throws Exception {
		SampleProject sampleProject = SampleProjectRegistry.getInstance().getAll().get(1);
		MockNewProjectWizardUI gui = new MockNewProjectWizardUI();
		gui.name.setValue(nameGen("bork"));
		File newLocationHome = TestUtils.createTempDirectory();
		gui.location.setValue(new File(newLocationHome, gui.name.getValue()).toString());
		gui.sampleProject.setValue(sampleProject);
		
		gui.newProjectOp.perform(new NullProgressMonitor());
		
		assertProjects(findProjectNames(gui.name.getValue()));
	}

	/**
	 * Given the name of a GradleProject root project, which is assumed to have been
	 * imported into the workspace, find the names of all the projects in that project's
	 * hierarchy (including the root project itself).
	 * @throws CoreException 
	 * @throws FastOperationFailedException 
	 */
	private String[] findProjectNames(String rootName) throws FastOperationFailedException, CoreException {
		LinkedHashSet<String> allNames = new LinkedHashSet<String>();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(rootName);
		assertTrue(project.exists());
		GradleProject gp = GradleCore.create(project);
		HierarchicalEclipseProject model = gp.getSkeletalGradleModel();
		findProjectNames(allNames, model);
		String[] names = allNames.toArray(new String[allNames.size()]);
		assertEquals(rootName, names[0]);
		return names;
	}

	/**
	 * Walks hierarchy of projects adding each project's name to list of names.
	 */
	private void findProjectNames(LinkedHashSet<String> allNames, HierarchicalEclipseProject rootModel) {
		allNames.add(rootModel.getName());
		for (HierarchicalEclipseProject child : rootModel.getChildren()) {
			findProjectNames(allNames, child);
		}
	}

	private static int unique = 1;
	
	private String nameGen(String string) {
		return string+(unique++);
	}
	
}
