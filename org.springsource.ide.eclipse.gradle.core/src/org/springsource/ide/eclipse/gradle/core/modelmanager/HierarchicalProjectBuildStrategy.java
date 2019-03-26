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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Build strategy specific for subtypes of HierarchicalEclipseProject. 
 * Produces a group of models for all projectst that are related to
 * the focus project in the project hierarchy.
 *  
 * @author Kris De Volder
 */
public class HierarchicalProjectBuildStrategy extends BuildStrategy {

	/**
	 * Property name used for storing information about a project 'Build Group'. This info is
	 * stored into the Gradle project preferences area of the root project of a project hierarchy/
	 * This info is updated each time there is a succesfull model build.
	 * <p>
	 * The info is used to allow determining the likely members of a Build Group even in the absence
	 * of a succesful build. (E.g to determine members of a build group before a build is complete,
	 * or to guess group members in case of failed builds).
	 */
	private static final String BUILD_FAMILY_PROP = "build.family."+HierarchicalEclipseProject.class.getName();
	
	public HierarchicalProjectBuildStrategy(ModelBuilder builder) {
		super(builder);
	}

	@Override
	public <T> List<ProjectBuildResult<T>> buildModels(GradleProject focusProject, Class<T> type, IProgressMonitor mon) throws CoreException {
		GradleProject rootProject = getRootProject(focusProject);
		GradleProject[] buildFamily = null;
		if (rootProject!=null) {
			buildFamily = getBuildFamily(rootProject);
		}
		GradleProject referenceProject = buildFamily==null ? focusProject : rootProject; //Try to use rootProject as 'reference project' if possible.
		BuildResult<? extends HierarchicalEclipseProject> referenceModel = buildReferenceModel(referenceProject, type, mon);
		if (referenceModel.isSucceeded()) {
			Walk walk = new Walk(referenceModel.getModel());
			ArrayList<ProjectBuildResult<T>> models = new ArrayList<ProjectBuildResult<T>>(walk.cache.size());
			HierarchicalEclipseProject focusModel = walk.cache.get(focusProject);
			if (focusModel!=null) {
				models.add(new ProjectBuildResult<T>(focusProject, new BuildResult<T>(type, cast(type, focusModel))));
			}
			for (Entry<GradleProject, HierarchicalEclipseProject> e : walk.cache.entrySet()) {
				GradleProject project = e.getKey();
				setRootProject(project, walk.rootProject);
				if (project!=focusProject) {
					models.add(new ProjectBuildResult<T>(project, new BuildResult<T>(type, cast(type, e.getValue()))));
				}
			}
			Set<GradleProject> newMembers = walk.cache.keySet();
			setBuildFamily(walk.rootProject, newMembers);
			//Update family members that became orphans
			if (buildFamily!=null) {
				for (GradleProject oldMember : buildFamily) {
					if (!newMembers.contains(oldMember)) {
						setRootProject(oldMember, null);
					}
				}
			}
			return models;
		} else { //FAILED 
			List<ProjectBuildResult<T>> results = new ArrayList<ProjectBuildResult<T>>();
			results.add(new ProjectBuildResult<T>(focusProject, referenceModel.cast(type))); //focus project always first!
			//Try to use persisted 'build family' to mark other family members as failed.
			if (rootProject!=null) {
				if (buildFamily!=null) {
					for (GradleProject familyMember : buildFamily) {
						if (focusProject!=familyMember) {
							results.add(new ProjectBuildResult<T>(familyMember, referenceModel.cast(type)));
						}
					}
				}
			}
			return results;
		}
	}

	private static GradleProject getRootProject(GradleProject project) {
		//Note: it is important to use this method rather than the similar one that is
		// defined on GradleProject because that one has some funky recovery logic for
		// when someone deleted the prefs files. This creates some bad recursion.
		File loc = project.getProjectPreferences().getRootProjectLocation();
		if (loc!=null && loc.exists()) {
			return GradleCore.create(loc);
		}
		return null;
	}
	
	private static void setRootProject(GradleProject project, GradleProject rootProject) {
		if (rootProject!=null) {
			project.getProjectPreferences().setRootProjectLocation(rootProject.getLocation());
		} else {
			project.getProjectPreferences().setRootProjectLocation(null);
		}
	}

	/**
	 * Get our 'build family'. This is set of projects that we where built-with together on the last succesfull
	 * build. This info is always stored and retreived from the rootProject. So if rootProject is not known then
	 * build family can not be determined.
	 */
	private GradleProject[] getBuildFamily(GradleProject rootProject) {
		File[] memberLocs = rootProject.getProjectPreferences().get(BUILD_FAMILY_PROP, (File[])null);
		if (memberLocs!=null) {
			GradleProject[] members = new GradleProject[memberLocs.length];
			for (int i = 0; i < members.length; i++) {
				members[i] = GradleCore.create(memberLocs[i]);
			}
			return members;
		}
		return null;
	}
	
	@Override
	public <T> Set<GradleProject> predictBuildFamily(GradleProject focusProject, Class<T> type) {
		GradleProject root = getRootProject(focusProject);
		if (root!=null) {
			GradleProject[] members = getBuildFamily(root);
			if (members!=null) {
				return new HashSet<GradleProject>(Arrays.asList(members));
			}
		}
		return null;
	}

	/**
	 * Record the build family just created by a build in the rootProject associated with that build.
	 */
	private void setBuildFamily(GradleProject rootProject, Set<GradleProject> members) {
		File[] memberLocations = new File[members.size()];
		int i = 0;
		for (GradleProject member : members) {
			memberLocations[i++] = member.getLocation();
		}
		rootProject.getProjectPreferences().put(BUILD_FAMILY_PROP, memberLocations);
	}

	/**
	 * "Safe" typecast with runtime check.
	 */
	@SuppressWarnings("unchecked")
	private <T> T cast(Class<T> type, Object obj) {
		Assert.isLegal(type.isAssignableFrom(obj.getClass()));
		return (T)obj;
	}
	
	public <T> BuildResult<? extends HierarchicalEclipseProject> buildReferenceModel(GradleProject project, Class<T> type, IProgressMonitor mon) throws CoreException, OperationCanceledException {
		Assert.isLegal(HierarchicalEclipseProject.class.isAssignableFrom(type));
		return builder.buildModel(project, (Class<? extends HierarchicalEclipseProject>)type, mon);
	}

	
	/** Walk the hierarchy and fill a given cache map */
	private static class Walk {
		
		Walk(HierarchicalEclipseProject referenceModel) {
			walk(referenceModel);
		}

		/**
		 * rootProject discovered and set during the walk.
		 */
		GradleProject rootProject = null; 
		Map<GradleProject, HierarchicalEclipseProject> cache = new IdentityHashMap<GradleProject, HierarchicalEclipseProject>();

		void walk(HierarchicalEclipseProject model) {
			if (model!=null) {
				if (model.getParent()==null) { // Make sure root is up-to-date
					GradleProject newRoot = GradleCore.create(model);
					//Should be the only one project who's parent is null.
					Assert.isLegal(rootProject==null || newRoot==rootProject);
					rootProject = newRoot;
				}
				GradleProject project = GradleCore.create(model);
				if (!cache.containsKey(project)) {
					cache.put(project, model);
					walk(model.getParent());
					for (HierarchicalEclipseProject it : model.getChildren()) {
						walk(it);
					}
				}
			}
		}
	}


}
