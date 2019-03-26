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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * A combination of 
 *   - an 'enable' checkbox 
 *   - a text input to list a set of tasks
 * to run before/after importing projects.
 * 
 * @author Kris De Volder
 */
public class TaskRunLine {
	
	public interface Listener {
		public void fire();
	}

	private Button enableCheckbox;
	private Text tasksText;
	private Listener listener = null;
	
//	private boolean userChangedEnablement;
	
	public TaskRunLine(String label, String toolTip, GradleImportWizardPageOne page, Group optionsGroup) {
		Composite line = new Composite(optionsGroup, SWT.NONE);
		GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		line.setLayout(layout);
		grabHor.applyTo(line);
		
		enableCheckbox = new Button(line, SWT.CHECK);
		enableCheckbox.setText(label);
		enableCheckbox.setToolTipText(toolTip);
//		enableCheckbox.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				userChangedEnablement = true;
//			}
//		});
		
		enableCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireListener();
			}
		});
		
		tasksText = new Text(line, SWT.SINGLE|SWT.BORDER);
		tasksText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				fireListener();
			}
		});
		grabHor.applyTo(tasksText);
		tasksText.setToolTipText("Enter task names separated by spaces");
	}
	
	private void fireListener() {
		if (listener!=null) {
			listener.fire();
		}
	}
	
	public boolean isEnabled() {
		return enableCheckbox.getSelection();
	}
	
//	/**
//	 * This sets the default enablement state of the checkbox. The default setting
//	 * will only have an effect if state of the checkbox was not otherwise
//	 * set by the user, or by calling the method setEnabled 
//	 */
//	public void setDefaultEnablement(boolean enabled) {
//		if (!userChangedEnablement) {
//			
//		}
//	}

/// Commented out for now, shouldn't be used (maybe useful again later though).
//	public void setEnabled(boolean enabled) {
//		enableCheckbox.setSelection(enabled);
//		userChangedEnablement = true;
//	}
	
	public void setTasks(String[] tasks) {
		StringBuilder text = new StringBuilder();
		boolean first = true;
		for (String task : tasks) {
			if (!first) {
				text.append(" ");
			}
			text.append(task.trim());
			first = false;
		}
		tasksText.setText(text.toString());
	}
	
	public String[] getTasks() {
		return tasksText.getText().split(" +");
	}

	public void setEnabled(boolean enabled) {
		enableCheckbox.setSelection(enabled);	
	}
	
	public void addListener(Listener l) {
		Assert.isLegal(this.listener==null);
		this.listener = l;
	}
	
}
