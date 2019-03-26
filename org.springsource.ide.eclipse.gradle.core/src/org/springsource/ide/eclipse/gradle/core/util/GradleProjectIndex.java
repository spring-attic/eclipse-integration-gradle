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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.gradle.api.Project;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.modelmanager.IGradleModelListener;

/**
 * Naive tasks index implementation based on the root project
 * 
 * @author Alex Boyko
 * 
 */
public class GradleProjectIndex {
	
	private static final Comparator<GradleTask> TASK_NAME_COMPARATOR = new Comparator<GradleTask>() {

		@Override
		public int compare(GradleTask o1, GradleTask o2) {
			return o1.getName().compareTo(o2.getName());
		}
		
	};
	
	private static final Comparator<GradleProject> PROJECT_PATH_COMPARATOR = new Comparator<GradleProject>() {

		@Override
		public int compare(GradleProject o1, GradleProject o2) {
			return o1.getPath().compareTo(o2.getPath());
		}
		
	};
	
	private static final ProjectTasksVisibility VISIBILITY_NOT_SUPPORTED = new ProjectTasksVisibility(null);
	
	private boolean initialized = false;
	private org.springsource.ide.eclipse.gradle.core.GradleProject ideProject = null;
	private LinkedList<org.springsource.ide.eclipse.gradle.core.GradleProject> trackedProjects = new LinkedList<org.springsource.ide.eclipse.gradle.core.GradleProject>();
	private EclipseProject project = null;
	private Map<String, GradleTask> aggregateTasks = Collections.emptyMap();
	private List<GradleTask> sortedAggregateTasks = Collections.emptyList();
	private List<GradleProject> sortedProjects = Collections.emptyList();
	private Map<String, ProjectTasksVisibility> tasksVisibilityCache = new ConcurrentHashMap<String, ProjectTasksVisibility>();
	private Map<GradleProject, EclipseProject> inverseProjectsMap = new HashMap<GradleProject, EclipseProject>();
	
	private ExecutorService executor;
	
	/**
	 * The lock is for resetting the cached data. For example
	 * {@link #tasksVisibilityCache} can be updated during a read operation.
	 * Tasks visibility data lazily fetched when
	 */
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private Future<?> indexRequest = null;
	
	private final IGradleModelListener MODEL_LISTENER = new IGradleModelListener() {
		@Override
		public <T> void modelChanged(org.springsource.ide.eclipse.gradle.core.GradleProject project, Class<T> type, T model) {
			try {
				if (model instanceof EclipseProject) {
					initializeIndexRequest((EclipseProject) model);
				}
			} catch (Exception e) {
				// ignore
			}
		}				
	};
	
	private final IGradleModelListener BUILD_INVOCATIONS_LISTENER = new IGradleModelListener() {
		
		@Override
		public <T> void modelChanged (
				org.springsource.ide.eclipse.gradle.core.GradleProject project,
				Class<T> type, T model) {
			if (model instanceof BuildInvocations) {
				updateVisibilityCache(project.getName(), (BuildInvocations) model);
			}
		}
	};
	
	public GradleProjectIndex() {
		super();
		this.executor = Executors.newFixedThreadPool(2);
	}
	
	public void dispose() {
		if (ideProject != null) {
			ideProject.removeModelListener(MODEL_LISTENER);
		}
		resetIndex();
		this.executor.shutdownNow();
	}
	
	public void setProject(org.springsource.ide.eclipse.gradle.core.GradleProject project) {
		try {
			if (ideProject != null) {
				ideProject.removeModelListener(MODEL_LISTENER);
			}
			resetIndex();
			if (project != null) {
				ideProject = project;
				ideProject.addModelListener(MODEL_LISTENER);
				initializeIndexRequest(ideProject.requestGradleModel());
				updateVisibilityCache(ideProject);
			}
		} catch (FastOperationFailedException e) {
			// ignore
		} catch (CoreException e) {
			// ignore
		}
	}
	
