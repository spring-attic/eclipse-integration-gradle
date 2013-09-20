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
package org.springsource.ide.eclipse.gradle.ui.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * Various utility methods for extracting information from UI selections. Goal for these methods is to factor as much as
 * possible the logic for extracting objects of certain types (as expected by operations operating on selections) so
 * that this logic doesn't get duplicated.
 * 
 * @author Kris De Volder
 */
public class SelectionUtils {

	public static GradleProject getGradleProject(ISelection selection) {
		IProject project = getProject(selection);
		if (project!=null && GradleNature.hasNature(project)) {
			return GradleCore.create(project);
		}
		return null;
	}

	public static IProject getProject(ISelection selection) {
		IResource rsrc = getResource(selection);
		if (rsrc!=null) {
			return rsrc.getProject();
		}
		return null;
	}

	private static IResource getResource(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object firstEl = ((IStructuredSelection) selection).getFirstElement(); 
			if (firstEl!=null) {
				if (firstEl instanceof IResource) {
					return ((IResource) firstEl).getProject();
				} else if (firstEl instanceof IAdaptable) {
					return (IResource) ((IAdaptable) firstEl).getAdapter(IResource.class);
				}
			}
		}
		return null;
	}

}
