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
package org.springsource.ide.eclipse.gradle.core.classpathcontainer;

import io.pivotal.tooling.model.eclipse.StsEclipseProject;
import io.pivotal.tooling.model.eclipse.StsEclipseProjectDependency;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * This class is an Adapter that lets us use either the old {@link EclipseProject}
 * model or the new {@link StsEclipseProject} as information source for computing 
 * a Gradle project's classpath.
 * 
 * @author Kris De Volder
 */
public abstract class ClassPathModel {
	
	protected final Object model;

	public ClassPathModel(Object model) {
		Assert.isNotNull(model);
		this.model = model;
	}

	//Design note:
	// This class should be the only one that has a direct reference to 
	// StsEclipseProject.
	
	public abstract DomainObjectSet<? extends ExternalDependency> getClasspath();
	
	/**
	 * If you are using the newer 'CustomToolingModel' then this method will actually
	 * return instances of {@link StsEclipseProjectDependency} instead of
	 * plain {@link EclipseProjectDependency}.
	 */
	public abstract DomainObjectSet<? extends EclipseProjectDependency> getProjectDependencies();	
	
	public static ClassPathModel getClassPathModel(GradleProject project) throws CoreException, FastOperationFailedException {
		if (project.useCustomToolingModel()) {
			return from(project.getModel(StsEclipseProject.class));
		} else {
			return from(project.getModel(EclipseProject.class));
		}
	}
	
	public static ClassPathModel getClassPathModel(GradleProject project, IProgressMonitor mon) throws CoreException {
		if (project.useCustomToolingModel()) {
			return from(project.getModel(StsEclipseProject.class, mon));
		} else {
			return from(project.getModel(EclipseProject.class, mon));
		}
	}
	

	
	////////////////// implementation cruft below ////////////////////////////////
	
	private static ClassPathModel from(StsEclipseProject model) {
		return new CustomClassPathModel(model);
	}

	private static ClassPathModel from(EclipseProject model) {
		return new LegacyClassPathModel(model);
	}

	private static class LegacyClassPathModel extends ClassPathModel {

		public LegacyClassPathModel(EclipseProject model) {
			super(model);
		}
		
		@Override
		public DomainObjectSet<? extends ExternalDependency> getClasspath() {
			return ((EclipseProject)model).getClasspath();
		}

		@Override
		public DomainObjectSet<? extends EclipseProjectDependency> getProjectDependencies() {
			return ((EclipseProject)model).getProjectDependencies();
		}

	}

	private static class CustomClassPathModel extends ClassPathModel {

		public CustomClassPathModel(StsEclipseProject model) {
			super(model);
		}

		@Override
		public DomainObjectSet<? extends ExternalDependency> getClasspath() {
			return ((StsEclipseProject)model).getClasspath();
		}

		@Override
		public DomainObjectSet<? extends EclipseProjectDependency> getProjectDependencies() {
			return ((StsEclipseProject)model).getProjectDependencies();
		}
		
	}
	
	@Override
	public int hashCode() {
		return model.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassPathModel) {
			return model.equals(((ClassPathModel) obj).model);
		}
		return false;
	}

}
