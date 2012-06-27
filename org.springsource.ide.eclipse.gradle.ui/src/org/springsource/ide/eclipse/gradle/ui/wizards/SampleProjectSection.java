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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProjectRegistry;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.core.validators.Validator;


/**
 * UI page section that allows user to choose a sample project. The list of
 * possible choices is obtained from the {@link SampleProjectRegistry}.
 * 
 * @author Kris De Volder
 */
public class SampleProjectSection extends WizardPageSection {

	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	
	private SampleProjectRegistry samples = SampleProjectRegistry.getInstance();
	private Combo sampleProjectField;
	private Validator validator;
	private LiveExpression<SampleProject> sampleProjectExp = new LiveExpression<SampleProject>(null) {
		@Override
		protected SampleProject compute() {
			if (sampleProjectField!=null) {
				String sampleProjectName = sampleProjectField.getText();
				return samples.get(sampleProjectName);
			}
			return null;
		}
	};

	public SampleProjectSection(NewGradleProjectWizardPage owner) {
		super(owner);
		owner.operation.setSampleProjectField(sampleProjectExp);
		this.validator = owner.operation.getSampleProjectValidator();
	}

	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	@Override
	public void createContents(Composite page) {
        Composite group = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label projectLabel = new Label(group, SWT.NONE);
        projectLabel.setText("Sample project:");

        sampleProjectField = new Combo(group, SWT.DROP_DOWN|SWT.READ_ONLY);
        sampleProjectField.setItems(getSampleProjectNames());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        sampleProjectField.setLayoutData(data);
        sampleProjectField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				sampleProjectExp.refresh();
			}
		});
        sampleProjectExp.refresh();
	}

	private String[] getSampleProjectNames() {
		List<SampleProject> sampleProjects = samples.getAll();
		String[] names = new String[sampleProjects.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = sampleProjects.get(i).getName();
		}
		return names;
	}

}
