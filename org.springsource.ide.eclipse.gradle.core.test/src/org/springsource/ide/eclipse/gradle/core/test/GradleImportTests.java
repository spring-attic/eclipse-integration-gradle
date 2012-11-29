/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.test;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.eclipse.core.builder.GroovyClasspathContainer;
import org.codehaus.groovy.eclipse.dsl.GroovyDSLCoreActivator;
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
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.dsld.GradleDSLDClasspathContainer;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.launch.GradleProcess;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.m2e.M2EUtils;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleAPIProperties;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleProjectPreferences;
import org.springsource.ide.eclipse.gradle.core.test.util.ACondition;
import org.springsource.ide.eclipse.gradle.core.test.util.ExternalCommand;
import org.springsource.ide.eclipse.gradle.core.test.util.GitProject;
import org.springsource.ide.eclipse.gradle.core.test.util.JUnitLaunchConfigUtil;
import org.springsource.ide.eclipse.gradle.core.test.util.MavenCommand;
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
		
		performImport(importOp);
		GradleProject project = getGradleProject(projectName);
		
		assertProjects(projectName); //no compile errors?
		
		assertFalse("Shouldn't have classpath container", 
				GradleClassPathContainer.isOnClassPath(project.getJavaProject()));
		assertTrue("DSLD support added?", DSLDSupport.getInstance().isEnabled(project));
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
		String[] projectNames = {
				"spring",
				"spring-aop",
				"spring-asm",
				"spring-aspects",
				"spring-beans",
				"spring-context",
				"spring-context-support",
				"spring-core",
				"spring-expression",
				"spring-instrument",
				"spring-instrument-tomcat",
				"spring-jdbc",
				"spring-jms",
				"spring-orm",
				"spring-oxm",
				"spring-struts",
				"spring-test",
				"spring-tx",
				"spring-web",
				"spring-webmvc",
				"spring-webmvc-portlet"				
		};
		
		//			boolean good = false;
		//			while(!good) {
		//				try {
		//					Thread.sleep(20000); //Wait out the fucking interrupted exception that eclipse is sending.
		//					good = true;
		//				} catch (InterruptedException e) {
		//				}
		//			}
		//		
		URI distro = new URI("http://repo.gradle.org/gradle/distributions-snapshots/gradle-1.0-milestone-8-20120112000036+0100-bin.zip");
		//distro from: https://github.com/SpringSource/spring-framework/blob/9a1a00a651410390ec317ca3d301d5e9d109f395/.wrapper/gradle-wrapper.properties
		//Note: tooling API doesn't pick up on this because the wrapper in this project is not in the standard location.
		GradleCore.getInstance().getPreferences().setDistribution(distro);

		final GradleImportOperation importOp = importGitProjectOperation(new GitProject("spring-framework", 
				new URI("git://github.com/kdvolder/spring-framework.git"),
				"1b255a18a0fb1")
				);

		String[] beforeTasks = {
				//These tasks are set based on the shell script included with spring framework:
				//https://github.com/SpringSource/spring-framework/blob/0ae973f995229bce0c5b9ffe25fe1f5340559656/import-into-eclipse.sh
				"cleanEclipse",
				":spring-oxm:compileTestJava",
				"eclipse"				
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

//	public void testSTS2276SetJavaHomeBis() throws Exception {
//		//Variant of the previous test, but this one uses a test project that has a wrapper defining
//		//a version of Gradle that doesn't support the API that sets JavaHome. Though setting Java home may not
//		//work in this case, that shouldn't really break imports etc. 
//		IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
//		try {
//			//File javaHome = defaultVM.getInstallLocation();
//			assertNotNull(defaultVM.getInstallLocation());
//			GradleCore.getInstance().getPreferences().setJavaHomeJRE(defaultVM);
//			String projectName = "multiproject-m6";
//			String subprojectName = "subproject";
//			importTestProject(projectName);
//			assertProjects(
//					projectName,
//					subprojectName);
//		} finally {
//			GradleCore.getInstance().getPreferences().setJavaHomeJRE(null); //Rest to default
//		}
//	}
	
	public void testImportSpringSecurity() throws Exception {
		//TODO: test currently failing.
		/// See http://forums.gradle.org/gradle/topics/tooling_api_1_2_model_build_fails_when_there_are_unresolved_dependencies
		
		setSnapshotDistro();
		GradleImportOperation importOp = importGitProjectOperation(new GitProject("spring-security", 
				new URI("git://github.com/SpringSource/spring-security.git"),
				"191fc9c8be80c7338ab8e183014de48f78fcffd1"
//				"734188206d26e7af09a238b4d34eaa01f2e937c0"
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
				"docs",
				"faq",
				"itest-context",
				"itest-web",
				"manual",
				"spring-security",
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
	}
	
	public void testSTS2185AddWebAppLibrariesContainerToWTPProjects() throws Exception {
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
	
	public void testImportSpringDataRedis() throws Exception {
//		JavaUtils.setJava15Compliance();
		setSnapshotDistro();
		importGitProject(new GitProject("spring-data-redis", 
				new URI("git://github.com/SpringSource/spring-data-redis.git"),
				"9a31eb13")
		);
		
		
//		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
//		for (IProject proj : projects) {
//			System.out.println(proj);
//		}
		
		assertProjects(
				"spring-data-redis",
				"docs"
		);
		
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
		GradleClassPathContainer.DEBUG = true;
		String name = "quickstart";
		importSampleProject(name);
		
		IJavaProject project = getJavaProject(name);
		dumpJavaProjectInfo(project);
		
		assertProjects(name);
		
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.11.jar", true);
//		assertJarEntry(project, "bogus-4.8.2.jar", true);
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
		assertJarEntry(project, "junit-4.11.jar", true);
		
		assertNoRawLibraryEntries(project);
//		assertJarEntry(project, "bogus-4.8.2.jar", true);
	}

	public void testImportGrailsCore() throws Exception {
    
        final GradleImportOperation importOp = importGitProjectOperation(new GitProject("grails-core", 
                new URI("git://github.com/grails/grails-core.git"), "master"), true);
    
        importOp.setEnableDSLD(false); // cause some compilation errors in this project so turn off
        importOp.setEnableDependencyManagement(false);
        importOp.setDoBeforeTasks(true);
    
        performImport(importOp,
                //Ignore errors: (expected!)
                "Project 'spring-aspects' is an AspectJ project"
                );
    
        
        //check that the refresh preferences got setup properly (only checking one property).
        //the one that has a non-default value.
        assertNoErrors(true);
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
		GradleClassPathContainer.DEBUG = true;
		String name = "quickstart";
		importSampleProject(name);
		IJavaProject project = getJavaProject(name);
		
		dumpJavaProjectInfo(project);
		
		assertProjects(name);
		
		assertJarEntry(project, "commons-collections-3.2.jar", true);
		assertJarEntry(project, "junit-4.11.jar", true);
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
			
			//File actualRootLocation = project.getRootProject().getLocation();
		}
	}
	
	public void testImportSpringIntegration() throws Exception {
		GradleAPIProperties props = GradleCore.getInstance().getAPIProperties();
		URI distro = null;
//		if (!props.isSnapshot()) {
			//We are running the 'regular' build!
			//This test requires M8 (project's wrapper properties says so, but it is non-standar location so 
			// tooling API doesn't know.
			distro = new URI("http://repo.gradle.org/gradle/distributions/gradle-1.0-milestone-8-bin.zip");
//		}
		GradleCore.getInstance().getPreferences().setDistribution(distro);
		importGitProject(
				new GitProject(
						"spring-integration", 
						new URI("git://github.com/SpringSource/spring-integration.git"), 
						"1ba24f7bb667e0f90"
				).setRecursive(true)
		);

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject proj : projects) {
			System.out.println("\""+proj.getName()+"\",");
		}

		assertProjects(
				"spring-integration",
				"spring-integration-amqp",
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
				"spring-integration-mail",
				"spring-integration-mongodb",
				"spring-integration-redis",
				"spring-integration-rmi",
				"spring-integration-scripting",
				"spring-integration-security",
				"spring-integration-sftp",
				"spring-integration-stream",
				"spring-integration-test",
				"spring-integration-twitter",
				"spring-integration-ws",
				"spring-integration-xml",
				"spring-integration-xmpp"
		);
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
	
//	public void testImportSpock() throws Exception {
//		setSnapshotDistro();
//		importGitProject(new GitProject("spock", 
//				new URI("git://github.com/spockframework/spock.git"),
//				"3f5723")
//		);
//		
//		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
//		for (IProject proj : projects) {
//			System.out.println(proj);
//		}
//		
//		assertProjects(
//				"spock", 
//				"spock-core", 
//				"spock-grails", 
//				"spock-grails-support", 
//				"spock-guice", 
//				"spock-specs", 
//				"spock-spring", 
//				"spock-tapestry", 
//				"spock-unitils"
//		);
//	}
	

//Timing results wiht a 'bulk' run of eclipse tasks
//1 min, 15 sec
//0 min, 44 sec
//0 min, 40 sec
//0 min, 42 sec
//0 min, 38 sec
//0 min, 40 sec
//0 min, 36 sec
//0 min, 38 sec
//0 min, 38 sec
//0 min, 41 sec
//----------------------
//Avg : 0 min, 43 sec
//<<<< import times ====
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
			assertEquals(GradleProjectPreferences.DEFAULT_ENABLE_CLASSPATH_SORTING, gp.getProjectPreferences().getEnableClasspatEntrySorting());
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

		dumpRawClasspath(jp);
		
		createFile(getProject("suba"),"build.gradle", bThenC);
		refreshDependencies();
		
		dumpRawClasspath(jp);
		
		assertArrayEquals(
				sts2175ExpectedCP("subb", "subc"), 
				jp.getRawClasspath());
		
		createFile(getProject("suba"),"build.gradle", cThenB); //Build script order changed!!
		refreshDependencies();
		assertArrayEquals(
				sts2175ExpectedCP("subb", "subc"), //Classpath order same (is sorted!)
				jp.getRawClasspath());
		
		//Now disable sorting... the ordering should change
		gp.getProjectPreferences().setEnableClasspatEntrySorting(false);
		for (String name : projectNames) {
			//Setting this should affect all projects in the hierarchy
			GradleProject p = getGradleProject(name);
			assertEquals(false, p.getProjectPreferences().getEnableClasspatEntrySorting());
		}
		
		
		refreshDependencies();
		assertArrayEquals(
				sts2175ExpectedCP("subc", "subb"), //Classpath order same (is sorted!)
				jp.getRawClasspath());
		
		createFile(getProject("suba"),"build.gradle", bThenC); //Order changed
		refreshDependencies();
		assertArrayEquals(
				sts2175ExpectedCP("subb", "subc"), //Classpath order same (is sorted!)
				jp.getRawClasspath());
	}

	public void testSTS2405RemapJarToMavenProject() throws Exception {
		assertTrue("This test requires m2e", M2EUtils.isInstalled());
		String userHome = System.getProperty("user.home");
		String home = System.getenv("HOME");
		System.out.println("HOME = "+home);
		System.out.println("user.home = "+System.getProperty("user.home"));
		System.out.println("maven.repo.local = "+System.getProperty("maven.repo.local"));
		IProject mvnProject = importEclipseProject("sts2405/myLib");
		String mvnLocalRepo = userHome +"/.m2/repository";
		assertNoErrors(mvnProject, true);
		new ExternalCommand(
			"which", "mvn"	
		).exec(mvnProject.getLocation().toFile());
		new ExternalCommand(
			"env"	
		).exec(mvnProject.getLocation().toFile());
		String mavenLocalProp = "-Dmaven.repo.local="+mvnLocalRepo;
		new MavenCommand(
				"mvn", mavenLocalProp, "install"
		).exec(mvnProject.getLocation().toFile());

		importTestProject("sts2405/main");
		IProject gradleProject = getProject("main");
		assertNoErrors(gradleProject, true);

		IJavaProject jp = JavaCore.create(gradleProject);
		assertNoClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", jp);
		assertClasspathProjectEntry(mvnProject, jp);

		GradleCore.create(gradleProject).getProjectPreferences().setRemapJarsToMavenProjects(false);
		RefreshDependenciesActionCore.synchCallOn(gradleProject);
		assertNoClasspathProjectEntry(mvnProject, jp);
		assertClasspathJarEntry("myLib-0.0.1-SNAPSHOT.jar", GradleCore.create(jp));
	}

	
	private void dumpRawClasspath(IJavaProject jp) throws JavaModelException {
		System.out.println(">>> raw classpath for "+jp.getElementName());
		for (IClasspathEntry e : jp.getRawClasspath()) {
			System.out.println(e);
		}
		System.out.println("<<< raw classpath for "+jp.getElementName());
	}

	public void refreshDependencies() throws Exception {
		Joinable<Void> j = RefreshDependenciesActionCore.callOn(Arrays.asList(getProjects()));
		if (j!=null) {
			j.join();
		}
	}

	public IClasspathEntry[] sts2175ExpectedCP(String firstProj, String secondProj) {
		IClasspathEntry[] expectedClasspath = {
				JavaCore.newSourceEntry(new Path("/suba/src/main/java")),
				JavaCore.newSourceEntry(new Path("/suba/src/test/java")),
				JavaCore.newProjectEntry(new Path("/"+firstProj), true),
				JavaCore.newProjectEntry(new Path("/"+secondProj), true),
				
				//Order is alphapbetic:
				JavaCore.newContainerEntry(GroovyDSLCoreActivator.CLASSPATH_CONTAINER_ID, false),
				JavaCore.newContainerEntry(GroovyClasspathContainer.CONTAINER_ID, false),
				//org.eclipse
				JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"), true),
				//org.springsource
				JavaCore.newContainerEntry(new Path(GradleClassPathContainer.ID), true),
				JavaCore.newContainerEntry(new Path(GradleDSLDClasspathContainer.ID), false),
		};
		return expectedClasspath;
	}
	
	//TODO: under what circumstances will linked resources returned by gradle api refer to files (not folders)?
	// We don't have a test for this situation so the code that supports it has never been run.
	
}
