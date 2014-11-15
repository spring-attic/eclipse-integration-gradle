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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.modelmanager.IGradleModelListener;


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
		
		@Override
		public <T> void modelChanged(GradleProject project, Class<T> type,
				T model) {
			assertEquals(this.project, project);
			try {
				if (HierarchicalEclipseProject.class.isAssignableFrom(type)) {
					receivedModel = (HierarchicalEclipseProject) model;
				}
			} catch (Exception e) {
				throw new Error(e);
			}
			notifyCount++;
		}

		public void checkExpected(Class<? extends HierarchicalEclipseProject> expectModelType, int expectCount) {
			assertEquals(expectCount, notifyCount);
			assertTrue(expectModelType.getSimpleName()+" is not assignable from "+ receivedModel.getClass().getSimpleName() ,
					expectModelType.isAssignableFrom(receivedModel.getClass()));
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
				project.getGradleModel(); //no FastOperationFailedException
			} catch (FastOperationFailedException e) {
				fail("Project "+project+" doesn't have a model");
			}
		}
		
		//Invalidate the models
		for (GradleProject project : projects) {
			project.invalidateGradleModel();
		}
		
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
			listener.checkExpected(EclipseProject.class, 2);
		}
		
	}

}
