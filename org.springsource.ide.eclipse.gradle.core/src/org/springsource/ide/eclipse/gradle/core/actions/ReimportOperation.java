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
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * An instance of this class is the 'workhorse' that does a 'reimport' style refresh of a project that does not 
 * have automatic dependency management enabled (i.e. no classpath container).
 * <p>
 * It essentially performs an import of a single project based on a set of 'Refresh' preferences associated with
 * the project. The refresh preferences are saved in the project when it is imported initially.
 * 
 * @author Kris De Volder
 * @author Alex Boyko
 */
public class ReimportOperation {
	
	private Collection<GradleProject> gradleProjects;

	public ReimportOperation(GradleProject p) throws FastOperationFailedException {
		this.gradleProjects = Collections.singletonList(p);
	}

	public ReimportOperation(Collection<GradleProject> gradleProjects) throws FastOperationFailedException {
		this.gradleProjects = gradleProjects;
	}
	
	public void perform(ErrorHandler eh, IProgressMonitor m, CancellationToken cancellationToken) {
		m.beginTask("Reimporting Gradle Projects", 3);
		try {
			if (!gradleProjects.isEmpty()) {
				GradleImportOperation op = createImportOperation(new SubProgressMonitor(m, 1), cancellationToken);
				op.perform(eh, new SubProgressMonitor(m, 2), cancellationToken);
			}
		} catch (FastOperationFailedException e) {
			eh.handleError(e);
		} catch (CoreException e) {
			eh.handleError(e);
		} finally {
			m.done();
		}
	}

	private GradleImportOperation createImportOperation(IProgressMonitor m, CancellationToken cancellationToken) throws FastOperationFailedException, OperationCanceledException, CoreException {
		m.beginTask("Create re-import operation", 1);
		try {
			GradleRefreshPreferences prefs = gradleProjects.iterator().next().getRootProject().getRefreshPreferences();
			List<HierarchicalEclipseProject> projects = new ArrayList<HierarchicalEclipseProject>(gradleProjects.size());
			Set<HierarchicalEclipseProject> relatedProjectsSet = new HashSet<HierarchicalEclipseProject>();
			for (GradleProject gradleProject : gradleProjects) {
				projects.add(gradleProject.getGradleModel(m, cancellationToken));
				relatedProjectsSet.addAll(gradleProject.getAllProjectsInBuild());
			}
			GradleImportOperation op = new GradleImportOperation(
				projects,
				false,
				GradleImportOperation.createProjectMapping(
						prefs.getUseHierarchicalNames(), 
						Arrays.asList(relatedProjectsSet.toArray(
								new HierarchicalEclipseProject[relatedProjectsSet.size()]
						)
				))
			);			
			op.setQuickWorkingSet(null); // this option only meaningfull on initial import. 
			op.setReimport(true);
			op.verify();
			return op;
		} finally {
			m.done();
		}
	}
	
}
