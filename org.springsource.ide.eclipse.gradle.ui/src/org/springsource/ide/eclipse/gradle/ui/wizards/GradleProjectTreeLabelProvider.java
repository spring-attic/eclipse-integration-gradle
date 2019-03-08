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
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.util.GradleLabelProvider;


/**
 * This class provides the labels for the Gradle project tree.
 * @author Kris De Volder
 */
public class GradleProjectTreeLabelProvider extends GradleLabelProvider implements ILabelProvider {

	private final boolean USE_TRANSPARANT_ICONS;

	/**
	 * Constructs a FileTreeLabelProvider
	 * @param useTransparantIcons Determines whether transparant icons will be used when non-leaf project is already imported in the workspace.
	 */
	public GradleProjectTreeLabelProvider(boolean useTransparantIcons) {
		this.USE_TRANSPARANT_ICONS = useTransparantIcons;
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
			return GradleUI.getDefault().getImageRegistry().get(GradleUI.IMAGE_PROJECT_FOLDER);
		} else {
			if (USE_TRANSPARANT_ICONS) {
				GradleProject gp = GradleCore.create(project);
				if (gp.getProject()==null) {
					//non-existent = not yet imported project
					return GradleUI.getDefault().getImageRegistry().get(GradleUI.IMAGE_MULTIPROJECT_FOLDER);
				} else {
					//existing project = already imported
					return GradleUI.getDefault().getImageRegistry().get(GradleUI.IMAGE_MULTIPROJECT_FOLDER_DISABLED);
				}
			} else {
				return GradleUI.getDefault().getImageRegistry().get(GradleUI.IMAGE_MULTIPROJECT_FOLDER);
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
