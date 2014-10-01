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
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.test.GradleTest;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * @author Kris De Volder
 */
public class RefreshAllActionCoreTests extends GradleTest {
	
	private static class BuildFile {
		String dependencies = null;
		String sourceSets = null;
		public BuildFile() {
		}
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("apply plugin: 'java'\n");
			buf.append("apply plugin: 'eclipse'\n");
			buf.append("repositories {\n" +
					"    mavenCentral()\n" +
					"}\n");
			if (sourceSets!=null) {
				buf.append("sourceSets {\n");
				buf.append(sourceSets+"\n");
				buf.append("}\n");
			}
			if (dependencies!=null) {
				buf.append("dependencies {\n");
				buf.append(dependencies+"\n");
				buf.append("}\n");
			}
			return buf.toString();
		}
	}

	private static final String[] DEFAULT_SRC_FOLDERS = {
		"src/main/java",
		"src/main/resources",
		"src/test/java",
		"src/test/resources"
	};
	
	public void testRefreshAllManaged() throws Exception {
		BuildFile build = new BuildFile();
		
		//    testCompile group: 'junit', name: 'junit', version: '4.8.2'
		String projectName = "theproj";
		
		build.dependencies = "compile group: 'commons-collections', name: 'commons-collections', version: '3.2'";
		GradleImportOperation importOp = simpleProjectImport(projectName, 
				build.toString()
		);
		importOp.setEnableDSLD(false);
		importOp.perform(new ErrorHandler.Test(), new NullProgressMonitor(), null);
		
		GradleProject project = getGradleProject(projectName);
		DSLDSupport.getInstance().enableFor(project, false, new NullProgressMonitor());
		assertProjects(projectName); //no compile errors?

		assertTrue(GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertClasspathJarEntry("commons-collections-3.2.jar", project);
		assertSourceFolders(project /*NONE*/);
		
		for (String srcPath : DEFAULT_SRC_FOLDERS) {
			createFolder(project.getProject(), srcPath);
		}
		
		build.dependencies = "    compile group: 'commons-collections', name: 'commons-collections', version: '3.1'";
		createFile(project.getProject(), "build.gradle",  build.toString());
		refreshAll(project);
		
		assertSourceFolders(project, DEFAULT_SRC_FOLDERS);
		assertClasspathJarEntry("commons-collections-3.1.jar", project);
	}
	
	public void testRefreshAllUnmanaged() throws Exception {
		BuildFile build = new BuildFile();
		
		//    testCompile group: 'junit', name: 'junit', version: '4.8.2'
		String projectName = "theproj";
		
		build.dependencies = "compile group: 'commons-collections', name: 'commons-collections', version: '3.2'";
		GradleImportOperation importOp = simpleProjectImport(projectName, 
				build.toString()
		);
		importOp.setEnableDSLD(false);
		importOp.setEnableDependencyManagement(false);
		importOp.perform(new ErrorHandler.Test(), new NullProgressMonitor(), null);
		
		GradleProject project = getGradleProject(projectName);
		DSLDSupport.getInstance().enableFor(project, false, new NullProgressMonitor());
		assertProjects(projectName); //no compile errors?

		assertFalse(GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertClasspathJarEntry("commons-collections-3.2.jar", project);
		assertSourceFolders(project /*NONE*/);
		
		for (String srcPath : DEFAULT_SRC_FOLDERS) {
			createFolder(project.getProject(), srcPath);
		}
		
		build.dependencies = "    compile group: 'commons-collections', name: 'commons-collections', version: '3.1'";
		createFile(project.getProject(), "build.gradle",  build.toString());
		assertFalse(GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		refreshAll(project);
		assertProjects(projectName); //no compile errors?
		
		assertSourceFolders(project, DEFAULT_SRC_FOLDERS);
		assertClasspathJarEntry("commons-collections-3.1.jar", project);
	}
	
	public void testNoProjectConfigurators() throws Exception {
		String projectName = "quickstart";
		File projectLoc = extractJavaSample(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		importOp.setEnableDependencyManagement(false); 
		importOp.setEnableDSLD(false);
		
		performImport(importOp);
		GradleProject gradleProject = getGradleProject(projectName);		
		IProject project = gradleProject.getProject();
		
		assertTrue("Comment must be empty after initial import",
				project.getDescription().getComment() == null
						|| project.getDescription().getComment().isEmpty());
		
		refreshAll(gradleProject);
		
		assertTrue("Configurators must have changed project comment",
				project.getDescription().getComment() == null
						|| project.getDescription().getComment().isEmpty());
	}

	public void testSingleProjectConfigurator() throws Exception {
		StringBuilder expected = new StringBuilder().append(TestProjectConfigurators.INITIAL_COMMENT_SINGLE)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.SINGLE_CONF);
		testConfigurators(TestProjectConfigurators.INITIAL_COMMENT_SINGLE, expected.toString());
	}
	
	public void testTreeProjectConfigurator() throws Exception {
		StringBuilder expected = new StringBuilder().append(TestProjectConfigurators.INITIAL_COMMENT_TREE)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.TREE_CONF1)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.TREE_CONF2);
		
		testConfigurators(TestProjectConfigurators.INITIAL_COMMENT_TREE, expected.toString());		
	}

	public void testDAGProjectConfigurator() throws Exception {
		StringBuilder expected = new StringBuilder().append(TestProjectConfigurators.INITIAL_COMMENT_DAG)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_CONF_B)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_CONF_A)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_CONF_C)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_CONF_E)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_CONF_D);				
				
		testConfigurators(TestProjectConfigurators.INITIAL_COMMENT_DAG, expected.toString());
	}
	
	public void testAfterDAGProjectConfigurator() throws Exception {
		StringBuilder expected = new StringBuilder().append(TestProjectConfigurators.INITIAL_COMMENT_DAG_AFTER)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_AFTER_B)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_AFTER_D)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_AFTER_C)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_AFTER_A);
		
		testConfigurators(TestProjectConfigurators.INITIAL_COMMENT_DAG_AFTER, expected.toString());
	}
	
	public void testBeforeDAGProjectConfigurator() throws Exception {
		StringBuilder expected = new StringBuilder().append(TestProjectConfigurators.INITIAL_COMMENT_DAG_BEFORE)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_BEFORE_C)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_BEFORE_B)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_BEFORE_A)
				.append(TestProjectConfigurators.DELIMITER)
				.append(TestProjectConfigurators.DAG_BEFORE_D);
		
		testConfigurators(TestProjectConfigurators.INITIAL_COMMENT_DAG_BEFORE, expected.toString());
	}

	private void testConfigurators(String testIdComment, String expectedResult) throws Exception {
		String projectName = "quickstart";
		File projectLoc = extractJavaSample(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		importOp.setEnableDependencyManagement(false); 
		importOp.setEnableDSLD(false);
		
		performImport(importOp);
		GradleProject gradleProject = getGradleProject(projectName);
		IProject project = gradleProject.getProject();
		IProjectDescription description = project.getDescription();
		
		// One project configurator
		description.setComment(testIdComment);
		project.setDescription(description, new NullProgressMonitor());
		refreshAll(gradleProject);
		
		assertEquals(expectedResult, project.getDescription().getComment());
	}

	/**
	 * Check that all expected source folders are found and no extra ones.
	 */
	private void assertSourceFolders(GradleProject project, String... srcFolderPaths) throws Exception {
		IJavaProject jp = project.getJavaProject();
		ClassPath cp = project.getClassPath();
		IClasspathEntry[] sourceFolders = cp.getSourceFolders();
		String[] actualPaths = new String[sourceFolders.length];
		for (int i = 0; i < actualPaths.length; i++) {
			actualPaths[i] = sourceFolders[i].getPath().makeRelativeTo(jp.getProject().getFullPath()).toString();
		}
		assertSameElements(srcFolderPaths, actualPaths);
	}

	public void testRefreshEmptyList() throws Exception {
		refreshAll();
	}

	private void refreshAll(GradleProject... _projects) throws Exception {
		List<IProject> projects = new ArrayList<IProject>();
		for (GradleProject p : _projects) {
			projects.add(p.getProject());
		}
		Joinable<Void> j = RefreshAllActionCore.callOn(projects);
		j.join();
	}

}
