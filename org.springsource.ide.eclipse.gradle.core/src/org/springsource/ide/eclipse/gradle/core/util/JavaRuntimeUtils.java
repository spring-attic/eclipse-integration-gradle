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
package org.springsource.ide.eclipse.gradle.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * Utilities to retrieve info about JVMs configured in the workspace.
 * 
 * @author Kris De Volder
 */
public class JavaRuntimeUtils {
	
	private List<IVMInstall> allInstalls = null; //Lazy initialised

	public List<IVMInstall> getWorkspaceJVMs() {
		if (allInstalls==null) {
			allInstalls = new ArrayList<IVMInstall>();
			IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
			for (int i = 0; i < types.length; i++) {
				IVMInstallType type = types[i];
				IVMInstall[] installs = type.getVMInstalls();
				for (int j = 0; j < installs.length; j++) {
					IVMInstall install = installs[j];
					allInstalls.add(install);
				}
			}
		}
		return allInstalls;
	}
	
	public String[] getWorkspaceJVMNames() {
		List<IVMInstall> jvms = getWorkspaceJVMs();
		String[] names = new String[jvms.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = jvms.get(i).getName();
		}
		return names;
	}

	public IVMInstall getInstall(String name) {
		List<IVMInstall> installs = getWorkspaceJVMs();
		for (IVMInstall install : installs) {
			if (install.getName().equals(name)) {
				return install;
			}
		}
		return null;
	}
	
	/**
	 * This method returns true if we are fairly certain that the given JVM is a JRE and not a proper
	 * JDK. If we can't determine whether it is a JDK then we conservatibley return false.
	 */
	public static boolean hasTheJREProblem(IVMInstall jvm) {
		String os = System.getProperty("os.name");
		if (os!=null) {
			if (os.startsWith("Windows") || os.equals("Linux")) {
				//Note: See http://lopica.sourceforge.net/os.html (list of OS names)
				//Only consider OS's where we are relatively confident that the check is valid
				if (jvm!=null) {
					File javaHome = jvm.getInstallLocation();
					if (javaHome!=null && javaHome.exists()) {
						File toolsJar = new File(new File(javaHome, "lib"), "tools.jar");
						return !toolsJar.exists();
					}
				}
			}
		}
		return false; // Conservatively assume check for 'not a JRE' is passing if unknown OS, or Mac OS, or something went wrong,
		// or Java home doesn't exist  etc.
	}

	
}
