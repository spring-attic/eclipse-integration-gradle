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
package org.springsource.ide.eclipse.gradle.ui.actions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;


/**
 * Abstract superclass for actions that require one or more projects to be selected.
 * 
 * @author Kris De Volder
 */
public abstract class GradleProjectActionDelegate implements IObjectActionDelegate {
	
	//TODO: all subclasses to support applying action to multiple projects. 

	/**
	 * This is private for a good reason: to force subclasses from getting it into a local
	 * varaible. The typical use pattern in subclasses is to create jobs and schedule these jobs.
	 * Since this field is mutable, it would be a bad idea for a job to refer to it. By the time
	 * the job executes, the list of projects may have changed!
	 */
	private LinkedHashSet<IProject> projects = new LinkedHashSet<IProject>();

	public void selectionChanged(IAction action, ISelection _selection) {
		if (_selection instanceof IStructuredSelection) {
			projects = new LinkedHashSet<IProject>(); //Mustn't reuse
			IStructuredSelection selection = (IStructuredSelection) _selection;
			if (!selection.isEmpty()) {
				for (Object element : selection.toArray()) {
					if (element instanceof IProject) {
						projects.add((IProject) element);
					} else if (element instanceof IResource) {
						projects.add(((IResource) element).getProject());
					} else if (element instanceof IWorkingSet) {
						IWorkingSet workingSet = (IWorkingSet) element;
						for (IAdaptable adaptable : workingSet.getElements()) {
							IProject project = (IProject) adaptable.getAdapter(IProject.class);
							if (project != null) {
								projects.add(project);
								continue;
							}
							// In case a working set has something other than a project
							IResource resource = (IResource) adaptable.getAdapter(IResource.class);
							if (resource != null && resource.getProject() != null) {
								projects.add(resource.getProject());
								continue;
							}
						}
					}
				}
			}
		}
		action.setEnabled(isEnabled());
	}
	
	protected final boolean isEnabled() {
		try {
			for (IProject project : getProjects()) {
				 if (!isEnabled(project)) {
					 return false;
				 }
			}
			return !projects.isEmpty();
		} catch (CoreException e) {
			GradleUI.log(e);
			return false;
		}
	}

	/**
	 * Test whether action can be enabled for a single project. If multiple projects are selected, this method
	 * is used to determine enablement for each project. The action will be enabled if all selected projects
	 * meet the condition.
	 */
	protected boolean isEnabled(IProject project) throws CoreException {
		return project!=null && project.isAccessible() && project.hasNature(GradleNature.NATURE_ID);
	}

	protected List<IProject> getProjects() {
		// Make copy, don't share state! This is used by jobs that may be run concurrently and/or much later on!
		return new ArrayList<IProject>(projects); 
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * For implementations of operations that really don't expect more than one project this method conveniently
	 * returns the first selected project. If the enablement condition on the menu are set correctly, this should
	 * in fact be the only project.
	 */
	protected IProject getProject() {
		if (!projects.isEmpty()) {
			for (IProject p : projects) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * For implementations of operations that really don't expect more than one project this method conveniently
	 * returns the first selected project. If the enablement condition on the menu are set correctly, this should
	 * in fact be the only project.
	 */
	protected IJavaProject getJavaProject() {
		IProject project = getProject();
		if (project!=null) {
			return JavaCore.create(project);
		}
		return null;
	}
	
	/**
	 * For implementations of operations that really don't expect more than one project this method conveniently
	 * returns the first selected project. If the enablement condition on the menu are set correctly, this should
	 * in fact be the only project.
	 */
	protected GradleProject getGradleProject() {
		IProject project = getProject();
		if (project!=null) {
			return GradleCore.create(project);
		}
		return null;
	}

	/**
	 * TODO: get rid of this method and replace its uses with a 'user' job.
	 */
	protected void runInUi(GradleRunnable runnable) {
		IWorkbenchWindow context = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(context, runnable, ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		} catch (Exception e) {
			GradleUI.log(e);
			String msg = ExceptionUtil.getMessage(e);
			if (msg!=null) {
				MessageDialog.openError(null, runnable.toString()+" Failed", msg);
			}
		}
	}

}
