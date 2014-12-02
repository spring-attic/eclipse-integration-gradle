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

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.GradleRefreshPreferences;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;
import org.springsource.ide.eclipse.gradle.core.actions.ReimportOperation;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer.IRefreshListener;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.launch.GradleProcess;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleProjectPreferences;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProjectRegistry;
import org.springsource.ide.eclipse.gradle.core.test.util.ACondition;
import org.springsource.ide.eclipse.gradle.core.test.util.GitProject;
import org.springsource.ide.eclipse.gradle.core.test.util.JUnitLaunchConfigUtil;
import org.springsource.ide.eclipse.gradle.core.test.util.JavaXXRuntime;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;
import org.springsource.ide.eclipse.gradle.core.util.TimeUtils;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;
import org.springsource.ide.eclipse.gradle.core.wtp.WTPUtil;

/**
 * Basic tests for the GradleImport operation. Imports a project, all its subprojects using
 * default settings.
 * 
 * @author Kris De Volder
 */
public class GradleImportTests extends GradleTest {
	
	public void testImportNoClasspathContainer() throws Exception {
		String projectName = "quickstart";
		File projectLoc = extractJavaSample(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		importOp.setEnableDependencyManagement(false); //use default values for everything else.
		boolean expectDsld = importOp.getEnableDSLD();
		
		performImport(importOp);
		GradleProject project = getGradleProject(projectName);
		
		assertProjects(projectName); //no compile errors?
		
		assertFalse("Shouldn't have classpath container", 
				GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertEquals("DSLD support enablement state", expectDsld, DSLDSupport.getInstance().isEnabled(project));
		assertTrue("Gradle nature added?", GradleNature.hasNature(getProject(projectName)));
	}
	
	public void testImportNoDSLDSupport() throws Exception {
		String projectName = "quickstart";
		File projectLoc = extractJavaSample(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		importOp.setEnableDSLD(false); //use default values for everything else.
		
		performImport(importOp);
		GradleProject project = getGradleProject(projectName);
		
		assertProjects(projectName); //no compile errors?
		
		assertTrue("Should have classpath container", GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertFalse("DSLD support added?", DSLDSupport.getInstance().isEnabled(project));
		assertTrue("Gradle nature added?", GradleNature.hasNature(getProject(projectName)));
	}
	
	public void testImportNoClasspathContainerNoDSLDSupport() throws Exception {
		String projectName = "quickstart";
		File projectLoc = extractJavaSample(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		importOp.setEnableDependencyManagement(false); 
		importOp.setEnableDSLD(false);
		
		performImport(importOp);
		GradleProject project = getGradleProject(projectName);
		
		assertProjects(projectName); //no compile errors?
		
		assertFalse("Shouldn't have classpath container", 
				GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertFalse("DSLD support should not have been added", DSLDSupport.getInstance().isEnabled(project));
		assertTrue("Gradle nature added?", GradleNature.hasNature(getProject(projectName)));
	}
	
	public void testImportSpringFramework() throws Exception {
		JavaXXRuntime.java8everyone();
		String[] projectNames = {
				"spring",
				"spring-aop",
				"spring-aspects",
				"spring-beans",
				"spring-beans-groovy",
				"spring-build-src",
				"spring-context",
				"spring-context-support",
				"spring-core",
				"spring-expression",
				"spring-framework-bom",
				"spring-instrument",
				"spring-instrument-tomcat",
				"spring-jdbc",
				"spring-jms",
				"spring-messaging",
				"spring-orm",
				"spring-orm-hibernate4",
				"spring-oxm",
				"spring-test",
				"spring-tx",
				"spring-web",
				"spring-webmvc",
				"spring-webmvc-portlet",
				"spring-webmvc-tiles2",
				"spring-websocket"
		};
		
		//			boolean good = false;
		//			while(!good) {
		//				try {
		//					Thread.sleep(20000); //Wait out the !@#$ interrupted exception that eclipse is sending.
		//					good = true;
		//				} catch (InterruptedException e) {
		//				}
		//			}
		//		
// Use wrapper version
//		URI distro = new URI("http://services.gradle.org/distributions/gradle-1.3-bin.zip");
//		GradleCore.getInstance().getPreferences().setDistribution(distro);

		final GradleImportOperation importOp = importGitProjectOperation(new GitProject("spring-framework", 
				new URI("git://github.com/SpringSource/spring-framework.git"),
				//"db3bbb5f8cb945b8f29fbd83aff9bbd2dbc70e1c"
				"v4.1.1.RELEASE"
			)
		);

		String[] beforeTasks = {
				//These tasks are set based on the shell script included with spring framework:
				//https://github.com/SpringSource/spring-framework/blob/0ae973f995229bce0c5b9ffe25fe1f5340559656/import-into-eclipse.sh
				"cleanEclipse",
				"eclipse",				
				":spring-oxm:compileTestJava",
		};
		
		importOp.setEnableDSLD(false); // cause some compilation errors in this project so turn off
		importOp.setEnableDependencyManagement(false);
		importOp.setDoBeforeTasks(true);
		importOp.setBeforeTasks(beforeTasks);

		performImport(importOp,
				//Ignore errors: (expected!)
				"Project 'spring-aspects' is an AspectJ project"
				);

		//		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
		//			System.out.println(p.getName());
		//		}
		
		//check that the refresh preferences got setup properly (only checking one property).
		//the one that has a non-default value.
		GradleProject project = getGradleProject("spring-aspects");
		GradleRefreshPreferences prefs = project.getRefreshPreferences();
		assertArrayEquals(beforeTasks, prefs.getBeforeTasks());

		assertProjects(projectNames);
	}

	public void testSTS2407OutputFolderDependencyInNestedMultiproject() throws Exception {
		String[] projectNames = {
				"sts-2407",
				"buildutilities", 
				"projectA", 
				"projectB", 
				"projectC", 
				"projectD",
		};
		importTestProject(projectNames[0]);
		assertProjects(projectNames);
	}
	
	public void testSTS2276SetJavaHome() throws Exception {
		//This test merely verifies whether nothing breaks when we set the JavaHome property. 
		//It doesn't actually test whether this setting actually makes Gradle use that JVM (how could we test that???)
		IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
		try {
			GradleCore.getInstance().getPreferences().setJavaHomeJREName(defaultVM.getName());
			String projectName = "multiproject";
			String subprojectName = "subproject";
			importTestProject(projectName);
			assertProjects(
					projectName,
					subprojectName);
			
		} finally {
			GradleCore.getInstance().getPreferences().unsetJavaHome(); //Reset to default
		}
	}

	public void DISABLEDtestImportSpringSecurity() throws Exception {
		//Test disabled. To reenable need to use more recent version of spring-security that is compatible
		// with current groovy eclipse compiler.
		
		setSnapshotDistro();
		GradleImportOperation importOp = importGitProjectOperation(new GitProject("spring-security", 
				new URI("git://github.com/SpringSource/spring-security.git"),
				"66357a2077cb3d94657a5b759771572a341e55f4"
				//"191fc9c8be80c7338ab8e183014de48f78fcffd1"
		));
		
		importOp.setDoBeforeTasks(true);
		
		performImport(importOp,
				//Ignore errors: (expected!)
				"Project 'spring-security-aspects' is an AspectJ project",
				"Project 'spring-security-samples-aspectj' is an AspectJ project"
		);
		
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject proj : projects) {
			System.out.println("\""+proj.getName()+"\","); //print out in a format convenient to copy paste below
		}
		
		assertProjects(
				"docs-3.2.x",
				"faq-3.2.x",
				"itest-context-3.2.x",
				"itest-web-3.2.x",
				"manual-3.2.x",
				"spring-security-3.2.x",
				"spring-security-acl-3.2.x",
				"spring-security-aspects-3.2.x",
				"spring-security-cas-3.2.x",
				"spring-security-config-3.2.x",
				"spring-security-core-3.2.x",
				"spring-security-crypto-3.2.x",
				"spring-security-ldap-3.2.x",
				"spring-security-openid-3.2.x",
				"spring-security-remoting-3.2.x",
				"spring-security-samples-aspectj-3.2.x",
				"spring-security-samples-cassample-3.2.x",
				"spring-security-samples-casserver-3.2.x",
				"spring-security-samples-contacts-3.2.x",
				"spring-security-samples-dms-3.2.x",
				"spring-security-samples-gae-3.2.x",
				"spring-security-samples-jaas-3.2.x",
				"spring-security-samples-ldap-3.2.x",
				"spring-security-samples-openid-3.2.x",
				"spring-security-samples-preauth-3.2.x",
				"spring-security-samples-servletapi-3.2.x",
				"spring-security-samples-tutorial-3.2.x",
				"spring-security-taglibs-3.2.x",
				"spring-security-web-3.2.x"
		);
		
		assertTrue(WTPUtil.isWTPProject(getProject("spring-security-samples-jaas-3.2.x")));
	}
	
	public void testSTS2185AddWebAppLibrariesContainerToWTPProjects() throws Exception {
		assertTrue("This test requires WTP functionality to be installed", WTPUtil.isInstalled());
		String[] projectNames = {
				"sts2185",
				"A",
				"B"
		};

		importTestProject(projectNames[0]);
		assertProjects(projectNames);

		new ACondition("testSTS2185AddWebAppLibrariesContainerToWTPProjects") {
			@Override
			public boolean test() throws Exception {
				/////////////////////
				//Check project A: 
				{ 
					IJavaProject project = getJavaProject("A");

					IClasspathEntry[] rawClasspath = project.getRawClasspath();
					IClasspathEntry[] resolvedClasspath = project.getResolvedClasspath(false);

					//Can we find the expected class path container?
					assertClasspathContainer(rawClasspath, WTPUtil.JST_J2EE_WEB_CONTAINER);

					//Can we find 'junk.jar' from the WEB-INF/lib directory of project A.
					assertClasspathJarEntry("junk.jar", resolvedClasspath); 
				}

				/////////////////////
				//Check project B: 
				{ 
					IJavaProject project = getJavaProject("B");

					IClasspathEntry[] rawClasspath = project.getRawClasspath();

					//It is a WTP project...
					assertTrue(WTPUtil.isWTPProject(project.getProject())); 

					//... but shouldn't have libraries container because not a jst.web project.
					assertNoClasspathContainer(rawClasspath, WTPUtil.JST_J2EE_WEB_CONTAINER);
				}
				return true;
			}
		}.waitFor(10000);
	}
	
	private void assertNoClasspathContainer(IClasspathEntry[] rawClasspath, String pathStr) {
		IPath path = new Path(pathStr);
		StringBuilder msg = new StringBuilder();
		for (IClasspathEntry e : rawClasspath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
				if (path.equals(e.getPath())) {
					fail("Found classpath container but shouldn't '"+pathStr+"':\n"+msg.toString());
				}
			}
			msg.append(e+"\n");
		}
	}

	public void testSTS1842SubProjectsWithSlashesInTheirName() throws Exception {
		String[] projectNames = {
				"sts1842",
				"greeteds.world",
				"hello"
		};
		
		importTestProject(projectNames[0]);
		assertProjects(projectNames);
	}
	
	

	public void _testSTS1950RuntimeClasspathMergingFromSubprojectContainers() throws Exception {
		//TODO: This test was disabled because test projects had a bunch of jars in it that 
		// we would have to raise IP log tickets for to put it on the open-sourced git repo.
		// Should try to reinstate the test in future.
		String[] projectNames = {
				"sts1950",
				"A", "B", "C"
		};
		
		importTestProject(projectNames[0]);
		assertProjects(projectNames);
		
		IJavaProject project = getJavaProject("B");
		
		ILaunchConfigurationWorkingCopy launchConf = JUnitLaunchConfigUtil.createLaunchConfiguration(project);
		IRuntimeClasspathEntry[] classpath = JavaRuntime.computeUnresolvedRuntimeClasspath(launchConf);
		System.out.println(">>> Raw runtime classpath for B");
		for (IRuntimeClasspathEntry e : classpath) {
			System.out.println(e);
		}
		System.out.println("<<< Raw runtime classpath for B");
		
		IRuntimeClasspathEntry[] runtimeClasspath = JavaRuntime.resolveRuntimeClasspath(classpath, launchConf);
		System.out.println(">>> Runtime classpath for B");
		for (IRuntimeClasspathEntry entry : runtimeClasspath) {
			System.out.println(entry);
		}
		System.out.println("<<< Runtime classpath for B");
		assertClasspathEntry("glazedlists-1.8.0-java15.jar", runtimeClasspath);
		assertClasspathEntry("junit-4.4.jar", runtimeClasspath);
	}
	
	private void assertClasspathEntry(String jarFile, IRuntimeClasspathEntry[] runtimeClasspath) {
		StringBuilder msg = new StringBuilder("Not found: "+jarFile+"\nFound:\n");
		for (IRuntimeClasspathEntry e : runtimeClasspath) {
			if (e.getType() == IRuntimeClasspathEntry.ARCHIVE) {
				String path = e.getPath().toString();
				if (path.endsWith(jarFile)) {
					return; //OK
				}
			}
			msg.append(e.toString()+"\n");
		}
		fail(msg.toString());
	}

	public void testSTS2202ImportFromSymlink() throws Exception {
		//Reuse test project for sts2175 
		String[] projectNames = {
				"sts2175",
				"suba",
				"subb",
				"subc"
		};
		
		File testProj = getTestProjectCopy(projectNames[0]);
		File tmpDir = TestUtils.createTempDirectory();
		File link = new File(tmpDir, projectNames[0]);
		Process process = Runtime.getRuntime().exec(new String[] {
			"ln",
			"-s",
			testProj.toString(),
			link.toString()
		});
		assertEquals(0, process.waitFor());
		assertTrue(link.exists());
		assertTrue(link.isDirectory());
		importTestProject(link);
		
		assertProjects(projectNames);
		
	}
	
//	/**
//	 * Projects that use version of wrapper generated pre 0.9 will have a wrapper properties file
//	 * that can't be read by 1.0-milestone-3, this causes a crash without a very good error message.
//	 * <p>
//	 * We attempt to recover from it by forcing a more recent version of Gradle, bypassing the wrapper
//	 * completely.
//	 * 
//	 * @throws Exception
//	 */
//	public void testImportOldWrapperFormat() throws Exception {
//		String projectName = "oldWrapperFormat";
//		GradleTest.MockFallBackDialog dialog = new GradleTest.MockFallBackDialog(true);
//		FallBackDistributionCore.setTestDialogProvider(dialog);
//		
//		importTestProject(projectName);
//		assertTrue(""+dialog.projectLoc, (""+dialog.projectLoc).endsWith(projectName)); //check if dialog called as expected
//		
//		assertProjects(projectName); //Check project imported and no errors.
//	}

//	/**
//	 * Different from earlier case: the wrapper format is readable by the tooling API, but it
//	 * specifies a version of Gradle that is too old.
//	 * <p>
//	 * Again, we should attempt to recover by trying to use more recent version.
//	 */
//	public void testImportTooOldVersionInWrapper() throws Exception {
//		String projectName = "oldWrapperVersion";
//		GradleTest.MockFallBackDialog dialog = new GradleTest.MockFallBackDialog(true);
//		FallBackDistributionCore.setTestDialogProvider(dialog);
//		
//		importTestProject(projectName);
//		assertTrue(""+dialog.projectLoc, (""+dialog.projectLoc).endsWith(projectName)); //check if dialog called as expected
//		
//		assertProjects(projectName); //Check project imported and no errors.
//	}
	
	public void testSTS2058ImportProjectThatIsStoredInWorkspaceLocation() throws Exception {
		IPath workspaceLoc = Platform.getLocation();
		
		String[] projectNames = {
				"multiproject",
				"api",
				"services",
				"services-shared",
				"webservice",
				"shared"
		};
		
		String projectName = projectNames[0];
		File orgTestProject = extractJavaSample(projectName);
		File inWorkspaceCopy = new File(workspaceLoc.toFile(), projectName);

		FileUtils.copyDirectory(orgTestProject, inWorkspaceCopy);
		
		importTestProject(inWorkspaceCopy);
		
		assertProjects(
				projectNames
		);
		
		IJavaProject shared = getJavaProject("shared");
		assertSourceFolder(shared, "src/main/java");
		assertSourceFolder(shared, "src/main/resources");
		assertSourceFolder(shared, "src/test/java");
		assertSourceFolder(shared, "src/test/resources");
	}		
	
	/** 
	 * Import preserves existing source folder exclusion filters?
	 */
	public void testSTS2205PreserveSourceFolderExclusions() throws Exception {
		String projectName = "sts_2205";
		File projectLoc = getTestProjectCopy(projectName);
		GradleImportOperation importOp = importTestProjectOperation(projectLoc);
		
		 //Check that the import operation has the expected default options
		assertTrue(importOp.getDoBeforeTasks());
		assertTrue(importOp.getDoAfterTasks()); 
		assertArrayEquals(new String[] { "cleanEclipse", "eclipse" }, 
				importOp.getBeforeTasks());
		assertArrayEquals(new String[] { "afterEclipseImport" }, 
				importOp.getAfterTasks());
		
		importOp.perform(defaultTestErrorHandler(), new NullProgressMonitor());
		assertProjects(projectName);
		
		//Check expected source folder has expected exclusions
		GradleProject gp = getGradleProject(projectName);
		ClassPath classpath = gp.getClassPath();
		IClasspathEntry[] srcFolders = classpath.getSourceFolders();
		assertEquals("Source folder count", 1, srcFolders.length);
		IClasspathEntry entry = srcFolders[0];
		IPath[] exclusions = entry.getExclusionPatterns();
		assertArrayEquals(new IPath[] { new Path("testb2/**/*")}, 
				exclusions);
	}
	
	public void testImportJavaQuickStart() throws Exception {
//		GradleClassPathContainer.DEBUG = true;
		String name = "quickstart";
		importSampleProject(name);
		
		IJavaProject project = getJavaProject(name);
		dumpJavaProjectInfo(project);
		
		assertProjects(name);
		
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
//		assertJarEntry(project, "bogus-4.8.2.jar", true);
	}
	
	public void testImportJavaQuickStartAndRefresh() throws Exception {
//		GradleClassPathContainer.DEBUG = true;
		String name = "quickstart";
		importSampleProject(name);
		
		IJavaProject project = getJavaProject(name);
		GradleProject gp = GradleCore.create(project);
		dumpJavaProjectInfo(project);
		
		assertProjects(name);
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
		assertTrue("Dependency management should be enabled", gp.isDependencyManaged());
		
		RefreshAllActionCore.callOn(Arrays.asList(gp.getProject())).join();
		
		//Project should basically still be the same:
		assertProjects(name);
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
		assertTrue("Dependency management should be enabled", gp.isDependencyManaged());

		//Now try disabling dep management...
		GradleClassPathContainer.removeFrom(project, new NullProgressMonitor());
		assertFalse("Dependency management should be disabled", gp.isDependencyManaged());
		RefreshAllActionCore.callOn(Arrays.asList(gp.getProject())).join();

		// project is pretty much same. Only difference is that dependency are now attached directly. But they should
		// still be there.
		assertProjects(name);
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
		assertFalse("Dependency management should be disabled", gp.isDependencyManaged());

	}
	
	
	/**
	 * Test whether afterImportTasks are executed after an import
	 */
	public void testAfterImportTasksExecuted() throws Exception {
		String projName = "afterImportTask";
		
		//Create a simple test project with a simple tasks
		GradleTaskRunTest.simpleProject(projName, 
				"apply plugin: 'java'\n" +
				"task afterEclipseImport << {\n" +
				"	File f = new File(\"$projectDir/sub/test.txt\")\n" + 
				"   f.getParentFile().mkdirs();\n" +
				"   f.write 'This is a test'\n"+
				"}\n");
		
		//simpl project creation actually uses 'import' to get project into workspace so...
		//the afterEclipseImport task should have run.
		
		IProject project = getProject(projName);
		File location = project.getLocation().toFile();
		
		IFile theFile = project.getFile("sub/test.txt");
		assertTrue(theFile.exists());
		
		//Now try again... reimport the project, but with the option to run the task disabled.
		theFile.delete(true, new NullProgressMonitor());
		project.delete(false, true, new NullProgressMonitor());
		
		GradleImportOperation importOp = importTestProjectOperation(location);
		importOp.setDoAfterTasks(false);
		
		importOp.perform(new ErrorHandler.Test(), new NullProgressMonitor());
		assertFalse(theFile.exists());
	}
	
	public void testImportAfterGradleEclipseTask() throws Exception {
		String projectName = "quickstart";
		File location = extractJavaSample(projectName);
		GradleProject testProj = GradleCore.create(location);
		generateEclipseFiles(testProj);
		testProj.invalidateGradleModel(); // import must be as if model has not yet been built
		importTestProject(testProj.getLocation());
		
		IJavaProject project = getJavaProject(projectName);
		dumpJavaProjectInfo(project);
		
		assertProjects("quickstart");
		
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
		
		assertNoRawLibraryEntries(project);
//		assertJarEntry(project, "bogus-4.8.2.jar", true);
	}

    /**
	 * Verify that project classpath does not have plain jar entries on it (all jars are managed in classpath containers).
	 * @throws JavaModelException 
	 */
	public static void assertNoRawLibraryEntries(IJavaProject project) throws JavaModelException {
		IClasspathEntry[] classpath = project.getRawClasspath();
		for (IClasspathEntry entry : classpath) {
			if (entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
				fail("Raw classpath of project "+project.getElementName()+" has a library entry: "+entry);
			}
		}
	}

	/**
	 * Executes the gradle 'eclipse' task on project associated with given folder.
	 * @throws CoreException 
	 * @throws OperationCanceledException 
	 */
	private void generateEclipseFiles(GradleProject project) throws OperationCanceledException, CoreException {
		String taskPath = ":eclipse";
		Set<String> tasks = project.getAllTasks(new NullProgressMonitor());
		assertTrue(tasks.contains(taskPath));
		
		ILaunchConfigurationWorkingCopy launchConf = (ILaunchConfigurationWorkingCopy) GradleLaunchConfigurationDelegate.createDefault(project, false);
		GradleLaunchConfigurationDelegate.setTasks(launchConf, Arrays.asList(taskPath));
		GradleProcess process = LaunchUtil.synchLaunch(launchConf);
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("BUILD SUCCESSFUL", output);
	}

	public void testSts1907RemoveReferencedLibraries() throws Exception {
//		GradleClassPathContainer.DEBUG = true;
		String name = "quickstart";
		importSampleProject(name);
		IJavaProject project = getJavaProject(name);
		
		dumpJavaProjectInfo(project);
		
		assertProjects(name);
		
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.12-beta-3.jar", true);
//		assertJarEntry(project, "bogus-4.8.2.jar", true);
	}
	
	private void dumpJavaProjectInfo(IJavaProject project) throws CoreException {
		System.out.println(">>>> JavaProject: "+project.getElementName());
		String[] natures = project.getProject().getDescription().getNatureIds();
		System.out.println("natures: ");
		for (String nature : natures) {
			System.out.println("   "+nature);
		}
		System.out.println("builders:");
		for (ICommand cmd : project.getProject().getDescription().getBuildSpec()) {
			System.out.println("   "+cmd);
		}
		System.out.println("Raw Class Path entries: ");
		for (IClasspathEntry entry : project.getRawClasspath()) {
			System.out.println("   "+entry);
		}
		System.out.println("<<<< JavaProject: "+project.getElementName());
	}

	public void testImportJavaMultiProject() throws Exception {
		do_testImportJavaMultiProject("multiproject");
	}
	
	public void do_testImportJavaMultiProject(String rootProjName) throws Exception {
		String[] projectNames = {
				rootProjName,
				"api",
				"services",
				"services-shared",
				"webservice",
				"shared"
		};
		IJavaProject rootProject = getJavaProject(projectNames[0]);
		
		importSampleProject(projectNames[0]);
		
		assertProjects(
				projectNames
		);
		
		IJavaProject shared = getJavaProject("shared");
		assertSourceFolder(shared, "src/main/java");
		assertSourceFolder(shared, "src/main/resources");
		assertSourceFolder(shared, "src/test/java");
		assertSourceFolder(shared, "src/test/resources");
		
		//Check that every eclipse project has been properly setup to have an association to
		//a root project.
		File rootLocation = GradleCore.create(rootProject).getLocation();
		for (String projectName : projectNames) {
			GradleProject project = getGradleProject(projectName);
			
			File actualRootLocation = project.getProjectPreferences().getRootProjectLocation();
			assertEquals("Root associated with "+project, rootLocation, actualRootLocation);
			
			//Also check that we can still get it via getRootProject.
			actualRootLocation = project.getRootProject().getLocation();
			assertEquals("Root associated with "+project, rootLocation, actualRootLocation);
			
			//Also check that we can still get it, even if preference is not set
			project.getProjectPreferences().setRootProjectLocation(null);
			actualRootLocation = project.getRootProject().getLocation();
			assertEquals("Root associated with "+project, rootLocation, actualRootLocation);
			
		}
	}
	
	public void testImportSpringIntegration() throws Exception {
		JavaXXRuntime.java8everyone();
//		GradleAPIProperties props = GradleCore.getInstance().getAPIProperties();
		URI distro = null;
//		if (!props.isSnapshot()) {
			//We are running the 'regular' build!
			//This test requires M8 (project's wrapper properties says so, but it is non-standar location so 
			// tooling API doesn't know.
			// distro = new URI("http://repo.gradle.org/gradle/distributions/gradle-1.0-milestone-8-bin.zip");
//		}
		GradleCore.getInstance().getPreferences().setDistribution(distro);
		GradleImportOperation op = importGitProjectOperation(
				new GitProject(
						"spring-integration", 
						new URI("git://github.com/spring-projects/spring-integration.git"),
						"3e08ca085b4ef9d0c0c6df4f16b5e8f7b724ae31"
				).setRecursive(true)
		);
		op.setEnableDSLD(true);
		op.perform(defaultTestErrorHandler(), new NullProgressMonitor());

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject proj : projects) {
			System.out.println("\""+proj.getName()+"\",");
		}

		String[] projectNames = {
				"spring-integration",
				"spring-integration-amqp",
				"spring-integration-bom",
				"spring-integration-core",
				"spring-integration-event",
				"spring-integration-feed",
				"spring-integration-file",
				"spring-integration-ftp",
				"spring-integration-gemfire",
				"spring-integration-groovy",
				"spring-integration-http",
				"spring-integration-ip",
				"spring-integration-jdbc",
				"spring-integration-jms",
				"spring-integration-jmx",
				"spring-integration-jpa",
				"spring-integration-mail",
				"spring-integration-mongodb",
				"spring-integration-mqtt",
				"spring-integration-redis",
				"spring-integration-rmi",
				"spring-integration-scripting",
				"spring-integration-security",
				"spring-integration-sftp",
				"spring-integration-stream",
				"spring-integration-syslog",
				"spring-integration-test",
				"spring-integration-twitter",
				"spring-integration-websocket",
				"spring-integration-ws",
				"spring-integration-xml",
				"spring-integration-xmpp"
		};
		
		TestUtils.disableCompilerLevelCheck(getProject("spring-integration-groovy"));
		TestUtils.disableCompilerLevelCheck(getProject("spring-integration-scripting"));
		
		assertProjects(
				projectNames
		);
		
		DSLDSupport dslSupport = DSLDSupport.getInstance();
		for (IProject p : projects) {
			GradleProject gp = GradleCore.create(p);
			//Iniatially dsl support is ...
			assertTrue("dslSupport enabled?", dslSupport.isEnabled(gp));
			assertTrue("Groovy Nature", p.hasNature(DSLDSupport.GROOVY_NATURE));
			//Disable dsl support
			dslSupport.enableFor(gp, false, new NullProgressMonitor());
		}
		
		// Now do a basic refresh all test.
		RefreshAllActionCore.callOn(Arrays.asList(projects)).join();
		for (IProject p : projects) {
			GradleProject gp = GradleCore.create(p);
			assertFalse(dslSupport.isEnabled(gp));
			if (p.getName().equals("spring-integration")) {
				//The 'root' project keeps groovy nature because it doesn't apply the 'eclipse'
				//  plugin. Therefore it has no 'cleanEclipse' task and so the nature isn't erased.
				// This is the expected behavior. (Or should we ourselves attempt to 'cleanEclipse'?).
				assertTrue(p.hasNature(DSLDSupport.GROOVY_NATURE));
			} else {
				assertFalse("Project "+p.getName()+" still has Groovy nature", 
						p.hasNature(DSLDSupport.GROOVY_NATURE));
			}
		}
		
		assertProjects(
				projectNames
		);
		
	}
	
	public void testNonExportedDependencies() throws Exception {
		GradlePreferences prefs = GradleCore.getInstance().getPreferences();
		prefs.setExportDependencies(false);
		prefs.setUseCustomToolingModel(true);
		importTestProject("non-exported-deps");
		
		assertContainerExported(false, getGradleProject("lib"));
		
		// Custom model will not be built as early as the legacy model because it
		// is only used for classpath infos. Thus the first time the model is needed
		// will be when projects get built (and classpath container is being initialized).
		// So before verifying that projects build without errors we must wait for
		// classpath containers to become populated with expected elements
		new ACondition("check resolved classpaths") {
			
			@Override
			public boolean test() throws Exception {
				assertClasspathJarEntry("commons-collections-3.2.1.jar", getGradleProject("main"));
				assertClasspathJarEntry("commons-collections-3.2.1.jar", getGradleProject("lib"));
				assertClasspathProjectEntry(getProject("lib"), getJavaProject("main"));
				return true;
			}
		}.waitFor(40000);
		
		assertProjects("non-exported-deps", "main", "lib");
	}
	
	public void testSTS2094() throws Exception {
		//This bug happens if one imports a set of projects then deletes them and then imports them all again.
		testImportSpringIntegration();
		
		GradleProject rootProject = getGradleProject("spring-integration");
		File rootLocation = rootProject.getLocation();
		
		for (IProject p : getProjects()) {
			p.delete(false, true, new NullProgressMonitor());
		}
		
		GradleImportOperation importOp = GradleImportOperation.importAll(rootLocation);
		importOp.verify();
		importOp.perform(new ErrorHandler.Test(IStatus.ERROR), new NullProgressMonitor());
	}
	
	public void disabledTestTimedImport() throws Exception {
		setSnapshotDistro();
		final GradleImportOperation importOp = importGitProjectOperation(new GitProject("spring-security", 
				new URI("git://git.springsource.org/spring-security/spring-security.git"),
				"359bd7c46")
		);
		
		importOp.excludeProjects(
				"itest-context", 
				"itest-web", 
				"manual", 
				"docs", 
				"faq", 
				"spring-security"
		);
		
		List<Long> times = new ArrayList<Long>();
		
		for (int i = 0; i < 3; i++) {
			long startTime = System.currentTimeMillis();
			performImport(importOp);
			long endTime = System.currentTimeMillis();
			times.add(endTime-startTime);
			
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject proj : projects) {
				System.out.println(proj);
			}
			
			assertProjects(
					"spring-security-acl", 
					"spring-security-aspects", 
					"spring-security-cas", 
					"spring-security-config", 
					"spring-security-core", 
					"spring-security-crypto", 
					"spring-security-ldap", 
					"spring-security-openid", 
					"spring-security-remoting", 
					"spring-security-samples-aspectj", 
					"spring-security-samples-cassample", 
					"spring-security-samples-casserver", 
					"spring-security-samples-contacts", 
					"spring-security-samples-dms", 
					"spring-security-samples-gae", 
					"spring-security-samples-jaas", 
					"spring-security-samples-ldap", 
					"spring-security-samples-openid", 
					"spring-security-samples-preauth", 
					"spring-security-samples-tutorial", 
					"spring-security-taglibs", 
					"spring-security-web"
			);
			
			assertTrue(WTPUtil.isWTPProject(getProject("spring-security-samples-jaas")));
			
			getGradleProject("spring-security-acl").invalidateGradleModel();
			ISchedulingRule buildRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
			Job.getJobManager().beginRule(buildRule, new NullProgressMonitor());
			try {
				for (IProject p : projects) {
					p.delete(false, true, new NullProgressMonitor());
				}
			} finally {
				Job.getJobManager().endRule(buildRule);
				
			}
		}
		System.out.println(">>>> import times ====");
		long sum = 0;
		for (Long time : times) {
			sum += time;
			System.out.println(TimeUtils.minutusAndSecondsFromMillis(time));
		}
		System.out.println("----------------------");
		System.out.println("Avg : "+TimeUtils.minutusAndSecondsFromMillis(sum/times.size()));
		System.out.println("<<<< import times ====");
	}

	public void setSnapshotDistro() {
// Disabled for now... we won't be running 'integration' style tests anymore. The Gradle guys don't
// really seem to care about the test failures or bugs I've raised about them. We'll just run tests
// now with the versions that are supposed to be used with the various test projects.
		
//		GradleProperties props = GradleCore.getInstance().getProperties();
//		if (props.isSnapshot()) {
//			URI distro =  props.getDistribution();
//			if (distro!=null) {
//				GradleCore.getInstance().getPreferences().setDistribution(distro);
//			}
//		}
	}

	/**
	 * Gradle 1.0-M4 uses linked resources to deal with funny source folders like "../scripts" which
	 * are not legal in Eclipse since they don't lie within the project.
	 */
	public void testImportProjectWithLinkedResource() throws Exception {
		String projectName = "multiproject";
		String subprojectName = "subproject";
		
		importTestProject(projectName);
		
		assertProjects(
				projectName,
				subprojectName);
		
		//Check whether the 'Main' type whose source code is actually inside of the parent project, but on the
		//source path of the subproject is actually found in the subproject.
		
		IJavaProject subproject = getJavaProject(subprojectName);
		assertNotNull(subproject);
		IType mainType = subproject.findType("Main");
		assertNotNull(mainType);
		
		mainType = subproject.findType("Spain");
		assertNull(mainType);
		
		//Next check whether after editing the build.gradle file the linked source folders
		//are updated by a refresh source folders.
		
		createFile(subproject.getProject(), "build.gradle", 
				"sourceSets {\n" + 
				"	main {\n" + 
				"		java {\n" + 
				"			srcDir '../boink2'\n" + 
				"		}\n" + 
				"	}\n" + 
				"}");
		
		GradleProject gSubproject = getGradleProject(subprojectName);
		gSubproject.invalidateGradleModel();
		gSubproject.refreshSourceFolders(new ErrorHandler.Test(), new NullProgressMonitor());

		//Now 'main' should no longer exist, but spain should
		mainType = subproject.findType("Main");
		assertNull(mainType);
		mainType = subproject.findType("Spain");
		assertNotNull(mainType);
		
		//Re-refreshing should not be a problem...
		gSubproject.invalidateGradleModel();
		gSubproject.refreshSourceFolders(new ErrorHandler.Test(), new NullProgressMonitor());
		
		//Nothing changed, should still pass the same assertions
		mainType = subproject.findType("Main");
		assertNull(mainType);
		mainType = subproject.findType("Spain");
		assertNotNull(mainType);
	}

	/**
	 * Gradle 1.0-M4 uses linked resources to deal with funny source folders like "../scripts" which
	 * are not legal in Eclipse since they don't lie within the project.
	 */
	public void testWithMultipleLinkedResources() throws Exception {
		String projectName = "multiproject";
		String subprojectName = "subproject";
		
		importTestProject(projectName);
		
		assertProjects(
				projectName,
				subprojectName);
		
		IJavaProject subproject = getJavaProject(subprojectName);
		createFile(subproject.getProject(), "build.gradle", 
				"sourceSets {\n" + 
				"	main {\n" + 
				"		java {\n" + 
				"			srcDir '../boink'\n" + 
				"			srcDir '../boink2'\n" + 
				"		}\n" + 
				"	}\n" + 
				"}");
		
		GradleProject gSubproject = getGradleProject(subprojectName);
		gSubproject.invalidateGradleModel();
		gSubproject.refreshSourceFolders(new ErrorHandler.Test(), new NullProgressMonitor());
		
		//This time, both of the main types should be found since we added both source folders.
		assertNotNull(subproject);
		IType mainType = subproject.findType("Main");
		assertNotNull(mainType);
		mainType = subproject.findType("Spain");
		assertNotNull(mainType);
	}
	
	public void testSTS2175ClassPathEntryOrder() throws Exception {
		String[] projectNames = {
				"sts2175",
				"suba",
				"subb",
				"subc"
		};
		importTestProject(projectNames[0]);
		assertProjects(projectNames);

		assertTrue(GradleProjectPreferences.DEFAULT_ENABLE_CLASSPATH_SORTING);
		for (String name : projectNames) {
			GradleProject gp = getGradleProject(name);
			assertEquals(GradleProjectPreferences.DEFAULT_ENABLE_CLASSPATH_SORTING, gp.getProjectPreferences().getEnableClasspathEntrySorting());
		}
		GradleProject gp = getGradleProject("suba");
		IJavaProject jp = gp.getJavaProject();
		
		String bThenC = "dependencies {\n" + 
		"	compile project(':subb')\n" + 
		"	compile project(':subc')\n" + 
		"}";
		String cThenB = "dependencies {\n" + 
		"	compile project(':subc')\n" + 
		"	compile project(':subb')\n" + 
		"}";

		createFile(getProject("suba"),"build.gradle", bThenC);
		refreshDependencies(getProject("suba"));
		
		assertSts2175ExpectedCP(
				"subb", "subc", 
				gp.getClassPathcontainer().getClasspathEntries());
		
		createFile(getProject("suba"),"build.gradle", cThenB); //Build script order changed!!
		refreshDependencies(getProject("suba"));
		assertSts2175ExpectedCP(
				"subb", "subc", //Classpath order same (is sorted!)
				gp.getClassPathcontainer().getClasspathEntries());
		
		//Now disable sorting... the ordering should change
		gp.getProjectPreferences().setEnableClasspatEntrySorting(false);
		for (String name : projectNames) {
			//Setting this should affect all projects in the hierarchy
			GradleProject p = getGradleProject(name);
			assertEquals(false, p.getProjectPreferences().getEnableClasspathEntrySorting());
		}
		
		
		refreshDependencies(getProject("suba"));
		assertSts2175ExpectedCP(
				"subc", "subb", //Classpath order same (is sorted!)
				gp.getClassPathcontainer().getClasspathEntries());
		
		createFile(getProject("suba"),"build.gradle", bThenC); //Order changed
		refreshDependencies(getProject("suba"));
		assertSts2175ExpectedCP(
				"subb", "subc", //Classpath order same (is sorted!)
				gp.getClassPathcontainer().getClasspathEntries());
	}
	
	public void testSTS3742ClassPathContainerEntryOrder() throws Exception {
		importTestProject("sts3742");
		assertProjects(new String[] { "sts3742" });

		GradleProject gp = getGradleProject("sts3742");
		IJavaProject jp = gp.getJavaProject();
		
		assertWtpAfterGradleDependencies(jp);
		
		gp.getProjectPreferences().setEnableClasspatEntrySorting(false);
		new ReimportOperation(Collections.singletonList(gp)).perform(null, new NullProgressMonitor());
		assertWtpAfterGradleDependencies(jp);
		
		gp.getProjectPreferences().setEnableClasspatEntrySorting(true);
		new ReimportOperation(Collections.singletonList(gp)).perform(null, new NullProgressMonitor());
		assertWtpAfterGradleDependencies(jp);		
	}
	
	private static void assertWtpAfterGradleDependencies(IJavaProject jp) throws JavaModelException {
		int wtpIndex = -1;
		int gradleDepIndex = -1;
		IClasspathEntry[] entries = jp.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			String path = entries[i].getPath().toString();
			if (path.startsWith(GradleClassPathContainer.ID)) {
				gradleDepIndex = i;
			} else if (path.startsWith(WTPUtil.JST_J2EE_WEB_CONTAINER)) {
				wtpIndex = i;
			}
		}
		
		assertTrue("Web App Libraries container is missing", wtpIndex >= 0);
		assertTrue("Gradle Dependencies container is missing", gradleDepIndex >= 0);
		assertTrue("Gradle Dependencies container is located after the Web App Libraries container on the classpath!", wtpIndex > gradleDepIndex);
	}

	
	private void dumpRawClasspath(IJavaProject jp) throws JavaModelException {
		System.out.println(">>> raw classpath for "+jp.getElementName());
		for (IClasspathEntry e : jp.getRawClasspath()) {
			System.out.println(e);
		}
		System.out.println("<<< raw classpath for "+jp.getElementName());
	}

	public void refreshDependencies() throws Exception {
		//WARNING, this method does not wait for the classpath containers to be refreshed
		Joinable<Void> j = RefreshDependenciesActionCore.callOn(Arrays.asList(getProjects()));
		if (j!=null) {
			j.join();
		}
	}
	
	public static class WaitForRefresh extends ACondition implements IRefreshListener {
		private boolean refreshed = false;
		
		@Override
		public void classpathContainerRefreshed() {
			refreshed = true;
		}

		@Override
		public boolean test() throws Exception {
			return refreshed;
		}

	}

	public void assertSts2175ExpectedCP(String firstProj, String secondProj, IClasspathEntry[] entries) {
		assertEquals(3, entries.length);
		assertEquals(JavaCore.newProjectEntry(new Path("/"+firstProj), true), entries[0]);
		assertEquals(JavaCore.newProjectEntry(new Path("/"+secondProj), true), entries[1]);
		assertJarEntry("junit-4.8.2.jar", entries[2]);
	}


	/**
	 * Relates to STS 3818, when project dependencies are included in classpath container they shouldn't be included in
	 * as explicit .classpath entries.
	 */
	public void testProjectDependciesManaged() throws Exception {
		MockNewProjectWizardUI gui = new MockNewProjectWizardUI();
		gui.name.setValue("flat-parent");
		gui.location.setValue(SampleProject.getDefaultProjectLocation(gui.name.getValue()));
		gui.sampleProject.setValue(SampleProjectRegistry.getInstance().get("flat-java-multiproject"));
		gui.newProjectOp.perform(new NullProgressMonitor());
		
		assertProjects("flat-parent", "my-lib", "product");
		
		GradleProject gp = GradleCore.create(getProject("product"));
		assertTrue(gp.isDependencyManaged());
		assertClasspathProjectEntry(getProject("my-lib"), gp.getJavaProject());
		
		assertNoExplicitProjectEntries(gp.getJavaProject());
	}
	
	//TODO: under what circumstances will linked resources returned by gradle api refer to files (not folders)?
	// We don't have a test for this situation so the code that supports it has never been run.
	
}
