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
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.io.File;

import org.codehaus.groovy.eclipse.dsl.tests.InferencerWorkload;

/**
 * @author Kris De Volder
 */
public class GradleInferencerWorkload extends InferencerWorkload {

	private static final String extraAliases = "";

	public GradleInferencerWorkload(String workloadDefinition) throws Exception {
		super(workloadDefinition, 
				"P",						"org.gradle.api.Project",
				"MAR", 						"org.gradle.api.artifacts.repositories.MavenArtifactRepository",
				"RH",						"org.gradle.api.artifacts.dsl.RepositoryHandler");
	}

}
