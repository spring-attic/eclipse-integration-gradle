/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.wizards;

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.samples.SampleProject;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.validators.NewProjectLocationValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ProjectNameValidator;
import org.springsource.ide.eclipse.gradle.core.validators.SampleProjectValidator;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;
import org.springsource.ide.eclipse.gradle.core.validators.Validator;
import org.springsource.ide.eclipse.gradle.core.wizards.PrecomputedProjectMapper.NameClashException;


/**
 * A class that contains all the relevant information for a 'NewGradleProject' operation
 * as carried out by the NewGradleProjectWizard. The information isn't actually stored
 * in the class, it may be stored anywhere. The class merely provides the necessary
 * wiring so that the info can be accessed easily, as well as 'listened' to.
 * <p>
 * This class also contains all the validation logic in a form that makes it easy to obtain
 * specific validators for portions of the data. This needed to make it easy to restructure UI, 
 * moving data between pages. It also makes it possible to create unit tests
 * for the validation logic.
 * 
 * @author Kris De Volder
 */
public class NewGradleProjectOperation {

	private LiveExpression<String> projectName = null;
	private ProjectNameValidator projectNameValidator = null;
	
	private LiveExpression<String> location = null;
	private NewProjectLocationValidator locationValidator = null;
	
	private LiveExpression<SampleProject> sampleProject = null;
	private Validator sampleProjectValidator = null;
	
	public void setProjectNameField(LiveExpression<String> name) {
		Assert.isLegal(projectName==null && name!=null);
		this.projectName = name;
		this.projectNameValidator = new ProjectNameValidator(projectName);
	}
	
	public void setLocationField(LiveExpression<String> location) {
		Assert.isLegal(this.location==null && location!=null);
		this.location = location;
		this.locationValidator = new NewProjectLocationValidator("Location", location, projectName);
	}
	
	public LiveExpression<String> getProjectNameField() {
		return projectName;
	}
	
	/**
	 * Verifies that all fields are wired up to some UI element (or another data source, e.g. mock data for unit testing).
	 */
	public void assertComplete() {
		Assert.isLegal(projectName!=null, "Forgot to wire up 'projectName'?");
		Assert.isLegal(location!=null, "Forgot to wire up 'location'?");
		Assert.isLegal(sampleProject!=null, "Forgot to wire up 'sampleProject'?");
	}

	public boolean perform(IProgressMonitor mon) throws CoreException {
		assertComplete();
		createProjectContents(mon);
		return true;
	}

	private void createProjectContents(IProgressMonitor mon) throws CoreException {
		mon.beginTask("Create project contents", 1);
		try {
			//Setup the directory where the project will be created.
			File location = new File(getLocation());
			if (!location.exists()) {
				if (!location.mkdirs()) {
					throw ExceptionUtil.coreException("Couldn't create directory: "+location);
				}
			}
			//Copy sample code into the location
			SampleProject sampleCode = getSampleProject();
			if (sampleCode!=null) {
				sampleCode.createAt(location);
			}
			
			//Import the sample code as a project (possibly may import subprojects as well).
			try {
				GradleImportOperation importOp = GradleImportOperation.importAll(location);
				ErrorHandler eh = ErrorHandler.forImportWizard();
				importOp.perform(eh, new SubProgressMonitor(mon, 1));
				eh.rethrowAsCore();
			} catch (NameClashException e) {
				throw ExceptionUtil.coreException(e);
			} catch (CoreException e) {
				throw ExceptionUtil.coreException(e);
			}
		} finally {
			mon.done();
		}
	}

	private SampleProject getSampleProject() {
		return sampleProject.getValue();
	}

	private String getLocation() {
		return location.getValue();
	}

	public LiveExpression<ValidationResult> getProjectNameValidator() {
		return projectNameValidator;
	}

	public Validator getLocationValidator() {
		return locationValidator;
	}

	public Validator getSampleProjectValidator() {
		return sampleProjectValidator;
	}

	public void setSampleProjectField(LiveExpression<SampleProject> sampleProject) {
		Assert.isLegal(this.sampleProject==null && sampleProject!=null);
		this.sampleProject = sampleProject;
		this.sampleProjectValidator = new SampleProjectValidator("Sample project", sampleProject);
	}

}
