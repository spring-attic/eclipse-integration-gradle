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
package org.springsource.ide.eclipse.gradle.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.preferences.GlobalSettings;



/**
 * The activator class controls the plug-in life cycle
 * @author Kris De Volder
 */
public class GradleUI extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.springsource.ide.eclipse.gradle.ui"; //$NON-NLS-1$
	
	public static final String IMAGE_TARGET = "target"; //$NON-NLS-1$
	public static final String IMAGE_PROJECT_FOLDER = "projectFolder"; //$NON-NLS-1$
	public static final String IMAGE_MULTIPROJECT_FOLDER = "multiProjectFolder"; //$NON-NLS-1$
	public static final String IMAGE_MULTIPROJECT_FOLDER_DISABLED = "multiProjectFolderDisabled"; //$NON-NLS-1$
	
	private static final Map<String, String> IMAGE_DESCRIPTOR_MAP = new HashMap<String, String>();
	
	static {
		IMAGE_DESCRIPTOR_MAP.put(IMAGE_TARGET, "icons/target.gif");
		IMAGE_DESCRIPTOR_MAP.put(IMAGE_PROJECT_FOLDER, "icons/gradle-proj-folder.png");
		IMAGE_DESCRIPTOR_MAP.put(IMAGE_MULTIPROJECT_FOLDER, "icons/gradle-multiproj-folder.png");
		IMAGE_DESCRIPTOR_MAP.put(IMAGE_MULTIPROJECT_FOLDER_DISABLED, "icons/gradle-multiproj-folder-disabled.png");		
	}

	// The shared instance
	private static GradleUI plugin;
	
	/**
	 * The constructor
	 */
	public GradleUI() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		if (GlobalSettings.DEBUG) {
			System.out.println("Starting plugin "+PLUGIN_ID);
		}
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static void log(Throwable e) {
		GradleCore.log(e);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static GradleUI getDefault() {
		return plugin;
	}

	/**
	 * Returns the active workbench shell or <code>null</code> if none
	 * 
	 * @return the active workbench shell or <code>null</code> if none
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}	

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);		
		for (Map.Entry<String, String> entry : IMAGE_DESCRIPTOR_MAP.entrySet()) {
	        URL url = FileLocator.find(plugin.getBundle(), new Path(entry.getValue()), null);
	        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
	        reg.put(entry.getKey(), desc);
		}
	}	
	
	
}
