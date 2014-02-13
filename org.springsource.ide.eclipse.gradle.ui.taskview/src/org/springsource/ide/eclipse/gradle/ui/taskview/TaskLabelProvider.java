package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.util.GradleLabelProvider;
import org.springsource.ide.eclipse.gradle.ui.wizards.GradleProjectTreeLabelProviderWithDescription;

public class TaskLabelProvider extends GradleLabelProvider
		implements
			ITableLabelProvider,
			ILabelProvider {

	GradleProjectTreeLabelProviderWithDescription projectLabelProvider = new GradleProjectTreeLabelProviderWithDescription(false);
	
	public Image getColumnImage(Object element, int columnIndex) {
		if (element instanceof EclipseProject) {
			return projectLabelProvider.getColumnImage(element, columnIndex);
		} else if (element instanceof Task) {
			return columnIndex==0? GradleUI.getDefault().getImageRegistry().get(GradleUI.IMAGE_TARGET) : null;
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof EclipseProject) {
			return projectLabelProvider.getColumnText(element, columnIndex);
		} else if (element instanceof Task) {
			return getColumnText((Task)element, columnIndex);
		} else {
			return element.toString();
		}
	}
	
	private String getColumnText(Task element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return element.getName();
		case 1:
//			return element.getPath(); //Hack: so we can see these (and understand what they look like) probably not for users to see
			return element.getDescription();
		default: //There should really only be 2 columns but ...
			return "";
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		projectLabelProvider.dispose();
	}

	public Image getImage(Object element) {
		return getColumnImage(element, 0);
	}

	public String getText(Object element) {
		return getColumnText(element, 0);
	}


}
