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
package org.springsource.ide.eclipse.gradle.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;


/**
 * @author Kris De Volder
 */
public class EnableDisableDependencyManagementActionDelegate extends GradleProjectActionDelegate {

	public EnableDisableDependencyManagementActionDelegate() {
	}

	public void run(IAction action) {
		final IProject project = getProject();
		runInUi(new GradleRunnable("Enable Dependency Management for "+project.getName()) {
			@Override
			public void doit(IProgressMonitor monitor) throws Exception {
				monitor.beginTask(jobName, 10);
				try {
					final IJavaProject javaProject = JavaCore.create(project);
					if (GradleClassPathContainer.isOnClassPath(javaProject)) {
						GradleClassPathContainer.removeFrom(javaProject, monitor);
					} else {
						GradleClassPathContainer.addTo(javaProject, monitor);
					}
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					monitor.done();
				}
			}
		});
	} 
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		if (action.isEnabled()) {
			if (GradleClassPathContainer.isOnClassPath(getJavaProject())) {
				action.setText("Disable Dependency Management");
			} else {
				action.setText("Enable Dependency Management");
			}
		}
	}

}
