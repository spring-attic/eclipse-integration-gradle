/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.taskview;

import static org.springsource.ide.eclipse.gradle.core.util.JobUtil.NO_RULE;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.gradle.tooling.model.Task;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.util.DialogSettingsUtil;
import org.springsource.ide.eclipse.gradle.ui.util.SelectionUtils;

/**
 * @author Kris De Volder
 * @author Alex Boyko
 * @since 3.5
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
	private static final String IS_DISPLAY_PROJECT_LOCAL_TASKS = "isDisplayProjectLocalTasks";
	private static final boolean DEFAULT_IS_HIDE_INTERNAL_TASKS = false;
	private static final String IS_HIDE_INTERNAL_TASKS = "isHideInternalTasks";
	private static final boolean DEFAULT_IS_DISPLAY_PROJECT_LOCAL_TASKS = false;
	private static final String SELECTED_PROJECT = "selectedProject";

	private TreeViewer viewer;
	private boolean displayProjectLocalTasks;
	private boolean hideInternalTasks;
	
	private Action linkWithSelectionAction;
	private Action refreshAction;
	private Action toggleProjectTasks;
	private Action doubleClickAction;
	private Action toggleHideInternalTasks;
	private TasksConsoleAction tasksConsoleAction;

	private SelectionListener selectionListener;

	private ProjectSelector projectSelector;

	private boolean isLinkingEnabled = false;
	private IDialogSettings dialogSettings;

	public GradleTasksView() {
		dialogSettings = GradleTasksViewPlugin.getDefault().getDialogSettingsSection(this.getClass().getName());
		isLinkingEnabled = DialogSettingsUtil.getBoolean(dialogSettings, IS_LINKING_ENABLED, DEFAULT_IS_LINKING_ENABLED);
		displayProjectLocalTasks = DialogSettingsUtil.getBoolean(
				dialogSettings, IS_DISPLAY_PROJECT_LOCAL_TASKS,
				DEFAULT_IS_DISPLAY_PROJECT_LOCAL_TASKS);
		hideInternalTasks = DialogSettingsUtil.getBoolean(
				dialogSettings, IS_HIDE_INTERNAL_TASKS,
				DEFAULT_IS_HIDE_INTERNAL_TASKS);
	}

	public void projectSelected(GradleProject p) {
		if (viewer!=null) {
			projectSelector.setProject(p);
			viewer.setInput(p);
			Tree tree = viewer.getTree();
			for (TreeColumn col : tree.getColumns()) {
				col.pack();
			}
			tasksConsoleAction.selectChanged(new StructuredSelection(Collections.singletonList(p.getProject())));
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
		FilteredTree filteredTree = new FilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, filter, true);
		
		viewer = filteredTree.getViewer();
		
		viewer.getTree().setLinesVisible(false);
		TreeColumn title = new TreeColumn(viewer.getTree(), SWT.NONE, 0);
		title.setAlignment(SWT.LEFT);
		TreeColumn description = new TreeColumn(viewer.getTree(), SWT.NONE, 1);
		description.setAlignment(SWT.LEFT);
		
		TaskTreeContentProvider tasksProvider = new TaskTreeContentProvider(viewer);
		tasksProvider.setLocalTasks(displayProjectLocalTasks);
		viewer.setContentProvider(tasksProvider);
		
		viewer.setLabelProvider(new TaskLabelProvider());
		viewer.setSorter(new ViewerSorter());

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
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(doubleClickAction);
		manager.add(toggleHideInternalTasks);
		manager.add(toggleProjectTasks);
		manager.add(tasksConsoleAction);
		manager.add(linkWithSelectionAction);
		manager.add(refreshAction);
	}

	private void makeActions() {
		tasksConsoleAction = new TasksConsoleAction();
		toggleHideInternalTasks = new ToggleHideInternalTasks(this, hideInternalTasks);
		toggleProjectTasks = new ToggleProjectTasks(this, displayProjectLocalTasks);
		linkWithSelectionAction = new ToggleLinkingAction(this);
		refreshAction = new RefreshAction(this);
		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				GradleProject project = projectSelector.getProject();
				if (project != null && !selection.isEmpty()) {
					StringBuilder taskStr = new StringBuilder();
					for (Object obj : selection.toArray()) {
						if (obj instanceof Task) {
							Task task = (Task) obj;
							taskStr.append(displayProjectLocalTasks ? task.getPath() : task.getName());
							taskStr.append(' ');
						}
					}
					if (taskStr.length() > 0) {
						final ILaunchConfiguration conf = GradleLaunchConfigurationDelegate.getOrCreate(project, taskStr.toString());
						JobUtil.schedule(NO_RULE, new GradleRunnable(project.getDisplayName() + " " + taskStr.toString()) {
							@Override
							public void doit(IProgressMonitor mon) throws Exception {
								conf.launch("run", mon, false, true);
							}
						});
					}
				}
			}
		};
		doubleClickAction.setDescription("Run Task");
		doubleClickAction.setToolTipText("Run a task");
		doubleClickAction.setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_RUN_TASK));

	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
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
		dialogSettings.put(IS_DISPLAY_PROJECT_LOCAL_TASKS, displayProjectLocalTasks);
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
			project.invalidateGradleModel();
			viewer.refresh();
		}
	}

	void setDisplayProjectLocalTasks(boolean displayProjectLocalTasks) {
		if (this.displayProjectLocalTasks != displayProjectLocalTasks) {
			this.displayProjectLocalTasks = displayProjectLocalTasks;
			((TaskTreeContentProvider) viewer.getContentProvider()).setLocalTasks(displayProjectLocalTasks);
			viewer.refresh();
		}		
	}
	
	void setHideInternalTasks(boolean hideInternalTasks) {
		if (this.hideInternalTasks != hideInternalTasks) {
			this.hideInternalTasks = hideInternalTasks;
			((TaskTreeContentProvider) viewer.getContentProvider()).setHideInternalTasks(hideInternalTasks);
			viewer.refresh();
		}		
	}
}
