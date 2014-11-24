/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.springsource.ide.eclipse.gradle.core.GradleCore;

/**
 * Provides some tools to ensure we have a Java 6 or 7 compatible Java runtime 
 * configured in the workspace. This is because some test projects
 * require specific Java runtimes to import and compile correctly.
 * 
 * @author Kris De Volder
 */
public class JavaXXRuntime {

	//By default the Eclipse workspace probably has no knowledge of any Java 7 runtimes.
	// So we must provide the means to find it. Below is a list of places that we will
	// check in order. If something exists there we will assume it is a Java 7 JDK.
	
	static String[] java8locations = {
		"/home/kdvolder/Applications/jdk1.8.0_20", // On Kris's machine
		"/opt/java/jdk/Sun/1.8",  // on springsource build server
		"/opt/java/jdk/Sun/8.0", // in spring.io build server
		"/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home"
	};
	static String[] java7locations = {
		"/home/kdvolder/Applications/jdk1.7.0_17", // On Kris's machine
		"/opt/java/jdk/Sun/1.7",  // on springsource build server
		"/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home"
	};
	static String[] java6locations = {
		"/home/kdvolder/Applications/jdk1.6.0_45", // On Kris's machine
		"/usr/lib/jvm/java-6-sun", // On a ubuntu machine that has sun java 6 installed
		"/usr/lib/jvm/java-6-oracle", // On a ubuntu machine that has oracle java 6 installed
		"/opt/java/jdk/Sun/1.6",  // on springsource build server
		"/opt/java/jdk/Sun/6.0", // in spring.io build server
		"/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home"
	};
	
	/**
	 * Switch 'everyone' to using Java 8 as a default.
	 */
	public static void java8everyone() throws CoreException {
		javaXXeveryone("1.8");
	}
	
	public static void java7everyone() throws CoreException {
		javaXXeveryone("1.7");
	}

	/**
	 * Switch 'everyone' to using Java 6 as a default.
	 */
	public static void java6everyone() throws CoreException {
		javaXXeveryone("1.6");
	}
	
	public static void javaXXeveryone(String version) throws CoreException {
		//1: The eclipse workspace default VM should be Java 7
		IVMInstall vm = ensureJavaXXdefaultVM(version); 
		//2: Compiler/source compliance for JDT
		JavaUtils.setJavaXXCompliance(version); //Compiler/source compliance also Java 7
		//3: Force Gradle JVM default
		GradleCore.getInstance().getPreferences().setJavaHomeJREName(vm.getName());
	}

	public static IVMInstall ensureJavaXXdefaultVM(String version) throws CoreException {
		//Before doing anything check the current default VM
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		if (JavaUtils.isJavaXX(vm, version)) {
			return vm; //Done!
		}
		
		vm = getJavaXXVM(version);
		if (vm==null) {
			vm = JavaUtils.createVM(getVMLocation(version));
			//Seems the name is not generated (any more?) so we have to set something reasonable:
			vm.setName("java_"+version);
		}
		if (!JavaUtils.isJavaXX(vm, version)) {
			throw new Error("vm at "+vm.getInstallLocation()+ " doesn't look like a Java "+version+". Eclipse thinks it is a Java "+JavaUtils.getVersion(vm));
		}
		JavaRuntime.setDefaultVMInstall(vm, new NullProgressMonitor());
		return vm;
	}

	private static IVMInstall getJavaXXVM(String version) {
		List<IVMInstall> vms = JavaUtils.getAllVMs();
		for (IVMInstall vm : vms) {
			if (JavaUtils.isJavaXX(vm, version)) {
				return vm; //Found one
			}
		}
		//Didn't find one
		return null;
	}

	/**
	 * Find an existing Sun Java XX installation on the local disk.
	 */
	private static File getVMLocation(String version) {
		String[] locations = null;
		if (version.equals("1.8")) {
			locations = java8locations;
		} else if (version.equals("1.7")) {
			locations = java7locations;
		} else if (version.equals("1.6")) {
			locations = java6locations;
		}
		for (String lookin : locations) {
			if (new File(lookin).isDirectory()) {
				return new File(lookin);
			}
		}
		throw new Error("Couldn't find a Java "+version+" VM");
	}

}
