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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.test.util.ACondition;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * @author Kris De Volder
 */
public class ClasspathContainerTests extends GradleTest {
	
	public void testSomeMissingDependencies() throws Throwable {
//		new InterruptEater() {
//			@Override
//			protected void run() throws Exception {
				String projectName = "bork";
				GradleImportOperation importOp = simpleProjectImport(projectName, 
						"apply plugin: 'java'\n" + 
								"dependencies {   \n" + 
								"	compile 'foo:bar:1.2.3'\n" + 
								"	compile 'peyo:smurf:1.0.0'\n" + 
						"}");

				importOp.verify();
				importOp.perform(defaultTestErrorHandler(), new NullProgressMonitor());
				assertErrors(getProject(projectName), true,
						"unresolved dependency - foo bar 1.2.3",
						"unresolved dependency - peyo smurf 1.0.0"
				);
//			}
//		};
	}
	
	public void testDisableEnableDepManagement() throws Throwable {
//		new InterruptEater() {
//			@Override
//			protected void run() throws Exception {
				String projectName = "bork";
				GradleImportOperation importOp = simpleProjectImport(projectName, 
						"apply plugin: 'java'\n" + 
								"dependencies {   \n" + 
								"	compile 'foo:bar:1.2.3'\n" + 
								"	compile 'peyo:smurf:1.0.0'\n" + 
						"}");

				importOp.verify();
				importOp.perform(defaultTestErrorHandler(), new NullProgressMonitor());
				
				IProject p = getProject(projectName);
				IJavaProject jp = getJavaProject(projectName);
//				GradleProject gp = getGradleProject(projectName);
				
				assertErrors(p, true,
						"unresolved dependency - foo bar 1.2.3",
						"unresolved dependency - peyo smurf 1.0.0"
				);
				
				//If container is removed... error markers should go away!
				GradleClassPathContainer.removeFrom(jp, new NullProgressMonitor());
				GradleClassPathContainer.waitForMarkerUpdates();
				assertNoErrors(p, false);
				
				//If container is added again... error markers should return.
				GradleClassPathContainer.addTo(jp, new NullProgressMonitor());
				GradleClassPathContainer.waitForMarkerUpdates();
				assertErrors(p, true,
						"unresolved dependency - foo bar 1.2.3",
						"unresolved dependency - peyo smurf 1.0.0"
				);
				
//			}
//		};
	}
	
	public void testFixAndRebreakDependency() throws Throwable {
//		new InterruptEater() {
//			@Override
//			protected void run() throws Exception {
				String projectName = "bork";
				GradleImportOperation importOp = simpleProjectImport(projectName, 
						"apply plugin: 'java'\n" + 
						"dependencies {   \n" + 
						"	compile 'foo:bar:1.2.3'\n" + 
						"	compile 'peyo:smurf:1.0.0'\n" + 
						"}");

				importOp.verify();
				importOp.perform(defaultTestErrorHandler(), new NullProgressMonitor());
				
				IProject p = getProject(projectName);
//				IJavaProject jp = getJavaProject(projectName);
//				GradleProject gp = getGradleProject(projectName);

				//Outcomment one of the entries. Error should go away if dependencies are refreshed
				createFile(p, "build.gradle", 
						"apply plugin: 'java'\n" + 
						"dependencies {   \n" + 
						"//	compile 'foo:bar:1.2.3'\n" + 
						"	compile 'peyo:smurf:1.0.0'\n" + 
						"}");
				refreshDependencies(p);
				
				assertErrors(p, true,
//						"unresolved dependency - foo#bar;1.2.3",
						"unresolved dependency - peyo smurf 1.0.0"
				);
				
				//Put a new bad entry. It should appear as error marker after refresh.
				createFile(p, "build.gradle", 
						"apply plugin: 'java'\n" + 
						"dependencies {   \n" + 
						"	compile 'new:one:1.2.3'\n" + 
						"	compile 'peyo:smurf:1.0.0'\n" + 
						"}");
				refreshDependencies(p);
				assertErrors(p, true,
						"unresolved dependency - new one 1.2.3",
						"unresolved dependency - peyo smurf 1.0.0"
				);
				
//			}
//		};
	}
	
	public void testSTS3981nonStickyExportedStatus() throws Exception {
		String name = "quickstart";
		importSampleProject(name);
		final GradleProject project = getGradleProject(name);
		assertTrue(GradleCore.getInstance().getPreferences().isExportDependencies());
		assertContainerExported(true, project);
		
		//Requirement 1: changing preference should update containers in workspace projects
		GradleCore.getInstance().getPreferences().setExportDependencies(false);
		new ACondition("container no longer exported") {
			public boolean test() throws Exception {
				assertContainerExported(false, project);
				return true;
			}
		}.waitFor(4000);
		
		//Requirement 2: refresh dependencies should synch 'export' status of container with preference
		project.getClassPathcontainer().setExported(true, new NullProgressMonitor()); //deliberately bring it outof synch again
		assertContainerExported(true, project);
		refreshDependencies(project.getProject());
		new ACondition("container no longer exported") {
			public boolean test() throws Exception {
				assertContainerExported(false, project);
				return true;
			}
		}.waitFor(4000);
	}
	
	
	/**
	 * Very similar test to testSTS3981nonStickyExportedStatus but with a test project that doesn't
	 * have the 'apply plugin: 'eclipse' line in its build script. This case works differently because
	 * it doesn't run 'cleanEclipse' task.
	 */
	public void testSTS3981nonStickyExportedStatusNoEclipsePlugin() throws Exception {
		String name = "quickstart-noeclipse";
		importTestProject(name);
		final GradleProject project = getGradleProject(name);
		assertTrue(GradleCore.getInstance().getPreferences().isExportDependencies());
		assertContainerExported(true, project);
		
		//Requirement 1: changing preference should update containers in workspace projects
		GradleCore.getInstance().getPreferences().setExportDependencies(false);
		new ACondition("container no longer exported") {
			public boolean test() throws Exception {
				assertContainerExported(false, project);
				return true;
			}
		}.waitFor(4000);
		
		//Requirement 2: refresh dependencies should synch 'export' status of container with preference
		project.getClassPathcontainer().setExported(true, new NullProgressMonitor()); //deliberately bring it outof synch again
		assertContainerExported(true, project);
		refreshDependencies(project.getProject());
		new ACondition("container no longer exported") {
			public boolean test() throws Exception {
				assertContainerExported(false, project);
				return true;
			}
		}.waitFor(4000);
	}

	

}
