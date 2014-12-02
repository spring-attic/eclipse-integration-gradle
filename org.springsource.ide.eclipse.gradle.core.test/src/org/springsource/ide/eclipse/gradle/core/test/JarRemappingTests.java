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

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;
import org.springsource.ide.eclipse.gradle.core.launch.GradleProcess;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.m2e.M2EUtils;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.test.util.ACondition;
import org.springsource.ide.eclipse.gradle.core.test.util.ExternalCommand;
import org.springsource.ide.eclipse.gradle.core.test.util.MavenCommand;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;

/**
 * @author Kris De Volder
 */
public class JarRemappingTests extends GradleTest {

	public void testSTS2405RemapJarToMavenProject() throws Exception {
		//Disable open/close listener so that we can reliably verify thatremapping works on
		// explicit dependency refreshes. (If open-close listener is enabled it will 
		// interfere and make stuff work even if without calls to 'refreshDependencies'
		GradleCore.getInstance().getPreferences().setJarRemappingOnOpenClose(false);
		
		assertTrue("This test requires m2e", M2EUtils.isInstalled());
		String userHome = System.getProperty("user.home");
		String restoreJvmArgs = GradleCore.getInstance().getPreferences().getJVMArguments();
		try {
			String home = System.getenv("HOME");
			System.out.println("HOME = "+home);
			System.out.println("user.home = "+System.getProperty("user.home"));
			System.out.println("maven.repo.local = "+System.getProperty("maven.repo.local"));
			final IProject mvnProject = importEclipseProject("sts2405/myLib");
			String mvnLocalRepo = userHome +"/.m2/repository";
			assertNoErrors(mvnProject, true);
			new ExternalCommand(
				"which", "mvn"	
			).exec(mvnProject.getLocation().toFile());
//			new ExternalCommand(
//				"env"	
//			).exec(mvnProject.getLocation().toFile());
			String mavenLocalProp = "-Dmaven.repo.local="+mvnLocalRepo;
			new MavenCommand(
					"mvn", mavenLocalProp, "install"
			).exec(mvnProject.getLocation().toFile());
	
			//Note: Gradle does not obey system property 'maven.repo.local'. The build script must 
			//read it and use it somehow for it to have an effect.
			GradleCore.getInstance().getPreferences().setJVMArguments(mavenLocalProp);
			
			importTestProject("sts2405/main");
			IProject gradleProject = getProject("main");
			assertNoErrors(gradleProject, true);
	
			final IJavaProject jp = JavaCore.create(gradleProject);
			assertNoClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", jp);
			assertClasspathProjectEntry(mvnProject, jp);
	
			GradleCore.getInstance().getPreferences().setRemapJarsToMavenProjects(false);
			refreshDependencies(gradleProject);
			assertNoClasspathProjectEntry(mvnProject, jp);
			assertClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", GradleCore.create(jp));
		} finally{
			GradleCore.getInstance().getPreferences().setJVMArguments(restoreJvmArgs);
		}
	}
	
