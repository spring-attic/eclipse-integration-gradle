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
package org.springsource.ide.eclipse.gradle.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.modelmanager.GradleModelManager;

/**
 * An instance of this class is responsible for managing the creation of GradleProject instances.
 * 
 * @author Kris De Volder
 */
public class GradleProjectManager {
	
	//TODO: delete entries when not needed anymore somehow... (how would we know?)
	
	/**
	 * Keeps an index of all known Gradle projects. The key used for the index is the canonical path
	 * of the projects folder location in the file system.
	 */
	private Map<String, GradleProject> gradleProjects = new HashMap<String, GradleProject>();
	private GradleModelManager modelManager;
	
	public GradleProjectManager(GradleModelManager mgr) {
		this.modelManager = mgr;
	}

//	/**
//	 * Get the GradleProject instance associated with a given IProject instance. Forces the creation
//	 * of a new instance of one doesn't yet exist. 
//	 */
//	public synchronized GradleProject getOrCreate(IProject project) {
//		try {
//			Assert.isNotNull(project);
//			File location = project.getLocation().toFile().getCanonicalFile();
//			Assert.isLegal(location.exists());
//			GradleProject existing = gradleProjects.get(location.getPath());
//			if (existing==null) {
//				existing = new GradleProject(location);
//				gradleProjects.put(location.getPath(), existing);
//			}
//			return existing;
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}
//	}

	public synchronized GradleProject getOrCreate(File location) {
		File canonicalFile = toCanonicalFile(location);
		GradleProject project = get(canonicalFile);
		if (project==null) {
			project = new GradleProject(canonicalFile, modelManager);
			gradleProjects.put(canonicalFile.getPath(), project);
		}
		return project;
	}
	
	private File toCanonicalFile(File location) {
		try {
			return location.getCanonicalFile();
		} catch (IOException e) {
			GradleCore.log(e);
			return location.getAbsoluteFile(); //Best we can do
		}
	}

	public GradleProject get(File canonicalFile) {
		return gradleProjects.get(canonicalFile.getPath());
	}

	public GradleProject getOrCreate(HierarchicalEclipseProject gProject) {
		GradleProject result = getOrCreate(gProject.getProjectDirectory().getAbsoluteFile());
		return result;
	}

	public GradleProject getOrCreate(IProject project) {
		return getOrCreate(project.getLocation().toFile().getAbsoluteFile());
	}

}
