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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.ProjectMapperFactory;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.test.util.ACondition;
import org.springsource.ide.eclipse.gradle.core.test.util.GitProject;
import org.springsource.ide.eclipse.gradle.core.test.util.KillGradleDaemons;
import org.springsource.ide.eclipse.gradle.core.test.util.LoggingProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler.Test;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.ZipFileUtil;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation.ExistingProjectException;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation.MissingProjectDependencyException;
import org.springsource.ide.eclipse.gradle.core.wizards.PrecomputedProjectMapper.NameClashException;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager.DownloadRequestor;


/**
 * @author Kris De Volder
 */
public abstract class GradleTest extends TestCase {
	
	@Override
	protected void setUp() throws Exception {
		//Disable greclipse background DSL support adding. Hopefully will stop random test failures because of 'missing external folder links'.
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("org.codehaus.groovy.eclipse.dsl");
		preferences.putBoolean("org.codehaus.groovy.eclipse.dsl.auto.add.support", false);
		preferences.flush();
		
		ISchedulingRule buildRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
		Job.getJobManager().beginRule(buildRule, new NullProgressMonitor());
		try {
			//		System.out.println("JobQueue idle?");
			//		new ACondition() {
			//			@Override
			//			public boolean test() throws Exception {
			//				GradleTest.assertJobManagerIdle();
			//				return true;
			//			}
			//		}.waitFor(120000);
			//		System.out.println("JobQueue idle ? => YES!");
			deleteAllProjects();
		} finally {
			Job.getJobManager().endRule(buildRule);
		}
	}

