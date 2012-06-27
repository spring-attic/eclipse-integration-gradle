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
package org.springsource.ide.eclipse.gradle.core.dsld;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleAPIProperties;


/**
 * @author Kris De Volder
 */
public class GradleDSLDClasspathContainer implements IClasspathContainer {
	
	public static final String ID = "org.springsource.ide.eclipse.gradle.dsld.classpathcontainer";
	
	private IClasspathEntry[] entries = null;

	public IClasspathEntry[] getClasspathEntries() {
		if (entries==null) {
			ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
			addGradleAPIJars(entryList);
			addDSLDFolder(entryList);
			entries = entryList.toArray(new IClasspathEntry[entryList.size()]);
		}
		return entries; 
	}

	private ArrayList<IClasspathEntry> addGradleAPIJars(ArrayList<IClasspathEntry> entryList) {
		GradleAPIProperties props = GradleCore.getInstance().getAPIProperties();
		List<File> jars = props.getGradleAPIJars();
		for (File jarFile : jars) {
			String sourceJarName = jarFile.getName();
			Assert.isTrue(sourceJarName.endsWith(".jar"));
			sourceJarName = sourceJarName.substring(0, sourceJarName.length()-".jar".length()) + "-sources.jar";
			File sourcesJar = new File(jarFile.getParentFile(), sourceJarName);
			IPath sourcesPath = null;
			if (sourcesJar.exists()) {
				sourcesPath = new Path(sourcesJar.toString());
			}
			entryList.add(JavaCore.newLibraryEntry(new Path(jarFile.toString()), sourcesPath, null, false));
		}
		return entryList;
	}

	private void addDSLDFolder(ArrayList<IClasspathEntry> entryList) {
		try {
			Bundle bundle = Platform.getBundle(GradleCore.PLUGIN_ID);
			File bundleFile = FileLocator.getBundleFile(bundle);
			if (bundleFile != null && bundleFile.exists() && bundleFile.isDirectory()) {
				File dslSupport = new File(bundleFile, "dslSupport");
				if (dslSupport.exists() && dslSupport.isDirectory()) {
					entryList.add(JavaCore.newLibraryEntry(new Path(dslSupport.toString()), null, null, false));
				} else {
					GradleCore.log("Couldn't find: '"+dslSupport+"'");
				}
			} else {
				GradleCore.log("Trouble accessing the bundle: '"+GradleCore.PLUGIN_ID+" as a directory. check that it is packaged as an 'explodedBundle'");
			}
		} catch (IOException e) {
			GradleCore.log(e);
		}
	}
	
	public String getDescription() {
		return "Gradle DSLD support";
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return new Path(ID);
	}

	public static boolean isOnClassPath(IJavaProject project) {
		try {
			IClasspathEntry[] classpath = project.getRawClasspath();
			for (IClasspathEntry e : classpath) {
				if (isGradleDSLDContainerEntry(e)) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			GradleCore.log(e);
		}
		return false;
	}

	private static boolean isGradleDSLDContainerEntry(IClasspathEntry e) {
		return e!=null && e.getEntryKind()==IClasspathEntry.CPE_CONTAINER && e.getPath().segment(0).equals(ID);
	}

	/**
	 * Removes {@link GradleDSLDClasspathContainer} entries from the project's classpath.
	 */
	public static void removeFrom(IJavaProject javaProject, IProgressMonitor mon) throws JavaModelException {
		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		ClassPath newClasspath = new ClassPath(GradleCore.create(javaProject), classpath.length);
		for (IClasspathEntry e : classpath) {
			if (!isGradleDSLDContainerEntry(e)) {
				newClasspath.add(e);
			}
		}
		newClasspath.setOn(javaProject, mon);
	}

	public static void addTo(IJavaProject project, IProgressMonitor mon) throws JavaModelException {
		mon.beginTask("Add classpath container", 10);
		try {
			mon.worked(1);
			if (!isOnClassPath(project)) {
				//Only add it if itsn't there yet
				ClassPath classpath = new ClassPath(GradleCore.create(project), project.getRawClasspath());
				classpath.add(JavaCore.newContainerEntry(new Path(ID), false));
				classpath.setOn(project, new SubProgressMonitor(mon, 9));
			}
		} finally {
			mon.done();
		}
	}

}
