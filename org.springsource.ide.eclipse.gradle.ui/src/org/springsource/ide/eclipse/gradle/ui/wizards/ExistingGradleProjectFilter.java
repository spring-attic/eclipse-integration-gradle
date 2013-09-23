/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;


/**
 * This is a ViewerFilter used to remove existing projects from the Gradle import wizard so it
 * only shows projects that can *actually* still be imported.
 * 
 * @author Kris De Volder
 */
public class ExistingGradleProjectFilter extends ViewerFilter {
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof HierarchicalEclipseProject) {
			GradleProject project = GradleCore.create((HierarchicalEclipseProject)element);
 			if (project.getProject()!=null) {
 				//If project has children we don't remove it since this also filters the children, and those children may be importable still!
 				try {
 					if (project.getSkeletalGradleModel().getChildren().isEmpty()) {
 						return false;
 					}
 				} catch (CoreException e) {
 					GradleCore.log(e); 
 				} catch (FastOperationFailedException e) {
 					return true; // Be conservative, we don't know if there are children, assume their could be
 				}
 			}
		} 
		return true;
	}

}
