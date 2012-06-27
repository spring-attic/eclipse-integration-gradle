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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;


/**
 * Super class for project-scoped preferences. Provides an implementation that stores preferences
 * in a location relative to a given Gradle project.
 * <p>
 * Note that we are not using the eclipse apis for preferences and preferences storing because
 * those APIs only work if a project is an eclipse project. Since not all Gradle projects are
 * imported to Eclipse we can't use the Eclipse APIs.  
 * <p>
 * Also, we may want to read preferences before a project is imported (i.e. for preferences
 * related to how the project should be imported!).
 * 
 * @author Kris De Volder
 */
public abstract class AbstractGradleProjectPreferences extends AbstractGradlePreferences {

	private GradleProject project;
	private File propertiesFile;
	private Properties properties;

	protected AbstractGradleProjectPreferences(GradleProject project, String nodeId) {
		moveLegacyPrefs(project, nodeId);
		this.project = project;
		this.propertiesFile = computePropertyFile(project, nodeId);
	}

	private static void moveLegacyPrefs(GradleProject project, String nodeId) {
		try {
			File legacyFile = computeLegacyPropertyFile(project, nodeId);
			if (legacyFile.exists()) {
				File newFile = computePropertyFile(project, nodeId);
				if (newFile.exists()) {
					legacyFile.delete();
				} else {
					File parent = newFile.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}
					legacyFile.renameTo(newFile);
				}
			}
		} catch (Exception e) {
			GradleCore.log(e);
		}
	}
	
	/**
	 * Computes the location of the properties file where preferences are stored
	 */
	private static File computePropertyFile(GradleProject project, String nodeId) {
		return new File(project.getLocation(), ".settings/gradle/"+nodeId+".prefs");
	}
	
	/**
	 * Computes the location of the properties file where preferences *used* to be stored
	 */
	private static File computeLegacyPropertyFile(GradleProject project, String nodeId) {
		return new File(project.getLocation(), ".settings/"+nodeId+".prefs");
	}
	
	public GradleProject getGradleProject() {
		return project;
	}
	
	@Override
	public String get(String name, String deflt) {
		Properties props = getProperties();
		if (props.containsKey(name)) {
			return props.getProperty(name);
		} 
		return deflt;
	}

	private Properties getProperties() {
		if (properties==null) {
			properties = new Properties();
			if (propertiesFile.exists()) {
				FileInputStream in = null;
				try {
					in = new FileInputStream(propertiesFile);
					properties.load(in);
				} catch (IOException e) {
					GradleCore.log(e);
				} finally {
					try {
						if (in!=null) { in.close(); }
					} catch (IOException e) {
					}
				}
			}
		}
		return properties;
	}

	@Override
	public void put(String key, String value) {
		Properties props = getProperties();
		boolean changed = false;
		if (value==null) {
			if (props.contains(key)) {
				props.remove(key);
				changed = true;
			}
		} else {
			if (!value.equals(props.getProperty(key, null))) {
				props.put(key, value);
				changed = true;
			}
		}
		if (changed) {
			try {
				flush();
			} catch (IOException e) {
				GradleCore.log(e);
			}
		}
	}

	public void flush() throws IOException {
		//TODO: find a way to not flush repeatedly if we set many properties in a row. 
		// Idea: use a job and schedule it a little later than now...
		// if a job exist already do not schedule a second one.
		if (properties.isEmpty() && !propertiesFile.exists()) {
			return; //don't bother creating an empty file
		}
		if (!propertiesFile.getParentFile().exists()) {
			if (!propertiesFile.getParentFile().mkdirs()) {
				throw new IOException("Couldn't create directory :"+propertiesFile.getParent());
			}
		}
		//We get here, it means the parent directory exists now
		OutputStream out = null; 
		try {
			out = new FileOutputStream(propertiesFile);
			//We make properties be persisted in sorted order...
			//This is a lot nicer for humans to skim the 'diff' output as may be
			//shown to them by SCM tools. (See for example 
			//https://issuetracker.springsource.com/browse/STS-2583)
			Properties sortedProperties = new Properties() {
				private static final long serialVersionUID = 1L;
				@Override
				public synchronized Enumeration<Object> keys() {
					Set<Object> unsorted = properties.keySet();
					return Collections.enumeration(new TreeSet<Object>(unsorted));
				}
			};
			sortedProperties.putAll(properties);
			sortedProperties.store(out, this.getClass().getName());
		} finally {
			if (out!=null) {
				out.close();
			}
		}
	}

	/**
	 * Stores path to a file in the preferences, the path is, if possible stored as a relative
	 * path, starting from the location of the project associated with this preferences object.
	 */
	public void put(String key, File file) {
		if (file!=null) {
			String encoded = encodeFile(file);
			put(key, encoded);
		} else { //file==null
			put(key, (String)null);
		}
	}

	/**
	 * Convert a file into a String that represents a relative path (if possible) starting
	 * at the project these prefs are associated with.
	 */
	public String encodeFile(File file) {
		IPath baseLocation = Path.fromOSString(getGradleProject().getLocation().getAbsolutePath());
		IPath rootLocation = Path.fromOSString(file.getAbsolutePath());
		IPath relativeRootLocation = rootLocation.makeRelativeTo(baseLocation);
		String encoded = relativeRootLocation.toPortableString();
		return encoded;
	}

	public File get(String key, File deflt) {
		String encoded = get(key, (String)null);
		if (encoded!=null) {
			return decodeFile(encoded);
		}
		return deflt;
	}

	/**
	 * Decode a file from a String that representation. The representation may be a relative path starting
	 * at the project these prefs are associated with.
	 */
	public File decodeFile(String encoded) {
		IPath fileLocation = Path.fromPortableString(encoded);
		if (!fileLocation.isAbsolute()) {
			IPath baseLocation = Path.fromOSString(getGradleProject().getLocation().getAbsolutePath());
			fileLocation = baseLocation.append(fileLocation);
		}
		return new File(fileLocation.toOSString());
	}
	

	/**
	 * @return The preferences associated with the root project of the project that the receiver is associated with.
	 */
	public GradleProjectPreferences getRootProjectPreferences() throws FastOperationFailedException {
		return project.getRootProject().getProjectPreferences();
	}

}
