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
package org.springsource.ide.eclipse.gradle.ui;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.DistributionValidator;
import org.springsource.ide.eclipse.gradle.core.validators.DistributionValidatorContext;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * Section on a preferences page that allows user to pick a specific Gradle distribution.
 * 
 * @author Kris De Volder
 */
public class DistributionSection extends PrefsPageSection implements DistributionValidatorContext {
	
	//////// alternative 1: Use wrapper default ///////////////////////
	private Button defaultButton;

	//////// alternative 2: Binary Zip URI ////////////////////////////
	private Button customURIButton;
	private Text customURIText;
	private Button browseURIButton;

	//////// alternative 3: Local install folder ///////////////////////
	private Button customHomeButton;
	private Text customHomeText;
	private Button browseHomeButton;

	private DistributionValidator validator;
	
	public DistributionSection(PreferencePageWithSections owner) {
		super(owner);
	}
	
	/**
	 * @param page
	 */
	public void createContents(Composite page) {
        validator = new DistributionValidator(this); //Add this one as the very first one. The validator is supposed to cope with nulls and such.
        
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);
		Label label = new Label(page, SWT.NONE);
		label.setText("Gradle Distribution");

		//Alternative 1
		
        Composite composite = new Composite(page, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);
        
		defaultButton = new Button(composite, SWT.RADIO); 
		defaultButton.setText("Use Gradle wrapper's default");

		//Alternative 2
		
		customURIButton = new Button(composite, SWT.RADIO);
		customURIButton.setText("URI: ");
		customURIButton.setToolTipText("Paste in the URL/URI of a specific Gradle binary distribution zip file");
		
		GridDataFactory span = GridDataFactory.fillDefaults().span(3, 1);
		span.applyTo(defaultButton);
				
        customURIText = new Text(composite, SWT.BORDER);
		browseURIButton = new Button(composite, SWT.PUSH);
		browseURIButton.setText("Browse");
		
        customURIButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
        browseURIButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				//Button clicked
				URI file = openFileDialog();
				if (file!=null) {
					customURIText.setText(file.toString());
				}
				
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
		customURIText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
        
        grabHorizontal.applyTo(composite);
        grabHorizontal.applyTo(customURIText);
        
        
        //Alternative 3: local Gradle 'home'
        
		customHomeButton = new Button(composite, SWT.RADIO);
		customHomeButton.setText("Folder: ");
		customHomeButton.setToolTipText("Enter an absolute path pointing to a local Gradle installation.");
        
        customHomeText = new Text(composite, SWT.BORDER);
		browseHomeButton = new Button(composite, SWT.PUSH);
		browseHomeButton.setText("Browse");

		customHomeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validator.refresh();
			}
		});
		
        customHomeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableDisableWidgets();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        
        browseHomeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				//Button clicked
				File file = openFolderDialog();
				if (file!=null) {
					customHomeText.setText(file.toString());
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        grabHorizontal.applyTo(customHomeText);

        setDistroInPage(getDistro());
        enableDisableWidgets();
	}
	
	private URI openFileDialog() {
		FileDialog fileDialog = new FileDialog(owner.getShell(),  SWT.OPEN);
		String file = fileDialog.open();
		if (file!=null) {
			return new File(file).toURI();
		}
		return null;
	}
	
	private void setDistro(URI distro) {
		GradleCore.getInstance().getPreferences().setDistribution(distro);
	}

	public URI getDistroInPage() throws URISyntaxException {
		if (customURIButton.getSelection()) {
			String distroString = customURIText.getText().trim();
			if (!"".equals(distroString)) {
				return new URI(distroString);
			}
		} else if (customHomeButton.getSelection()) {
			String homeString = customHomeText.getText().trim();
			if (!"".equals(homeString)) {
				return new File(homeString).toURI();
			}
		}
		return null;
	}
	
	private File openFolderDialog() {
		DirectoryDialog fileDialog = new DirectoryDialog(owner.getShell(),  SWT.OPEN);
		String file = fileDialog.open();
		if (file!=null) {
			return new File(file);
		}
		return null;
	}
	
	private void enableDisableWidgets() {
		enableDisableWidgets(customURIButton, customURIText, browseURIButton);
		enableDisableWidgets(customHomeButton, customHomeText, browseHomeButton);
		validator.refresh();
	}

	public void enableDisableWidgets(Button radio, Text text, Button browse) {
		boolean enable = radio.getSelection();
		text.setEnabled(enable);
		browse.setEnabled(enable);
	}
	
	private void setDistroInPage(URI distro) {
		if (distro==null) {
			defaultButton.setSelection(true);
			customHomeButton.setSelection(false);
			customURIButton.setSelection(false);
		} else {
			if ("file".equals(distro.getScheme())) {
				File file = new File(distro);
				if (file.isDirectory()) {
					//Should be local Gradle distribution home:
					customHomeButton.setSelection(true);
					customHomeText.setText(file.toString());
					return;
				} else if (!file.exists()) {
					//The file doesn't exist, revert to default setting
					defaultButton.setSelection(true);
					return;
				} else {
					//Assume its a zip file distribution
					customURIButton.setSelection(true);
					customURIText.setText(distro.toString());
					return;
				}
			} else {
				//Assume its the URI of a zip somewhere on the web
				customURIButton.setSelection(true);
				customURIText.setText(distro.toString());
			}
		}
	}

	private URI getDistro() {
		return GradleCore.getInstance().getPreferences().getDistribution();
	}

	public boolean performOK() {
		try {
			setDistro(getDistroInPage());
			return true;
		} catch (URISyntaxException e) {
			//owner.setErrorMessage(e.getMessage());
			GradleCore.log(e);
			return false;
		}
	}

	public void performDefaults() {
		setDistroInPage(null);
	}

	/* (non-Javadoc)
	 * @see org.springsource.ide.eclipse.gradle.ui.PageSection#getValidator()
	 */
	@Override
	public LiveExpression<ValidationResult> getValidator() {
		return validator;
	}
	
}