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
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;


/**
 * Note: the initial version of this class was originally copied from org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne.WorkingSetGroup.
 * <p>
 * It contains some code to create a group of UI widgets to choose whether or not to add imported projects to a Java working set.
 * <p>
 * We have augmented our version of this component with a button to enable the quick creation of a workingset based
 * on a default working set name (in our case, based on the name of the project at the root of the imported project
 * hierarchy.
 * <p>
 * Note that this extra button doesn't actually live inside the same group of widgets but is added to another group
 * of checkable options in the page (this to make the UI clearer and more intuitive). We still keep all the workingset
 * related GUI code here together however.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public final class WorkingSetGroup {
	
	private WorkingSetConfigurationBlock fWorkingSetBlock;
	private String quickWorkingSetName;
	private Button quickWorkingSetButton;

	public WorkingSetGroup() {
		String[] workingSetIds= new String[] { IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE };
		fWorkingSetBlock= new WorkingSetConfigurationBlock(workingSetIds, JavaPlugin.getDefault().getDialogSettings());
	}

	/**
	 * @param placeForQuickWorkingSetButton	The parent composite to which the quick workingSetButton will be added.
	 * @param parent							The parent composite to which the "standard" working set group elements will be added. 
	 */
	public Control createControl(Composite placeForQuickWorkingSetButton, Composite parent) {
		Group workingSetGroup= new Group(parent, SWT.NONE);
		workingSetGroup.setFont(parent.getFont());
		workingSetGroup.setText("Additional working sets");
		workingSetGroup.setLayout(new GridLayout(1, false));
		
		quickWorkingSetButton = new Button(placeForQuickWorkingSetButton, SWT.CHECK);
		quickWorkingSetButton.setText("Create working set based on root project name");
		quickWorkingSetButton.setToolTipText("Creates a working set named after the " +
						"root project and adds all imported projects to this working set.");
		quickWorkingSetButton.setSelection(GradleImportOperation.DEFAULT_QUICK_WORKINGSET_ENABLED);

		fWorkingSetBlock.createContent(workingSetGroup);

		return workingSetGroup;
	}

	public IWorkingSet[] getSelectedWorkingSets() {
		return fWorkingSetBlock.getSelectedWorkingSets();
	}
	
//	public void setSelectedWorkingSets(IWorkingSet[] workingSets) {
//		try { 
//			fWorkingSetBlock.setWorkingSets(workingSets);
//		} catch (IllegalArgumentException e) {
//			//A eclipse bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=358785
//			//causes this so this method won't work... but we can't really do much about it... so ignore.
//		}
//	}
	
	public String getQuickWorkingSetName() {
		if (quickWorkingSetButton.getSelection()) {
			return quickWorkingSetName;
		}
		return null;
	}

	/**
	 * Sets the default working set name that will be automatically selected.
	 */
	public void setQuickWorkingSetName(String quickWorkingSetName) {
		if (this.quickWorkingSetName==null && quickWorkingSetName == null) {
			return;
		}
		if (this.quickWorkingSetName==null || !this.quickWorkingSetName.equals(quickWorkingSetName)) {
			this.quickWorkingSetName = quickWorkingSetName;
			IWorkingSet existing = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(quickWorkingSetName);
			this.quickWorkingSetButton.setText(
					existing==null 
						?"Create workingset '"+quickWorkingSetName+"'"
						:"Add to workingset '"+quickWorkingSetName+"'"
			);
		}
	}
	
	public boolean getQuickWorkingSetEnabled() {
		return quickWorkingSetButton.getSelection();
	}
	
	public void setQuickWorkingSetEnabled(boolean enabled) {
		quickWorkingSetButton.setSelection(enabled);
	}

}
