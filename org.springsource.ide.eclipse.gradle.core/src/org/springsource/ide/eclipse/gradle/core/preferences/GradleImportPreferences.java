/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * Project preferences related to importing. Used by the import wizard to preserve and set default
 * options.
 * 
 * @author Kris De Volder
 */
public class GradleImportPreferences extends AbstractGradleProjectPreferences {

	private static final String WORKING_SETS = "workingSets";
//	private static final String QUICK_WORKING_SET = "quickWorkingSetEnabled";
	private static final String DO_AFTER_TASKS = "enableAfterTasks";
	private static final String DO_BEFORE_TASKS = "enableBeforeTasks";
	private static final String BEFORE_TASKS = "beforeTasks";
	private static final String AFTER_TASKS = "afterTasks";
	private static final String SELECTED_PROJECTS = "projects";
	private static final String ENABLE_DEPENDENCY_MANAGEMENT = "enableDependendencyManagement";
	private static final String ENABLE_DSLD = "enableDSLD";
	private static final String ADD_RESOURCE_FILTERS = "addResourceFilters";

	public GradleImportPreferences(GradleProject project) {
		super(project, GradleCore.PLUGIN_ID+".import");
	}

	public void setWorkingSets(IWorkingSet[] selectedWorkingSets) {
		String[] workingSetNames = new String[selectedWorkingSets.length];
		int i = 0;
		for (IWorkingSet ws : selectedWorkingSets) {
			workingSetNames[i++] = ws.getName();
		}
		putStrings(WORKING_SETS, workingSetNames); 
	}
	
	public IWorkingSet[] getSelectedWorkingSets() {
		IWorkingSetManager wsm = PlatformUI.getWorkbench().getWorkingSetManager();
		String[] workingSetNames = getStrings(WORKING_SETS, new String[0]);
		List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>(workingSetNames.length);
		for (String name : workingSetNames) {
			IWorkingSet ws = wsm.getWorkingSet(name);
			if (ws!=null) { // If a ws no longer exists... ignore it.
				workingSets.add(ws);
			}
		}
		return workingSets.toArray(new IWorkingSet[workingSets.size()]);
	}
	
	public boolean getAddResourceFilters() {
		return get(ADD_RESOURCE_FILTERS, GradleImportOperation.DEFAULT_ADD_RESOURCE_FILTERS);
	}

	public void setAddResourceFilters(boolean enable) {
		put(ADD_RESOURCE_FILTERS, enable);
	}

	public void setDoBeforeTasks(boolean enabled) {
		put(DO_BEFORE_TASKS, enabled);
	}

	public boolean getDoBeforeTasks() {
		boolean deflt = GradleImportOperation.determineDefaultDoBefore(getGradleProject());
		return get(DO_BEFORE_TASKS, deflt);
	}
	
	public void setBeforeTasks(String[] tasks) {
		putStrings(BEFORE_TASKS, tasks);
	}
	
	public String[] getBeforeTasks() {
		return getStrings(BEFORE_TASKS, GradleImportOperation.DEFAULT_BEFORE_TASKS);
	}

	public boolean getDoAfterTasks() {
		return get(DO_AFTER_TASKS, GradleImportOperation.DEFAULT_DO_AFTER_TASKS);
	}

	public void setDoAfterTasks(boolean enabled) {
		put(DO_AFTER_TASKS, enabled);
	}
	
	public void setAfterTasks(String[] tasks) {
		putStrings(AFTER_TASKS, tasks);
	}
	
	public String[] getAfterTasks() {
		return getStrings(AFTER_TASKS, GradleImportOperation.DEFAULT_AFTER_TASKS);
	}

	public void setSelectedProjects(List<HierarchicalEclipseProject> selectedProjects) {
		String[] projectLocs = new String[selectedProjects.size()];
		int i = 0;
		for (HierarchicalEclipseProject _p : selectedProjects) {
			GradleProject p = GradleCore.create(_p);
			projectLocs[i++] = encodeFile(p.getLocation());
		}
		putStrings(SELECTED_PROJECTS, projectLocs);
	}
	
	public GradleProject[] getSelectedProjects() {
		String[] projectLocs = getStrings(SELECTED_PROJECTS, null);
		if (projectLocs!=null) {
			List<GradleProject> projects = new ArrayList<GradleProject>(projectLocs.length);
			for (String loc : projectLocs) {
				File locFile = decodeFile(loc);
				if (locFile.exists()) {
					//Don't bother with non-existing locations they are certainly bogus (deleted?)
					projects.add(GradleCore.create(locFile));
				}
			}
			return projects.toArray(new GradleProject[projects.size()]);
		}
		return new GradleProject[0];
	}

//	public boolean getQuickWorkingSetEnabled() {
//		return get(QUICK_WORKING_SET, GradleImportOperation.DEFAULT_QUICK_WORKINGSET_ENABLED);
//	}
//
//	public void setQuickWorkingSetEnabled(boolean enable) {
//		put(QUICK_WORKING_SET, enable);
//	}

	public void setEnableDependencyManagement(boolean isEnabled) {
		put(ENABLE_DEPENDENCY_MANAGEMENT, isEnabled);
	}
	public boolean getEnableDependencyManagement() {
		return get(ENABLE_DEPENDENCY_MANAGEMENT, GradleImportOperation.DEFAULT_ENABLE_DEPENDENCY_MANAGEMENT);
	}

	public boolean getEnableDSLD() {
		return get(ENABLE_DSLD, GradleImportOperation.DEFAULT_ENABLE_DSLD);
	}
	
	public void setEnableDSLD(boolean enableDSLD) {
		put(ENABLE_DSLD, enableDSLD);
	}

}
