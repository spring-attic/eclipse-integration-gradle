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

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * A label provider for a two column tree viewer that has a gradle project tree in the first column and
 * descriptions in the second column.
 *  
 * @author Kris De Volder
 */
public class GradleProjectTreeLabelProviderWithDescription extends BaseLabelProvider implements ILabelProvider, IBaseLabelProvider, ITableLabelProvider {
	
	private GradleProjectTreeLabelProvider wrapped;

	/**
	 * @param useTransparantIcons Determines whether transparant icon will be used when non-leaf project is already imported in the workspace.
	 */
	public GradleProjectTreeLabelProviderWithDescription(boolean useTransparantIcons) {
		wrapped = new GradleProjectTreeLabelProvider(useTransparantIcons);
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex==0) {
			return wrapped.getImage(element);
		} else {
			return null;
		}
	}

	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex==0) {
			return wrapped.getText(element);
		} else {
			HierarchicalEclipseProject project = (HierarchicalEclipseProject) element;
			String desc = project.getDescription();
			return desc == null ? "" : desc;
		}
	}

	@Override
	public void dispose() {
		wrapped.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return wrapped.isLabelProperty(element, property);
	}

	// For convenience, also implement the ILabelProvider interface, so that it can be used as plain (no columns)
	// label provider. If used as such, the labels will be the same as those in column 0 for the tabel label provider.
	
	public Image getImage(Object element) {
		return wrapped.getImage(element);
	}

	public String getText(Object element) {
		return wrapped.getText(element);
	}

}
