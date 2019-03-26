/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.wtp;

import static org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil.getDefaultRuntimePath;
import static org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil.isClassFolderEntry;
import static org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil.modifyDependencyPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.util.IModuleConstants;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.actions.RefreshDependenciesActionCore;
import org.springsource.ide.eclipse.gradle.core.util.WorkspaceUtil;

/** 
 * WTPUtilit methods, that have a 'dynamic' implementation. If WTP plugins are installed in
 * Eclipse, then they provide a 'real' implementation calling on WTP methods and classes.
 * Otherwise a 'null' implementation is provided that doesn't do anything.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class WTPUtil {
	
	public static final String JST_J2EE_WEB_CONTAINER = "org.eclipse.jst.j2ee.internal.web.container";

	/**
	 * @return true if WTP is installed
	 */
	public static boolean isInstalled() {
		return implementation.isInstalled();
	}
	
	/**
	 * Returns true if the given project is a WTP project, and WTP is installed.
	 * @throws CoreException 
	 */
	public static boolean isWTPProject(IProject project) throws CoreException {
		return implementation.isWTPProject(project);
	}

	/**
	 * Rewrites a raw classpath entry to add the necessary classpath attributes to add the entry
	 * to the deployment assembly. The classpath entry is assumed to be an entry from
	 * the given javaProject's classpath.
	 */
	public static IClasspathEntry addToDeploymentAssembly(IJavaProject jproj, IClasspathEntry e) {
		return implementation.addToDeploymentAssembly(jproj, e);
	}
	
	/**
	 * Decides whether a given (resolved) jar dependency should be deployed and adds an extra classpath 
	 * attribute as needed to exclude(or not) the jar.
	 */
	public static void excludeFromDeployment(IJavaProject javaProject, IPath jarPath, List<IClasspathAttribute> extraAttributes) {
		implementation.excludeFromDeployment(javaProject, jarPath, extraAttributes);
	}
	
	/**
	 * Adds the 'Web Libraries' classpath container if the project is a WTP webapp project and doesn't 
	 * already have this container on its classpath.
	 */
	public static void addWebLibraries(GradleProject project) {
		implementation.addWebLibraries(project);
	}
	
	/**
	 * Refresh dependencies for all WTP projects.
	 */
	public static void refreshAllDependencies() {
		implementation.refreshAllDependencies();
	}
	
	////////////// implementations are below //////////////////////
	
	private interface IWTPUtil {
		
		IClasspathEntry addToDeploymentAssembly(IJavaProject javaProject, IClasspathEntry e);

		void addWebLibraries(GradleProject project);

		boolean isInstalled();

		void excludeFromDeployment(IJavaProject javaProject, IPath jarPath,
				List<IClasspathAttribute> extraAttributes);

		boolean isWTPProject(IProject project) throws CoreException;

		void refreshAllDependencies();

	}
	
	private static class NullImplementation implements IWTPUtil {
		public IClasspathEntry addToDeploymentAssembly(IJavaProject javaProject, IClasspathEntry e) {
			return e;
		}
		public boolean isWTPProject(IProject project) {
			return false;
		}
		public void excludeFromDeployment(IJavaProject javaProject,
				IPath jarPath, List<IClasspathAttribute> extraAttributes) {
		}
		public boolean isInstalled() {
			return false;
		}
		public void refreshAllDependencies() {
		}
		public void addWebLibraries(GradleProject p) {
		}
	}

	private static class DefaultImplementation implements IWTPUtil {
		
		public boolean isWTPProject(IProject project) {
			try {
				return project!=null && project.hasNature(IModuleConstants.MODULE_NATURE_ID);
			} catch (CoreException e) {
				GradleCore.log(e);
				return false;
			}
		}
			
		
		public IClasspathEntry addToDeploymentAssembly(IJavaProject jproj, IClasspathEntry cpeOriginal) {
			if (isWTPProject(jproj.getProject())) {
				//This code was based on code found in 
				// org.eclipse.jst.j2ee.internal.ui.AddJavaBuildPathEntriesWizardFragment.handleSelectionChanged()
				final IVirtualComponent virtualComponent = ComponentCore.createComponent(jproj.getProject());
				final boolean isWebApp = JavaEEProjectUtilities.isDynamicWebProject( jproj.getProject() );
				IPath runtimePath = null;
				if(virtualComponent == null){
					runtimePath = getDefaultRuntimePath( isWebApp, isClassFolderEntry( cpeOriginal ) );
				} else {
					runtimePath = getDefaultRuntimePath(virtualComponent, cpeOriginal);
				}
				final IClasspathEntry cpeTagged = modifyDependencyPath( cpeOriginal, runtimePath );
				return cpeTagged;
			} else {
				return cpeOriginal;
			}
		}

		private boolean shouldExcludeFromDeploment(IJavaProject jproj, IPath jarPath) {
			String jarName = jarPath.lastSegment();
			if (jarName!=null && jarName.endsWith(".jar")) {
				DeploymentExclusions exclusions = GradleCore.getInstance().getPreferences().getDeploymentExclusions();
				return exclusions.shouldExclude(jarName);
			}
			return false;
		}

		public void excludeFromDeployment(IJavaProject jproj, IPath jarPath, List<IClasspathAttribute> extraAttributes) {
			if (shouldExcludeFromDeploment(jproj, jarPath)) {
				try {
					extraAttributes.add(UpdateClasspathAttributeUtil.createNonDependencyAttribute());
				} catch (CoreException e) {
					GradleCore.log(e);
				}
			}
		}

		public boolean isInstalled() {
			return true;
		}

		public void refreshAllDependencies() {
			//TODO: in the case where this is currently called (after changings deploy exclusions), 
			// a full refresh isn't necessary. Just reinitializing the CP container without
			//rebuilding the gradle models should suffice (because only need to update classpath attributes).
			RefreshDependenciesActionCore.callOn(getAllWTPProjects());
		}

		private List<IProject> getAllWTPProjects() {
			IProject[] projects = WorkspaceUtil.getProjects();
			List<IProject> wtpProjects = new ArrayList<IProject>();
			for (IProject project : projects) {
				if (isWTPProject(project)) {
					wtpProjects.add(project);
				}
			}
			return wtpProjects;
		}


		/* (non-Javadoc)
		 * @see org.springsource.ide.eclipse.gradle.core.wtp.WTPUtil.IWTPUtil#addWebLibraries(org.eclipse.jdt.core.IJavaProject)
		 */
		public void addWebLibraries(GradleProject project) {
			try {
				IJavaProject jproj = project.getJavaProject();
				if (isWTPProject(jproj.getProject())) {
					if (isWebApp(jproj)) {
						ClassPath classpath = project.getClassPath();
						if (classpath.getContainer(JST_J2EE_WEB_CONTAINER) == null) {
							classpath.add(JavaCore.newContainerEntry(new Path(JST_J2EE_WEB_CONTAINER)));
						}
						classpath.setOn(jproj, new NullProgressMonitor());
					}
				}
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		}

		private boolean isWebApp(IJavaProject jproj) throws CoreException {
			IFacetedProject fproj = ProjectFacetsManager.create(jproj.getProject());
			if (fproj!=null) {
				Set<IProjectFacetVersion> facets = fproj.getProjectFacets();
				for (IProjectFacetVersion fv : facets) {
					IProjectFacet f = fv.getProjectFacet();
					return "jst.web".equals(f.getId());
				}
			}
			return false;
		}
	}

	private static final IWTPUtil implementation = createImplementation();

	private static IWTPUtil createImplementation() {
		try {
			Class.forName("org.eclipse.wst.common.componentcore.ComponentCore");
			Class.forName("org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants");
			return new DefaultImplementation();
		} catch (Throwable e) {
			//Most likely reason for the exception is that WTP is not installed (the WTP plugins are declared as
			//optional dependencies).
			GradleCore.logInfo(e); //Don't log this as a real error: see https://issuetracker.springsource.com/browse/STS-3385
			return new NullImplementation();
		}
	}

}