	public void testRemapJarToMavenOpenCloseListener() throws Exception {
		System.out.println("==== starting: testRemapJarToMavenOpenCloseListener ===");
		String userHome = System.getProperty("user.home");
		String restoreJvmArgs = GradleCore.getInstance().getPreferences().getJVMArguments();
		try {
			
			String home = System.getenv("HOME");
			System.out.println("HOME = "+home);
			System.out.println("user.home = "+System.getProperty("user.home"));
			System.out.println("maven.repo.local = "+System.getProperty("maven.repo.local"));
			
			final IProject mvnProject = importEclipseProject("sts2405/myLib");
			String mvnLocalRepo = userHome +"/.m2/repository";
			assertNoErrors(mvnProject, true);
			new ExternalCommand(
				"which", "mvn"	
			).exec(mvnProject.getLocation().toFile());
//			new ExternalCommand(
//				"env"	
//			).exec(mvnProject.getLocation().toFile());
			String mavenLocalProp = "-Dmaven.repo.local="+mvnLocalRepo;
			new MavenCommand(
					"mvn", mavenLocalProp, "install"
			).exec(mvnProject.getLocation().toFile());
	
			//Note: Gradle does not obey system property 'maven.repo.local'. The build script must 
			//read it and use it somehow for it to have an effect.
			GradleCore.getInstance().getPreferences().setJVMArguments(mavenLocalProp);
			
			
			importTestProject("sts2405/main");
			final GradleProject gradleProject = getGradleProject("main");
			assertNoErrors(gradleProject.getProject(), true);
			
			/// the actual test begins here, stuff above is setting up the test projects.
			//////////////////////////////////////////////////////////////////////////////
			
			assertTrue(GradleCore.getInstance().getPreferences().getRemapJarsToMavenProjects());
			assertNoClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", gradleProject.getJavaProject());
			assertClasspathProjectEntry(mvnProject, gradleProject.getJavaProject());
			
			mvnProject.close(new NullProgressMonitor());
			new ACondition("Mvn project remapped to Jar") {
				@Override
				public boolean test() throws Exception {
					assertNoClasspathProjectEntry(mvnProject, gradleProject.getJavaProject());
					assertClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", gradleProject.getJavaProject());
					return true;
				}
			}.waitFor(4000);
			
			mvnProject.open(new NullProgressMonitor());
			new ACondition("Mvn project remapped to Jar") {
				@Override
				public boolean test() throws Exception {
					assertClasspathProjectEntry(mvnProject, gradleProject.getJavaProject());
					assertNoClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", gradleProject.getJavaProject());
					return true;
				}
			}.waitFor(4000);
			
			int openCloseListeners = GradleCore.getInstance().countOpenCloseListeners();
			assertTrue(openCloseListeners>0); //Should have at least one (the one for jar remapping).
			assertEquals(openCloseListeners, M2EUtils.countOpenCloseListeners());
			
			GradleCore.getInstance().getPreferences().setJarRemappingOnOpenClose(false);
			assertEquals(openCloseListeners-1, GradleCore.getInstance().countOpenCloseListeners());
			assertEquals(openCloseListeners-1, M2EUtils.countOpenCloseListeners());
			
			mvnProject.close(new NullProgressMonitor());
			try { 
				new ACondition("Mvn project remapped to Jar") {
					@Override
					public boolean test() throws Exception {
						assertNoClasspathProjectEntry(mvnProject, gradleProject.getJavaProject());
						assertClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", gradleProject.getJavaProject());
						return true;
					}
				}.waitFor(4000);
				fail("Remapping should fail because open close listener is disabled");
			} catch (Throwable e) {
				assertEquals("Found 'P/myLib'", ExceptionUtil.getDeepestCause(e).getMessage());
			}
			
		} finally {
			GradleCore.getInstance().getPreferences().setJVMArguments(restoreJvmArgs);
		}
	}
	
	public void testSTS2834RemapJarToGradleProject() throws Exception {
		//Disable open/close listener so that we can reliably verify thatremapping works on
		// explicit dependency refreshes. (If open-close listener is enabled it will 
		// interfere and make stuff work even if without calls to 'refreshDependencies'
		GradleCore.getInstance().getPreferences().setJarRemappingOnOpenClose(false);
		GradleCore.getInstance().getPreferences().setRemapJarsToGradleProjects(true);

		
		createGeneralProject("repos"); //useds as 'flatFile' repo by the two
									 // test projects. Will be cleaned up (deleted)
									 // by setup of next test.
		
		importTestProject("sts2834/my-lib", true);
		IProject libProject = getProject("my-lib");
		assertProjects("repos", "my-lib");
		
		GradleProcess process = LaunchUtil.launchTasks(GradleCore.create(libProject), ":uploadArchives");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("BUILD SUCCESSFUL", output);

		importTestProject("sts2834/my-app", true);
		assertProjects("repos", "my-lib", "my-app");
		final GradleProject app = GradleCore.create(getProject("my-app"));
		final GradleProject lib = GradleCore.create(getProject("my-lib"));

		//Initially, remapping should be enabled:
		assertTrue(GradleCore.getInstance().getPreferences().getRemapJarsToGradleProjects());
		new ACondition("Jar remapped to Gradle Project") {
			public boolean test() throws Exception {
				assertNoClasspathJarEntry("my-lib-1.0.jar", app.getJavaProject());
				assertClasspathProjectEntry(lib.getProject(), app.getJavaProject());
				return true;
			}
		}.waitFor(4000);
		
		//Disable mapping and check whether changes are made to classpath accordingly:
		GradleCore.getInstance().getPreferences().setRemapJarsToGradleProjects(false);
		refreshDependencies(app.getProject());
		assertNoClasspathProjectEntry(libProject, app.getJavaProject());
		assertClasspathJarEntry("my-lib-1.0.jar", app);
	}
	
