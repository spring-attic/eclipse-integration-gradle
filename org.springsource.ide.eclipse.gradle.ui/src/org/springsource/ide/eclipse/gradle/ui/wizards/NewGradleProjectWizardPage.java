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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.springsource.ide.eclipse.gradle.core.wizards.NewGradleProjectOperation;


/**
 * @author Kris De Volder
 */
public class NewGradleProjectWizardPage extends WizardPageWithSections {
	
	//TODO: Create an image for new project wizard
	private static final ImageDescriptor WIZBAN_IMAGE = ImageDescriptor.createFromURL(
			GradleImportWizardPageOne.class.getClassLoader().getResource("icons/gradle-import-wizban.png"));
	
	public final NewGradleProjectOperation operation;
	
	public NewGradleProjectWizardPage(NewGradleProjectOperation operation) {
		super("newGradleProjectWizardPage1", "New Gradle Project", WIZBAN_IMAGE);
		this.operation = operation;
	}

	@Override
	protected List<WizardPageSection> createSections() {
		List<WizardPageSection> sections = new ArrayList<WizardPageSection>();
		sections.add(new NewProjectNameSection(this));
		sections.add(new ProjectLocationSection(this));
		sections.add(new SampleProjectSection(this));
		return sections;
	}

}
