package org.springsource.ide.eclipse.gradle.ui.taskview;

import java.util.HashMap;
import java.util.Map;

import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.gradle.BuildInvocations;

final class ProjectTasksVisibility {
	
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
