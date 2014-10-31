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

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFix.MakeTypeAbstractOperation;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.launch.GradleProcess;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.util.Distributions;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;


/**
 * @author Kris De Volder
 */
public class GradleTaskRunTest extends GradleTest {
	
	/**
	 * Example 5.3 from Gradle Docs. Same as above, but with slightly different build file.
	 */
	public void testShortcutTaskDef() throws Exception {
		String name = "shortcutTaskDef";
		IJavaProject jproject = simpleProject(name, 
				"task hello << {\n" + 
				"    println 'Hello world!'\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		Set<String> tasks = project.getAllTasks(new NullProgressMonitor());
		assertTrue(tasks.contains(":hello"));
		
		ILaunchConfigurationWorkingCopy launchConf = (ILaunchConfigurationWorkingCopy) GradleLaunchConfigurationDelegate.createDefault(project, false);
		GradleLaunchConfigurationDelegate.setTasks(launchConf, Arrays.asList(":hello"));
		GradleProcess process = LaunchUtil.synchLaunch(launchConf);
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("BUILD SUCCESSFUL", output);
		assertContains("Hello world!", output);
	}
	
	/**
	 * Regression test for STS 2280: check that location isn't set in a launch configuration if the project 
	 * exists in the Eclipse workspace.
	 */
	public void testNoLocationInLaunchConf() throws Exception {
		String name = "noLocation";
		IJavaProject jproject = simpleProject(name, 
				"task upper << {\n" + 
				"    String someString = 'mY_nAmE'\n" + 
				"    println \"Original: \" + someString \n" + 
				"    println \"Upper case: \" + someString.toUpperCase()\n" + 
				"}"
		);
		assertProjects(name);
		
		ILaunchConfiguration conf = GradleLaunchConfigurationDelegate.createDefault(GradleCore.create(jproject), false);
		assertEquals("not set", conf.getAttribute(GradleLaunchConfigurationDelegate.PROJECT_LOCATION, "not set"));
		assertEquals(GradleCore.create(jproject), GradleLaunchConfigurationDelegate.getProject(conf));
	}
	
	/**
	 * Example 5.4 build script with some executable code
	 */
	public void testBuildScriptsAreCode() throws Exception {
		String name = "buildScriptsAreCode";
		IJavaProject jproject = simpleProject(name, 
				"task upper << {\n" + 
				"    String someString = 'mY_nAmE'\n" + 
				"    println \"Original: \" + someString \n" + 
				"    println \"Upper case: \" + someString.toUpperCase()\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":upper");

		GradleProcess process = LaunchUtil.launchTasks(project, ":upper");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains("BUILD SUCCESSFUL", output);
		assertContains(
				"Original: mY_nAmE\n" + 
				"Upper case: MY_NAME", 
				output);
	}
	
	/**
	 * Example 5.5 using groovy in Gradle tasks
	 */
	public void testUsingGroovy() throws Exception {
		String name = "usingGroovy";
		IJavaProject jproject = simpleProject(name, 
				"task count << {\n" + 
				"    4.times { print \"$it \" }\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":count");

		GradleProcess process = LaunchUtil.launchTasks(project, ":count");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains("BUILD SUCCESSFUL", output);
		assertContains(
				":count\n" +
				"0 1 2 3", 
				output);
		
	}
	
	/**
	 * Example 5.6 task dependencies
	 */
	public void testTaskDependencies() throws Exception {
		String name = "taskDependencies";
		IJavaProject jproject = simpleProject(name, 
				"task hello << {\n" + 
				"    println 'Hello world!'\n" + 
				"}\n" + 
				"task intro(dependsOn: hello) << {\n" + 
				"    println \"I'm Gradle\"\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":hello", ":intro");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":intro");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":hello\n" +
				"Hello world!\n" + 
				":intro\n" +
				"I'm Gradle", 
				output);
		assertContains("BUILD SUCCESSFUL", output);
	}
	
	/**
	 * Example 5.7 Lazy depends on
	 */
	public void testLazyDepends() throws Exception {
		String name = "taskLazyDependencies";
		IJavaProject jproject = simpleProject(name, 
				"task taskX(dependsOn: 'taskY') << {\n" + 
				"    println 'taskX'\n" + 
				"}\n" + 
				"task taskY << {\n" + 
				"    println 'taskY'\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":taskX", ":taskY");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":taskX");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":taskY\n" +
				"taskY\n" + 
				":taskX\n" +
				"taskX", 
				output);
		assertContains("BUILD SUCCESSFUL", output);
	}

	/**
	 * Example 5.8 dynamic creation of a task
	 */
	public void testDynamicCreationOfTask() throws Exception {
		String name = "dynamicTasks";
		IJavaProject jproject = simpleProject(name, 
				"4.times { counter ->\n" + 
				"    task \"task$counter\" << {\n" + 
				"        println \"I'm task number $counter\"\n" + 
				"    }\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":task0", ":task1", ":task2", ":task3");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":task1");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":task1\n" + 
				"I'm task number 1\n", 
				output);
		assertContains("BUILD SUCCESSFUL", output);
	}
	
	/**
	 * Example 5.9 adding a task dep via API
	 */
	public void testDependencyByAPI() throws Exception {
		//TODO: this test is failing, but report submitted:
		// http://issues.gradle.org/browse/GRADLE-1555
		String name = "dependencyByAPI";
		IJavaProject jproject = simpleProject(name, 
				"4.times { counter ->\n" + 
				"    task \"task$counter\" << {\n" + 
				"        println \"I'm task number $counter\"\n" + 
				"    }\n" + 
				"}\n" + 
				"task1.dependsOn task2, task3"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":task0", ":task1", ":task2", ":task3");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":task1");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":task2\n" +
				"I'm task number 2\n" + 
				":task3\n" +
				"I'm task number 3\n" + 
				":task1\n" +
				"I'm task number 1", 
				output);
		assertContains("BUILD SUCCESSFUL", output);
	}
	
	/**
	 * Example 5.10 adding a task dep via API
	 */
	public void testAddingBehaviorToTasks() throws Exception {
		String name = "addingBehavior";
		IJavaProject jproject = simpleProject(name, 
				"task hello << {\n" + 
				"    println 'Hello Earth'\n" + 
				"}\n" + 
				"hello.doFirst {\n" + 
				"    println 'Hello Venus'\n" + 
				"}\n" + 
				"hello.doLast {\n" + 
				"    println 'Hello Mars'\n" + 
				"}\n" + 
				"hello << {\n" + 
				"    println 'Hello Jupiter'\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":hello");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":hello");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":hello\n" +
				"Hello Venus\n" + 
				"Hello Earth\n" + 
				"Hello Mars\n" + 
				"Hello Jupiter\n" +
				"\n" +
				"BUILD SUCCESSFUL", 
				output);
	}
	
	/**
	 * Example 5.11 accessing task as a property of the build script
	 */
	public void testTaskAsProperty() throws Exception {
		String name = "taskAsProp";
		IJavaProject jproject = simpleProject(name, 
				"task hello << {\n" + 
				"    println 'Hello world!'\n" + 
				"}\n" + 
				"hello.doLast {\n" + 
				"    println \"Greetings from the $hello.name task.\"\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":hello");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":hello");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":hello\n" +
				"Hello world!\n" + 
				"Greetings from the hello task.\n" +
				"\n" +
				"BUILD SUCCESSFUL", 
				output);
	}

	/**
	 * Example: 5.12 Adding properties to a task
	 */
	public void testTaskAddingProperty() throws Exception {
		String name = "taskAddProp";
		IJavaProject jproject = simpleProject(name, 
				"task myTask\n" + 
				"myTask.ext.myProperty = 'myCustomPropValue'\n" + 
				"\n" + 
				"task showProps << {\n" + 
				"    println myTask.myProperty\n" + 
				"}"
		);
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":myTask", ":showProps");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":showProps");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":showProps\n" +
				"myCustomPropValue\n" +
				"\n" +
				"BUILD SUCCESSFUL", 
				output);
	}
	
	/**
	 * Example 5.13: using Ant tasks
	 */
	public void testTaskUsingAntTask() throws Exception {
		String name = "usingAntTask";
		IJavaProject jproject = simpleProject(name, 
				"task loadfile << {\n" + 
				"    def files = file('./antLoadfileResources').listFiles().sort()\n" + 
				"    files.each { File file ->\n" + 
				"        if (file.isFile()) {\n" + 
				"            ant.loadfile(srcFile: file, property: file.name)\n" + 
				"            println \" *** $file.name ***\"\n" + 
				"            println \"${ant.properties[file.name]}\"\n" + 
				"        }\n" + 
				"    }\n" + 
				"}");
		assertProjects(name);
		
		addTestFolder(jproject, "antLoadfileResources");

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":loadfile");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":loadfile");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":loadfile\n" + 
				" *** bar.txt ***\n" + 
				"This is my bar!\n" + 
				"which is not quite foo!\n" + 
				"instead it is bar...\n" + 
				"but it also has text.\n" + 
				" *** foo.txt ***\n" + 
				"This my foo\n" + 
				"and it has some very foo-like\n" + 
				"text.\n" + 
				"\n" + 
				"BUILD SUCCESSFUL", 
				output);
	}
	
	/**
	 * Example 5.14: using methods
	 */
	public void testTaskUsingMethod() throws Exception {
		String name = "usingMethod";
		IJavaProject jproject = simpleProject(name, 
				"task checksum << {\n" + 
				"    fileList('./antLoadfileResources').each {File file ->\n" + 
				"        ant.checksum(file: file, property: \"cs_$file.name\")\n" + 
				"        println \"$file.name Checksum: ${ant.properties[\"cs_$file.name\"]}\"\n" + 
				"    }\n" + 
				"}\n" + 
				"\n" + 
				"task loadfile << {\n" + 
				"    fileList('./antLoadfileResources').each {File file ->\n" + 
				"        ant.loadfile(srcFile: file, property: file.name)\n" + 
				"        println \"I'm fond of $file.name\"\n" + 
				"    }\n" + 
				"}\n" + 
				"\n" + 
				"File[] fileList(String dir) {\n" + 
				"    file(dir).listFiles({file -> file.isFile() } as FileFilter).sort()\n" + 
				"}");
		assertProjects(name);
		addTestFolder(jproject, "antLoadfileResources");

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":loadfile", ":checksum");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":loadfile");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":loadfile\n" + 
				"I'm fond of bar.txt\n" + 
				"I'm fond of foo.txt\n" + 
				"\n" + 
				"BUILD SUCCESSFUL", 
				output);
		
		process = LaunchUtil.launchTasks(project, ":checksum");
		output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":checksum\n" + 
				"bar.txt Checksum: 5328fe1917a392046586632d35d43aaa\n" + 
				"foo.txt Checksum: 9011ee2c4b32beb0489c54f1ff953b05\n" + 
				"\n" + 
				"BUILD SUCCESSFUL", 
				output);
		
	}

	//TODO: Default tasks... (Example 5.15)
	// Waiting for http://issues.gradle.org/browse/GRADLE-1560

	/**
	 * Example 5.16: using task DAG
	 */
	public void tesUsingTaskDAG() throws Exception {
		String name = "usingMethod";
		IJavaProject jproject = simpleProject(name, 
				"gradle.taskGraph.whenReady {taskGraph ->\n" + 
				"    if (taskGraph.hasTask(':release')) {\n" + 
				"        version = '1.0'\n" + 
				"    } else {\n" + 
				"        version = '1.0-SNAPSHOT'\n" + 
				"    }\n" + 
				"}\n" + 
				"\n" + 
				"task distribution << {\n" + 
				"    println \"We build the zip with version=$version\"\n" + 
				"}\n" + 
				"task release(dependsOn: 'distribution') << {\n" + 
				"    println 'We release now'\n" + 
				"}");
		assertProjects(name);

		GradleProject project = GradleCore.create(jproject);
		assertTasks(project, ":distribution", ":release");
		
		GradleProcess process = LaunchUtil.launchTasks(project, ":distribution");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":distribution\n" + 
				"We build the zip with version=1.0-SNAPSHOT\n" + 
				"\n" + 
				"BUILD SUCCESSFUL", 
				output);
		
		process = LaunchUtil.launchTasks(project, ":release");
		output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		
		assertContains(
				":distribution\n" + 
				"We build the zip with version=1.0\n" + 
				"\n" +
				":release\n" +
				"We release now\n" +
				"\n" + 
				"BUILD SUCCESSFUL", 
				output);
	}
	
	public void testSetSystemPropertyViaGlobalVMArgsForTaskRun() throws Exception {
		IJavaProject project = simpleProject("simplePropTest", 
				"task showProp << {\n" + 
				"	println System.getProperty(\"foo.prop\")\n" + 
				"}");
		GradleProject gp = GradleCore.create(project);
		
		GradleProcess process = LaunchUtil.launchTasks(gp, ":showProp");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains(":showProp\nnull", output);

		GradleCore.getInstance().getPreferences().setJVMArguments("-Dfoo.prop=KrisSetThis");
		process = LaunchUtil.launchTasks(gp, ":showProp");
		output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains(":showProp\nKrisSetThis", output);
	}

	public void testSetSystemPropertyViaLaunchConfVMArgsForTaskRun() throws Exception {
		IJavaProject project = simpleProject("simplePropTest", 
				"task showProp << {\n" + 
				"	println System.getProperty(\"foo.prop\")\n" + 
				"}");
		GradleProject gp = GradleCore.create(project);
		
		String task = ":showProp";
		ILaunchConfiguration conf = GradleLaunchConfigurationDelegate.getOrCreate(gp, task);
		ILaunchConfigurationWorkingCopy workingCopy = conf.getWorkingCopy();
		GradleLaunchConfigurationDelegate.setJVMArguments(workingCopy, "-Dfoo.prop=SetOnLaunchConf");
		workingCopy.doSave();
		
		GradleProcess process = LaunchUtil.synchLaunch(conf);
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains(":showProp\nSetOnLaunchConf", output);
	}
	
	public void testProjectPropertyViaProgramArgsForTaskRun() throws Exception {
		IJavaProject project = simpleProject("simplePropTest", 
				"task showProp << {\n" + 
				"	println myProp\n" + 
				"}");
		GradleProject gp = GradleCore.create(project);
		
		GradleProcess process = LaunchUtil.launchTasks(gp, ":showProp");
		String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains("Could not find property 'myProp' on task ':showProp'", output);

		GradleCore.getInstance().getPreferences().setProgramArguments("-PmyProp=KrisSetThis");
		process = LaunchUtil.launchTasks(gp, ":showProp");
		output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
		assertContains(":showProp\nKrisSetThis", output);
	}
	
	/**
	 * Tests whether it is possible to run something that grabs workspace loc concurrently with the Gradle Build.
	 * @throws Exception 
	 */
	public void testCanRunStuffConcurrentWithTask() throws Exception {
		IJavaProject jp = simpleProject("concurrent", 
				"def waitForFile(file) {\n" + 
				"	file = new File(file)\n" + 
				"	long sleepTime = 0;\n" + 
				"   long sleptFor = 0;\n" +
				"	while (!file.exists()) {\n" + 
				"		println \"Waiting... ${sleptFor}\"\n" + 
				"		sleepTime += 100;\n" + 
				"		Thread.sleep(sleepTime)\n" +
				"		sleptFor += sleepTime\n"+
				"		if (sleptFor > 10000) {\n" +
				"			throw new Error('time out')\n" +
				"		}\n" + 
				"	}\n" + 
				"}\n" + 
				"\n" + 
				"task slow << {\n" + 
				"	File start = new File(\"${projectDir}/START\");\n" + 
				"	println start.absolutePath;\n" + 
				"	start.createNewFile()\n" + 
				"	waitForFile(\"${projectDir}/STOP\")\n" +
				"   println 'Looks like it worked'\n" + 
				"}\n");
		final GradleProject gp = GradleCore.create(jp);
		JobUtil.schedule(JobUtil.NO_RULE, new GradleRunnable("Do some stuff while :slow task is running") {
			@Override
			public void doit(IProgressMonitor mon) throws Exception {
				//Wait until we are certain the slow task is actually running
				File start = new File(gp.getLocation(), "START");
				while (!start.exists()) {
					Thread.sleep(200);
				}
				//Now do something that will make the task stop... do to this in block that grabs a strong lock!
				JobUtil.withRule(JobUtil.buildRule(), mon, 1, new GradleRunnable("concurrent stopper") {
					@Override
					public void doit(IProgressMonitor mon) throws Exception {
						File stop = new File(gp.getLocation(), "STOP");
						stop.createNewFile();
					}
				});
			}
		});
		//The task will create a file 'START' then it waits for a file 'STOP' to be created. Then it terminates.
		//This means that above scheduled Job must be running concurrently for the task to succeed.
		GradleProcess process = LaunchUtil.launchTasks(gp, ":slow");
		
		assertContains("Looks like it worked", process.getStreamsProxy().getOutputStreamMonitor().getContents());
	}
	
	/**
	 * Tests whether task cancellation works.
	 * @throws Exception 
	 */
	public void testTaskCancellation() throws Exception {
		IJavaProject jp = simpleProject("concurrent", 
				"def waitSomeTime(long timeout) {\n" + 
				"	long sleepTime = 0;\n" + 
				"   long sleptFor = 0;\n" +
				"	while (sleptFor < timeout) {\n" + 
				"		println \"Waiting... ${sleptFor}\"\n" + 
				"		sleepTime += 100;\n" + 
				"		Thread.sleep(sleepTime)\n" +
				"		sleptFor += sleepTime\n"+
				"	}\n" + 
				"}\n" + 
				"\n" + 
				"task slow << {\n" + 
				"	File start = new File(\"${projectDir}/START\");\n" + 
				"	println start.absolutePath;\n" + 
				"	start.createNewFile()\n" + 
				"	waitSomeTime(20000)\n" +
				"}\n");
		final GradleProject gp = GradleCore.create(jp);
		
		ILaunch launch = LaunchUtil.createLaunch(gp, ":slow");
		final GradleProcess process = LaunchUtil.findGradleProcess(launch);

		new Job("Cancellation") {

			@Override
			protected IStatus run(IProgressMonitor arg0) {
				File start = new File(gp.getLocation(), "START");
				for (long counter = 0; counter <= 10000 && !start.exists(); counter += 200) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					process.terminate();
				} catch (DebugException e) {
					e.printStackTrace();
				}

				for (long counter = 0; counter <= 10000 && !process.isTerminated(); counter += 200) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				return Status.OK_STATUS;
			}
			
		}.schedule();

		LaunchUtil.synchLaunch(launch);
		assertContains("Build cancelled", process.getStreamsProxy().getOutputStreamMonitor().getContents());
	}

	public void testSimpleTaskWithDifferentReleases() throws Exception {
		class Result {
			int minorVersion;
			Throwable error;
			private int majorVersion;
			public Result(int majorVersion, int minorVersion) {
				this.majorVersion = majorVersion;
				this.minorVersion = minorVersion;
			}
			@Override
			public String toString() {
				return "Release "+majorVersion+"."+minorVersion+" = "+ ((error==null) ? "OK" : ExceptionUtil.getMessage(error));
			}
		}
		String[] versions = {  
				"1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9", "1.10", "1.11", "1.12",
				"2.0", "2.1"
		};
		Result[] results = new Result[versions.length];
		boolean failed = false;
		for (int i = 0; i < results.length; i++) {
			String[] parts = versions[i].split("\\.");
			int majorVersion = Integer.parseInt(parts[0]);
			int minorVersion = Integer.parseInt(parts[1]);
			results[i] = new Result(majorVersion, minorVersion);
			try {
				doSimpleTaskWithRelease(majorVersion, minorVersion);
			} catch (Throwable e) {
				failed = true;
				e.printStackTrace();
				results[i].error = e;
			}
		}
		if (failed) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < results.length; i++) {
				buf.append(results[i]+"\n");
			}
			throw new Error(buf.toString());
		}
	}
	
	public void doSimpleTaskWithRelease(int majorVersion, int minorVersion) throws Exception {
		GradleCore.getInstance().getPreferences().setDistribution(Distributions.releaseURI(majorVersion, minorVersion));

		String name = "releaseTest"+majorVersion+"_"+minorVersion;
		try {
			IJavaProject jproject = simpleProject(name, 
					"task hello << {\n" + 
							"    println 'Hello world!'\n" + 
							"}"
					);
			assertProjects(name);

			GradleProject project = GradleCore.create(jproject);
			Set<String> tasks = project.getAllTasks(new NullProgressMonitor());
			assertTrue(tasks.contains(":hello"));

			ILaunchConfigurationWorkingCopy launchConf = (ILaunchConfigurationWorkingCopy) GradleLaunchConfigurationDelegate.createDefault(project, false);
			GradleLaunchConfigurationDelegate.setTasks(launchConf, Arrays.asList(":hello"));
			GradleProcess process = LaunchUtil.synchLaunch(launchConf);
			String output = process.getStreamsProxy().getOutputStreamMonitor().getContents();
			assertContains("BUILD SUCCESSFUL", output);
			assertContains("Hello world!", output);
		} finally {
			IProject project = getProject(name);
			if (project.exists()) {
				project.delete(true, true, new NullProgressMonitor());
			}
		}
	}
	
	public static void assertTasks(GradleProject project, String... expectedTasks) throws OperationCanceledException, CoreException {
		Set<String> actualTasks = project.getAllTasks(new NullProgressMonitor());
		for (String expectedTask : expectedTasks) {
			assertTrue("Expected task is missing "+expectedTask, actualTasks.contains(expectedTask));
		}
	}

}
