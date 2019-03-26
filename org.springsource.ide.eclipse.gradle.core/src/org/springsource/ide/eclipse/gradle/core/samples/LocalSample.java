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
package org.springsource.ide.eclipse.gradle.core.samples;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;


/**
 * A sample project the contents of which is copied from a location in the local file
 * system.
 * 
 * @author Kris De Volder
 */
public class LocalSample extends SampleProject {

	private String name;
	private File copyFrom;

	/**
	 * @param distribution The URI of an official Gradle distribution.
	 * @param location The location of the sample inside the distribution zip.
	 */
	public LocalSample(String name, File copyFrom) {
		Assert.isNotNull(name);
		Assert.isNotNull(copyFrom);
		this.name = name;
		this.copyFrom = copyFrom;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void createAt(final File location) throws CoreException {
		try {
			FileUtils.copyDirectory(copyFrom, location);
		} catch (Exception e) {
			throw ExceptionUtil.coreException(e);
		}
	}

}
