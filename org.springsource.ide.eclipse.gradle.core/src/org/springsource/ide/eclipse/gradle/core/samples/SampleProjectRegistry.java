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
package org.springsource.ide.eclipse.gradle.core.samples;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager;


/**
 * Instance of this class is responsible for knowing all the sample projects that can be used by 
 * the New Gradle Project Wizard. 
 * <p>
 * For now the registry is not extensible. The samples are more or less hard-code in here. In the future
 * we might create an extension point to register the samples from external plugins.
 * 
 * @author Kris De Volder
 */
public class SampleProjectRegistry {
	
	private static final FilenameFilter IGNORED_NAMES = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return !name.startsWith(".");
		}
	};

	private static SampleProjectRegistry instance = null;
	public static synchronized SampleProjectRegistry getInstance() {
		if (instance==null) {
			instance = new SampleProjectRegistry();
		}
		return instance;
	}
	
	/**
	 * This is a singleton, call getInstance method to get the instance.
	 */
	private SampleProjectRegistry() {
		File stateLocation = GradleCore.getInstance().getStateLocation().toFile();
		String userHome = System.getProperty("user.home");
		if (userHome!=null) {
			File userHomeFile = new File(userHome);
			File downloadLocation = new File(userHomeFile, ".sts/gradle/download-cache");
			if (downloadLocation.exists() || downloadLocation.mkdirs()) {
				downloadManager = new DownloadManager(new File(stateLocation, "samples").toString());
				return;
			}
		}
		//Just in case the above fails we can try to place the download cache in the workspace state location
		//The disadvantage of this is that each workspace will re-download the same files again.
		downloadManager = new DownloadManager(new File(stateLocation, "samples").toString());
	}
	
	private DownloadManager downloadManager;
	
	private URI distribution;
	{
		try {
			distribution = new URI("http://services.gradle.org/distributions/gradle-2.1-all.zip");
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

	private ArrayList<SampleProject> samples = null; //Lazy initialised.
	
	public synchronized List<SampleProject> getAll() {
		if (samples==null) {
			samples = new ArrayList<SampleProject>();
			registerDistributionSamples();
//The sample project below doesn't represent a layout that works well in the STS Eclipse environment							
//					new GradleDistributionSample(downloadManager, distribution, 
//							"Java Multiproject", "samples/java/multiproject")
// The sample project below doesn't work properly. See http://issues.gradle.org/browse/GRADLE-2251
//					new GradleDistributionSample(downloadManager, distribution, 
//							"Base- and Sub-project", "samples/java/base")
			registerLocalSamples();
		}
		return samples;
	}

	private void registerLocalSamples() {
		File localSamplesDir = getLocalSamplesDir();
		File[] projects = localSamplesDir.listFiles(IGNORED_NAMES);
		for (File project : projects) {
			register(new LocalSample(project.getName(), project));
		}
	}

	private File getLocalSamplesDir() {
		Bundle bundle = Platform.getBundle(GradleCore.PLUGIN_ID);
		try {
			File bundleFile = FileLocator.getBundleFile(bundle);
			if (bundleFile != null && bundleFile.exists() && bundleFile.isDirectory()) {
				File samplesDir = new File(bundleFile, "samples");
				if (samplesDir.isDirectory()) {
					return samplesDir;
				} else {
					GradleCore.log("Directory 'samples' not found in plugin "+GradleCore.PLUGIN_ID);
				}
			} else {
				GradleCore.log("Couldn't access the plugin "+GradleCore.PLUGIN_ID+" as a directory. Maybe it is not installed as an 'exploded' bundle?");
			}
		} catch (IOException e) {
			GradleCore.log(e);
		}
		return null;
	}

	private void registerDistributionSamples() {
		register(
				new GradleDistributionSample(downloadManager, distribution, 
						"Java Quickstart", "samples/java/quickstart"),
				new GradleDistributionSample(downloadManager, distribution, 
						"Java API and Implementation", "samples/java/apiAndImpl")
		);
	}

	private void register(SampleProject... toRegister) {
		for (SampleProject s : toRegister) {
			samples.add(s);
		}
	}

	public SampleProject get(String sampleProjectName) {
		for (SampleProject candidate : getAll()) {
			if (candidate.getName().equals(sampleProjectName)) {
				return candidate;
			}
		}
		return null;
	}
	
}
