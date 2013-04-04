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
package org.springsource.ide.eclipse.gradle.core.actions;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
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
 */
public class ReimportOperation {
	
	private GradleProject p;
	private GradleRefreshPreferences prefs;

	public ReimportOperation(GradleProject p) throws FastOperationFailedException {
		this.p = p;
		this.prefs = p.getRefreshPreferences();
	}

	public void perform(ErrorHandler eh, IProgressMonitor m) {
		m.beginTask("Reimporting "+p.getDisplayName(), 3);
		try {
			GradleImportOperation op = createImportOperation(new SubProgressMonitor(m, 1));
			op.perform(eh, new SubProgressMonitor(m, 2));
		} catch (FastOperationFailedException e) {
			eh.handleError(e);
		} catch (CoreException e) {
			eh.handleError(e);
		} finally {
			m.done();
		}
	}

	private GradleImportOperation createImportOperation(IProgressMonitor m) throws FastOperationFailedException, OperationCanceledException, CoreException {
		m.beginTask("Create re-import operation", 1);
		try {
			List<HierarchicalEclipseProject> projects = Arrays.asList((HierarchicalEclipseProject)p.getGradleModel(m));
			List<HierarchicalEclipseProject> relatedProjects = p.getAllProjectsInBuild();
			GradleImportOperation op = new GradleImportOperation(
				projects,
				prefs.getAddResourceFilters(),
				GradleImportOperation.createProjectMapping(prefs.getUseHierarchicalNames(), relatedProjects)
			);
			//op.setWorkingSets(workingSets); // this option only meaningfull on initial import. 
			op.setQuickWorkingSet(null); // this option only meaningfull on initial import. 
			op.setEnableDependencyManagement(p.isDependencyManaged());
			op.setDoAfterTasks(prefs.getDoAfterTasks());	
			op.setAfterTasks(prefs.getAfterTasks());
			op.setDoBeforeTasks(prefs.getDoBeforeTasks());
			op.setBeforeTasks(prefs.getBeforeTasks());
			op.setEnableDSLD(prefs.getEnableDSLD());
			op.setReimport(true);
			op.verify();
			return op;
		} finally {
			m.done();
		}
	}

}
