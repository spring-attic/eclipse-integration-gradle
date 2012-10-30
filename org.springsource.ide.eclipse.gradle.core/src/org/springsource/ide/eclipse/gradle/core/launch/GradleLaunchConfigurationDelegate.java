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
package org.springsource.ide.eclipse.gradle.core.launch;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.gradle.tooling.LongRunningOperation;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.preferences.IJavaHomePreferences;
import org.springsource.ide.eclipse.gradle.core.util.ArgumentsParser;

/**
 * Launch configuration delegate to support the execution of Gradle tasks.
 * 
 * @author Kris De Volder
 */
public class GradleLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
	
	public static final String ID = "org.springsource.ide.eclipse.gradle.launch";

	private static final boolean DEBUG = true;
	
	private static final String TASK_LIST = ID + ".TASKLIST";
	private static final List<String> DEFAULT_TASK_LIST = Arrays.asList(new String[0]);
	private static final boolean DEFAULT_ENABLE_WORKSPACE_REFRESH = true;
	
	private static final String JVM_ARGS = ID+".JVM_ARGS";
	private static final String PGM_ARGS = ID+".PGM_ARGS";
	private static final String JRE_NAME = ID+".JRE_NAME";
	private static final String EE_NAME = ID+".EE_NAME";

	/*
	 * Of the two properties PROJECT and PROJECT_LOCATION at least one of them must be set to
	 * identify a Gradle project. If both are set, the PROJECT property is used and the other one
	 * is ignored. 
	 */
	private static final String PROJECT = ID +".PROJECT";
	public static final String PROJECT_LOCATION = ID+".LOCATION";


	public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Launching Gradle Tasks", 1);
		try {
			launch.addProcess(new GradleProcess(configuration, launch));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Allows using a Gradle launch configuration as a {@link IJavaHomePreferences} instance.
	 */
	public static IJavaHomePreferences asJavaHomePrefs(final ILaunchConfiguration conf) {
		return new IJavaHomePreferences() {
			public void unsetJavaHome() {
				this.setJavaHomeEEName(null);
				this.setJavaHomeJREName(null);
			}
			
			public void setJavaHomeJREName(String name) {
				GradleLaunchConfigurationDelegate.setJavaHomeJREName((ILaunchConfigurationWorkingCopy)conf, name);
			}
			
			public void setJavaHomeEEName(String name) {
				GradleLaunchConfigurationDelegate.setJavaHomeEEName((ILaunchConfigurationWorkingCopy)conf, name);
			}
			
			public String getJavaHomeJREName() {
				return GradleLaunchConfigurationDelegate.getJavaHomeJREName(conf);
			}
			
			public String getJavaHomeEEName() {
				return GradleLaunchConfigurationDelegate.getJavaHomeEEName(conf);
			}
		};
	}

	public static String getJVMArguments(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(JVM_ARGS, (String)null);
		} catch (CoreException e) {
			GradleCore.log(e);
			return null;
		}
	}
	
	public static String getJavaHomeJREName(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(JRE_NAME, (String)null);
		} catch (CoreException e) {
			GradleCore.log(e);
			return null;
		}
	}
	
	public static String getJavaHomeEEName(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(EE_NAME, (String)null);
		} catch (CoreException e) {
			GradleCore.log(e);
			return null;
		}
	}

	public static void setJavaHomeJREName(ILaunchConfigurationWorkingCopy conf, String name) {
		if (name==null) {
			conf.removeAttribute(JRE_NAME);
		} else {
			conf.setAttribute(JRE_NAME, name);
			//only one of JRE / EE name should be set to non-null value enforce this:
			conf.removeAttribute(EE_NAME);
		}
	}
	public static void setJavaHomeEEName(ILaunchConfigurationWorkingCopy conf, String name) {
		if (name==null) {
			conf.removeAttribute(EE_NAME);
		} else {
			conf.setAttribute(EE_NAME, name);
			//only one of JRE / EE name should be set to non-null value enforce this:
			conf.removeAttribute(JRE_NAME);
		}
	}
	
	public static void setJVMArguments(ILaunchConfigurationWorkingCopy conf, String args) {
		if (args==null) {
			conf.removeAttribute(JVM_ARGS);
		} else {
			conf.setAttribute(JVM_ARGS, args);
		}
	}

	private static String[] getJVMArgumentsArray(ILaunchConfiguration conf) {
		String args = getJVMArguments(conf);
		if (args!=null) {
			return ArgumentsParser.parseArguments(args);
		}
		return null;
	}
	
	public static String getProgramArguments(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(PGM_ARGS, (String)null);
		} catch (CoreException e) {
			GradleCore.log(e);
			return null;
		}
	}

	public static void setProgramArguments(ILaunchConfigurationWorkingCopy conf, String args) {
		if (args==null) {
			conf.removeAttribute(PGM_ARGS);
		} else {
			conf.setAttribute(PGM_ARGS, args);
		}
	}

	
	private static String[] getProgramArgumentsArray(ILaunchConfiguration conf) {
		String args = getProgramArguments(conf);
		if (args!=null) {
			return ArgumentsParser.parseArguments(args);
		}
		return null;
	}

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
	
	/**
	 * Returns the gradle project associated with this launch configuration. This returns null if no project is associated
	 * with the configuration. 
	 * 
	 * @return GradleProject associated with this launch configuration.
	 */
	public static GradleProject getProject(ILaunchConfiguration conf) {
		try {
			String projectName = conf.getAttribute(PROJECT, (String)null);
			if (projectName!=null) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (project.exists()) {
					return GradleCore.create(project);
				}
			} else {
				String projectLocString = conf.getAttribute(PROJECT_LOCATION, (String)null);
				if (projectLocString!=null) {
					File projectLoc = new File(projectLocString);
					if (projectLoc.isDirectory()) {
						return GradleCore.create(projectLoc);
					}
				}
			}
		} catch (Exception e) {
			GradleCore.log(e);
		}
		return null;
	}

	/**
	 * Sets the project associated with the given launch configuration. If project is null, or if the project doesn't
	 * 
	 * @param conf
	 * @param project
	 */
	public static void setProject(ILaunchConfigurationWorkingCopy conf, GradleProject project) {
		if (project==null) {
			conf.removeAttribute(PROJECT);
			conf.removeAttribute(PROJECT_LOCATION);
		} else {
			String name = project.getName();
			if (name != null) {
				conf.setAttribute(PROJECT, project.getName());
				conf.removeAttribute(PROJECT_LOCATION);
			} else {
				//Only use location if name is not known (typically this only happens if the project hasn't been imported
				//into eclipse (yet).
				conf.removeAttribute(PROJECT);
				String location = project.getLocation().getAbsolutePath();
				conf.setAttribute(PROJECT_LOCATION, location);
			}
		}
	}

	/**
	 * @return list of Gradle path strings identifying a set/list of tasks that are selected for execution 
	 * (by the user, doesn't include taks executed automatically because of dependencies).
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getTasks(ILaunchConfiguration conf) {
		try {
			return conf.getAttribute(TASK_LIST, DEFAULT_TASK_LIST);
		} catch (CoreException e) {
			GradleCore.log(e);
			return DEFAULT_TASK_LIST;
		}
	}

	public static void setTasks(ILaunchConfigurationWorkingCopy conf, List<String> checked) {
		conf.setAttribute(TASK_LIST, checked);
	}

	/**
	 * Creates a Gradle launch configuration for a given gradle project with default values.
	 * This launch configuration will be a working copy (i.e. not saved) if the save parameter
	 * is false. 
	 * <p>
	 * If the save parameter is true, then an attempt will be made to save the launch configuration 
	 * before this method returns and a reference to the saved configuration will be returned instead
	 * of the working copy.
	 */
	public static ILaunchConfiguration createDefault(GradleProject project, boolean save) {
		ILaunchConfigurationWorkingCopy conf = null; 
		try {
			conf = createDefault(project, LaunchUtil.generateConfigName(project.getDisplayName()));
			if (save) {
				return conf.doSave();
			} 
		}
		catch (CoreException e) {
			GradleCore.log(e);
		}
		return conf;
	}

	/**
	 * Creates a new launch configuration with a given name, for a give project. The configuration will be an empty 'base-line'
	 * configuration with no preselected tasks to execute. 
	 * <p>
	 * Beware that the name passed in to this method must meet certain restrictions (e.g. it should not be the name of
	 * an existing configuration, and should not contain certain 'funny' characters that may interfere with the
	 * OS file system. A safe way to create a name that meets the requirements is to use the 
	 * {@link LaunchUtil}.generateConfigName() method.
	 */
	public static ILaunchConfigurationWorkingCopy createDefault(GradleProject project, String name) throws CoreException {
		ILaunchConfigurationType lcType = getLaunchConfigurationType();
		ILaunchConfigurationWorkingCopy conf = lcType.newInstance(null, name);
		setProject(conf, project);
		enableWorkspaceRefresh(conf, DEFAULT_ENABLE_WORKSPACE_REFRESH);
		return conf;
	}
	
	private static void enableWorkspaceRefresh(ILaunchConfigurationWorkingCopy conf, boolean enable) {
		if (enable) {
			conf.setAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, RefreshUtil.MEMENTO_WORKSPACE);
		} else {
			conf.removeAttribute(RefreshUtil.ATTR_REFRESH_SCOPE);
		}
	}

	/**
	 * Creates a Gradle launch configuration for a given gradle project with default values.
	 * This launch configuration will be a working copy (i.e. not saved) if the save parameter
	 * is false. 
	 * <p>
	 * If the save parameter is true, then an attempt will be made to save the launch configuration 
	 * before this method returns and a reference to the saved configuration will be returned instead
	 * of the working copy.
	 */
	public static ILaunchConfiguration createDefault(GradleProject project, String task, boolean save) {
		ILaunchConfigurationWorkingCopy conf = null; 
		try {
			conf = createDefault(project, LaunchUtil.generateConfigName(project.getName()+" "+task));
			setTasks(conf, Arrays.asList(task));
			if (save) {
				return conf.doSave();
			} 
		}
		catch (CoreException e) {
			GradleCore.log(e);
		}
		return conf;
	}
		
	private static ILaunchConfigurationType getLaunchConfigurationType() {
		return LaunchUtil.getLaunchManager().getLaunchConfigurationType(ID);
	}

	public static ILaunchConfiguration getOrCreate(GradleProject project, String task) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = getLaunchConfigurationType();
		if (type != null) {
			try {
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				for (ILaunchConfiguration conf : configs) {
					GradleProject confProject = GradleLaunchConfigurationDelegate.getProject(conf);
					if (confProject==project) {
						List<String> tasks = GradleLaunchConfigurationDelegate.getTasks(conf);
						if (tasks.size()==1 && tasks.get(0).equals(task)) {
							return conf;
						}
					}
				}
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		}
		return createDefault(project, task, true);
	}

	public static void configureOperation(LongRunningOperation gradleOp, ILaunchConfiguration conf) {
		if (conf!=null) {
			File javaHome = GradlePreferences.getJavaHome(asJavaHomePrefs(conf));
			if (javaHome!=null) {
				gradleOp.setJavaHome(javaHome);
			}
			String[] jvmArgs = getJVMArgumentsArray(conf);
			if (jvmArgs!=null) {
				gradleOp.setJvmArguments(jvmArgs);
			}
			String[] programArgs = getProgramArgumentsArray(conf);
			if (programArgs!=null) {
				gradleOp.withArguments(programArgs);
			}
		}
	}

}
