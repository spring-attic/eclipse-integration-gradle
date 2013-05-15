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
package org.springsource.ide.eclipse.gradle.core.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.TaskUtil;
import org.springsource.ide.eclipse.gradle.core.actions.GradleRefreshPreferences;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectSorter;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectUtil;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.NatureUtils;
import org.springsource.ide.eclipse.gradle.core.util.ResourceFilterFactory;
import org.springsource.ide.eclipse.gradle.core.wizards.PrecomputedProjectMapper.NameClashException;

/**
 * This is the 'core' counter part of GradleImportWizard. An instance of this class specifies 
 * an import operation that can be executed to import gradle projects into the workspace.
 * <p>
 * Essentially it contains just the information needed to execute the operation, extracted from
 * the wizard UI when the user presses the finish button.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GradleImportOperation {
	
	//TODO: check that root folder project doesn't have parents... (that situation isn't really considered in this code, not sure
	//  what behavior may ensue if user points "root folder" to a nested project.

	public static final boolean DEFAULT_ADD_RESOURCE_FILTERS = true;
	public static final boolean DEFAULT_QUICK_WORKINGSET_ENABLED = true;
	public static final boolean DEFAULT_USE_HIERARCHICAL_NAMES = false;
	
	public static final boolean DEFAULT_DO_AFTER_TASKS = true; //Is not affected by GRADLE-1792 so can be on by default
	public static final boolean DEFAULT_ENABLE_DEPENDENCY_MANAGEMENT = true; //Default setting preserves 'old' behavior.
	public static final boolean DEFAULT_ENABLE_DSLD = false; //Less chance of classpath issues created by adding DSL stuff to classpath.
	
	/**
	 * Tasks to run before doing actual import (if option is enabled)
	 */
	public static String[] DEFAULT_BEFORE_TASKS = {"cleanEclipse", "eclipse"};
	
	/**
	 * Names of tasks to run after doing import (if option is enabled)
	 */
	public static String[] DEFAULT_AFTER_TASKS = {"afterEclipseImport"};
	
	//	private File rootFolder;
	private List<HierarchicalEclipseProject> projectsToImport;
	private boolean addResourceFilters = DEFAULT_ADD_RESOURCE_FILTERS;
	
	// options related to executing additional tasks
	private boolean doBeforeTasks;
	private String[] beforeTasks = DEFAULT_BEFORE_TASKS;

	private boolean doAfterTasks = DEFAULT_DO_AFTER_TASKS;
	private String[] afterTasks = DEFAULT_AFTER_TASKS;
		
	private PrecomputedProjectMapper projectMapper;
	private IWorkingSet[] workingSets = new IWorkingSet[0];
	private String quickWorkingSetName;
	private boolean enableDependencyManagement = DEFAULT_ENABLE_DEPENDENCY_MANAGEMENT;
	private boolean isReimport = false;
	private boolean enableDSLD = DEFAULT_ENABLE_DSLD;
	
	public GradleImportOperation(/*File rootFolder,*/ List<HierarchicalEclipseProject> projectsToImport, boolean addResourceFilters, PrecomputedProjectMapper projectMapping) {
//		this.rootFolder = rootFolder;
		this.projectsToImport = projectsToImport;
		this.projectMapper = projectMapping;
		this.addResourceFilters = addResourceFilters;
		if (!projectsToImport.isEmpty()) {
			this.doBeforeTasks = determineDefaultDoBefore(projectsToImport.get(0));
		} else {
			this.doBeforeTasks = false;
		}
	}

	private boolean determineDefaultDoBefore(HierarchicalEclipseProject p) {
		return determineDefaultDoBefore(GradleCore.create(p));
	}
	
	public void setUseHierachicalNames(boolean hierarchical) throws NameClashException, CoreException {
		this.projectMapper = createProjectMapping(hierarchical, projectMapper.getAllProjects());
	}

	public void setReimport(boolean isReimport) {
		this.isReimport = isReimport;
	}
	
	public static boolean determineDefaultDoBefore(GradleProject rootProject) {
		if (rootProject!=null) { // null means unknown project (in wizard: before project is chosen)
			try {
				if (rootProject.isAtLeastM4()) {
					return true;
				}
			} catch (FastOperationFailedException e) {
				GradleCore.log(e);
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		}
		return false;
	}

	public void perform(ErrorHandler eh, IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		int totalWork = projectsToImport.size();
		int tasksWork = (totalWork+4)/5;
		if (doBeforeTasks) {
			totalWork += tasksWork;
		}
		if (doAfterTasks) {
			totalWork += tasksWork;
		}
		int derivedMarkingWork = tasksWork+1/2;
		totalWork += derivedMarkingWork;
		monitor.beginTask("Importing Gradle Projects", totalWork);
		try {
			if (!projectsToImport.isEmpty()) {
				List<HierarchicalEclipseProject> sorted = new GradleProjectSorter(projectsToImport).getSorted();
				if (doBeforeTasks) {
					doTasks(sorted, beforeTasks, eh, new SubProgressMonitor(monitor, tasksWork));
				}
				for (HierarchicalEclipseProject project : sorted) {
					importProject(project, eh, new SubProgressMonitor(monitor, 1));
					JobUtil.checkCanceled(monitor);
				}
				if (doAfterTasks) {
					boolean doneSome = doTasks(sorted, afterTasks, eh, new SubProgressMonitor(monitor, tasksWork-tasksWork/3));
					if (doneSome) {
						refreshProjects(sorted, new SubProgressMonitor(monitor, tasksWork/3));
					}
				}
				markBuildFolderAsDerived(sorted, new SubProgressMonitor(monitor, derivedMarkingWork));
			}
		} finally {
			monitor.done();
		}
	}

	private void markBuildFolderAsDerived(List<HierarchicalEclipseProject> sorted, IProgressMonitor mon) {
		mon.beginTask("Mark derived resources", sorted.size());
		try {
			for (HierarchicalEclipseProject hp : sorted) {
				GradleProject gp = GradleCore.create(hp);
				markBuildFolderAsDerived(gp, new SubProgressMonitor(mon, 1));
			}
		} finally {
			mon.done();
		}
	}

	/**
	 * Marks folders like 'build' where gradle typically puts stuff created by the build
	 * as 'derived'. This will stop these folders from being shared with CVS, git etc.
	 * and also stop validation.
	 */
	private void markBuildFolderAsDerived(GradleProject gp, IProgressMonitor mon) {
		mon.beginTask("Mark build folder derived", 1);
		try {
			IFolder buildFolder = gp.getBuildFolder();
			if (buildFolder!=null) {
				if (!buildFolder.exists()) {
					//Can't mark it when it doesn't exist. This could be problematic if
					//it is created later by running a task/build. So we pro-actively create it
					//now so we can mark it!
					buildFolder.create(true, true, new NullProgressMonitor());
				}
				buildFolder.setDerived(true, new SubProgressMonitor(mon, 1));
			}
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		finally {
			mon.done();
		}
	}
	
	private void refreshProjects(List<HierarchicalEclipseProject> sorted, IProgressMonitor mon) {
		mon.beginTask("Refreshing projects", sorted.size()*2);
		try {
			for (HierarchicalEclipseProject _p : sorted) {
				IProject p = GradleCore.create(_p).getProject();
				try {
					p.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(mon, 1));
				} catch (CoreException e) {
					GradleCore.log(e);
				}
			}
		} finally {
			mon.done();
		}
	}

	private boolean doTasks(List<HierarchicalEclipseProject> sorted, String[] taskNames, ErrorHandler eh, IProgressMonitor monitor) {
		try {
			return TaskUtil.bulkRunEclipseTasksOn(sorted, taskNames, monitor);
		} catch (Exception e) {
			eh.handleError(e);
			return true; // conservatively assume that something was done before the error happened.
		}
	}

	/**
	 * Make sure that we get the real up-to-date raw classpath. If we don't call this function at certain
	 * points then JDT may give us stale information after the .classpath file has been changed on
	 * disk, for example by running a Gradle task.
	 */
	private static void forceClasspathUpToDate(IProject project) {
		try {
			JavaProject javaProject = (JavaProject)JavaCore.create(project);
			PerProjectInfo info = javaProject.getPerProjectInfo();
			if (info!=null) {
				info.readAndCacheClasspath(javaProject);
			}
		} catch (Exception e) {
			GradleCore.log(e);
		}
	}
	
	private void importProject(HierarchicalEclipseProject projectModel, ErrorHandler eh, IProgressMonitor monitor) {
		final boolean haveWorkingSets = workingSets.length>0 || quickWorkingSetName!=null;
		//This provisional implementation just creates a linked project pointing to wherever the root folder
		// is pointing to.
		int totalWork = 8;
		if (addResourceFilters) { 
			totalWork++; //9
		}
		if (haveWorkingSets) {
			totalWork++; //10
		}
		monitor.beginTask("Import "+projectModel.getName(), totalWork);
		try {
			GradleProject gProj = GradleCore.create(projectModel);
			
			//1
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			String projectName = getEclipseName(projectModel);
			File projectDir = projectModel.getProjectDirectory().getCanonicalFile(); //TODO: is this right for subfolders (locations maybe better relative to ws somehow)
			IProjectDescription projectDescription = ws.newProjectDescription(projectName);
			Path projectLocation = new Path(projectDir.getAbsolutePath());
			if (!isDefaultProjectLocation(projectName, projectDir)) {
				projectDescription.setLocation(projectLocation);
			}
			//To improve error message... check validity of project location vs name
			//note: in import wizard use, this error is impossible since wizard validates this constraint.
			//Be careful that this constraint only needs to hold in a very specific case where the
			//location is nested exactly one level below the workspace location on disk.
			IPath wsLocation = ws.getRoot().getLocation();
			if (wsLocation.isPrefixOf(projectLocation) && wsLocation.segmentCount()+1==projectLocation.segmentCount()) {
				String expectedName = projectDir.getName();
				if (!expectedName.equals(projectName)) {
					eh.handleError(ExceptionUtil.coreException("Project-name ("+projectName+") should match last segment of location ("+projectDir+")"));
				}
			}
			monitor.worked(1);
			
			//2
			IProject project = ws.getRoot().getProject(projectName);
			if (isReimport) {
				Assert.isLegal(project.exists());
			} else {
				project.create(projectDescription, new SubProgressMonitor(monitor, 1));
			}

			//3
			GradleRefreshPreferences refreshPrefs = gProj.getRefreshPreferences();
			refreshPrefs.copyFrom(this);
			
			//4
			if (addResourceFilters) {
				createResourceFilters(project, projectModel, new SubProgressMonitor(monitor, 1));
			}
			
			//5
			if (isReimport) {
				project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
				forceClasspathUpToDate(project);
			} else {
				project.open(new SubProgressMonitor(monitor, 1));
			}
			
			//6..7
			if (project.hasNature(GradleNature.OLD_NATURE_ID)) {
				// project needs migration (i.e. remove old nature and classpath container entries)
				NatureUtils.remove(project, GradleNature.OLD_NATURE_ID,  new SubProgressMonitor(monitor, 1));
				IJavaProject javaproject = gProj.getJavaProject();
				IClasspathEntry[] oldEntries = javaproject.getRawClasspath();
				List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>(oldEntries.length);
				for (IClasspathEntry e : oldEntries) {
					boolean remove = e.getEntryKind()==IClasspathEntry.CPE_CONTAINER &&
							e.getPath().toString().startsWith("com.springsource.sts.gradle");
					if (!remove) {
						newEntries.add(e);
					}
				}
				javaproject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), true, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(2);
			}	
			
			//8..9
			boolean generateOnly = !getEnableDependencyManagement();
			if (generateOnly) {
				try {
					NatureUtils.ensure(project, new SubProgressMonitor(monitor, 1), 
								GradleNature.NATURE_ID, //Must be first to make gradle project icon have gradle nature showing 
								JavaCore.NATURE_ID
					);
					DSLDSupport.maybeAdd(gProj, eh, new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					eh.handleError(e);
				}
			} else {
				gProj.convertToGradleProject(projectMapper, eh, new SubProgressMonitor(monitor, 2));
			}
			
			//10
			if (haveWorkingSets) {
				addToWorkingSets(project, new SubProgressMonitor(monitor, 1));
			}
			
		} catch (Exception e) {
			eh.handleError(e);
		}
		finally {
			monitor.done();
		}
	}

	private boolean isDefaultProjectLocation(String projectName, File projectDir) {
		IPath workspaceLoc = Platform.getLocation();
		if (workspaceLoc!=null) {
			File defaultLoc = new File(workspaceLoc.toFile(), projectName);
			return defaultLoc.equals(projectDir);
		}
		return false;
	}

	private void addToWorkingSets(IProject project, IProgressMonitor monitor) {
		monitor.beginTask("Add '"+project.getName()+"' to working sets", 1);
		try {
			IWorkingSetManager wsm = PlatformUI.getWorkbench().getWorkingSetManager();
			if (quickWorkingSetName!=null) {
				IWorkingSet quickWorkingSet = wsm.getWorkingSet(quickWorkingSetName);
				if (quickWorkingSet==null) {
					quickWorkingSet = wsm.createWorkingSet(quickWorkingSetName, new IAdaptable[0]);
					quickWorkingSet.setId(IWorkingSetIDs.JAVA);
					wsm.addWorkingSet(quickWorkingSet);
				}
				wsm.addToWorkingSets(project, new IWorkingSet[] {quickWorkingSet});
			}
			
			wsm.addToWorkingSets(project, workingSets);
		} finally {
			monitor.done();
		}
	}

	private String getEclipseName(HierarchicalEclipseProject projectModel) {
		return projectMapper.get(projectModel).getName();
	}

	private void createResourceFilters(IProject project, HierarchicalEclipseProject projectModel, IProgressMonitor monitor) throws CoreException {
		//TODO: we now delete all existing filters, perhaps it would be better to somehow mark filters that we "own" and not
		//   delete those that we don't own.
		
		IResourceFilterDescription[] existing = project.getFilters();
		monitor.beginTask("Create resource filters for "+project.getName(), existing.length*2);
		try {
			for (IResourceFilterDescription filter : existing) {
				filter.delete(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1));
			}

			DomainObjectSet<? extends HierarchicalEclipseProject> children = projectModel.getChildren();
			List<FileInfoMatcherDescription> childProjectFilters = new ArrayList<FileInfoMatcherDescription>(children.size());
			IPath parent = new Path(projectModel.getProjectDirectory().getAbsolutePath());
			for (HierarchicalEclipseProject childProject : children) {
				IPath child = new Path(childProject.getProjectDirectory().getAbsolutePath());
				if (parent.isPrefixOf(child)) {
					//Ignore if child isn't nested inside parent (this shouldn't happen but you never know :-)
					childProjectFilters.add(ResourceFilterFactory.projectRelativePath(child.makeRelativeTo(parent)));
				}
			}
			if (!childProjectFilters.isEmpty()) {
				project.createFilter(IResourceFilterDescription.EXCLUDE_ALL|IResourceFilterDescription.FOLDERS|IResourceFilterDescription.INHERITABLE, 
						ResourceFilterFactory.or(childProjectFilters), IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, existing.length));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Exception thrown when a project being imported already exists in the workspace.
	 */
	public static class ExistingProjectException extends CoreException {
		
		private static final long serialVersionUID = 1L;

		public ExistingProjectException(IProject existing, HierarchicalEclipseProject mapped, String conflictType) {
			super(new Status(IStatus.ERROR, GradleCore.PLUGIN_ID, 
					GradleProject.getHierarchicalName(mapped)+" existing workspace project "+existing.getName()+" has the same "+conflictType));
		}

	}

	
	
	/**
	 * Exception thrown when a project being imported has a project depedency that is not imported.
	 */
	public class MissingProjectDependencyException extends CoreException {

		private static final long serialVersionUID = 1L;

		public MissingProjectDependencyException(HierarchicalEclipseProject p, EclipseProjectDependency dep) {
			super(new Status(IStatus.ERROR, GradleCore.PLUGIN_ID, "Project '"+projectMapper.get(p).getName()+"' has a dependency on non-imported project '"+
					projectMapper.get(dep.getTargetProject()).getName()+"'"));
		}
		
	}

	
	/**
	 * Gets a name for the project in the eclipse workspace. This name will be composed of the project's own name and
	 * those of the parent project, to reflect the gradle project hierarchy.
	 */
	public static String getDefaultEclipseName(HierarchicalEclipseProject target) {
		return GradleProject.getHierarchicalName(target);
	}

	/**
	 * Verifies whether this operation can be performed without causing problems. Returns normally if no
	 * problems are detected. Otherwise, it raises an exception indicating the detected problem.
	 * 
	 * @throws ExistingProjectException if any project involved in the import conflicts with one that 
	 * already exists in the workspace. 
	 * 
	 * @throws MissingProjectDependencyException if an imported project has a dependency on a non-imported
	 * project
	 */
	public void verify() throws ExistingProjectException, MissingProjectDependencyException {
		for (HierarchicalEclipseProject p : projectsToImport) {
			verify(p);
		}
	}

	private void verify(HierarchicalEclipseProject p) throws ExistingProjectException, MissingProjectDependencyException {
		//Check the project is free of conflicts with existing project in the workspace
		if (isReimport) {
			//Skip this for 're-import' the conflicts are expected
		} else {
			IProject sameName = projectMapper.get(p);
			if (sameName.exists()) {
				throw new ExistingProjectException(sameName, p, "name");
			}

			IProject sameLocationProject = GradleCore.create(p).getProject();
			if (sameLocationProject!=null) {
				if (sameLocationProject.getName().equals(p.getName()))
					throw new ExistingProjectException(sameLocationProject, p, "location");
			}
		}
			
		//Check that project dependencies are also imported
		for (EclipseProjectDependency dep : p.getProjectDependencies()) {
			HierarchicalEclipseProject targetProject = dep.getTargetProject();
			if (!projectsToImport.contains(targetProject) && !isAlreadyImported(targetProject)) {  
				throw new MissingProjectDependencyException(p, dep);
			}
		}
	}

	private boolean isAlreadyImported(HierarchicalEclipseProject targetProject) {
		GradleProject gp = GradleCore.create(targetProject);
		return gp.getProject()!=null;
	}

	/**
	 * Set optional "workingSets" parameter for the import operation. If set, the imported projects will be automatically
	 * added to the specified workingsets.
	 */
	public void setWorkingSets(IWorkingSet[] workingSets) {
		this.workingSets = workingSets; 
	}
	
	/**
	 * Set the 'quick' workingset parameter for the import operation. The quick workingset is given by name, and separately
	 * from setWorkingSets parameter because it may or may not yet exist. If it doesn't exist it will be created when the import 
	 * operation is performed.
	 */
	public void setQuickWorkingSet(String name) {
		this.quickWorkingSetName = name;
	}

	public boolean getDoBeforeTasks() {
		return this.doBeforeTasks;
	}
	public boolean getDoAfterTasks() {
		return this.doAfterTasks;
	}
	
	public void setDoBeforeTasks(boolean runEclipse) {
		this.doBeforeTasks = runEclipse;
	}

	public static PrecomputedProjectMapper getDefaultProjectMapping(Collection<HierarchicalEclipseProject> projects) throws NameClashException, CoreException {
		return DEFAULT_USE_HIERARCHICAL_NAMES ? new HierarchicalProjectMapper(projects) : new FlatPrecomputedProjectMapper(projects);
	}

	public void excludeProjects(String... name) {
		Set<String> exclude = new HashSet<String>(Arrays.asList(name));
		excludeProjects(exclude);
	}

	public void excludeProjects(Set<String> excludeNames) {
		for (Iterator<HierarchicalEclipseProject> iterator = this.projectsToImport.iterator(); iterator.hasNext();) {
			HierarchicalEclipseProject project = iterator.next();
			if (excludeNames.contains(GradleProject.getName(project))) {
				iterator.remove();
			}
		}
	}

	public void setDoAfterTasks(boolean b) {
		this.doAfterTasks = b;
	}

	public void setBeforeTasks(String... tasks) {
		this.beforeTasks = tasks;
	}

	public void setAfterTasks(String... tasks) {
		this.afterTasks = tasks;
	}

	public String[] getBeforeTasks() {
		return this.beforeTasks;
	}

	public String[] getAfterTasks() {
		return this.afterTasks;
	}

	public boolean getEnableDependencyManagement() {
		return this.enableDependencyManagement;
	}
	
	public void setEnableDependencyManagement(boolean enable) {
		this.enableDependencyManagement = enable;
	}

	/**
	 * This is true if the 'eclipse' task will be run before the import.
	 */
	public boolean isDoingEclipseTask() {
		if (getDoBeforeTasks()) {
			String[] tasks = getBeforeTasks();
			for (String t : tasks) {
				if (t.equals("eclipse")) {
					return true;
				}
			}
		}
		return false;
	}

	public static PrecomputedProjectMapper createProjectMapping(boolean hierarchical, Collection<HierarchicalEclipseProject> projects)
			throws CoreException, NameClashException {
		return hierarchical
				? new HierarchicalProjectMapper(projects) 
			    : new FlatPrecomputedProjectMapper(projects);
	}
	
	public boolean getAddResourceFilters() {
		return addResourceFilters;
	}

	public boolean getUseHierarchicalNames() {
		return projectMapper instanceof HierarchicalProjectMapper;
	}

	public void setAddResourceFilters(boolean enable) {
		this.addResourceFilters = enable;
	}

	public boolean getEnableDSLD() {
		return this.enableDSLD;
	}
	public void setEnableDSLD(boolean enable) {
		this.enableDSLD = enable;
	}

	public static List<HierarchicalEclipseProject> allProjects(File rootFolder) throws OperationCanceledException, CoreException {
		GradleProject proj = GradleCore.create(rootFolder);
		HierarchicalEclipseProject root = proj.getSkeletalGradleModel(new NullProgressMonitor());
		return new ArrayList<HierarchicalEclipseProject>(GradleProjectUtil.getAllProjects(root));
	}

	public static GradleImportOperation importAll(File rootFolder) throws NameClashException, CoreException {
		List<HierarchicalEclipseProject> projects = allProjects(rootFolder);
		PrecomputedProjectMapper mapping = getDefaultProjectMapping(projects);
		return new GradleImportOperation(projects, true, mapping);
	}

}
