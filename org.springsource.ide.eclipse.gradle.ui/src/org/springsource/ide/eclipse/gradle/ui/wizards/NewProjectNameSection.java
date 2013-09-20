/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * Wizard page section that allows the user to enter the name of a new project. 
 * 
 * @author Kris De Volder
 */
public class NewProjectNameSection extends WizardPageSection {
	
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;

	private Text projectNameField;

	private final LiveExpression<String> projectNameExp = new LiveExpression<String>(null) {
		protected String compute() {
			if (projectNameField!=null) {
				return projectNameField.getText();
			}
			return null;
		}
	};
	
	private final LiveExpression<ValidationResult> validator;

	public NewProjectNameSection(NewGradleProjectWizardPage owner) {
		super(owner);
		owner.operation.setProjectNameField(projectNameExp);
		this.validator = owner.operation.getProjectNameValidator();
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	@Override
	public void createContents(Composite page) {
        // project specification group
        Composite projectGroup = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label projectLabel = new Label(projectGroup, SWT.NONE);
        projectLabel.setText("Project name:");

        projectNameField = new Text(projectGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        projectNameField.setLayoutData(data);
        projectNameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				projectNameExp.refresh();
			}
		});
        projectNameExp.refresh();
	}

	public LiveExpression<String> getProjectName() {
		return projectNameExp;
	}

}