	public void testRemapJarToGradleOpenCloseListener() throws Exception {
		GradleCore.getInstance().getPreferences().setRemapJarsToGradleProjects(true);
		System.out.println("==== starting: testRemapJarToGradleOpenCloseListener ===");
		createGeneralProject("repos"); //useds as repos by the two
		 // test projects. Will be cleaned up (deleted)
		 // by setup of next test.
		System.out.println("project 'repos' created");
		
		
		System.out.println("import 'my-lib' project...");
		importTestProject("sts2834/my-lib", true);
		final IProject libProject = getProject("my-lib");
		assertProjects("repos", "my-lib");
		System.out.println("import 'my-lib' project OK");
		
		System.out.println("publish 'my-lib' jar to repos...");
		GradleProcess process = LaunchUtil.launchTasks(GradleCore.create(libProject), ":uploadArchives");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("BUILD SUCCESSFUL", output);
		System.out.println("publish 'my-lib' jar to repos OK");
		
		
		System.out.println("import 'my-app' project...");
		importTestProject("sts2834/my-app", true);
		System.out.println("import 'my-app' OK");
		
		final GradleProject app = GradleCore.create(getProject("my-app"));
		final GradleProject lib = GradleCore.create(getProject("my-lib"));
		
		System.out.println("Checking projects...");
		assertProjects("repos", "my-lib", "my-app");

		//Initially, remapping should be enabled:
		assertTrue(GradleCore.getInstance().getPreferences().getRemapJarsToGradleProjects());
		new ACondition("Initial jar remapping") {
			public boolean test() throws Exception {
				assertNoClasspathJarEntry("my-lib-1.0.jar", app.getJavaProject());
				assertClasspathProjectEntry(lib.getProject(), app.getJavaProject());
				return true;
			}
		};
		System.out.println("Checking projects OK");
		
		System.out.println("Closing 'my-lib'");
		libProject.close(new NullProgressMonitor());
		new ACondition("Project remapped to jar") {
			public boolean test() throws Exception {
				assertNoClasspathProjectEntry(libProject, app.getJavaProject());
				assertClasspathJarEntry("my-lib-1.0.jar", app.getJavaProject());
				return true;
			}
		}
		.waitFor(4000);
		
		libProject.open(new NullProgressMonitor());
		new ACondition("Jar remapped to project") {
			public boolean test() throws Exception {
				assertClasspathProjectEntry(libProject, app.getJavaProject());
				assertNoClasspathJarEntry("my-lib-1.0.jar", app.getJavaProject());
				return true;
			}
		}.waitFor(4000);
		
		int openCloseListeners = GradleCore.getInstance().countOpenCloseListeners();
		assertTrue(openCloseListeners>0); //Should have at least one (the one for jar remapping).
		assertEquals(openCloseListeners, M2EUtils.countOpenCloseListeners());
		
		GradleCore.getInstance().getPreferences().setJarRemappingOnOpenClose(false);
		assertEquals(openCloseListeners-1, GradleCore.getInstance().countOpenCloseListeners());
		assertEquals(openCloseListeners-1, M2EUtils.countOpenCloseListeners());

		System.out.println("Closing 'my-lib'");
		libProject.close(new NullProgressMonitor());

		try { 
			new ACondition("Project remapped to jar") {
				public boolean test() throws Exception {
					assertNoClasspathProjectEntry(libProject, app.getJavaProject());
					assertClasspathJarEntry("my-lib-1.0.jar", app.getJavaProject());
					return true;
				}
			}
			.waitFor(4000);
			fail("Remapping should fail because open close listener is disabled");
		} catch (Throwable e) {
			assertEquals("Found 'P/my-lib'", ExceptionUtil.getDeepestCause(e).getMessage());
		}
		
	}
	
	public static void assertClasspathProjectEntry(GradleProject expectProject, GradleProject project) throws JavaModelException {
		assertClasspathProjectEntry(expectProject.getProject(), project.getJavaProject());
	}
	
