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
package org.springsource.ide.eclipse.gradle.ui.taskview;

import static org.springsource.ide.eclipse.gradle.core.util.JobUtil.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseTask;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.launch.LaunchUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.ui.launch.GradleTaskTreeContentProvider;
import org.springsource.ide.eclipse.gradle.ui.launch.GradleTaskTreeLabelProvider;
import org.springsource.ide.eclipse.gradle.ui.util.DialogSettingsUtil;
import org.springsource.ide.eclipse.gradle.ui.util.SelectionUtils;


import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * @author Kris De Volder
 * @since 2.9
 */
public class GradleTasksView extends ViewPart {

	/**
	 * Listens for any selection in workbench and attempts to convert it into a GradleProject. 
	 * If successful, notifies the task view that a GradleProject selection occurred.
	 * 
	 * @author Kris De Volder
	 */
	public class SelectionListener implements ISelectionListener {
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (isLinkingEnabled()) {
				GradleProject p = SelectionUtils.getGradleProject(selection);
				if (p!=null) {
					GradleTasksView.this.projectSelected(p);
				}
			}
		}

	}

	private static final String IS_LINKING_ENABLED = "isLinkingEnabled";
	private static final boolean DEFAULT_IS_LINKING_ENABLED = false;
	private static final String SELECTED_PROJECT = "selectedProject";

	private TreeViewer viewer;
	
//	private DrillDownAdapter drillDownAdapter;
	private Action linkWithSelectionAction;
	private Action refreshAction;
//
//	private Action action1;
//	private Action action2;
	private Action doubleClickAction;

	private SelectionListener selectionListener;

	private ProjectSelector projectSelector;

	private boolean isLinkingEnabled = false;
	private IDialogSettings dialogSettings;

	public GradleTasksView() {
		dialogSettings = GradleTasksViewPlugin.getDefault().getDialogSettingsSection(this.getClass().getName());
		isLinkingEnabled = DialogSettingsUtil.getBoolean(dialogSettings, IS_LINKING_ENABLED, DEFAULT_IS_LINKING_ENABLED);
	}

	public void projectSelected(GradleProject p) {
		if (viewer!=null) {
			projectSelector.setProject(p);
			viewer.setInput(p);
			saveDialogSettings();
		}
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialise it.
	 */
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		projectSelector = new ProjectSelector(parent);
		projectSelector.setProjectSelectionListener(this);
		
		PatternFilter filter = new PatternFilter();
		FilteredTree filteredTree = new FilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL, filter, true);
//		{
//			@Override
//			protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
//				return new ContainerCheckedTreeViewer(parent, style);
//			}
//		};
		
		
		viewer = filteredTree.getViewer();
//		drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setContentProvider(new GradleTaskTreeContentProvider(viewer, false));
		viewer.setLabelProvider(new GradleTaskTreeLabelProvider());
//		viewer.setSorter(new ViewerSorter());

		// Create the help context id for the viewer's control
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "org.springsource.ide.eclipse.gradle.ui.taskview.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		initSelectionListener();
		GradleProject selectProject = DialogSettingsUtil.getGradleProject(dialogSettings, SELECTED_PROJECT);
		if (selectProject!=null) {
			projectSelected(selectProject);
		}
	}

	/**
	 * Register our selection listener with the workbench selection service.
	 */
	private void initSelectionListener() {
		IWorkbenchPartSite site = getSite();
		selectionListener = new SelectionListener();
		site.getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				GradleTasksView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(action1);
//		manager.add(new Separator());
//		manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) {
//		manager.add(action1);
//		manager.add(action2);
//		manager.add(new Separator());
////		drillDownAdapter.addNavigationActions(manager);
//		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(linkWithSelectionAction);
		manager.add(refreshAction);
//		manager.add(action1);
//		manager.add(action2);
//		manager.add(new Separator());
//		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		linkWithSelectionAction = new ToggleLinkingAction(this);
		refreshAction = new RefreshAction(this);
//		action1 = new Action() {
//			public void run() {
//				showMessage("Action 1 executed");
//			}
//		};
//		action1.setText("Action 1");
//		action1.setToolTipText("Action 1 tooltip");
//		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		
//		action2 = new Action() {
//			public void run() {
//				showMessage("Action 2 executed");
//			}
//		};
//		action2.setText("Action 2");
//		action2.setToolTipText("Action 2 tooltip");
//		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof Task) {
					Task task = (Task) obj;
					GradleProject project = projectSelector.getProject();
					if (project!=null) {
						final ILaunchConfiguration conf = GradleLaunchConfigurationDelegate.getOrCreate(project, task.getPath());
						JobUtil.schedule(NO_RULE, new GradleRunnable(project.getDisplayName()+ " "+task.getPath()) {
							@Override
							public void doit(IProgressMonitor mon) throws Exception {
								conf.launch("run", mon, false, true);
							}
						});
					}
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Gradle Tasks View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public boolean isLinkingEnabled() {
		return isLinkingEnabled;
	}

	public void setLinkingEnabled(boolean enable) {
		if (this.isLinkingEnabled!=enable) {
			this.isLinkingEnabled = enable;
			saveDialogSettings();
		}
	}

	private void saveDialogSettings() {
		dialogSettings.put(IS_LINKING_ENABLED, isLinkingEnabled());
		DialogSettingsUtil.put(dialogSettings, SELECTED_PROJECT, projectSelector.getProject());
	}
	
	@Override
	public void dispose() {
		try {
			IWorkbenchPartSite site = getSite();
			if (site!=null) {
				if (selectionListener!=null) {
					site.getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
				}
			}
		} finally {
			selectionListener = null;
		}
	}

	public void requestRefreshTasks() throws CoreException {
		projectSelector.updateProjects();
		GradleProject project = projectSelector.getProject();
		if (project!=null) {
			project.requestGradleModelRefresh();
			viewer.refresh();
		}
	}
}
