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

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;


/**
 * Unit tests for GradleProject. 
 * 
 * @author Kris De Volder
 */
public class GradleProjectTest extends GradleTest {
	
	public class TestProjectListener implements IGradleModelListener {
		
		int notifyCount = 0; //Counts number of times the listener is called.
		private GradleProject project;
		private HierarchicalEclipseProject receivedModel;

		/**
		 * @param gradleProject
		 */
		public TestProjectListener(GradleProject gradleProject) {
			this.project = gradleProject;
		}

		public void modelChanged(GradleProject project) {
			assertEquals(this.project, project);
			try {
				receivedModel = project.getSkeletalGradleModel();
			} catch (Exception e) {
				throw new Error(e);
			}
			notifyCount++;
		}

		public void checkExpected(Class<? extends HierarchicalEclipseProject> expectModelType, int expectCount) {
			assertEquals(expectCount, notifyCount);
			assertTrue(expectModelType.isAssignableFrom(receivedModel.getClass()));
		}

	}

	public void testProjectModelListener() throws Exception {
		String[] projectNames = {
				"multiproject",
				"api",
				"services",
				"services-shared",
				"webservice",
				"shared"
		};
		
		importSampleProject(projectNames[0]);
		
		assertProjects(
				projectNames
		);
		
		IJavaProject shared = getJavaProject("shared");
		assertSourceFolder(shared, "src/main/java");
		assertSourceFolder(shared, "src/main/resources");
		assertSourceFolder(shared, "src/test/java");
		assertSourceFolder(shared, "src/test/resources");
		
		GradleProject[] projects = new GradleProject[projectNames.length];
		TestProjectListener[] listeners = new TestProjectListener[projectNames.length];
		for (int i = 0; i < projects.length; i++) {
			projects[i] = GradleCore.create(getProject(projectNames[i]));
			listeners[i] = new TestProjectListener(projects[i]);
			projects[i].addModelListener(listeners[i]);
		}
		
		//After projects where build by Eclipse, we expect all projects to have full models
		for (GradleProject project : projects) {
			try {
				project.getGradleModel(); //no Fast
			} catch (FastOperationFailedException e) {
				fail("Project "+project+" doesn't have a model");
			}
		}
		
		//Invalidate the models
		projects[0].invalidateGradleModel();
		
		//With Group model provider, invalidating one project should invalidate all
		for (GradleProject project : projects) {
			try {
				project.getSkeletalGradleModel();
				fail("Project "+project+" still has a model");
			} catch (FastOperationFailedException e) {
				//expected!
			}
		}
		
		//With Group model provider, forcing one model build should force all related models
		//to become available.
		projects[3].getSkeletalGradleModel(new NullProgressMonitor()); 
		for (TestProjectListener listener : listeners) {
			listener.checkExpected(HierarchicalEclipseProject.class, 1);
		}

		//Forcing more detailed model should cause another round of update events.
		projects[2].getGradleModel(new NullProgressMonitor()); 
		for (TestProjectListener listener : listeners) {
			listener.checkExpected(StsEclipseProject.class, 2);
		}
		
	}

	
// Test below removed because we are no longer trying to leep stuff working with pre 1.0 milestones now.
	
//	public void testIsAtLeastM4() throws Exception {
//		GradleCore.getInstance().getPreferences().setDistribution(Distributions.M3_URI);
//		IJavaProject jProj = GradleTaskRunTest.simpleProject("testIsAtLeastM4", "apply plugin: 'java'");
//		GradleProject project = GradleCore.create(jProj);
//		project.getSkeletalGradleModel(new NullProgressMonitor()); //Ensure we have a model before proceeding
//		assertFalse(project.isAtLeastM4());
//		
//		GradleCore.getInstance().getPreferences().setDistribution(Distributions.M4_URI);
//		project.invalidateGradleModel();
//		
//		project.getSkeletalGradleModel(new NullProgressMonitor()); //Ensure we have a model before proceeding
//		assertTrue(project.isAtLeastM4());
//		
//	}
	

}
