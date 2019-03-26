/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jdt.launching.IVMInstall;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshAllActionCore;
import org.springsource.ide.eclipse.gradle.core.autorefresh.DependencyRefresher;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.util.JavaRuntimeUtils;
import org.springsource.ide.eclipse.gradle.core.wtp.DeploymentExclusions;
import org.springsource.ide.eclipse.gradle.core.wtp.RegexpListDeploymentExclusions;
import org.springsource.ide.eclipse.gradle.core.wtp.WTPUtil;

/**
 * Convenience wrapper class to access Gradle preferences, however they may be stored.
 * 
 * @author Kris De Volder
 */
public class GradlePreferences extends AbstractGradlePreferences implements IPreferenceChangeListener, IJavaHomePreferences {

	private static final String JAVA_HOME_JRE_NAME = GradlePreferences.class.getName()+".JAVA_HOME_JRE";
	private static final String JAVA_HOME_EE_NAME = GradlePreferences.class.getName()+".JAVA_HOME_EE";
	
	private static final String DISTRIBUTION = GradlePreferences.class.getName()+".DISTRIBUTION";
	private static final String GRADLE_USER_HOME = GradlePreferences.class.getName()+".GRADLE_USER_HOME";
	
	private static final String DEPLOYMENT_EXCLUSIONS = GradlePreferences.class.getName()+".WTP_EXCLUDE";
	
	private static final String JVM_ARGS = GradlePreferences.class.getName()+".JVM_ARGS";
	private static final String DEFAULT_JVM_ARGS = null;
	
	private static final String PROGRAM_ARGUMENTS = GradlePreferences.class.getName()+".PGM_ARGS";
	
	private static final String DEFAULT_PROGRAM_ARGUMENTS = null;
	
	private static final String AUTO_REFRESH_DEPENDENCIES = GradlePreferences.class.getName()+".AUTO_REFRESH_DEPENDENCIES";
	public static final boolean DEFAULT_AUTO_REFRESH_DEPENDENCIES = false;
	
	public static final String AUTO_REFRESH_DELAY = GradlePreferences.class.getName()+".AUTO_REFRESH_DELAY";
	public static final int DEFAULT_AUTO_REFRESH_DELAY = 5000;
	
	private static final String EXPORT_DEPENDENCIES =  GradlePreferences.class.getName()+".EXPORT_DEPENDENCIES";
	public static final boolean DEFAULT_EXPORT_DEPENDENCIES =  true; //This is probably the wrong behavior but preserves backwards compatibility
			//See: https://issuetracker.springsource.com/browse/STS-3405

	public static final boolean DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN = true;
	public static final boolean DEFAULT_JAR_REMAP_GRADLE_TO_GRADLE = false; //still experimental, may turn on by default on next release
	public static final boolean DEFAULT_JAR_REMAP_ON_OPEN_CLOSE = true;
	public static final String JAR_REMAP_GRADLE_TO_MAVEN = GradlePreferences.class.getName()+".JAR_REMAP_GRADLE_TO_MAVEN";
	public static final String JAR_REMAP_GRADLE_TO_GRADLE = GradlePreferences.class.getName()+".JAR_REMAP_GRADLE_TO_GRADLE";
	public static final String JAR_REMAP_ON_OPEN_CLOSE = GradlePreferences.class.getName()+".JAR_REMAP_ON_OPEN_CLOSE";
	
	public static final String USE_CUSTOM_TOOLING_MODEL = GradlePreferences.class.getName()+"USE_CUSTOM_TOOLING_MODEL";
	public static final boolean DEFAULT_USE_CUSTOM_TOOLING_MODEL = false;
	
	private static URI builtInDistribution = null;

	private RegexpListDeploymentExclusions cachedExclusions;
	private IEclipsePreferences eclipsePrefs;
	
	public GradlePreferences(IEclipsePreferences prefs) {
		super();
		this.eclipsePrefs = prefs;
		eclipsePrefs.addPreferenceChangeListener(this);
	}

	/**
	 * Return a URI pointing to a distribution chosen by the user, or null if none was user specified.
	 */
	public URI getDistribution() {
		String distributionString = get(DISTRIBUTION, null);
		if (distributionString!=null) {
			try {
				return new URI(distributionString);
			} catch (URISyntaxException e) {
				GradleCore.log(e);
			}
		}
		return null;
	}

	/**
	 * Set a URI pointing to a distribution chosen by the user, or null if none was user specified.
	 */
	public void setDistribution(URI distro) {
		put(DISTRIBUTION, distro!=null?distro.toString():null);
	}
	
	/**
	 * Returns a java.io.File instance pointing to a gradle user home directory specified by the user
	 * or null if none was specified. 
	 */
	public File getGradleUserHome() {
		String gradleUserHomeString = get(GRADLE_USER_HOME, null);
		if (gradleUserHomeString!=null) {
			return new File(gradleUserHomeString);
		}
		return null;
	}

