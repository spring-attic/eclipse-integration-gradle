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
package org.springsource.ide.eclipse.gradle.core;

import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

/**
 * A listener that can be added to a project to get notified whenever the Gradle project's
 * model is set.
 * 
 * @author Kris De Volder
 */
public interface IGradleModelListener {
	
	/**
	 * Gets called when the model for a given project is changed. Note that the oldModel and new model could be instances
	 * of EclipseProject which is a subtype of HierarchicalEclipseProject.
	 */
	public void modelChanged(GradleProject project);
}
