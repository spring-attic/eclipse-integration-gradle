/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.taskview;

import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.gradle.tooling.model.Launchable;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.util.GradleLabelProvider;
import org.springsource.ide.eclipse.gradle.ui.wizards.GradleProjectTreeLabelProviderWithDescription;

/**
 * Label provider for Gradle Tasks
 * 
 * @author Kris De Volder
 * @author Alex Boyko
 * 
 */
public class TaskLabelProvider extends GradleLabelProvider
		implements
			ITableLabelProvider,
			ILabelProvider,
			ITableFontProvider {

	GradleProjectTreeLabelProviderWithDescription projectLabelProvider = new GradleProjectTreeLabelProviderWithDescription(false);
	
	private Font taskNameFont = null;
	
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			if (element instanceof EclipseProject) {
				return projectLabelProvider.getColumnImage(element, columnIndex);
			} else if (element instanceof Launchable) {
				Launchable task = (Launchable) element;
				return task.isPublic() ? GradleUI.getDefault().getImageRegistry()
						.get(GradleUI.IMAGE_PUBLIC_TASK) : GradleUI.getDefault()
						.getImageRegistry().get(GradleUI.IMAGE_INTERNAL_TASK);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof EclipseProject) {
			return projectLabelProvider.getColumnText(element, columnIndex);
		} else if (element instanceof Launchable) {
			return getColumnText((Launchable)element, columnIndex);
		} else {
			return element.toString();
		}
	}
	
	private String getColumnText(Launchable element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof Task) {
				return ((Task)element).getName();
			} else if (element instanceof TaskSelector) {
				return ((TaskSelector)element).getName();
			} else {
				return element.getDisplayName();
			}
		case 1:
			return element.getDescription();
		default: //There should really only be 2 columns but ...
			return "";
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		projectLabelProvider.dispose();
		if (taskNameFont != null) {
			taskNameFont.dispose();
		}
	}

	public Image getImage(Object element) {
		return getColumnImage(element, 0);
	}

	public String getText(Object element) {
		return getColumnText(element, 0);
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getTaskNameFont();
		} else {
			return JFaceResources.getDefaultFont();
		}
	}

	private Font getTaskNameFont() {
		if (taskNameFont == null) {
			FontData[] fontData = FontDescriptor.copy(JFaceResources.getDefaultFontDescriptor().getFontData());
			for (int i = 0; i < fontData.length; i++) {
				fontData[i].setStyle(fontData[i].getStyle() | SWT.BOLD);
			}
			taskNameFont = FontDescriptor.createFrom(fontData).createFont(PlatformUI.getWorkbench().getDisplay());
		}
		return taskNameFont;
	}
}
