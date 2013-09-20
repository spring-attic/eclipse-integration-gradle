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

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * This is a content provider providing a tree of Gradle projects.
 * @author Kris De Volder
 */
public class GradleProjectTreeContentProvider implements ITreeContentProvider {

	/**
	 * Gets the children of the specified object
	 * 
	 * @param arg0
	 *            the parent object
	 * @return Object[]
	 */
	public Object[] getChildren(Object arg) {
		HierarchicalEclipseProject project = (HierarchicalEclipseProject) arg;
		DomainObjectSet<? extends HierarchicalEclipseProject> children = project.getChildren();
		return children.toArray(new HierarchicalEclipseProject[children.size()]);
	}

	/**
	 * Gets the parent of the specified object
	 * 
	 * @param arg0
	 *            the object
	 * @return Object
	 */
	public Object getParent(Object arg) {
		HierarchicalEclipseProject project = (HierarchicalEclipseProject) arg;
		return project.getParent();
	}

	/**
	 * Returns whether the passed object has children
	 * 
	 * @param arg0
	 *            the parent object
	 * @return boolean
	 */
	public boolean hasChildren(Object arg0) {
		// Get the children
		Object[] obj = getChildren(arg0);

		// Return whether the parent has children
		return obj == null ? false : obj.length > 0;
	}

	/**
	 * Gets the root element(s) of the tree
	 * 
	 * @param arg0
	 *            the input data
	 * @return Object[]
	 */
	public Object[] getElements(Object input) {
		if (input!=null) {
			if (input instanceof List) {
				List<?> list = (List<?>) input;
				HierarchicalEclipseProject model = (HierarchicalEclipseProject) list.get(0);
				return new Object[] {model};
			}
		}
		return new Object[0];
	}

	/**
	 * Disposes any created resources
	 */
	public void dispose() {
		// Nothing to dispose
	}

	/**
	 * Called when the input (root folder) changes 
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		//	  if (newInput==null || newInput instanceof File) {
		//		  this.root = (File) newInput;
		//	  }
	}
}
