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
package org.springsource.ide.eclipse.gradle.core.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.toolingapi.GradleToolingApi;

/**
 * An instance of this class represents the contents of the "gradle.properties" file
 * found in the gradle.core plugin. 
 * 
 * @author Kris De Volder
 */
public class GradleAPIProperties {

	/**
	 * ID of the plugin where all the tooling API jar files reside inside a 'lib' folder.
	 */
	private static final String LIB_PLUGIN_ID = GradleToolingApi.PLUGIN_ID;
	
	public List<File> getGradleAPIJars() {
		ArrayList<File> result = new ArrayList<File>();
		try {
			File bundle = FileLocator.getBundleFile(Platform.getBundle(LIB_PLUGIN_ID));
			Assert.isTrue(bundle.exists() && bundle.isDirectory(), "Bundle '"+LIB_PLUGIN_ID+" must be exploded");
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
