/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.CoreException;
import org.gradle.api.Project;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;

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
	
	private boolean initialized = false;
	private StsEclipseProject project = null;
	private Map<String, GradleTask> aggregateTasks = Collections.emptyMap();
	private List<GradleTask> sortedAggregateTasks = Collections.emptyList();
	private List<GradleProject> sortedProjects = Collections.emptyList();
	
	private ExecutorService executor;
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private Future<?> indexRequest = null;
	
	public GradleProjectIndex() {
		super();
		this.executor = Executors.newFixedThreadPool(1);
	}
	
	public void dispose() {
		this.executor.shutdownNow();
	}
	
	public void setProject(org.springsource.ide.eclipse.gradle.core.GradleProject project) {
		try {
			resetIndex();
			if (project != null) {
				initializeIndexRequest(project.requestGradleModel());
			}
		} catch (FastOperationFailedException e) {
			project.addModelListener(new IGradleModelListener() {
				@Override
				public void modelChanged(
						org.springsource.ide.eclipse.gradle.core.GradleProject project) {
					try {
						initializeIndexRequest(project.getGradleModel());
					} catch (Exception e) {
						// ignore
					}
				}				
			});
		} catch (CoreException e) {
			// ignore
		}
	}
	
	private void initializeIndexRequest(final StsEclipseProject project) {
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
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void initializeIndex(StsEclipseProject project) {
		lock.writeLock().lock();
		try {
			this.project = project;
			this.initialized = true;
			this.aggregateTasks = Collections.emptyMap();
			this.sortedAggregateTasks = Collections.emptyList();
			this.sortedProjects = Collections.emptyList();
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
	
	private static void collectAggregateTasks(StsEclipseProject model, Map<String, GradleTask> tasksMap, List<GradleProject> sortedProjects) {
		DomainObjectSet<? extends StsEclipseProject> projects = model.getChildren();
		for (StsEclipseProject p : projects) {
			collectAggregateTasks(p, tasksMap, sortedProjects);
		}
		GradleProject project = model.getGradleProject();
		sortedProjects.add(project);
		DomainObjectSet<? extends GradleTask> tasks = project.getTasks();
		for (GradleTask t : tasks) {
			tasksMap.put(t.getName(), t);
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
	
	public StsEclipseProject getProject() {
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
					tasks.add(task);
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
	

	private static List<GradleTask> findTasks(GradleProject project, String prefix, List<GradleTask> tasks) {
		for (GradleTask task : project.getTasks()) {
			if (task.getName().startsWith(prefix)) {
				tasks.add(task);
			}
		}
		return tasks;
	}
	
}
