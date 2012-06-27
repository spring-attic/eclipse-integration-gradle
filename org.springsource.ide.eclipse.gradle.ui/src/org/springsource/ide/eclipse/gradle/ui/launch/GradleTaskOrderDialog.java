/******************************************************************************************
 * Copyright (c) 2011 SpringSource, a division of VMware, Inc. All rights reserved.
 ******************************************************************************************/
/*
 * Copied and adapted from org.eclipse.ant.internal.ui.launchConfigurations.TargetOrderDialog
 * Copyright notice from original file below.
 * 
 **********************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

/**
 * Dialog to specify target execution order
 * @author Kris De Volder (Note: mostly this code is actually copied and modified from org.eclipse.ant.internal.ui.launchConfigurations.TargetOrderDialog
 * original code doesn't list author)
 */
public class GradleTaskOrderDialog extends Dialog implements ISelectionChangedListener {
	
	private Button fUp;
	private Button fDown;
	private TableViewer fViewer;
	private String[] fTargets;

	/**
	 * Constructs the dialog.
	 * 
	 * @param parentShell
	 */
	public GradleTaskOrderDialog(Shell parentShell, String[] targets) {
		super(parentShell);
		fTargets = targets;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Gradle task execution order");
		
		Composite comp = (Composite)super.createDialogArea(parent);
		((GridLayout)comp.getLayout()).numColumns= 2;
		Label label = new Label(comp, SWT.NONE);
		label.setText("Specify task execution order");
		label.setFont(comp.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);		
		
		createTargetList(comp);
		
		createButtons(comp);
		
		updateButtons();
		
		return comp;
	}

	/**
	 * Create button area & buttons
	 * 
	 * @param comp
	 */
	private void createButtons(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.verticalAlignment = GridData.BEGINNING;
		comp.setLayout(layout);
		comp.setLayoutData(gd);
		
		fUp = new Button(comp, SWT.PUSH);
		fUp.setFont(parent.getFont());
		fUp.setText("Move Up");
		setButtonLayoutData(fUp);
		fUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleUpPressed();
			}
		});
		
		fDown = new Button(comp, SWT.PUSH);
		fDown.setFont(parent.getFont());
		fDown.setText("Move Down");
		setButtonLayoutData(fDown);
		fDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDownPressed();
			}
		});
	}

	private void handleDownPressed() {
		List targets = getOrderedSelection();
		if (targets.isEmpty()) {
			return;
		}
		List list= new ArrayList(Arrays.asList(fTargets));
		int bottom = list.size() - 1;
		int index = 0;
		for (int i = targets.size() - 1; i >= 0; i--) {
			Object target = targets.get(i);
			index = list.indexOf(target);
			if (index < bottom) {
				bottom = index + 1;
				Object temp = list.get(bottom);
				list.set(bottom, target);
				list.set(index, temp);
			}
			bottom = index;
		} 
		setEntries(list);
	}

	private void handleUpPressed() {
		List targets = getOrderedSelection();
		if (targets.isEmpty()) {
			return;
		}
		int top = 0;
		int index = 0;
		List list= new ArrayList(Arrays.asList(fTargets));
		Iterator entries = targets.iterator();
		while (entries.hasNext()) {
			Object target = entries.next();
			index = list.indexOf(target);
			if (index > top) {
				top = index - 1;
				Object temp = list.get(top);
				list.set(top, target);
				list.set(index, temp);
			}
			top = index;
		} 
		setEntries(list);
	}
	
	/**
	 * Updates the entries to the entries in the given list
	 */
	private void setEntries(List<String> list) {
		fTargets= list.toArray(new String[list.size()]);
		fViewer.setInput(fTargets);
		// update all selection listeners
		fViewer.setSelection(fViewer.getSelection());
	}
	
	/**
	 * Returns the selected items in the list, in the order they are
	 * displayed (not in the order they were selected).
	 * 
	 * @return targets for an action
	 */
	private List getOrderedSelection() {
		List targets = new ArrayList();
		List selection = ((IStructuredSelection)fViewer.getSelection()).toList();
		Object[] entries = fTargets;
		for (int i = 0; i < entries.length; i++) {
			Object target = entries[i];
			if (selection.contains(target)) {
				targets.add(target);
			}
		}
		return targets;		
	}

	/**
	 * Creates a list viewer for the targets
	 * 
	 * @param comp
	 */
	private void createTargetList(Composite comp) {
		fViewer = new TableViewer(comp, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		fViewer.setLabelProvider(new ToStringLabelProvider());
	
		fViewer.setContentProvider(new ArrayContentProvider());
		fViewer.setInput(fTargets);
		fViewer.addSelectionChangedListener(this);
		Table table = fViewer.getTable();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 250;		
		table.setLayoutData(gd);
		table.setFont(comp.getFont());
	}
	
	/**
	 * Returns the ordered targets
	 */
	public String[] getTargets() {
		return fTargets;
	}
	
	/**
	 * Update button enablement
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		updateButtons();
	}
	
	private void updateButtons() {
		int[] selections = fViewer.getTable().getSelectionIndices();
		int last = fTargets.length - 1;
		boolean up = true && selections.length > 0;
		boolean down = true && selections.length > 0;
		for (int i = 0; i < selections.length; i++) {
			if (selections[i] == 0) {
				up = false;
			}
			if (selections[i] == last) {
				down = false;
			}
		}
		fUp.setEnabled(up);
		fDown.setEnabled(down);		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.TARGET_ORDER_DIALOG);
	}
}