	public static void deleteAllProjects() throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : projects) {
			boolean deleteContent = false;
			p.delete(deleteContent, true, new NullProgressMonitor());
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		GradleClassPathContainer.DEBUG = false;
		super.tearDown();
		GradlePreferences prefs = GradleCore.getInstance().getPreferences();
		prefs.setDistribution(null); //Reset to default.
		prefs.setJVMArguments(null); //Reset to default.
		prefs.setProgramArguments(null); //Reset to default.
		KillGradleDaemons.killem(); //Keep the number of daemons under control.
		try {
			new ACondition() {
				@Override
				public boolean test() throws Exception {
					ACondition.assertJobManagerIdle();
					return true;
				}
			}.waitFor(20000);
		} catch (Throwable e) {
			//Print this as interesting information about the jobs that keep on chugging away...
			//but do not let this cause test failures.
			GradleCore.log(e);
		}
	}
	
	public static File getTestFile(String path) throws IOException {
		Bundle bundle = Platform.getBundle(AllGradleCoreTests.PLUGIN_ID);
		File bundleFile = FileLocator.getBundleFile(bundle);
		Assert.assertNotNull(bundleFile);
		Assert.assertTrue("The bundle "+bundle.getBundleId()+" must be unpacked to allow using the embedded test resources", bundleFile.isDirectory());
		return new File(bundleFile, path);
	}

	public static void assertNoErrors(IProject project, boolean build) throws CoreException {
		if (build) {
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		}
		GradleClassPathContainer.waitForMarkerUpdates();
		IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		for (IMarker problem : problems) {
			if (problem.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				fail(project+": Expecting no problems but found: " + markerMessage(problem));
			}
		}
	}

	public static void assertNoErrors(boolean build) throws CoreException {
	    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	    IProject[] projects = root.getProjects();
	    for (IProject project : projects) {
            assertNoErrors(project, build);
        }
        
	}
	/**
	 * Assert that a given set of projects exists in the workspace. Also forces a full build of
	 * the projects and checks that the projects have no errors.
	 * <p>
	 * If the workspace has extra or missing projects this results in an AssertionFailedError.
	 */
	public void assertProjects(String... names) throws CoreException {
		IProject[] projects = buildProjects();
		Set<String> expected = new HashSet<String>(Arrays.asList(names));
		System.out.println("CHECKING FOR ERRORS IN PROJECTS");
		
		for (IProject p : projects) {
			String nameSeen = p.getName();
			assertTrue("Unexpected project found in workspace: "+nameSeen, 
					expected.contains(nameSeen) || nameSeen.equals("Servers"));
			expected.remove(nameSeen);
			assertNoErrors(p, false);
		}
		assertTrue("Some expected projects where not found: "+expected, expected.isEmpty());
	}
	
	public void assertErrors(IProject project, boolean build, String... expectedRegexps) throws Exception {
		if (build) {
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		}
		GradleClassPathContainer.waitForMarkerUpdates();
		IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		Set<String> notSeenYet = new HashSet<String>(Arrays.asList(expectedRegexps));
		for (IMarker problem : problems) {
			if (problem.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				checkExpectedExpectedError(notSeenYet, expectedRegexps, markerMessage(problem));
			}
		}
		if (notSeenYet.isEmpty()) {
			//ok
		} else {
			StringBuilder error = new StringBuilder("Some expected errors where not found:\n");
			for (String string : notSeenYet) {
				error.append(string+"\n");
			}
			throw new AssertionFailedError(error.toString());
		}
	}

	private void checkExpectedExpectedError(Set<String> notSeenYet, String[] expectedMsgs, String markerMessage) {
		boolean ok = false;
		for (String expected : expectedMsgs) {
			boolean matches = markerMessage.contains(expected);
//			Pattern m = Pattern.compile(expected);
//			boolean matches = m.matcher(markerMessage).find();
			if (matches) {
				notSeenYet.remove(expected);
				ok = true;
			}
		}
		if (!ok) {
			fail("unexpected problem marker found: "+markerMessage);
		}
	}

	protected IProject[] buildProjects() throws CoreException {
		//Wait long enough for the gradle model to become available.
		IProject[] projects = getProjects();
		for (IProject p : projects) {
			GradleProject gp = GradleCore.create(p);
			gp.getGradleModel(new NullProgressMonitor()); 
		}
		
		//Now do an eclipse build
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		System.out.println("CLEANING WORKSPACE...");
		ws.build(IncrementalProjectBuilder.CLEAN_BUILD, new LoggingProgressMonitor());
		System.out.println("BUILDING WORKSPACE...");
		ws.build(IncrementalProjectBuilder.FULL_BUILD, new LoggingProgressMonitor());
		return projects;
	}

	public static IProject[] getProjects() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}

	public static void assertContains(String expected, String string) {
		if (!string.contains(expected)) {
			fail("Didn't find expected substring '"+expected+"' in \n'"+string+"'");
		}
	}
	
	/**
	 * Imports a test project and all its subprojects into the workspace.
	 */
	public static void importTestProject(String projectName) throws IOException,
			NameClashException, CoreException, ExistingProjectException,
			MissingProjectDependencyException {
		File testProj = getTestProjectCopy(projectName);
		importTestProject(testProj);
	}

	/**
	 * Creates a 'fresh' copy of a test project and return a reference to its new location in the
	 * file system. The project is not yet imported in the workspace.
	 */
	public static File getTestProjectCopy(String projectName) throws IOException {
		return getTestFolderCopy("resources/projects/"+projectName);
	}

	/**
	 * Imports a test project and all its subprojects into the workspace.
	 */
	public void importSampleProject(final String projectName) throws Exception,
			NameClashException, CoreException, ExistingProjectException,
			MissingProjectDependencyException {
		importTestProject(extractJavaSample(projectName));
	}

	/**
	 * Extracts one of the java sample projects from the official Gradle distribution zip, placing it into a new folder
	 * somewhere in the /tmp directory. 
	 * 
	 * @return A File pointing to the root of the extracted project.
	 */
	public static File extractJavaSample(final String projectName)
			throws IOException, Exception, URISyntaxException {
		final File targetLocation = new File(TestUtils.createTempDirectory(), projectName);
		DownloadManager.getDefault().doWithDownload(new URI("http://services.gradle.org/distributions/gradle-1.0-all.zip"), new DownloadRequestor() {
			public void exec(File downloadedFile) throws Exception {
				ZipFileUtil.unzip(downloadedFile.toURI().toURL(), targetLocation, "gradle-1.0/samples/java/"+projectName);
			}
		});
		return targetLocation;
	}

