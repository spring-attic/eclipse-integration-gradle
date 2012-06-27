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
package org.springsource.ide.eclipse.gradle.ui.taskview;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;


/**
 * Widgets that reflect a 'current' project selection. 
 * 
 * @author Kris De Volder
 */
public class ProjectSelector {

	private Combo projectCombo;
	private Collection<GradleProject> projects = new ArrayList<GradleProject>();
	private GradleTasksView listener;
	private GradleProject currentProject;

	/**
	 * @param parent
	 */
	public ProjectSelector(Composite _parent) {
		Composite line = new Composite(_parent, SWT.NONE);
		GridLayout lineLayout = new GridLayout(2, false);
		lineLayout.marginLeft = 0;
		lineLayout.marginRight = 0;
		line.setLayout(lineLayout);
		
		Label label = new Label(line, SWT.NONE);
		label.setText("Project:");
		GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
		
		projectCombo = new Combo(line, SWT.READ_ONLY | SWT.DROP_DOWN);
		grabHor.applyTo(line);
		grabHor.applyTo(projectCombo);
		
		projectCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setProject(projectCombo.getText());
			}
		});

		projectCombo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				//Ensures the projects are always up-to-date when the user clicks in the combo.
				updateProjects();
			}
		});
		updateProjects();
	}

	private void setProject(String name) {
		if (name!=null && !"".equals(name)) {
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (GradleNature.hasNature(p)) {
				setProject(GradleCore.create(p));
			}
		}
	}
	
	public void setProjectSelectionListener(GradleTasksView listener) {
		this.listener = listener;
	}

	public void setProject(GradleProject selectProject) {
		if (currentProject!=selectProject) {
			currentProject = selectProject;
			updateProjects();
			if (listener!=null) {
				listener.projectSelected(currentProject);
			}
		}
	}

	/**
	 * Refreshes the list of known projects from the workspace and uses that to set the list of items
	 * in the project selection combo.
	 */
	public void updateProjects() {
		projects = GradleCore.getGradleProjects();
		String[] projectNames = new String[projects.size()];
		int i = 0;
		int select = -1;
		for (GradleProject p : projects) {
			if (p==currentProject) {
				select = i;
			}
			projectNames[i++] = p.getName();
		}
		projectCombo.setItems(projectNames);
		if (select>=0) {
			projectCombo.setText(projectNames[select]);
		}
	}

	public GradleProject getProject() {
		return currentProject;
	}

}
