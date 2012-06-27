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
package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.preferences.GlobalSettings;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.util.SelectionUtils;


/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class GradleLaunchShortcut implements ILaunchShortcut2, IExecutableExtension {

	public GradleLaunchShortcut() {
		debug("Instantiating "+this.getClass().getName());
	}
	
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		showDialog = "showDialog".equals(data);
	}

	private void debug(String string) {
		if (GlobalSettings.DEBUG) {
			System.out.println(string);
		}
	}

	private static final ILaunchConfiguration[] NO_LAUNCH_CONFIGURATIONS = new ILaunchConfiguration[0];
	private boolean showDialog = true;

	public void launch(ISelection selection, String mode) {
		GradleProject project = SelectionUtils.getGradleProject(selection);
		if (project!=null) {
			ILaunchConfiguration[] confs = getLaunchConfigurations(project);
			ILaunchConfiguration theConf;
			if (confs.length==1) {
				theConf = confs[0];
			} else if (confs.length==0) {
				theConf = createDefaultConfiguration(project);
			} else {
				theConf = chooseConfiguration(project, confs);
			}
			if (theConf!=null) {
				launch(theConf, mode);
			} else {
				//This means the launch was canceled by chooseConfiguration so we don't need to
				//report an error.
			}
		} else {
			MessageDialog.openInformation(getShell(), "No Gradle Project Selected", 
					"A single Gradle project must be selected to Launch a Gradle Build.");
		}
	}

	/**
	 * When asked to launch Gradle build on a project that doesn't yet have an associated launch configuration, 
	 * this method is called upon to produce a default configuration.
	 */
	protected ILaunchConfiguration createDefaultConfiguration(GradleProject project) {
		return GradleLaunchConfigurationDelegate.createDefault(project, true);
	}

	@SuppressWarnings("deprecation")
	private void launch(ILaunchConfiguration theConf, String mode) {
        if (showDialog || GradleLaunchConfigurationDelegate.getTasks(theConf).isEmpty()) {
			// Offer to save dirty editors before opening the dialog as the contents
			// of an Ant editor often affect the contents of the dialog.
			if (!DebugUITools.saveBeforeLaunch()) {
				return;
			}
			String groupId;
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			    groupId= IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP;
			} else {
			    groupId= org.eclipse.ui.externaltools.internal.model.IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP;
			}
			DebugUITools.openLaunchConfigurationDialog(AntUIPlugin.getActiveWorkbenchWindow().getShell(), theConf, groupId, null);
		} else {
			DebugUITools.launch(theConf, mode);
		}
	}

	/**
	 * If more than one configuration is found for a given "launch" request
	 */
	protected ILaunchConfiguration chooseConfiguration(GradleProject project, ILaunchConfiguration[] confs) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(confs);
		dialog.setTitle("Choose a Gradle Launch Configuration");  
		dialog.setMessage("Multiple configurations were found for project "+project.getName());
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;		
	}

	protected Shell getShell() {
		return GradleUI.getActiveWorkbenchShell();
	}

	public void launch(IEditorPart editor, String mode) {
		//TODO: implement launch from editor
		throw new Error("Support for launching Gradle from editor not yet implemened");
	}

	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		//Let framework handle based on resource mapping
		return null;
	}

	/**
	 * Gets existing Gradle launch configurations associated with a given project
	 */
	private ILaunchConfiguration[] getLaunchConfigurations(GradleProject project) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		try {
			ILaunchConfiguration[] candidates = launchManager.getLaunchConfigurations(
					launchManager.getLaunchConfigurationType(GradleLaunchConfigurationDelegate.ID));
			List<ILaunchConfiguration> result = new ArrayList<ILaunchConfiguration>();
			for (ILaunchConfiguration conf : candidates) {
				GradleProject confProject = GradleLaunchConfigurationDelegate.getProject(conf);
				if (confProject==project) {
					result.add(conf);
				}
			}
			return result.toArray(new ILaunchConfiguration[result.size()]);
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return NO_LAUNCH_CONFIGURATIONS;
	}

	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		//Let framework handle based on resource mapping
		return null;
	}

	public IResource getLaunchableResource(ISelection selection) {
		return SelectionUtils.getProject(selection);
	}

	public IResource getLaunchableResource(IEditorPart editorpart) {
		//Let framework handle based on resource mapping
		return null;
	}

}
