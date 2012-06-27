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
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

/**
 * Utility methods related to Java versions, VMs and stuff like that.
 * 
 * @author Kris De Volder
 */
public class JavaUtils {

	private static final String STANDARD_VM_TYPE_ID = "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType";
//	private static final IVMInstallType STANDARD_VM_TYPE = JavaRuntime.getVMInstallType(STANDARD_VM_TYPE_ID);

	public static boolean defaultJVMInstallIsJava16() {
		IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
		return isJava16(defaultVM);
	}

	public static List<IVMInstall> getAllVMs() {
		List<IVMInstall> result = new ArrayList<IVMInstall>();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			for (IVMInstall vm : type.getVMInstalls()) {
				result.add(vm);
			}
		}
		return result;
	}
	
	/**
	 * @return true if given VM can be determined to be Java 1.6 (or above).
	 */
	public static boolean isJava16(IVMInstall vm) {
		if (vm instanceof IVMInstall2) {
			IVMInstall2 defaultVM = (IVMInstall2) vm;
			String version = defaultVM.getJavaVersion();
			if (version!=null) {
				return "1.6".compareTo(version)<=0;
			}
		}
		return false;
	}

	public static void ensureJava16Default() throws CoreException {
		debug("Checking for Java 16 Default Workspace VM...");
		debug("Current workspace default VM = "+JavaRuntime.getDefaultVMInstall().getInstallLocation());
		
		//Check the current default first
		if (defaultJVMInstallIsJava16()) {
			debug("Java 16 FOUND");
			return;
		}
		
		//Next try to find an existing VM that meets our need...
		debug("Checking for an existing Java 16 (non-default) Workspace VM...");
		List<IVMInstall> allVMs = getAllVMs();
		for (IVMInstall vm : allVMs) {
			if (isJava16(vm)) {
				//Found a good one... so use it.
				debug("Java 16 FOUND: "+vm.getInstallLocation());
				JavaRuntime.setDefaultVMInstall(vm, new NullProgressMonitor());
				debug("Changed workspace default VM = "+JavaRuntime.getDefaultVMInstall().getInstallLocation());
				return;
			}
		}
		
		//All else failed, create a new VM... this paths used in this code are rather specific to springsource.com build server setup.
		debug("Trying to find and configure a Java 16 Default Workspace VM...");
		IVMInstallType vmType = JavaRuntime.getVMInstallType(STANDARD_VM_TYPE_ID);
		String id = createVMId(vmType);
		VMStandin detectedVMStandin = new VMStandin(vmType, id);
		File detectedLocation = detectJava16Location(vmType);
		if (detectedLocation!=null) {
			debug("Java 16 detection successfull. Attempting to cinfigure detected VM...");
			detectedVMStandin.setInstallLocation(detectedLocation);
			detectedVMStandin.setName("Java16");
			if (vmType instanceof AbstractVMInstallType) {
				AbstractVMInstallType abs = (AbstractVMInstallType)vmType;
				URL url = abs.getDefaultJavadocLocation(detectedLocation);
				detectedVMStandin.setJavadocLocation(url);
				String arguments = abs.getDefaultVMArguments(detectedLocation);
				if (arguments != null) {
					detectedVMStandin.setVMArgs(arguments);
				}
			}
			detectedVMStandin.convertToRealVM();
			debug("Converted standin VM to real VM");
			JavaRuntime.saveVMConfiguration();
			debug("Save VM config infos");
		}
	}
	
	private static void debug(String string) {
		System.out.println(string);
	}

	private static File detectJava16Location(IVMInstallType type) {
		String[] placesToLook = {
				"/opt/java/jdk/Sun/1.6" //Where it lives on springsource build server
		};
		
		for (String pathStr : placesToLook) {
			debug("Trying location: "+pathStr);
			File path = new File(pathStr);
			IStatus validationStatus = type.validateInstallLocation(path);
			if (validationStatus.isOK()) {
				debug("Validation OK: "+pathStr);
				return path;
			} else {
				debug("Validation failed: "+pathStr+" msg = "+validationStatus.getMessage());
			}
		}
		return null;
	}

	public static String createVMId(IVMInstallType type) {
		//This code based on code copied from 
		//org.eclipse.jdt.launching.JavaRuntime.detectEclipseRuntime()
		
		// Make sure the VM id is unique
		long unique = System.currentTimeMillis();
		while (type.findVMInstall(String.valueOf(unique)) != null) {
			unique++;
		}
		return String.valueOf(unique);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setJava15Compliance() {
		Hashtable options = JavaCore.getDefaultOptions();
	    options.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
	    options.put(JavaCore.COMPILER_SOURCE, "1.5");
	    JavaCore.setOptions(options);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setJava16Compliance() {
		Hashtable options = JavaCore.getDefaultOptions();
	    options.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
	    options.put(JavaCore.COMPILER_SOURCE, "1.6");
	    JavaCore.setOptions(options);
	}
	
}
