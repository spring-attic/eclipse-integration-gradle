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
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
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
	
	private static final boolean DEBUG 
		=  (""+Platform.getLocation()).contains("kdvolder")
		|| (""+Platform.getLocation()).contains("bamboo");

	private void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	private static final String LINKED_RESOURCES_PREF = "org.springsource.ide.eclipse.gradle.linkedresources";
	private static final String ROOT_LOCATION_PREF = "org.springsource.ide.eclipse.gradle.rootprojectloc";
	private static final String ENABLE_CLASSPATH_SORTING = "org.springsource.ide.eclipse.gradle.classpath.enableSorting";
	
	public static final boolean DEFAULT_ENABLE_CLASSPATH_SORTING = true; 
	
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
		debug("set " +getGradleProject().getLocation().getName()+ ".root = "+file.getName() );
		put(ROOT_LOCATION_PREF, file);
	}

	public File getRootProjectLocation() {
		return get(ROOT_LOCATION_PREF, (File)null);
	}
		
	/**
	 * @return Whether classpath entry sorting is enabled. This preference is shared by all projects in a project hierarchy.
	 */
	public boolean getEnableClasspathEntrySorting() {
		try {
			AbstractGradleProjectPreferences rootPrefs = getRootProjectPreferences();
			return rootPrefs.get(ENABLE_CLASSPATH_SORTING, DEFAULT_ENABLE_CLASSPATH_SORTING);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
			return DEFAULT_ENABLE_CLASSPATH_SORTING;
		}
	}
	
	public void setEnableClasspatEntrySorting(boolean enable) {
		try {
			AbstractGradleProjectPreferences rootPrefs = getRootProjectPreferences();
			rootPrefs.put(ENABLE_CLASSPATH_SORTING, enable);
		} catch (FastOperationFailedException e) {
			GradleCore.log(e); 
		}
	}
	
	public File getJavaHome() {
		//Presently this preference can not be specified on projects so we just use the
		//global preference to determine it.
		return GradlePreferences.getJavaHome(GradleCore.getInstance().getPreferences());
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
