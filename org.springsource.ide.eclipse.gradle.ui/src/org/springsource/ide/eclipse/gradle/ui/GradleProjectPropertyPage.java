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
package org.springsource.ide.eclipse.gradle.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleProjectPreferences;

/**
 * For setting preferences that are associated with a Gradle 'build'. These preferences/properties
 * are stored at the root of a project hierarchy. 
 * <p>
 * Eclipse has no concept where a group of projects share some common preferences / settings. 
 * So we implement this as a project property page. But bo matter which project in a hierachy
 * the page is opened on, it will always store/fetch properties in the root project associated
 * with that project.
 * 
 * @author Kris De Volder
 */
public class GradleProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	
	private Button enablePathSortingButton;
	private Button enableNameSortingButton;

	public GradleProjectPropertyPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
        GridDataFactory grabBoth = GridDataFactory.fillDefaults().grab(true, true);
        
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
        grabBoth.applyTo(page);
        
        GradleProject project = getGradleProject();
        if (project==null) {
        	Label errorMsg = new Label(page, SWT.WRAP);
        	errorMsg.setText("Can't open Gradle project preferences: No project selected");
        } else {
            Group group1 = new Group(page, SWT.BORDER);
            grabHorizontal.applyTo(group1);
            group1.setText("Classpath sorting strategy");
            group1.setLayout(new GridLayout(3, true));
            enablePathSortingButton = new Button(group1, SWT.RADIO);
            enablePathSortingButton.setText("Alphabetically by path");
            enableNameSortingButton = new Button(group1, SWT.RADIO);
            enableNameSortingButton.setText("Alphabetically by name");
            Button disableSortingButton = new Button(group1, SWT.RADIO);
            disableSortingButton.setText("As returned by build script");
            if (project.getProjectPreferences().getEnableClasspathEntrySorting()) {
            	enablePathSortingButton.setSelection(true);
            } else if (project.getProjectPreferences().getEnableClassnameEntrySorting()) {
            	enableNameSortingButton.setSelection(true);
            } else {
            	disableSortingButton.setSelection(true);
            }
        }
        return page;
	}

	@Override
	public boolean performOk() {
		GradleProject gradleProject = getGradleProject();
		GradleProjectPreferences prefs = gradleProject.getProjectPreferences();
		prefs.setEnableClasspathEntrySorting(enablePathSortingButton.getSelection());
		prefs.setEnableClassnameEntrySorting(enableNameSortingButton.getSelection());
		return true;
	}

	private GradleProject getGradleProject() {
		IAdaptable el = getElement();
		IProject project = (IProject) el.getAdapter(IProject.class);
		if (project!=null) {
			return GradleCore.create(project);
		}
		return null;
	}
}
