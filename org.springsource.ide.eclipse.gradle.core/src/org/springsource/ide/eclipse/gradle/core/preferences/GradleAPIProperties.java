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
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * An instance of this class represents the contents of the "gradle.properties" file
 * found in the gradle.core plugin. 
 * 
 * @author Kris De Volder
 */
public class GradleAPIProperties extends Properties {

	private static final long serialVersionUID = 1L;

	/**
	 * @return The value of "gradle.distribution.uri" property as a URI. May return null if the property is not defined or the property file does not exist.
	 */
	public URI getDistribution() {
		String uriString = getProperty("gradle.distribution.uri");
		if (uriString!=null) {
			try {
				return new URI(uriString);
			} catch (URISyntaxException e) {
				GradleCore.log(e);
			}
		}
		return null;
	}

	public String getApiVersion() {
		return getProperty("gradle.api.version");
	}

	public List<File> getGradleAPIJars() {
		ArrayList<File> result = new ArrayList<File>();
		try {
			File bundle = FileLocator.getBundleFile(Platform.getBundle(GradleCore.PLUGIN_ID));
			Assert.isTrue(bundle.exists() && bundle.isDirectory(), "Bundle '"+GradleCore.PLUGIN_ID+" must be exploded");
			File libFolder = new File(bundle, "lib");
			for (File file : libFolder.listFiles()) {
				String name = file.getName();
				if (name.endsWith(".jar") && !name.endsWith("sources.jar")) {
					result.add(file);
				}
			}
		} catch (IOException e) {
			GradleCore.log(e);
		}
		return result;
	}

}