	public void setGradleUserHome(File loc) {
		put(GRADLE_USER_HOME, loc!=null?loc.toString():null);
	}
	
	/**
	 * Return a workspace configured JVM to use for Gradle's JAVA_HOME. May be null in which case
	 * Gradle should use its own default.
	 * <p>
	 * A custom Java home can be defined in one of two ways, either via selecting a specific JRE,
	 * or by selecting an Execution environment that has a default JRE associated with it. 
	 */
	public static IVMInstall getJavaHomeJRE(IJavaHomePreferences prefs) {
		JavaRuntimeUtils jres = new JavaRuntimeUtils();
		String jreName = prefs.getJavaHomeJREName();
		if (jreName!=null) {
			return jres.getInstall(jreName);
		}
		String eeName = prefs.getJavaHomeEEName();
		if (eeName!=null) {
			return jres.getInstallForEE(eeName);
		}
		return null;
	}
	
	/**
	 * Sets the JavaHome by selecting a JRE name chosen by the user or null, if the JRE was chosen.
	 */
	public void setJavaHomeJREName(String jreName) {
		put(JAVA_HOME_JRE_NAME, jreName);
		if (jreName!=null) {
			//When a JRE name is used to specify the Java home, make sure the EE_NAME is
			//not active anymore.
			put(JAVA_HOME_EE_NAME, null);
		}
	}
	
	public void setJavaHomeEEName(String eeName) {
		put(JAVA_HOME_EE_NAME, eeName);
		if (eeName!=null) {
			//When a EE name is used to specify the Java home, make sure the JRE_NAME selection is
			//not active anymore.
			put(JAVA_HOME_JRE_NAME, null);
		}
	}
	
	/**
	 * Ensures that no custom Java home is selected anymore.
	 */
	public void unsetJavaHome() {
		setJavaHomeEEName(null);
		setJavaHomeJREName(null);
	}

