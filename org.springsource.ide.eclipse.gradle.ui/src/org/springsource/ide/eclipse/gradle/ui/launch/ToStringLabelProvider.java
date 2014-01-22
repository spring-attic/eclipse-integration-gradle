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
package org.springsource.ide.eclipse.gradle.ui.launch;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider that can be used for list-like view with no columns, or at least one columm. Provides lables
 * for one column of the viewer simply by callign the toString method of the content objects.
 * 
 * @author Kris De Volder
 */
public class ToStringLabelProvider extends BaseLabelProvider implements ILabelProvider, ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex==0) {
			return getText(element);
		} else {
			return "";
		}
	}

	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		return ""+element;
	}

}
