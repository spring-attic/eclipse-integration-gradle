/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.eclipse.EclipseLinkedResource;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.model.gradle.ProjectPublications;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.gradle.core.actions.GradleRefreshPreferences;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClasspathContainerInitializer;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleDependencyComputer;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.modelmanager.GradleModelManager;
import org.springsource.ide.eclipse.gradle.core.modelmanager.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleImportPreferences;
import org.springsource.ide.eclipse.gradle.core.preferences.GradlePreferences;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleProjectPreferences;
import org.springsource.ide.eclipse.gradle.core.util.ArgumentsCustomizerHelper;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.IllegalClassPathEntryException;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.NatureUtils;
import org.springsource.ide.eclipse.gradle.core.wtp.WTPUtil;
import org.springsource.ide.eclipse.gradle.toolingapi.GradleToolingApi;


/**
 * An instance of GradleProject represent a project in a Gradle build. It may be the root project
 * of the build, or it may be one of the children of the root project.
 * <p>
 * There may or may not be a corresponding IProject in the workspace, depending on whether the project
 * was imported into the workspace.
 * <p>
 * To be able to easily map between the Gradle "EclipseProject" model instances, GradleProject instances
 * and IProject instances the absolute location of the project is used as a key. Therefore,
 * no GradleProject instance should ever be created without a location.
 * 
 * @author Kris De Volder
 */
public class GradleProject {
	
	public static boolean DEBUG = (""+Platform.getLocation()).contains("kdvolder");
	
	private static final String GRADLE_SETTINGS_FILE = "settings.gradle";
	
	private void debug(String msg) {
		if (DEBUG) {
			System.out.println(this+": "+msg);
		}
	}
	
	/**
	 * Canonical File pointing to the root of this project's location in the file system.
	 * Never null.
	 */
	private File location;
	
	private GradleModelManager mgr;
	
	/**
	 * The class path container for this project is created lazily.
	 */
	private GradleClassPathContainer classPathContainer = null;

	private IProject cachedProject;

	private Map<Class<?>, Job> modelFetchingJobsCache = new ConcurrentHashMap<Class<?>, Job>();

	private Job modelUpdateJob;

	private GradleProjectPreferences preferences;
	private GradleImportPreferences importPrefs;

	private GradleRefreshPreferences refreshPrefs;

	private GradleDependencyComputer dependencyComputer;

	public GradleProject(File canonicalFile, GradleModelManager mgr) {
		Assert.isLegal(mgr!=null, "GradleModelManager must not be null");
		Assert.isLegal(canonicalFile!=null, "Project location must not be null");
		Assert.isLegal(canonicalFile.exists(), "Project location doesn't exist: "+canonicalFile);
		Assert.isLegal(canonicalFile.isAbsolute(), "Project location must be absolute: "+canonicalFile);
		Assert.isLegal(canonicalFile.isDirectory(), "Project location must be a directory: "+canonicalFile);
		this.location = canonicalFile;
		this.mgr = mgr;
	}
	
	/**
	 * Refreshes the contents of the classpath container to bring it in synch with the gradleModel.
	 * (Note that this doesn't force the gradleModel itself to be updated!)
	 */
	public void refreshDependencies(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Refresh dependencies "+getName(), 3);
		try {
			// TODO JON remove this line!
//			refreshProjectDependencies(ProjectMapperFactory.workspaceMapper(), new SubProgressMonitor(monitor, 1), cancellationToken);
			refreshClasspathContainer(new SubProgressMonitor(monitor, 1));
//			WTPUtil.addWebLibraries(this); only doing this on project import for now.
		} finally {
			monitor.done();
		}
	}

	public boolean isDependencyManaged() {
		IJavaProject jp = getJavaProject();
		if (jp!=null) {
			return GradleClassPathContainer.isOnClassPath(jp);
		}
		return false;
	}
	