	/**
	 * @return URI of a gradle distribution that is packaged up with GradleCore plugin.
	 * @throws IOException if there is some problem getting the embedded distribution.
	 */
	public static URI getBuiltinDistribution() throws IOException {
		if (builtInDistribution==null) {
			debug(">>>> Searching for embedded Gradle install");
			Bundle coreBundle = Platform.getBundle(GradleCore.PLUGIN_ID);
			File coreBundleFile = FileLocator.getBundleFile(coreBundle);
			Assert.isNotNull(coreBundleFile);
			Assert.isLegal(coreBundleFile.isDirectory(), "The bundle "+coreBundle.getSymbolicName()+" must be unpacked to allow using the embedded gradle distribution");
			File libDir = new File(coreBundleFile, "lib");
			File[] candidates = libDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".zip");
				}
			});
			if (GlobalSettings.DEBUG) {
				for (File file : candidates) {
					debug("found: "+file);
				}
			}
			Assert.isTrue(candidates.length<=1, "At most one embedded distribution should be found!");
			if (candidates.length==1) {
				builtInDistribution = candidates[0].toURI();
				debug("Found embedded install: "+builtInDistribution);
			} else {
				debug("No embedded install found");
			}
			debug("<<<< Searching for embedded Gradle install");
		}
		return builtInDistribution;
	}

	private static void debug(String string) {
		if (GlobalSettings.DEBUG) {
			System.out.println(string);
		}
	}
	
	public RegexpListDeploymentExclusions getDeploymentExclusions() {
		if (cachedExclusions==null) {
			cachedExclusions = createDeploymentExclusions();
		}
		return cachedExclusions;
	}

	private RegexpListDeploymentExclusions createDeploymentExclusions() {
		String[] regexps = getStrings(DEPLOYMENT_EXCLUSIONS, null);
		if (regexps!=null) {
			return new RegexpListDeploymentExclusions(regexps);
		}
		return DeploymentExclusions.getDefault();
	}

	/**
	 * Sets the WTP deployment exclusions. May throw an exception if the exclusion expression have syntax errors.
	 */
	public void setDeploymentExclusions(RegexpListDeploymentExclusions exclusions) throws PatternSyntaxException {
		exclusions.verify(); //Only verified exclusions will be accepted!
		RegexpListDeploymentExclusions current = getDeploymentExclusions();
		if (!current.equals(exclusions)) { //Avoid uneccesary change events.
			putStrings(DEPLOYMENT_EXCLUSIONS, exclusions.getSourceExps());
		}
	}

	@Override
	public void put(String name, String value) {
		if (value==null) {
			eclipsePrefs.remove(name);
		} else {
			eclipsePrefs.put(name, value);
		}
		try {
			eclipsePrefs.flush();
		} catch (BackingStoreException e) {
			GradleCore.log(e);
		}
	}
	
	@Override
	public String get(String name, String deflt) {
		return eclipsePrefs.get(name, deflt);
	}

	public void dispose() {
	}

	public void preferenceChange(PreferenceChangeEvent event) {
		if (DEPLOYMENT_EXCLUSIONS.equals(event.getKey())) {
			cachedExclusions = null;
			WTPUtil.refreshAllDependencies();
		} else if (AUTO_REFRESH_DELAY.equals(event.getKey()) 
				|| AUTO_REFRESH_DEPENDENCIES.equals(event.getKey())) {
			DependencyRefresher.refresh();
		} else if (EXPORT_DEPENDENCIES.equals(event.getKey())) {
			try {
				//All projects that have dependency management enabled are affected.
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				List<IProject> affected = new ArrayList<IProject>();
				for (IProject p : projects) {
					if (GradleCore.create(p).isDependencyManaged()) {
						affected.add(p);
					}
				}
				RefreshAllActionCore.callOn(affected);
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		} else if (JAR_REMAP_ON_OPEN_CLOSE.equals(event.getKey())
				|| JAR_REMAP_GRADLE_TO_GRADLE.equals(event.getKey())
				|| JAR_REMAP_GRADLE_TO_MAVEN.equals(event.getKey())) {
			GradleClassPathContainer.ensureOpenCloseListener();
		}
	}

	/**
	 * Return custom JVM arguments for the Gradle Daemon. May return null, in which case
	 * it means we want to let Gradle use its own defaults.
	 */
	public String getJVMArguments() {
		return get(JVM_ARGS, DEFAULT_JVM_ARGS);
	}
	
	public void setJVMArguments(String args) {
		put(JVM_ARGS, args);
	}

	public String getProgramArguments() {
		return get(PROGRAM_ARGUMENTS, DEFAULT_PROGRAM_ARGUMENTS);
	}

	public void setProgramArguments(String args) {
		put(PROGRAM_ARGUMENTS, args);
	}

	public String getJavaHomeJREName() {
		return get(JAVA_HOME_JRE_NAME, null);
	}

	public String getJavaHomeEEName() {
		return get(JAVA_HOME_EE_NAME, null);
	}

	public static File getJavaHome(IJavaHomePreferences prefs) {
		IVMInstall install = getJavaHomeJRE(prefs);
		if (install!=null) {
			return install.getInstallLocation();
		}
		return null;
	}

	public boolean isAutoRefreshDependencies() {
		return get(AUTO_REFRESH_DEPENDENCIES, DEFAULT_AUTO_REFRESH_DEPENDENCIES);
	}
	public void setAutoRefreshDependencies(boolean e) {
		put(AUTO_REFRESH_DEPENDENCIES, e);
	}
	
	public int getAutoRefreshDelay() {
		return get(AUTO_REFRESH_DELAY, DEFAULT_AUTO_REFRESH_DELAY);
	}
	public void setAutoRefreshDelay(int v) {
		put(AUTO_REFRESH_DELAY, v);
	}

	public boolean isExportDependencies() {
		return get(EXPORT_DEPENDENCIES, DEFAULT_EXPORT_DEPENDENCIES);
	}

	public void setExportDependencies(boolean e) {
		put(EXPORT_DEPENDENCIES, e);
	}
	
	public boolean getRemapJarsToMavenProjects() {
		return get(JAR_REMAP_GRADLE_TO_MAVEN, DEFAULT_JAR_REMAP_GRADLE_TO_MAVEN);
	}
	
	public void setRemapJarsToMavenProjects(boolean enable) {
		put(JAR_REMAP_GRADLE_TO_MAVEN, enable);
	}
	
	public boolean getRemapJarsToGradleProjects() {
		return get(JAR_REMAP_GRADLE_TO_GRADLE, DEFAULT_JAR_REMAP_GRADLE_TO_GRADLE);
	}
	
	public void setRemapJarsToGradleProjects(boolean enable) {
		put(JAR_REMAP_GRADLE_TO_GRADLE, enable);
	}
	
	public boolean getUseCustomToolingModel() {
		return get(USE_CUSTOM_TOOLING_MODEL, DEFAULT_USE_CUSTOM_TOOLING_MODEL);
	}
	
	public void setUseCustomToolingModel(boolean enable) {
		put(USE_CUSTOM_TOOLING_MODEL, enable);
	}
	

	public boolean getJarRemappingOnOpenClose() {
		return get(JAR_REMAP_ON_OPEN_CLOSE, DEFAULT_JAR_REMAP_ON_OPEN_CLOSE);
	}

	public void setJarRemappingOnOpenClose(boolean enable) {
		put(JAR_REMAP_ON_OPEN_CLOSE, enable);
	}

}
