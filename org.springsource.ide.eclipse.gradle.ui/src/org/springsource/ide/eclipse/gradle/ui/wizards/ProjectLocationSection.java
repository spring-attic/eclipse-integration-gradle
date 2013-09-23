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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.core.validators.Validator;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;



/**
 * @author Kris De Volder
 */
public class ProjectLocationSection extends WizardPageSection {
	
	//Much of the code in this class was copied from Eclipse:
	
	//See org.eclipse.ui.internal.ide.dialogs.ProjectContentsLocationArea;
	
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	private static final String SAVED_LOCATION_ATTR = "OUTSIDE_LOCATION"; //$NON-NLS-1$

	private Button useDefaultsButton;
	private Label locationLabel;
	private Text locationPathField;
	private Button browseButton;
	
//	private String userPath = "";
//	private IProject existingProject;

	private LiveExpression<String> locationExp = new LiveExpression<String>("") {
		@Override
		protected String compute() {
			if (isDefault()) {
				return getDefaultPathDisplayString();
			}
			return locationPathField.getText();
		}
	};
	private final Validator validator;

	public ProjectLocationSection(NewGradleProjectWizardPage owner) {
		super(owner);
		owner.operation.setLocationField(locationExp);
		validator = owner.operation.getLocationValidator();
	}
	
	/**
	 * Return whether or not we are currently showing the default location for
	 * the project.
	 * 
	 * @return boolean
	 */
	private boolean isDefault() {
		if (useDefaultsButton!=null) {
			return useDefaultsButton.getSelection();
		}
		return true;
	}

	public LiveExpression<String> getLocation() {
		return locationExp;
	}
	
	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.util.PageSection#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createContents(Composite page) {
		boolean defaultEnabled = true;
		int columns = 4;

		// project specification group
		Composite projectGroup = new Composite(page, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = columns;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		useDefaultsButton = new Button(projectGroup, SWT.CHECK | SWT.RIGHT);
		useDefaultsButton.setText("Use default location");
		useDefaultsButton.setSelection(defaultEnabled);
		GridData buttonData = new GridData();
		buttonData.horizontalSpan = columns;
		useDefaultsButton.setLayoutData(buttonData);

		createUserEntryArea(projectGroup, defaultEnabled);

		useDefaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean useDefaults = useDefaultsButton.getSelection();
				if (useDefaults) {
					locationPathField.setText(TextProcessor
							.process(getDefaultPathDisplayString()));
				}
//				String error = checkValidLocation();
//				errorReporter.reportError(error,
//						error != null && error.equals(IDEWorkbenchMessages.WizardNewProjectCreationPage_projectLocationEmpty));
				setUserAreaEnabled(!useDefaults);
				locationExp.refresh();
			}
		});
		locationPathField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				locationExp.refresh();
			}
		});
		setUserAreaEnabled(!defaultEnabled);
		locationExp.refresh();
	}

	/**
	 * Return the path we are going to display.
	 * 
	 * @return String
	 */
	private String getDefaultPathDisplayString() {
		String projectName = getProjectName();
		return SampleProject.getDefaultProjectLocation(projectName);
	}

	private String getProjectName() {
		return getProjectNameExp().getValue();
	}

	private LiveExpression<String> getProjectNameExp() {
		return ((NewGradleProjectWizardPage)owner).operation.getProjectNameField();
	}

	/**
	 * Set the enablement state of the receiver.
	 * 
	 * @param enabled
	 */
	private void setUserAreaEnabled(boolean enabled) {
		locationLabel.setEnabled(enabled);
		locationPathField.setEnabled(enabled);
		browseButton.setEnabled(enabled);
	}
	
	/**
	 * Create the area for user entry.
	 * 
	 * @param composite
	 * @param defaultEnabled
	 */
	private void createUserEntryArea(Composite composite, boolean defaultEnabled) {
		// location label
		locationLabel = new Label(composite, SWT.NONE);
		locationLabel.setText("Location:");

		// project location entry field
		locationPathField = new Text(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		data.horizontalSpan = 2;
		locationPathField.setLayoutData(data);

		// browse button
		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleLocationBrowseButtonPressed();
			}
		});

		if (defaultEnabled) {
			locationPathField.setText(TextProcessor
					.process(getDefaultPathDisplayString()));
		} else {
			locationPathField.setText("");
		}

		getProjectNameExp().addListener(new ValueListener<String>() {
			public void gotValue(LiveExpression<String> exp, String newProjectName) {
				if (isDefault()) {
					locationPathField.setText(TextProcessor.process(getDefaultPathDisplayString()));
				}
			}
		});
	}
	
	/**
	 * Open an appropriate directory browser
	 */
	private void handleLocationBrowseButtonPressed() {
		String selectedDirectory = null;
		String dirName = getPathFromLocationField();

		if (dirName!=null && !dirName.equals("")) {
			File dir = new File(dirName);
			if (!dir.exists()) {
				dirName = "";
			}
		}
		if (dirName==null || dirName.equals("")) {
			String value = getDialogSettings().get(SAVED_LOCATION_ATTR);
			if (value != null) {
				dirName = value;
			}
		}

		DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell(), SWT.SHEET);
		dialog.setMessage("Select the location directory");
		dialog.setFilterPath(dirName);
		selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			updateLocationField(selectedDirectory);
			getDialogSettings().put(SAVED_LOCATION_ATTR, selectedDirectory);
		}
	}

	/**
	 * Update the location field based on the selected path.
	 * 
	 * @param selectedPath
	 */
	private void updateLocationField(String selectedPath) {
		locationPathField.setText(TextProcessor.process(selectedPath));
	}
	
	/**
	 * Return the path on the location field.
	 * 
	 * @return the path or the field's text if the path is invalid
	 */
	private String getPathFromLocationField() {
		URI fieldURI;
		try {
			fieldURI = new URI(locationPathField.getText());
		} catch (URISyntaxException e) {
			return locationPathField.getText();
		}
		String path= fieldURI.getPath();
		return path != null ? path : locationPathField.getText();
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings ideDialogSettings = GradleUI.getDefault().getDialogSettings();
		IDialogSettings result = ideDialogSettings.getSection(getClass().getName());
		if (result == null) {
			result = ideDialogSettings.addNewSection(getClass().getName());
		}
		return result;
	}
	
}
