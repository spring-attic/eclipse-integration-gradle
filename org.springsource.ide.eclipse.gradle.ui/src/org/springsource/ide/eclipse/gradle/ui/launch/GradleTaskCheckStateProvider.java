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
package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * Provide checked and grayed state for the task selection tree. This state is based on a list of selected
 * tasks, each task identified by a Gradle path string.
 * 
 * Note: this is a bit finicky, in order for it to work well, it should also implement ICheckStateListener so
 * that can keep its internal state consistent with the GUI widget. It must emulate the selection behavior of
 * 
 * @author Kris De Volder
 */
public class GradleTaskCheckStateProvider implements ICheckStateProvider , ICheckStateListener {
	
	GradleLaunchTasksTab owner;
	
	public GradleTaskCheckStateProvider(GradleLaunchTasksTab gradleLaunchTasksTab) {
		this.owner = gradleLaunchTasksTab;
	}
	
	/**
	 * To cache grayed state of projects, which is computed based on the checked state of its tasks.
	 */
	Map<EclipseProject, Boolean> grayed = new HashMap<EclipseProject, Boolean>();
	LinkedHashSet<String> checked = new LinkedHashSet<String>(); //Ordered set of "path" strings of all the tasks that are currently checked.
	
	public boolean isChecked(Object element) {
		if (element instanceof EclipseProject) {
			return isGrayed(element);
		} else if (element instanceof Task) {
			return isChecked((Task)element);
		}
		return false;
	}

	private boolean isChecked(Task task) {
		return checked.contains(task.getPath());
	}

	public boolean isGrayed(Object element) {
		if (element instanceof EclipseProject) {
			Boolean cached = grayed.get(element);
			if (cached==null) {
				cached = computeGrayed((EclipseProject)element);
				grayed.put((EclipseProject) element, cached);
			}
			return cached;
		}
		return false;
	}

	private boolean computeGrayed(EclipseProject project) {
		for (EclipseProject child : project.getChildren()) {
			if (isGrayed(child)) {
				return true;
			}
		}
		for (Task task : GradleProject.getTasks(project)) {
			if (isChecked(task)) {
				return true;
			}
		}
		return false;
	}

	public void checkStateChanged(CheckStateChangedEvent event) {
		Object element = event.getElement();
		debug("Check state changed: "+element+" checked = "+event.getChecked());
		if (element instanceof Task) {
			Task task = (Task) element;
			if (event.getChecked()) {
				//Task became checked
				checked.add(task.getPath());
			} else {
				//Task became unchecked
				checked.remove(task.getPath());
			}
		} else if (element instanceof EclipseProject) {
			setChecked((EclipseProject)element, event.getChecked());
		}
		owner.updateOrderedTargets();
		owner.updateLaunchConfigurationDialog();
	}

	private void setChecked(EclipseProject project, boolean isChecked) {
		for (EclipseProject c : project.getChildren()) {
			setChecked(c, isChecked);
		}
		for (Task t : GradleProject.getTasks(project)) {
			if (isChecked) {
				checked.add(t.getPath());
			} else {
				checked.remove(t.getPath());
			}
		}
	}

	/**
	 * @param string
	 */
	private void debug(String string) {
		System.out.println(string);
	}

	/**
	 * Reset the state of check state provider based on a list of selected task strings
	 */
	public void setChecked(List<String> tasks) {
		grayed = new HashMap<EclipseProject, Boolean>();
		checked = new LinkedHashSet<String>();
		for (String task : tasks) {
			checked.add(task);
		}
	}

	public void setChecked(String[] targets) {
		setChecked(Arrays.asList(targets));
	}
	

	public List<String> getChecked() {
		return new ArrayList<String>(checked);
	}

	public String[] toArray() {
		return checked.toArray(new String[checked.size()]);
	}

}