	public static void assertNoClasspathProjectEntry(GradleProject expectProject, GradleProject project) throws JavaModelException {
		assertNoClasspathProjectEntry(expectProject.getProject(), project.getJavaProject());
	}
	
	public void testRemappingMultiProject() throws Exception {
		GradlePreferences prefs = GradleCore.getInstance().getPreferences();
		prefs.setExportDependencies(false);
		prefs.setUseCustomToolingModel(true);
		prefs.setRemapJarsToGradleProjects(true);
		prefs.setJarRemappingOnOpenClose(true);
		
		createGeneralProject("repos"); //used as repos to publish to by the test project
		
		final String[] projectNames = { "remapping-multiproject", "main", "lib", "sublib", "repos"};
		final String[] javaProjectNames = { "remapping-multiproject", "main", "lib", "sublib"};
		importTestProject(projectNames[0], true); //must copy to workspace to have correct relative location to 'repos'
		
		final GradleProject root = getGradleProject("remapping-multiproject");
		final GradleProject main = getGradleProject("main");
		final GradleProject lib = getGradleProject("lib");
		final GradleProject sublib = getGradleProject("sublib");
		
		for (String p : javaProjectNames) {
			assertContainerExported(false, getGradleProject(p));
		}
		
		new ACondition() {
			public boolean test() throws Exception {
				assertClasspathJarEntry("commons-collections-3.2.1.jar", main);
				assertNoClasspathJarEntry("commons-collections-3.2.jar", main); //thanks to custom model this problem can be solved!
				assertClasspathJarEntry("commons-collections-3.2.1.jar", lib);
				assertClasspathJarEntry("commons-collections-3.2.jar", sublib);
				
				assertClasspathProjectEntry(lib, main);
				assertClasspathProjectEntry(sublib, main); // transitive project dep also included
				assertClasspathProjectEntry(sublib, lib);
				
				return true;
			}

		}.waitFor(40000);
		
		// Remapping doesn't work unless artifacts are published
		GradleProcess process = LaunchUtil.launchTasks(root, "publish");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("BUILD SUCCESSFUL", output);
		
		refreshDependencies(root.getProject(), main.getProject(), lib.getProject(), sublib.getProject());
		
		close(lib);
		
		new ACondition() {
			public boolean test() throws Exception {
				assertClasspathJarEntry("commons-collections-3.2.1.jar", main);
				assertNoClasspathJarEntry("commons-collections-3.2.jar", main); //thanks to custom model this problem can be solved!
//				assertClasspathJarEntry("commons-collections-3.2.1.jar", lib); // CLOSED! not applicable
				assertClasspathJarEntry("commons-collections-3.2.jar", sublib);
				
				assertNoClasspathProjectEntry(lib, main);
				assertClasspathJarEntry("lib.jar", main);
				
				assertClasspathProjectEntry(sublib, main); // transitive project dep also included
//				assertClasspathProjectEntry(sublib, lib); // CLOSED! not applicable
				return true;
			}
		}.waitFor(40000);
			
		close(sublib);
		
		new ACondition() {
			public boolean test() throws Exception {
				assertClasspathJarEntry("commons-collections-3.2.1.jar", main);
				assertNoClasspathJarEntry("commons-collections-3.2.jar", main); //thanks to custom model this problem can be solved!
//				assertClasspathJarEntry("commons-collections-3.2.1.jar", lib);  // CLOSED! not applicable
//				assertClasspathJarEntry("commons-collections-3.2.jar", sublib); // CLOSED! not applicable
				
				assertNoClasspathProjectEntry(lib, main);
				assertClasspathJarEntry("lib.jar", main);
				
				assertNoClasspathProjectEntry(sublib, main); // transitive project dep also included
				assertClasspathJarEntry("sublib.jar", main);
				return true;
			}
		}.waitFor(40000);
		
	}

	public static void close(final GradleProject project) throws Exception {
		JobUtil.withRule(JobUtil.buildRule(), new NullProgressMonitor(), 1, new GradleRunnable("Close "+project.getDisplayName()) {
			public void doit(IProgressMonitor mon) throws Exception {
				project.getProject().close(new NullProgressMonitor());
			}
		});
	}

	
}