	/**
	 * Refreshes the contents of the classpath container to bring it in synch with the gradleModel.
	 * (Note that this doesn't force the gradleModel itself to be updated!)
	 */
	private void refreshClasspathContainer(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Refresh Classpath Container", 2);
		try {
			IProject project = getProject();
			if (project != null) {
				boolean shouldExport = GradleCore.getInstance().getPreferences().isExportDependencies();
				boolean isExported = GradleClassPathContainer.isExported(getJavaProject());
				if (shouldExport!=isExported) {
					GradleClassPathContainer container = getClassPathcontainer();
					if (container!=null) {
						container.setExported(shouldExport, new SubProgressMonitor(monitor, 1));
					}
				}
				//TODO: the requestUpdateFor is asynchronous... make it synchronous!
				GradleClasspathContainerInitializer.requestUpdateFor(project, false);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Reconfigures the project's source folders in Java classpath based on current gradle model.
	 * (Note that this doesn't force the gradle model itself to be updated!)
	 */
	public void refreshSourceFolders(ErrorHandler eh, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		monitor.beginTask("Refreshing source folders", 3);
		try {
			HierarchicalEclipseProject projectModel = getSkeletalGradleModel(new SubProgressMonitor(monitor, 1));
			
			try {
				DomainObjectSet<? extends EclipseLinkedResource> linkedResources = projectModel.getLinkedResources();
				setLinkedResources(linkedResources.getAll(), eh, new SubProgressMonitor(monitor, 1));
			} catch (UnsupportedOperationException e) {
				//Probably using too old a verion of Gradle... this is not serious but could be a problem for some users.
				//Record a warning and proceed.
				eh.handle(IStatus.WARNING, e);
			}
			
			DomainObjectSet<? extends EclipseSourceDirectory> sourceDirs = projectModel.getSourceDirectories();
			setSourceFolders(sourceDirs.getAll(), eh, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	private void setLinkedResources(List<? extends EclipseLinkedResource> linkedResources, ErrorHandler eh, IProgressMonitor mon) {
		
		//TODO: only delete old linked resources if they are changing.
		
		IProject project = getProject();
		Assert.isNotNull(project); //Must have an associated eclipse project to create linked Eclipse resources.
		Collection<IResource> existingLinkedResources = getLinkedResources();
		
		//Delete all the old linked resources
		for (IResource r : existingLinkedResources) {
			try {
				if (r.exists() && r.isLinked()) {
					r.delete(true, null);
				}
			} catch (CoreException e) {
				eh.handleError(e);
			}
		}
		
		mon.beginTask("Create linked resources", linkedResources.size());
		List<IResource> createdResources = new ArrayList<IResource>();
		try {
			
			//Create new ones
			for (EclipseLinkedResource linkedResource : linkedResources) {
				try {
					int type = Integer.valueOf(linkedResource.getType());
					String location = linkedResource.getLocation();
					if (location!=null) {
						if (type==1) {
							IFile f = project.getFile(new Path(linkedResource.getName()));
							if (f.exists() && f.isLinked()) {
								//This shouldn't happen, but it might if, for some reason project state became inconsistent,
								//perhaps because of a Eclipse crash or something like that. So we try to deal with it.
								f.delete(true, null); 
							}
							f.createLink(new Path(location), IResource.ALLOW_MISSING_LOCAL, null);
							createdResources.add(f);
						} else if (type==2) {
							//Folder
							IFolder f = project.getFolder(new Path(linkedResource.getName()));
							if (f.exists() && f.isLinked()) {
								//This shouldn't happen, but it might if, for some reason project state became inconsitent,
								//perhaps because of a Eclipse crash or something like that. So we try to deal with it.
								f.delete(true, null); 
							}
							f.createLink(new Path(location), IResource.ALLOW_MISSING_LOCAL, null);
							createdResources.add(f);
						} else {
							eh.handleError(new IllegalStateException("Unknown linked resource type: "+type));
						}
					} else {
						location = linkedResource.getLocationUri();
						if (location!=null) {
							eh.handleError(new Error("Not implemented: linked resource with URI '"+location+"'"));
						} else {
							eh.handleError(new IllegalStateException("linked resource has neither location nor locationURI "+linkedResource));
						}
					}
				} catch (CoreException e) {
					eh.handleError(e);
				} finally {
					mon.worked(1);
				}
			}
		} catch (Exception e) {
			GradleCore.log(e);
		}
		finally {
			getProjectPreferences().setLinkedResources(createdResources);
			mon.done();
		}
		
	}
	
	/**
	 * Retrieves a collection of linked resources previously set by setLinkedResources. Other linked resources
	 * might exist in the project. (E.g. if a user created them manually). I.e. only the linked resources
	 * that where previously created by Gradle tooling are returned.
	 */
	private Collection<IResource> getLinkedResources() {
		return getProjectPreferences().getLinkedResources();
	}
	
	/**
	 * @return preferences associated with the corresponding eclipse project. This may return null if there is no
	 * corresponding eclipse project.
	 */
	public synchronized GradleProjectPreferences getProjectPreferences() {
		if (preferences==null) {
			preferences = new GradleProjectPreferences(this);
		}
		return preferences;
	}
	
	public synchronized void refreshProjectPreferences() {
		GradleProject root = getRootProjectMaybe();
		if (root!=null && root!=this) {
			root.refreshProjectPreferences();
		}
		//Wait to set this to null until the very last. Otherwise getRootProjectMaybe
		//call will end up reinitializing it right away (determining associated root
		//project requires the project prefs store which contains a pointer to the
		//root project!
		preferences = null;
	}


	/**
	 * @param conf May be null in contexts where there is no launch configuration (e.g. build model operations, or tasks executed for an import
	 * rather than directly by the user). 
	 */
	public void configureOperation(LongRunningOperation gradleOp, ILaunchConfiguration conf) {
		try {
			GradleProjectPreferences projectPrefs = getProjectPreferences();
			File javaHome = projectPrefs.getJavaHome();
			if (javaHome!=null) {
				gradleOp.setJavaHome(javaHome);
			}
			ArgumentsCustomizerHelper jvmArgs = new ArgumentsCustomizerHelper(projectPrefs.getJVMArgs());
			customizeJVMArguments(jvmArgs, this);
			if (jvmArgs.hasArguments()) {
				gradleOp.setJvmArguments(jvmArgs.getArguments());
			}
			ArgumentsCustomizerHelper pgmArgs = new ArgumentsCustomizerHelper(projectPrefs.getProgramArgs());
			customizeProgramArguments(pgmArgs, this);
			if (pgmArgs.hasArguments()) {
				gradleOp.withArguments(pgmArgs.getArguments());
			}
			GradleLaunchConfigurationDelegate.configureOperation(gradleOp, conf);
		} catch (Exception e) {
			//The idea of this catch block is capture 'unsupported' operation exception
			// when running against older version of Gradle. 
			// However, the exception won't be thrown... the exception will only
			// be thrown later on during model build or task execution. 
			// That really doesn't leave us with a practical way to handle the exceptions.
			GradleCore.log(e);
		}
	}
	
	private File getCustomToolingModelRepo() {
		Bundle bundle = Platform.getBundle(GradleToolingApi.PLUGIN_ID);
		try {
			File bundleFile = FileLocator.getBundleFile(bundle);
			if (bundleFile != null && bundleFile.exists() && bundleFile.isDirectory()) {
				File repository = new File(bundleFile, "lib");
				if (repository.exists()) {
					return repository;
				} else {
					GradleCore.log("repository not found in plugin "+GradleCore.PLUGIN_ID);
				}
			} else {
				GradleCore.log("Couldn't access the plugin "+GradleCore.PLUGIN_ID+" as a directory. Maybe it is not installed as an 'exploded' bundle?");
			}
		} catch (IOException e) {
			GradleCore.log(e);
		}
		return null;
	}
	
	public boolean useCustomToolingModel() {
		GradlePreferences prefs = GradleCore.getInstance().getPreferences();
		return prefs.getUseCustomToolingModel();
	}

	
	private File getCustomToolingModelInitScript() {
		if (useCustomToolingModel()) {
			Bundle bundle = Platform.getBundle(GradleToolingApi.PLUGIN_ID);
			try {
				File bundleFile = FileLocator.getBundleFile(bundle);
				if (bundleFile != null && bundleFile.exists() && bundleFile.isDirectory()) {
					File initScript = new File(bundleFile, "toolingCustomModel/init.gradle");
					if (initScript.exists()) {
						return initScript;
					} else {
						GradleCore.log("init.gradle not found in plugin "+GradleCore.PLUGIN_ID);
					}
				} else {
					GradleCore.log("Couldn't access the plugin "+GradleCore.PLUGIN_ID+" as a directory. Maybe it is not installed as an 'exploded' bundle?");
				}
			} catch (IOException e) {
				GradleCore.log(e);
			}
		}
		return null;
	}
	
	/**
	 * Customize program arguments for the Gradle operation. Main purpose incorporate
	 * various 'tooling generated' arguments with the arguments provided by user preferences.
	 * 
	 * @return parameters as array of strings
	 */
	final public static void customizeProgramArguments(ArgumentsCustomizerHelper pgmArgs, GradleProject project) {
		//Add '--settings-file' argument to help gradle find settings.gradle, but only in cases where it needs the
		// help (i.e. when rootproject is not the project's ancestor in the file system).
		if (project != null) {
			GradleProject rootProject = project.getRootProjectMaybe();
			if (rootProject != null) {
				if (rootProject != project
						&& !new File(project.getLocation(), GRADLE_SETTINGS_FILE).exists()
						&& !new Path(project.getLocation().getPath()).isPrefixOf(new Path(rootProject.getLocation().getPath()))) {
					File settingsFile = new File(rootProject.getLocation(), GRADLE_SETTINGS_FILE);
					if (settingsFile.exists()) {
						pgmArgs.addSettingsFile(settingsFile);
					}
				}
			}
			
			//add 'init-script' parameter for custom tooling model if any features that require it are enabled.
			File initScript = project.getCustomToolingModelInitScript();
			if (initScript!=null && initScript.exists()) {
				pgmArgs.addInitScript(initScript);
			}
		}
	}
	
	public static void customizeJVMArguments(ArgumentsCustomizerHelper jvmArgs, GradleProject project) {
		if (project.getCustomToolingModelInitScript()!=null) {
			File repo = project.getCustomToolingModelRepo();
			if (repo!=null && repo.exists()) {
				jvmArgs.add("-Dorg.springsource.ide.eclipse.gradle.toolingApiRepo=" + repo.getAbsolutePath());
				jvmArgs.add("-Dorg.springsource.ide.eclipse.gradle.toolingApiEquivalentBinaryVersion=latest.integration");
			}
		}
	}

	/**
	 * Gets the import preferences for a given project... 
	 * @return
	 */
	public synchronized GradleImportPreferences getImportPreferences() {
		if (importPrefs==null) {
			importPrefs = new GradleImportPreferences(this);
		}
		return importPrefs;
	}
	
	public synchronized GradleRefreshPreferences getRefreshPreferences() {
		if (refreshPrefs==null) {
			refreshPrefs = new GradleRefreshPreferences(this);
		}
		return refreshPrefs;
	}

	
	private void setSourceFolders(List<? extends EclipseSourceDirectory> gradleSourceDirs, ErrorHandler eh, IProgressMonitor monitor) throws JavaModelException {
		
		IJavaProject javaProject = getJavaProject();
		IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();
		int totalWork = 2 * (gradleSourceDirs.size()+oldClasspath.length);
		monitor.beginTask("Converting gradle source folders to Eclipse", totalWork);
		try {
			//To recognise and remove duplicate source entries.
			Set<String> seen = new HashSet<String>();
			//To quickly find oldEntries by path and copy over exclusion and inclusion filters.
			Map<IPath, IClasspathEntry> oldSourceEntries = new HashMap<IPath, IClasspathEntry>();
			for (IClasspathEntry e : oldClasspath) {
				if (e.getEntryKind()==IClasspathEntry.CPE_SOURCE) {
					oldSourceEntries.put(e.getPath(), e);
				}
			}

			//Convert gradle entries into Eclipse classpath entries
			List<IClasspathEntry> sourceEntries = new ArrayList<IClasspathEntry>();
			for (EclipseSourceDirectory gradleSourceDir : gradleSourceDirs) {
				try {
					if (!seen.contains(gradleSourceDir.getPath())) {
						seen.add(gradleSourceDir.getPath());
						IClasspathEntry newSourceEntry = newSourceEntry(gradleSourceDir, oldSourceEntries);
						if (newSourceEntry!=null) {
							sourceEntries.add(newSourceEntry);
						}
					}
				} catch (IllegalClassPathEntryException e) {
					eh.handleError(e);
				}
				monitor.worked(1);
			}

			//Remove old source entries and replace with new ones.
			ClassPath newClasspath = new ClassPath(this);
			newClasspath.addAll(sourceEntries);
			for (IClasspathEntry oldEntry : oldClasspath) {
				if (oldEntry.getEntryKind()!=IClasspathEntry.CPE_SOURCE) {
					newClasspath.add(oldEntry);
				}
				monitor.worked(1);
			}
			
			newClasspath.setOn(javaProject, new SubProgressMonitor(monitor,totalWork/2));
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Create an Eclipse source classpath entry from a Gradle source entry. May return null
	 * if the entry looks invalid (e.g. the corresponding folder doesn't exist in the project)
	 * @param oldEntries old source entries indexed by path, used to copy over exclusions and inclusions so they don't get lost.
	 */
	private IClasspathEntry newSourceEntry(EclipseSourceDirectory gradleSourceDir, Map<IPath, IClasspathEntry> oldEntries) throws IllegalClassPathEntryException {
		Path gradleSourcePath = new Path(gradleSourceDir.getPath());
		try { 
			IFolder srcFolder = getProject().getFolder(gradleSourcePath);
			if (!srcFolder.exists()) {
				throw new IllegalClassPathEntryException("non-existent source folder", this, gradleSourceDir);
			}
			IPath path = srcFolder.getFullPath();
			IClasspathEntry oldEntry = oldEntries.get(path);
			if (oldEntry==null) {
				return JavaCore.newSourceEntry(path);
			} else {
				return JavaCore.newSourceEntry(path, 
						oldEntry.getInclusionPatterns(), 
						oldEntry.getExclusionPatterns(), 
						oldEntry.getOutputLocation(), 
						oldEntry.getExtraAttributes());
			}
		} catch (IllegalClassPathEntryException e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalClassPathEntryException("illegal source folder", this, gradleSourceDir, e);
		}
	}

	public GradleClassPathContainer getClassPathcontainer() {
		return classPathContainer;
	}

	/**
	 * Called by {@link GradleClasspathContainerInitializer} when the class path container
	 * for this project is instantiated.
	 */
	public void setClassPathContainer(GradleClassPathContainer it) {
		Assert.isLegal(classPathContainer==null, "Classpath container set multiple times");
		this.classPathContainer = it;
	}
	
	public synchronized void disposeClassPathcontainer() {
		this.classPathContainer = null;
		this.dependencyComputer = null;
	}

	/**
	 * Return the "eclipse" name of this gradle project. That is the name of the project in the workspace
	 * that corresponds to this Gradle project. 
	 * <p>
	 * @return Name of an IProject that exists in the workpace, or null if this project was not imported into the workspace.
	 */
	public String getName() {
		IProject project = getProject();
		if (project!=null) {
			return project.getName();
		} 
		return null; 
	}

	public <T> T getModel(Class<T> type) throws CoreException, FastOperationFailedException {
		return mgr.getModel(this, type);
	}
	
	public <T> T getModel(Class<T> type, IProgressMonitor mon) throws CoreException {
		return mgr.getModel(this, type, mon);
	}
	
	public <T> T requestModel(final Class<T> type) throws CoreException, FastOperationFailedException {
		try {
			return mgr.getModel(this, type);
		} catch (FastOperationFailedException e) {
			//TODO: looks like race condition. Yes this map is synchronized, but what is stopping two
			// separate threads from getting here at the same time and both putting a new job in 
			// because they find that modelFetchingJobsCache.get(type)==null???
			Job job = modelFetchingJobsCache.get(type);
			if (job == null) {
				modelFetchingJobsCache.put(type, JobUtil.schedule(
						JobUtil.NO_RULE,
						new GradleRunnable("Obtaining Gradle model: "
								+ type.getName()) {

							@Override
							public void doit(IProgressMonitor mon)
									throws Exception {
								try {
									mgr.getModel(GradleProject.this, type, mon);
								} finally {
									modelFetchingJobsCache.remove(type);
								}
							}
						}));
			}
			throw e;
		}
	}
	
	public EclipseProject getGradleModel() throws FastOperationFailedException, CoreException {
		return mgr.getModel(this, EclipseProject.class);
	}
	
	public ProjectPublications getPublications(IProgressMonitor mon) throws CoreException {
		return mgr.getModel(this,ProjectPublications.class, mon);
	}
	
	public ProjectPublications getPublications() throws CoreException, FastOperationFailedException {
		return mgr.getModel(this,ProjectPublications.class);
	}
	
	public <T extends HierarchicalEclipseProject> T getGradleModel(Class<T> type) throws FastOperationFailedException, CoreException {
		return mgr.getModel(this, type);
	}
	
	/**
	 * This is similar to getGradleModel except that when it fails to return a model right away,
	 * it will schedule a background Job to compute the model. Typically, this method is used
	 * by UI based clients that can't afford to wait for the model, but still want to make
	 * sure that at least a model will become available in the future via a registered
	 * {@link IGradleModelListener}.
	 * @throws CoreException 
	 */
	public EclipseProject requestGradleModel() throws FastOperationFailedException, CoreException {
		try {
			return getGradleModel();
		} catch (FastOperationFailedException e) {
			scheduleModelUpdate();
			throw e;
		}
	}

	private synchronized void scheduleModelUpdate() {
		if (modelUpdateJob==null) {
			//If not null, another request for the same model is already active so no need to schedule again.
			modelUpdateJob = JobUtil.schedule(JobUtil.NO_RULE, new GradleRunnable("Build Gradle Model for "+getDisplayName()) {
				@Override
				public void doit(IProgressMonitor mon) throws Exception {
					try {
						getGradleModel(mon);
					} finally {
						finishedModelUpdateJob();
					}
				}
			});
		}
	}
	
	private synchronized void finishedModelUpdateJob() {
		modelUpdateJob = null;
	}

	public EclipseProject getGradleModel(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		return mgr.getModel(this, EclipseProject.class, monitor);
	}
	
	public HierarchicalEclipseProject getSkeletalGradleModel() throws FastOperationFailedException, CoreException {
		return mgr.getModel(this, HierarchicalEclipseProject.class);
	}
	
	public HierarchicalEclipseProject getSkeletalGradleModel(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		return mgr.getModel(this, HierarchicalEclipseProject.class, monitor);
	}
	
	/**
	 * @return the IJavaProject instance associated with this project, or null if this project is not
	 * imported in the workspace.
	 */
	public IJavaProject getJavaProject() {
		//TODO: cache this?
		IProject project = getProject();
		if (project!=null) {
			return JavaCore.create(project);
		}
		return null;
	}

	public void invalidateGradleModel() {
		//TODO model manager: the old behavior invalidated whole project family by invalidating grouped model provider. 
		// Here it only flushes models for this project and not any of its family members.
		//Is this a problem?
		mgr.invalidate(this);
	}

	/**
	 * Add whatever configuration stuff is expected of a Gradle project. Note that the fact that already a
	 * GradleProject instance exists is no guarantee that things like required natures are already added
	 * to the project. A GradleProject instance is only a kind of proxy/wrapper but its creation doesn't
	 * itself enforce any kind of constraints on the underlying project.
	 */
	public void convertToGradleProject(IProjectMapper projectMapping, ErrorHandler eh, IProgressMonitor monitor) {
		debug("convertToGradleProject called");
		monitor.beginTask("Convert to Gradle project", 7);
		try {
			//1: natures
			NatureUtils.ensure(getProject(), new SubProgressMonitor(monitor, 1), 
					GradleNature.NATURE_ID, //Must be first to make gradle project icon have gradle nature showing 
					JavaCore.NATURE_ID);
			debug("convertToGradleProject natures added");
			IJavaProject jProject = getJavaProject();

			//2: Configure class path containers for java
			ClassPath classPath = new ClassPath(this, jProject.getRawClasspath());
			classPath.ensureJREContainer(); //TODO: should be based on java preferences from gradle
			classPath.setOn(jProject, new SubProgressMonitor(monitor, 1));
			debug("convertToGradleProject JRE container added");
			
			//3: Refresh source folders
			refreshSourceFolders(eh, new SubProgressMonitor(monitor, 1));
			debug("refreshed source folders");
			
			//4: Force root project cache to be set
//Removed calling getRootProject doesn't force anything into the cache. 
// model builds do that now. It should not be possible to get this far in here without 
// a model, and if there's a model then rootProject should be set.
//			try {
//				getRootProject();
//				debug("root project cached");
//			} catch (FastOperationFailedException e) {
//				debug("FAILED to cache root project: " + e.getMessage());
//				
//				 //Shouldn't happen... because by now, there should already be gradle model available
//				throw ExceptionUtil.coreException(e);
//			} finally {
//				monitor.worked(1);
//			}
			
			//4: Enable DSL support
			DSLDSupport.maybeAdd(this, eh, new SubProgressMonitor(monitor, 1));
			debug("DSLDSupport maybe added");
			
			//5: Add classpath container
			GradleClassPathContainer.addTo(getJavaProject(), new SubProgressMonitor(monitor, 1));
			debug("Classpath container added");

			//6: Add WTP fixups
			WTPUtil.addWebLibraries(this);
			monitor.worked(1);
		} catch (CoreException e) {
			eh.handleError(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Get the IProject in the workspace corresponding to this GradleProject. If a corresponding project
	 * doesn't exist in the workspace this method returns null.
	 */
	public IProject getProject() {
		//TODO: This cached doesn't work well if correct value for getProject is null. But this is tricky,
		//  since 'caching' a null value is a bit dangerous. We would need to nvalidate the cached value
		//  somehow if when projects get created in the workspace.
		if (cachedProject!=null && cachedProject.exists()) {
			return cachedProject;
		}
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IPath loc = project.getLocation();
			if (loc!=null && loc.toFile().equals(location)) {
				this.cachedProject = project;
				return project;
			}
		};
		return null;
	}

	@Override
	public String toString() {
		IProject project = getProject();
		if (project!=null) {
			return "G"+project;
		}
		return "G"+location;
	}
	
//	/**
//	 * For debugging purposes, dump dependency graph (as a tree) onto system out.
//	 */
//	public void printDependencyGraph() {
//		System.out.println(">>>>>>>> dependencies for "+getName()+">>>>>>>>>>>>>");
//		try {
//			printDependencyGraph(0, getGradleModel(new NullProgressMonitor()));
//		} catch (OperationCanceledException e) {
//			e.printStackTrace();
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//		System.out.println("<<<<<<<< dependencies for "+getName()+"<<<<<<<<<<<<<<");
//	}
//
//	private static void printDependencyGraph(int i, StsEclipseProject project) {
//		for (int j = 0; j < i; j++) {
//			System.out.print("  ");
//		}
//		System.out.println(GradleImportOperation.getDefaultEclipseName(project));
//		for (StsEclipseProjectDependency dep : project.getProjectDependencies().getAll()) {
//			printDependencyGraph(i+1, dep.getTargetProject());
//		}
//	}

	/**
	 * @return Absolute file system location corresponding to this project. This is never null.
	 */
	public File getLocation() {
		return location;
	}

	public static String getHierarchicalName(HierarchicalEclipseProject model) {
		HierarchicalEclipseProject parent = model.getParent();
		String name = parent == null ? "" : getHierarchicalName(parent) + ".";
		name += getName(model);
		return name;
	}

	/**
	 * Use this method instead of HierachicalEclipseProject.getName() to avoid bugs like 
	 * https://issuetracker.springsource.com/browse/STS-1841.
	 * 
	 * @param model
	 * @return
	 */
	public static String getName(HierarchicalEclipseProject model) {
		return model.getName().replace('/', '.');
	}

	public void removeModelListener(IGradleModelListener listener) {
		mgr.removeListener(this, listener);
	}

	public void addModelListener(IGradleModelListener listener) {
		mgr.addListener(this, listener);
	}

	/**
	 * @return a set of tasks obtained from this project alone.
	 */
	public DomainObjectSet<? extends GradleTask> getTasks(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		monitor.beginTask("Retrieve tasks for "+getDisplayName(), 1);
		try {
			EclipseProject model = getGradleModel(new SubProgressMonitor(monitor, 1));
			return GradleProject.getTasks(model);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * @return a set of "task path" strings obtained from this project and all subprojects.
	 */
	public Set<String> getAllTasks() throws FastOperationFailedException, CoreException {
		EclipseProject model = getGradleModel();
		return getAllTasks(model);
	}

	/**
	 * @return a set of "task path" strings obtained from this project and all subprojects.
	 */
	public Set<String> getAllTasks(IProgressMonitor mon) throws OperationCanceledException, CoreException {
		EclipseProject model = getGradleModel(mon);
		return getAllTasks(model);
	}
	
	private static Set<String> getAllTasks(EclipseProject model) {
		Set<String> result = new HashSet<String>();
		collectAllTasks(model, result);
		return result;
	}

	private static void collectAllTasks(EclipseProject model, Set<String> result) {
		DomainObjectSet<? extends GradleTask> tasks = getTasks(model);
		for (GradleTask t : tasks) {
			result.add(t.getPath());
		}
		DomainObjectSet<? extends EclipseProject> projects = model.getChildren();
		for (EclipseProject p : projects) {
			collectAllTasks(p, result);
		}
	}

	public static DomainObjectSet<? extends GradleTask> getTasks(EclipseProject model) {
		return model.getGradleProject().getTasks();
//		return model.getTasks();
	}
	
	public static Map<String, GradleTask> getAggregateTasks(EclipseProject model) {
		Map<String, GradleTask> tasksMap = new HashMap<String, GradleTask>();
		collectAggregateTasks(model, tasksMap);
		return tasksMap;
	}
	
	private static void collectAggregateTasks(EclipseProject model, Map<String, GradleTask> tasksMap) {
		DomainObjectSet<? extends EclipseProject> projects = model.getChildren();
		for (EclipseProject p : projects) {
			collectAggregateTasks(p, tasksMap);
		}
		DomainObjectSet<? extends GradleTask> tasks = getTasks(model);
		for (GradleTask t : tasks) {
			tasksMap.put(t.getName(), t);
		}	
	}


	/**
	 * Is guaranteed to return some non-null String that can be used to identigy the project 
	 * to the user. Uses the Eclipse project name if available or the project location otherwise
	 */
	public String getDisplayName() {
		String name = getName();
		if (name!=null) {
			return name;
		}
		return getLocation().toString();
	}

	/**
	 * Get the parent of this project in the Gradle project hierarchy.
	 * @return parent project or null if the project has no parent.
	 * @throws FastOperationFailedException
	 * @throws CoreException 
	 */
	public GradleProject getParent() throws FastOperationFailedException, CoreException {
		HierarchicalEclipseProject model = getSkeletalGradleModel();
		HierarchicalEclipseProject parentModel = model.getParent();
		if (parentModel!=null) {
			return GradleCore.create(parentModel);
		}
		return null;
	}

	public boolean dependsOn(GradleProject other) throws FastOperationFailedException, CoreException {
		HierarchicalEclipseProject model = getSkeletalGradleModel();
		DomainObjectSet<? extends EclipseProjectDependency> deps = model.getProjectDependencies();
		for (EclipseProjectDependency _dep : deps) {
			GradleProject dep = GradleCore.create(_dep.getTargetProject());
			if (dep==other) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A 'conservative' version of dependsOn. If for some reason it cannot be established whether
	 * this project depends on the other project (typically, because we don't have a model).
	 * Then this method returns a value which is assumed to be conservatively safe in the context
	 * where the method is called. 
	 */
	public boolean conservativeDependsOn(GradleProject other, boolean safeValue) {
		try {
			return this==other || dependsOn(other);
		} catch (FastOperationFailedException e) {
			//Ignore... expected
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return safeValue;
	}

	/**
	 * This method returns the root project associated with a given eclipse project.
	 * This operation may fail (FastOperationFailedException) if a link to the root 
	 * project was not persisted and there is no available model to determine the 
	 * root project.
	 */
	public GradleProject getRootProject() throws FastOperationFailedException {
		GradleProjectPreferences prefs = getProjectPreferences();
		if (prefs!=null) {
			File rootLocation = prefs.getRootProjectLocation();
			if (rootLocation!=null) {
				return GradleCore.create(rootLocation);
			}
		}
		//Sometimes the prefs get damaged because somebody deleted them... or whatever.
		// Try to recover if we have cached models.
		try {
			HierarchicalEclipseProject root = getGradleModel(HierarchicalEclipseProject.class);
			HierarchicalEclipseProject parent;
			while ((parent=root.getParent())!=null) {
				root = parent;
			}
			prefs.setRootProjectLocation(root.getProjectDirectory());
			return GradleCore.create(root);
		} catch (Throwable e) {
			//ignore recovery attempt failures.
		}
		throw new FastOperationFailedException("GradleProject '"+getDisplayName()+"' does not have a persisted root project location or model that allows computing it");
	}

	/**
	 * Like getRootProject, but returns null instead of throwing {@link FastOperationFailedException}
	 */
	public GradleProject getRootProjectMaybe() {
		try {
			return getRootProject();
		} catch (FastOperationFailedException e) {
			return null;
		}
	}

	public boolean isAtLeastM4() throws FastOperationFailedException, CoreException {
		//TODO: remove this stuff? Who still cares about 1.0.M4 ???
		HierarchicalEclipseProject model = getSkeletalGradleModel();
		try {
			model.getLinkedResources();
			return true;
		} catch (UnsupportedOperationException e) {
			//This operation was added in M4
			return false;
		}
	}

	public ClassPath getClassPath() throws JavaModelException {
		return new ClassPath(this, getJavaProject().getRawClasspath());
	}

	/**
	 * Invalidate current model, if any, and then request a new model to be built asynchronously.
	 */
	public void requestGradleModelRefresh() throws CoreException {
		invalidateGradleModel();
		try {
			requestGradleModel();
		} catch (FastOperationFailedException e) {
			//Expected... swallow
		}
	}

	public synchronized GradleDependencyComputer getDependencyComputer() {
		if (dependencyComputer==null) {
			dependencyComputer = new GradleDependencyComputer(this);
		}
		return dependencyComputer;
	}

	public List<HierarchicalEclipseProject> getAllProjectsInBuild() throws FastOperationFailedException, CoreException {
		//TODO: cache this and keep cache as long as model of root project is the same object.
		final List<HierarchicalEclipseProject> projects = new ArrayList<HierarchicalEclipseProject>();
		new ProjectHierarchyVisitor() {
			public void visit(HierarchicalEclipseProject project) throws CoreException {
				projects.add(project);
			}
		}.accept(getRootProject().getGradleModel(HierarchicalEclipseProject.class));
		return projects;
	}

	/**
	 * This returns the folder where the Gradle build output for this project are
	 * being stored.
	 * 
	 * This may return null if the GradleProject is not imported in the workspace.
	 * 
	 * This may return a IFolder instance (handle) that doesn't physically 
	 * exist (yet). E.g. this may happen if a project is imported to the workspace 
	 * but has not yet been built by gradle prior to importing it. In this case 
	 * the build folder may not have been created yet.
	 */
	public IFolder getBuildFolder() {
		IProject p = getProject();
		if (p!=null) {
			//TODO: presumably it is possible to change this from the default. So we need to able to
			// configure this and/or ask Gradle via tooling API where it lives.
			return p.getFolder("build");
		}
		return null;
	}

}
