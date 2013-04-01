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

import static org.eclipse.jdt.internal.launching.StandardVMType.ID_STANDARD_VM_TYPE;

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
		return isJavaXX(vm, "1.6");
	}

	/**
	 * @return true if given VM can be determined to be Java 1.6 (or above).
	 */
	public static boolean isJava17(IVMInstall vm) {
		return isJavaXX(vm, "1.7");
	}

	public static boolean isJavaXX(IVMInstall vm, String wantedVersion) {
		if (vm instanceof IVMInstall2) {
			IVMInstall2 defaultVM = (IVMInstall2) vm;
			String version = defaultVM.getJavaVersion();
			if (version!=null) {
				return version.startsWith(wantedVersion);
			}
		}
		return false;
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
	
	public static void setJava15Compliance() {
		setJavaXXCompliance("1.5");
	}
	public static void setJava16Compliance() {
		setJavaXXCompliance("1.6");
	}
	public static void setJava7Compliance() {
		setJavaXXCompliance("1.7");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setJavaXXCompliance(String version) {
		Hashtable options = JavaCore.getDefaultOptions();
	    options.put(JavaCore.COMPILER_COMPLIANCE, version);
	    options.put(JavaCore.COMPILER_SOURCE, version);
	    JavaCore.setOptions(options);
	}
	
	/**
	 * Create a new IVMInstall of DefaultVM install type.
	 * @return
	 */
	public static IVMInstall createVM(File location) {
		@SuppressWarnings("restriction")
		IVMInstallType vmType = JavaRuntime.getVMInstallType(ID_STANDARD_VM_TYPE);
		IVMInstall vm = vmType.createVMInstall(createVMId(vmType));
		vm.setInstallLocation(location);
		return vm;
	}


}