//	public String javaSamplePath(String projectName) {
//		return "resources/gradle-1.0-milestone-9/samples/java/"+projectName;
//	}
	
	private ErrorHandler ignoreErrors(final ErrorHandler superHandler, final String[] ignoreableErrors) {
		return new ErrorHandler() {
			@Override
			public void handle(int severity, Throwable e) {
				String msg = e.getMessage();
				if (msg!=null) {
					for (String ignoreMsg : ignoreableErrors) {
						if (msg.contains(ignoreMsg)) {
							//Message found...don't forward to super handler.
							return;
						}
					}
				}
				superHandler.handle(severity, e);
			}
			
			@Override
			protected void internalHandle(int severity, Throwable e) {
				//We shouldn't get here because all calls to handle should either be completely ignored, 
				//or delegated to the superHandler
				throw new Error("Shouldn't get here");
			}
			
			@Override
			public void rethrowAsCore() throws CoreException {
				superHandler.rethrowAsCore();
			}
		};
	}
	
	public void performImport(final GradleImportOperation importOp, final String... ignoreableErrors)
			throws InterruptedException, ExistingProjectException,
			MissingProjectDependencyException {
				importOp.verify();
				Job job = JobUtil.schedule(new GradleRunnable("Import Gradle project") {
					@Override
					public void doit(IProgressMonitor mon) throws Exception {
						importOp.perform(ignoreErrors(new ErrorHandler.Test(IStatus.ERROR), ignoreableErrors), mon);
					}
			
				});
				job.join();
				assertOKStatus(job.getResult());
	}
	
	public static void assertOKStatus(IStatus status) {
		if (!status.isOK()) {
			fail(status.toString());
		}
	}

	public static void importTestProject(File testProj)
			throws NameClashException, CoreException, ExistingProjectException,
			MissingProjectDependencyException {
		importTestProjectOperation(testProj)
			.perform(defaultTestErrorHandler(), new NullProgressMonitor());
	}

	public static GradleImportOperation importTestProjectOperation(File testProj)
			throws NameClashException, CoreException, ExistingProjectException,
			MissingProjectDependencyException {
		GradleImportOperation importOp = GradleImportOperation.importAll(testProj);
		importOp.verify();
		return importOp;
	}
	
	/**
	 * Checkouts the given git project and creates an import operation to import the project and all its subproject.
	 */
	public static GradleImportOperation importGitProjectOperation(GitProject project) throws IOException, InterruptedException, NameClashException, ExistingProjectException, MissingProjectDependencyException, CoreException {
		return importGitProjectOperation(project, false);
	}

	/**
	 * Checkouts the given git project and creates an import operation to import the project and all its subproject.
	 * Set forcePull to be true if the project should be pulled to the specified ref
	 */
	public static GradleImportOperation importGitProjectOperation(GitProject project, boolean forcePull) throws IOException, InterruptedException, NameClashException, ExistingProjectException, MissingProjectDependencyException, CoreException {
	    File projectDir;
        if (forcePull) {
            projectDir = project.forcePull();
        } else {
            projectDir = project.checkout();
        }
        return importTestProjectOperation(projectDir);
	}
	
	
	public static void importGitProject(GitProject project) throws IOException, InterruptedException, NameClashException, ExistingProjectException, MissingProjectDependencyException, CoreException {
		GradleImportOperation op = importGitProjectOperation(project);
		op.perform(defaultTestErrorHandler(), new NullProgressMonitor());
	}

	public static Test defaultTestErrorHandler() {
		return new ErrorHandler.Test(IStatus.ERROR);
	}
	

	public static String markerMessage(IMarker m) throws CoreException {
		StringBuffer msg = new StringBuffer("marker {\n");
		msg.append("   type: "+m.getType());
		msg.append("   resource: "+m.getResource()+"\n");
		for (Object name : m.getAttributes().keySet()) {
			msg.append("   "+name+": "+m.getAttribute((String)name, "<nothing>")+"\n");
		}
		msg.append("}");
		return msg.toString();
	}
	
	
	public static IJavaProject getJavaProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return JavaCore.create(project);
	}
	
	public static IProject getProject(String name) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return project;
	}
	
	public static GradleProject getGradleProject(String eclipseName) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(eclipseName);
		if (project!=null) {
			return GradleCore.create(project);
		}
		return null;
	}

	public static void assertJarEntry(IJavaProject project, String jarName, boolean expectSource) throws JavaModelException {
		IClasspathEntry[] classpath = project.getResolvedClasspath(false);
		StringBuffer seen = new StringBuffer();
		for (IClasspathEntry entry : classpath) {
			if (entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
				seen.append(entry.getPath()+"\n");
				if (entry.getPath().toString().endsWith(jarName)) {
					//Found the jar!
					IPath sourcePath = entry.getSourceAttachmentPath();
					if (expectSource) {
						assertNotNull(entry.getPath()+" has no source attachement", sourcePath);
						assertTrue(entry.getPath()+" has source attachement "+sourcePath+" but it doesn't exist",
								sourcePath.toFile().exists());
					}
					return; //OK
				}
			}
		}
		fail("Jar entry not found: "+jarName+"\nfound: "+seen.toString());
	}
	
	public static void assertSourceFolder(IJavaProject project, String pathInProject) throws JavaModelException {
		IClasspathEntry[] classpath = project.getRawClasspath();
		StringBuffer seen = new StringBuffer();
		for (IClasspathEntry entry : classpath) {
			if (entry.getEntryKind()==IClasspathEntry.CPE_SOURCE) {
				seen.append(entry.getPath()+"\n");
				if (entry.getPath().toString().equals("/"+project.getElementName()+"/"+pathInProject)) {
					//Found the source folder
					return; //OK
				}
			}
		}
		fail("Source folder not found: "+pathInProject+"\nfound: "+seen.toString());
	}

	/**
	 * Find a directory in the "test resources" of this test bundle and copy its contents into a new
	 * temporary directory. Return a reference to the new directory.
	 */
	public static File getTestFolderCopy(String path) throws IOException {
		File orgFolder = GradleTest.getTestFile(path);
		return getFolderCopy(orgFolder);
	}
	
	/**
	 * Copy the contents of a given folder to a new temp folder. The temp folder
	 * will have the same name as the original folder in its last path segment.
	 */
	public static File getFolderCopy(File orgFolder) throws IOException {
		boolean done = false;
		File copyFolder = null; 
		while (!done) {
			//This funny loop is here because during shutdown interrupts are being delivered to
			//test thread causing it to throw the exception below and aborting the test. This can
			//sometimes disrupt the last test in a suite because it got 'interrupted'. 
			//Apparantly Eclipse JUnit plugin testing harness already proceeds to shutting down eclipse,
			//while the last test is still running.
			//Mostly this doesn't cause problems, unless the test is doing something that is susceptible to
			//'interrupts', like the file copying methods in apache FileUtils.
			//See also here: http://stackoverflow.com/questions/1161297/why-are-we-getting-closedbyinterruptexception-from-filechannel-map-in-java-1-6
			try {
				copyFolder = new File(TestUtils.createTempDirectory(), orgFolder.getName());
				FileUtils.copyDirectory(orgFolder, copyFolder);
				done = true;
			} catch (ClosedByInterruptException e) {
				e.printStackTrace();
				Thread.interrupted(); // clear interrupted status
				//Ignore and try again to copy the stuff and complete the test.
			}
		}
		return copyFolder;
	}

	/**
	 * Find a folder "resources/data/<folder-name>" and copy it into the project 
	 * as a subfolder of the project, with name <folder-name>
	 */
	public static void addTestFolder(IJavaProject jproject, String folder) throws IOException, CoreException {
		File orgFolder = getTestFile("resources/data/"+folder);
		File projectFolder = jproject.getProject().getLocation().toFile();
		FileUtils.copyDirectory(orgFolder, new File(projectFolder, folder));
		jproject.getProject().getFolder(new Path(folder)).refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}


	public static void createFile(IProject project, String projPath, String contents) throws CoreException {
		IFile file = project.getFile(new Path(projPath));
		if (file.exists()) {
			file.delete(true, null);
		}
		file.create(new ByteArrayInputStream(contents.getBytes()), true, null);
	}

	public static void createFolder(IProject project, String srcPath) throws Exception {
		IFolder folder = project.getFolder(new Path(srcPath));
		if (folder.exists()) {
			folder.delete(true, new NullProgressMonitor());
		}
		create(folder);
	}

	private static void create(IFolder folder) throws Exception {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder && !parent.exists()) {
			create(((IFolder)parent));
		}
		folder.create(true, true, new NullProgressMonitor());
	}

	/**
	 * Creates a simple Gradle project containing just a single "build.gradle" file and
	 * nothing else.
	 */
	public static GradleImportOperation simpleProjectImport(String projName, String buildFileContents) throws Exception {
		File folder = TestUtils.createTempDirectory(projName);
		File buildFile = new File(folder, "build.gradle");
		FileUtils.writeStringToFile(buildFile, buildFileContents);
		return importTestProjectOperation(folder);
	}
	
	/**
	 * Creates a simple Gradle project containing just a single "build.gradle" file and
	 * nothing else.
	 */
	public static IJavaProject simpleProject(String projName, String buildFileContents) throws Exception {
		simpleProjectImport(projName, buildFileContents).perform(defaultTestErrorHandler(), new NullProgressMonitor());
		return getJavaProject(projName);
	}

	/**
	 * Creates a completely empty gradle project without even so much as a build file.
	 */
	public static GradleProject emptyGradleJavaProject(String projName) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		
		GradleProject gp = GradleCore.create(project);
		ErrorHandler eh = new ErrorHandler.Test();
		gp.convertToGradleProject(ProjectMapperFactory.workspaceMapper(), eh, new NullProgressMonitor());
		return gp;
	}
	
	/**
	 * @param rawClasspath
	 */
	public static void assertClasspathContainer(IClasspathEntry[] rawClasspath, String pathStr) {
		IPath path = new Path(pathStr);
		StringBuilder msg = new StringBuilder();
		for (IClasspathEntry e : rawClasspath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
				if (path.equals(e.getPath())) {
					return; //found it
				}
			}
			msg.append(e+"\n");
		}
		fail("Not found classpath container '"+pathStr+"':\n"+msg.toString());
	}

	public static void assertClasspathJarEntry(String jarFile, IClasspathEntry[] resolvedClasspath) {
		StringBuilder msg = new StringBuilder();
		for (IClasspathEntry e : resolvedClasspath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
				IPath path = e.getPath();
				if (jarFile.equals(path.lastSegment())) {
					return; //OK
				}
				msg.append(e+"\n");
			}
		}
		fail("Not found '"+jarFile+"':\n"+msg.toString());
	}
	
	public static void assertNoClasspathJarEntry(String jarFile, IClasspathEntry[] resolvedClasspath) {
		for (IClasspathEntry e : resolvedClasspath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_LIBRARY) {
				IPath path = e.getPath();
				if (jarFile.equals(path.lastSegment())) {
					fail("Found '"+jarFile+"'\n");
				}
			}
		}
		//ok!
	}



	public static void assertClasspathProjectEntry(IProject expectProject, IJavaProject project) throws JavaModelException {
		IClasspathEntry[] classpath = project.getRawClasspath();
		StringBuilder msg = new StringBuilder();
		for (IClasspathEntry e : classpath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_PROJECT) {
				IPath path = e.getPath();
				if (expectProject.getFullPath().equals(path)) {
					return; //OK
				}
				msg.append(e+"\n");
			}
		}
		fail("Not found '"+expectProject+"':\n"+msg.toString());
	}

	public static void assertNoClasspathProjectEntry(IProject expectProject, IJavaProject project) throws JavaModelException {
		IClasspathEntry[] classpath = project.getRawClasspath();
		for (IClasspathEntry e : classpath) {
			if (e.getEntryKind()==IClasspathEntry.CPE_PROJECT) {
				IPath path = e.getPath();
				if (expectProject.getFullPath().equals(path)) {
					fail("Found '"+expectProject+"'");
				}
			}
		}
	}


	
	public static void assertClasspathJarEntry(String jarFile, GradleProject project)
			throws JavaModelException {
				assertClasspathJarEntry(jarFile, project.getJavaProject().getResolvedClasspath(true));
			}

	public static void assertNoClasspathJarEntry(String string, IJavaProject jp) throws JavaModelException {
		assertNoClasspathJarEntry(string, jp.getResolvedClasspath(true));
	}
	
	public static void assertSameElements(String[] _expecteds, String[] actuals) {
		StringBuilder msg = new StringBuilder();
		Set<String> expecteds = new HashSet<String>(Arrays.asList(_expecteds));
		for (String a : actuals) {
			if (expecteds.contains(a)) {
				expecteds.remove(a);
			} else {
				msg.append("Not expected: "+a+"\n");
			}
		}
		for (String e : expecteds) {
			msg.append("Not found: "+e+"\n");
		}
		String m = msg.toString();
		if (!"".equals(m)) {
			fail(m);
		}
	}

	public static IProject importEclipseProject(String path) throws Exception {
		File projectDir = getTestProjectCopy(path);
		IPath segments = new Path(path);
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		String projectName = segments.lastSegment();
		IProjectDescription projectDescription = ws.newProjectDescription(projectName);
		projectDescription.setLocation(new Path(projectDir.getAbsolutePath()));

		IProject project = ws.getRoot().getProject(projectName);
		project.create(projectDescription, new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		return project;
	}
	
}