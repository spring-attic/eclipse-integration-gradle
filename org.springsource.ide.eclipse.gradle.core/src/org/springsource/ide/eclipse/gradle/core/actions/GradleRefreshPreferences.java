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
package org.springsource.ide.eclipse.gradle.core.actions;

import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.preferences.AbstractGradleProjectPreferences;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;

/**
 * An instance of this class is associated with a GradleProject and contains a set of options
 * that define how a project that does not have dependency management enabled gets refreshed.
 * 
 * @author Kris De Volder
 */
public class GradleRefreshPreferences extends AbstractGradleProjectPreferences {

	//Very similar to import preferences but...
	//   - not exactly the same set of options apply to refreshes
	//   - must be stored in a separate location (i.e. it is a snapshot of the options at the time of import). 
		
	private static final String ADD_RESOURCE_FILTERS = "addResourceFilters";
	private static final String USE_HIERARCHICAL_NAMES = "useHierarchicalNames";
	private static final String DO_AFTER_TASKS = "enableAfterTasks";
	private static final String DO_BEFORE_TASKS = "enableBeforeTasks";
	private static final String BEFORE_TASKS = "beforeTasks";
	private static final String AFTER_TASKS = "afterTasks";

	public GradleRefreshPreferences(GradleProject project) {
		super(project, "org.springsource.ide.eclipse.gradle.refresh");
	}

	public boolean getAddResourceFilters() {
		return get(ADD_RESOURCE_FILTERS, GradleImportOperation.DEFAULT_ADD_RESOURCE_FILTERS);
	}

	public void setAddResourceFilters(boolean enable) {
		put(ADD_RESOURCE_FILTERS, enable);
	}

	public boolean getUseHierarchicalNames() {
		return get(USE_HIERARCHICAL_NAMES, GradleImportOperation.DEFAULT_USE_HIERARCHICAL_NAMES);
	}
	
	public void setUseHierarchicalNames(boolean enable) {
		put(USE_HIERARCHICAL_NAMES, enable);
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
	
	/**
	 * Set all the refresh preferences based on a the properties of a given importOperation.
	 */
	public void copyFrom(GradleImportOperation importOperation) {
		setAddResourceFilters(importOperation.getAddResourceFilters());
		setAfterTasks(importOperation.getAfterTasks());
		setBeforeTasks(importOperation.getBeforeTasks());
		setDoAfterTasks(importOperation.getDoAfterTasks());
		setDoBeforeTasks(importOperation.getDoBeforeTasks());
		setUseHierarchicalNames(importOperation.getUseHierarchicalNames());
	}

}