	private void updateVisibilityCache(final String projectName, final BuildInvocations buildInvocations) {
		ProjectTasksVisibility tasksVisibility;
		try {
			tasksVisibility = new ProjectTasksVisibility(buildInvocations);
		} catch (UnsupportedMethodException e) {
			tasksVisibility = VISIBILITY_NOT_SUPPORTED;
			GradleCore
					.log(new Status(
							IStatus.WARNING,
							GradleCore.PLUGIN_ID,
							"Tasks for project '"
									+ projectName
									+ "' don't support visibility feature. Most likely because of old version of Gradle is set for the project",
							e));
		}
		tasksVisibilityCache.put(projectName, tasksVisibility);
	}
	
	private void initializeIndexRequest(final EclipseProject project) {
		if (indexRequest != null && !indexRequest.isDone()) {
			indexRequest.cancel(true);
		}
		indexRequest = executor.submit(new Runnable() {

			@Override
			public void run() {
				initializeIndex(project);
			}
			
		});
	}
	
	private void resetIndex() {
		lock.writeLock().lock();
		try {
			this.initialized = false;
			this.project = null;
			this.aggregateTasks = Collections.emptyMap();
			this.sortedAggregateTasks = Collections.emptyList();
			this.sortedProjects = Collections.emptyList();
			inverseProjectsMap.clear();
			tasksVisibilityCache.clear();
			while (!trackedProjects.isEmpty()) {
				trackedProjects.pollFirst().removeModelListener(BUILD_INVOCATIONS_LISTENER);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void initializeIndex(EclipseProject project) {
		lock.writeLock().lock();
		try {
			this.project = project;
			this.initialized = true;
			this.aggregateTasks = Collections.emptyMap();
			this.sortedAggregateTasks = Collections.emptyList();
			this.sortedProjects = Collections.emptyList();
			inverseProjectsMap.clear();
			tasksVisibilityCache.clear();
			while (!trackedProjects.isEmpty()) {
				trackedProjects.pollFirst().removeModelListener(BUILD_INVOCATIONS_LISTENER);
			}
			if (project != null) {
				this.aggregateTasks = new HashMap<String, GradleTask>();
				this.sortedProjects = new ArrayList<GradleProject>();
				collectAggregateTasks(project, this.aggregateTasks, this.sortedProjects);
				
				sortedAggregateTasks = new ArrayList<GradleTask>(aggregateTasks.values());
				Collections.sort(sortedAggregateTasks, TASK_NAME_COMPARATOR);
				
				Collections.sort(this.sortedProjects, PROJECT_PATH_COMPARATOR);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void collectAggregateTasks(EclipseProject model, Map<String, GradleTask> tasksMap, List<GradleProject> sortedProjects) {
		inverseProjectsMap.put(model.getGradleProject(), model);
		DomainObjectSet<? extends EclipseProject> projects = model.getChildren();
		for (EclipseProject p : projects) {
			collectAggregateTasks(p, tasksMap, sortedProjects);
		}
		GradleProject project = model.getGradleProject();
		sortedProjects.add(project);
		DomainObjectSet<? extends GradleTask> tasks = project.getTasks();
		for (GradleTask t : tasks) {
			tasksMap.put(t.getName(), t);
		}	
	}
	
	private void updateVisibilityCache(org.springsource.ide.eclipse.gradle.core.GradleProject ideProject) {
		trackedProjects.add(ideProject);
		ideProject.addModelListener(BUILD_INVOCATIONS_LISTENER);
		try {
			updateVisibilityCache(ideProject.getName(), 
					ideProject.requestModel(BuildInvocations.class));
		} catch (CoreException e) {
			GradleCore.log(e.getStatus());
		} catch (FastOperationFailedException e) {
			// ignore
		}
	}
	
	public boolean isInitialized() {
		lock.readLock().lock();
		try {
			return initialized;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public EclipseProject getProject() {
		lock.readLock().lock();
		try {
			return project;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public List<GradleTask> findAggeregateTasks(String prefix) {
		lock.readLock().lock();
		try {
			if (prefix.startsWith(Project.PATH_SEPARATOR)) {
				return Collections.emptyList();
			}
			List<GradleTask> tasks = new ArrayList<GradleTask>();
			for (GradleTask task : sortedAggregateTasks) {
				if (task.getName().startsWith(prefix)) {
					tasks.add(createTaskProxy(project.getName(), task, true));
				}
			}
			return tasks;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public GradleTask getTask(String taskStr) {
		lock.readLock().lock();
		try {
			if (taskStr.startsWith(Project.PATH_SEPARATOR)) {
				if (project == null) {
					return null;
				}
				int index = taskStr.lastIndexOf(Project.PATH_SEPARATOR);
				String projectPath = taskStr.substring(0, index == 0 ? 1 : index);
				String taskName = taskStr.substring(index + 1);
				GradleProject targetProject = project.getGradleProject()
						.findByPath(projectPath);
				if (targetProject != null) {
					for (GradleTask task : targetProject.getTasks()) {
						if (task.getName().equals(taskName)) {
							return task;
						}
					}
				}
			} else {
				return aggregateTasks.get(taskStr);
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public List<GradleTask> findTasks(String prefix) {
		lock.readLock().lock();
		try {
			if (project != null && prefix.startsWith(Project.PATH_SEPARATOR)) {
				int index = prefix.lastIndexOf(Project.PATH_SEPARATOR);
				String projectPrefix = prefix.substring(0, index == 0 ? 1 : index);
				String suffix = prefix.substring(index + 1, prefix.length());
				
				GradleProject targetProject = project.getGradleProject()
						.findByPath(projectPrefix);
				
				if (targetProject != null) {
					EclipseProject eclipseProject = inverseProjectsMap.get(targetProject);
					if (eclipseProject != null) {
						updateVisibilityCache(GradleCore.create(eclipseProject));
					}
					return findTasks(targetProject, suffix, new ArrayList<GradleTask>());
				}					
			}
			return Collections.emptyList();
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public List<GradleProject> findProjects(String prefix) {
		lock.readLock().lock();
		try {
			List<GradleProject> projects = new ArrayList<GradleProject>();
			for (GradleProject project : sortedProjects) {
				if (project.getPath().startsWith(prefix)) {
					projects.add(project);
				}
			}
			return projects;	
		} finally {
			lock.readLock().unlock();
		}
	}
	

	private List<GradleTask> findTasks(GradleProject project, String prefix, List<GradleTask> tasks) {
		for (GradleTask task : project.getTasks()) {
			if (task.getName().startsWith(prefix)) {
				tasks.add(createTaskProxy(project.getName(), task, false));
			}
		}
		return tasks;
	}
	
	private GradleTask createTaskProxy(final String projectName, final GradleTask task, final boolean aggregate) {
		return new GradleTask() {
			@Override
			public String getPath() {
				return task.getPath();
			}

			@Override
			public String getName() {
				return task.getName();
			}

			@Override
			public String getDescription() {
				return task.getDescription();
			}

			@Override
			public String getDisplayName() {
				return task.getDisplayName();
			}

			@Override
			public boolean isPublic() {
				lock.readLock().lock();
				ProjectTasksVisibility tasksVisibility = null;
				try {
					tasksVisibility = tasksVisibilityCache.get(projectName);
				} finally {
					lock.readLock().unlock();
				}
				if (tasksVisibility == null) {
					/*
					 * Tasks visibility info ahsn't been loaded yet (BuildInvocations model not fetched yet)
					 */
					throw new IllegalArgumentException("BuildInvocations model not loaded yet");
				} else {
					/*
					 * If visibility feature for tasks is not supported for project then just make task public
					 */
					if (tasksVisibility != VISIBILITY_NOT_SUPPORTED) {
						try {
							return aggregate ? tasksVisibility.isTaskSelectorPublic(task.getName()) : tasksVisibility.isTaskPublic(task.getName());
						} catch (IllegalArgumentException e) {
							/*
							 * Exception means that there is visibility property
							 * for task, which is some sort of a bug either on
							 * Gradle or Gradle Eclipse side
							 */
							GradleCore
									.log(new Status(
											IStatus.WARNING,
											GradleCore.PLUGIN_ID,
											"Task '"
													+ task.getPath()
													+ "' is displayed as public because visibility property is unavailable for it",
											e));
						}
					}
					return true;
				}
			}

			@Override
			public GradleProject getProject() {
				return task.getProject();
			}
		};
	}
	
}
