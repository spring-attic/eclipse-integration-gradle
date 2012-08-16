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
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.launching.IVMInstall;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.util.ArgumentsParser;
import org.springsource.ide.eclipse.gradle.core.util.ResourceListEncoder;


/**
 * Various project specific preferences.
 * <p>
 * Also used to store other persisted properties associated with a project, that should be stored in
 * the files associated with the project (inside the .settings folder).
 * 
 * @author Kris De Volder
 */
public class GradleProjectPreferences extends AbstractGradleProjectPreferences {
	
	private void debug(String string) {
//		System.out.println(string);
	}

	private static final String LINKED_RESOURCES_PREF = "org.springsource.ide.eclipse.gradle.linkedresources";
	private static final String ROOT_LOCATION_PREF = "org.springsource.ide.eclipse.gradle.rootprojectloc";
	private static final String ENABLE_CLASSPATH_SORTING = "org.springsource.ide.eclipse.gradle.classpath.enableSorting";
	public static final String JAR_REMAP_GRADLE_TO_MAVEN = "org.springsource.ide.eclipse.gradle.classpath.jar.remap.gradle.to.maven";
	
	public static final boolean DEFAULT_ENABLE_CLASSPATH_SORTING = true; 
	public static final boolean DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN = true;
	
	/**
	 * Get preferences associated with this project.
	 *  
	 * @param project
	 */
	public GradleProjectPreferences(GradleProject project) {
		super(project, GradleCore.PLUGIN_ID);
	}

	/**
	 * This isn't actually a preference, it can't be set by a preferences page but it is stored in the
	 * prefs store of the project because it needs to be persisted in a similar way.
	 */
	public Collection<IResource> getLinkedResources() {
		String encoded = get(LINKED_RESOURCES_PREF, "");
		return ResourceListEncoder.decode(getGradleProject().getProject(), encoded);
	}

	public void setLinkedResources(List<IResource> resources) {
		String encoded = ResourceListEncoder.encode(true, resources);
		put(LINKED_RESOURCES_PREF, encoded);
	}

	public void setRootProjectLocation(File file) {
		debug("set " +getGradleProject()+ ".root = "+file );
		put(ROOT_LOCATION_PREF, file);
	}

	public File getRootProjectLocation() {
		return get(ROOT_LOCATION_PREF, (File)null);
	}
	
	/**
	 * @return Whether classpath entry sorting is enabled. This preference is shared by all projects in a project hierarchy.
	 */
	public boolean getEnableClasspatEntrySorting() {
		try {
			GradleProjectPreferences rootPrefs = getRootProjectPreferences();
			return rootPrefs.get(ENABLE_CLASSPATH_SORTING, DEFAULT_ENABLE_CLASSPATH_SORTING);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
			return DEFAULT_ENABLE_CLASSPATH_SORTING;
		}
	}
	
	public void setEnableClasspatEntrySorting(boolean enable) {
		try {
			GradleProjectPreferences rootPrefs = getRootProjectPreferences();
			rootPrefs.put(ENABLE_CLASSPATH_SORTING, enable);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
		}
	}
	
	public boolean getRemapJarsToMavenProjects() {
		try {
			GradleProjectPreferences rootPrefs = getRootProjectPreferences();
			return rootPrefs.get(JAR_REMAP_GRADLE_TO_MAVEN, DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
			return DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN;
		}
	}
	
	public void setRemapJarsToMavenProjects(boolean enable) {
		try {
			GradleProjectPreferences rootPrefs = getRootProjectPreferences();
			rootPrefs.put(JAR_REMAP_GRADLE_TO_MAVEN, enable);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
		}
	}

	public File getJavaHome() {
		//Presently this preference can only be specified Globally, but eventually, it might make sense to
		//override the global setting by setting it on the root project of a hierarchy.
		IVMInstall install = GradleCore.getInstance().getPreferences().getJavaHomeJRE();
		if (install!=null) {
			return install.getInstallLocation();
		}
		return null;
	}

	public String[] getJVMArgs() {
		//Presently this preference can only be specified Globally, but eventually, it might make sense to
		//override the global setting by setting it on the root project of a hierarchy.
		String args = GradleCore.getInstance().getPreferences().getJVMArguments();
		if (args!=null) {
			return ArgumentsParser.parseArguments(args);
		}
		return null;
	}

	public String[] getProgramArgs() {
		//Presently this preference can only be specified Globally, but eventually, it might make sense to
		//override the global setting by setting it on the root project of a hierarchy.
		String args = GradleCore.getInstance().getPreferences().getProgramArguments();
		if (args!=null) {
			return ArgumentsParser.parseArguments(args);
		}
		return null;
	}
}
