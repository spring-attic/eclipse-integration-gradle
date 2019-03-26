/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.HashMap;
import java.util.Map;

import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.gradle.BuildInvocations;

/**
 * Indexes tasks visibility properties against tasks and task selector names
 * 
 * @author Alex Boyko
 *
 */
public final class ProjectTasksVisibility {
	
	private Map<String, Boolean> tasks = new HashMap<String, Boolean>();
	
	private Map<String, Boolean> taskSelectors = new HashMap<String, Boolean>();
	
	public ProjectTasksVisibility(BuildInvocations model) {
		super();
		if (model != null) {
			for (TaskSelector taskSelector : model.getTaskSelectors()) {
				taskSelectors.put(taskSelector.getName(), taskSelector.isPublic());
			}
			for (Task task : model.getTasks()) {
				tasks.put(task.getName(), task.isPublic());
			}
		}
	}
	
	public boolean isTaskPublic(String task) {
		if (tasks.containsKey(task)) {
			return tasks.get(task);
		} else if (taskSelectors.containsKey(task)) {
			return taskSelectors.get(task);
		} else {
			throw new IllegalArgumentException("There is no such Task!");
		}
	}
	
	public boolean isTaskSelectorPublic(String taskSelector) {
		if (taskSelectors.containsKey(taskSelector)) {
			return taskSelectors.get(taskSelector);
		} else {
			throw new IllegalArgumentException("There is no such Task Selector!");
		}
	}

}
