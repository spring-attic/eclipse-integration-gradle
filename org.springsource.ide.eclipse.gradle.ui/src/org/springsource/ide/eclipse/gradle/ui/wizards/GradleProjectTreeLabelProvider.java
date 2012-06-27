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
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.ui.util.GradleLabelProvider;


/**
 * This class provides the labels for the Gradle project tree.
 * @author Kris De Volder
 */
public class GradleProjectTreeLabelProvider extends GradleLabelProvider implements ILabelProvider {

	// Images for tree nodes
	private Image file;

	private Image dir;
	
	private Image disabled;

	private final boolean USE_TRANSPARANT_ICONS;

	/**
	 * Constructs a FileTreeLabelProvider
	 * @param useTransparantIcons Determines whether transparant icons will be used when non-leaf project is already imported in the workspace.
	 */
	public GradleProjectTreeLabelProvider(boolean useTransparantIcons) {
		this.USE_TRANSPARANT_ICONS = useTransparantIcons;
		// Create the images
		try {
			file = getImage("icons/gradle-proj-folder.png");
			dir =  getImage("icons/gradle-multiproj-folder.png");
			if (useTransparantIcons) {
				disabled = getImage("icons/gradle-multiproj-folder-disabled.png");
			}
		} catch (Exception e) {
			// Swallow it; we'll do without images
		}
	}
	
	/**
	 * Gets the image to display for a node in the tree
	 * 
	 * @param arg
	 *            the node
	 * @return Image
	 */
	public Image getImage(Object arg) {
		HierarchicalEclipseProject project = (HierarchicalEclipseProject) arg;
		if (project.getChildren().isEmpty()) {
			return file;
		} else {
			if (USE_TRANSPARANT_ICONS) {
				GradleProject gp = GradleCore.create(project);
				if (gp.getProject()==null) {
					//non-existent = not yet imported project
					return dir;
				} else {
					//existing project = already imported
					return disabled;
				}
			} else {
				return dir;
			}
		}
	}

	/**
	 * Gets the text to display for a node in the tree
	 * 
	 * @param arg0
	 *            the node
	 * @return String
	 */
	public String getText(Object arg) {
		HierarchicalEclipseProject project = (HierarchicalEclipseProject) arg;
		return project.getName();
	}

	/**
	 * Called when this LabelProvider is being disposed
	 */
	public void dispose() {
		// Dispose the images
		if (dir != null)
			dir.dispose();
		if (file != null)
			file.dispose();
		if (disabled != null)
			disabled.dispose();
	}

	/**
	 * Returns whether changes to the specified property on the specified
	 * element would affect the label for the element
	 * 
	 * @param arg0
	 *            the element
	 * @param arg1
	 *            the property
	 * @return boolean
	 */
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

}