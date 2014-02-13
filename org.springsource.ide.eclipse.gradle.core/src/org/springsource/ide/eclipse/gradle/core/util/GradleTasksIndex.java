package org.springsource.ide.eclipse.gradle.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.CoreException;
import org.gradle.api.Project;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;

public class GradleTasksIndex {
	
	private boolean initialized = false;
	private EclipseProject project = null;
	private Map<String, GradleTask> aggregateTasks = null;
	private List<GradleTask> sortedAggregateTasks = null;
	
	private ReentrantLock lock = new ReentrantLock();
	
	public GradleTasksIndex() {
		super();
	}
	
	public void setProject(org.springsource.ide.eclipse.gradle.core.GradleProject project) {
		lock.lock();
		try {
			this.initialized = false;
			this.project = null;
			this.aggregateTasks = null;
			this.sortedAggregateTasks = null;
			this.project = project.requestGradleModel();
		} catch (FastOperationFailedException e) {
			project.addModelListener(new IGradleModelListener() {
				@Override
				public void modelChanged(
						org.springsource.ide.eclipse.gradle.core.GradleProject project) {
					lock.lock();
					try {
						GradleTasksIndex.this.initialized = true;
						GradleTasksIndex.this.project = project.getGradleModel();
					} catch (Exception e) {
						// ignore
					} finally {
						lock.unlock();
					}
				}				
			});
		} catch (CoreException e) {
			// ignore
		} finally {
			lock.unlock();
		}
	}
	
	public boolean isInitialized() {
		lock.lock();
		try {
			return initialized;
		} finally {
			lock.unlock();
		}
	}
	
	public EclipseProject getProject() {
		lock.lock();
		try {
			return project;
		} finally {
			lock.unlock();
		}
	}
	
	public List<GradleTask> findAggeregateTasks(String prefix) {
		lock.lock();
		try {
			if (project == null) {
				return Collections.emptyList();
			}
			if (prefix.startsWith(Project.PATH_SEPARATOR)) {
				return Collections.emptyList();
			}
			List<GradleTask> tasks = new ArrayList<GradleTask>();
			for (GradleTask task : getSoretedAggregateTasks()) {
				if (task.getName().startsWith(prefix)) {
					tasks.add(task);
				}
			}			
			return tasks;
		} finally {
			lock.unlock();
		}
	}
	
	private Map<String, GradleTask> getAggreageTasks() {
		if (this.aggregateTasks == null) {
			this.aggregateTasks = org.springsource.ide.eclipse.gradle.core.GradleProject.getAggregateTasks(project);
		}
		return aggregateTasks;
	}
	
	private List<GradleTask> getSoretedAggregateTasks() {
		if (sortedAggregateTasks == null) {
			sortedAggregateTasks = new ArrayList<GradleTask>(getAggreageTasks().values());
			Collections.sort(sortedAggregateTasks, new Comparator<GradleTask>() {

				@Override
				public int compare(GradleTask o1, GradleTask o2) {
					return o1.getName().compareTo(o2.getName());
				}
				
			});
		}
		return sortedAggregateTasks;
	}
	
	public GradleTask getTask(String taskStr) {
		lock.lock();
		try {
			if (project == null) {
				return null;
			}
			if (taskStr.startsWith(Project.PATH_SEPARATOR)) {
				int index = taskStr.lastIndexOf(Project.PATH_SEPARATOR);
				String projectPath = taskStr.substring(0, index == 0 ? 1 : index);
				String taskName = taskStr.substring(index + 1);
				GradleProject targetProject = this.project.getGradleProject().findByPath(projectPath);
				if (targetProject != null) {
					for (GradleTask task : targetProject.getTasks()) {
						if (task.getName().equals(taskName)) {
							return task;
						}
					}
				}
			} else {
				return getAggreageTasks().get(taskStr);
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	public List<GradleTask> findTasks(String prefix) {
		lock.lock();
		try {
			if (project == null) {
				return Collections.emptyList();
			}
			List<GradleTask> results = new ArrayList<GradleTask>();
			if (prefix.isEmpty()) {
				List<GradleProject> projects = new ArrayList<GradleProject>();
				projects.add(project.getGradleProject());
				collectAllTasksBfs(projects, results);
			} else if (prefix.startsWith(Project.PATH_SEPARATOR)) {
				int index = prefix.lastIndexOf(Project.PATH_SEPARATOR);
				String projectPrefix = prefix.substring(0, index == 0 ? 1 : index);
				String suffix = prefix.substring(index + 1, prefix.length());
				
				GradleProject candidateProject = project.getGradleProject()
						.findByPath(projectPrefix);
				
				if (candidateProject != null) {
					findTasks(candidateProject, suffix, results);
					collectAllTasksBfs(findEclipseProjects(candidateProject, suffix), results);
				}					
			}
			return results;
		} finally {
			lock.unlock();
		}
	}
	
	private static List<GradleProject> findEclipseProjects(GradleProject project, String prefix) {
		List<GradleProject> projects = new ArrayList<GradleProject>();
		for (GradleProject child : project.getChildren()) {
			if (child.getName().startsWith(prefix)) {
				projects.add(child);
			}
		}
		return projects;
	}
	
	private static void findTasks(GradleProject project, String prefix, List<GradleTask> tasks) {
		for (GradleTask task : project.getTasks()) {
			if (task.getName().startsWith(prefix)) {
				tasks.add(task);
			}
		}
	}
	
	private static void collectAllTasksBfs(List<GradleProject> projects,
			List<GradleTask> tasks) {
		while (!projects.isEmpty()) {
			GradleProject project = projects.remove(0);
			for (GradleTask task : project.getTasks()) {
				tasks.add(task);
			}
			for (GradleProject childProject : project.getChildren()) {
				projects.add(childProject);
			}
		}
	}

}
