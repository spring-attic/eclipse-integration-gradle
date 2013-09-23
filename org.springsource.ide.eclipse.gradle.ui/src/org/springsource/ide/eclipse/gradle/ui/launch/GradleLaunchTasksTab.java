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
package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;


/**
 * @author Kris De Volder
 */
public class GradleLaunchTasksTab extends AbstractLaunchConfigurationTab {

	private static final boolean DEBUG = false;
	
	private Combo projectCombo;
	private CheckboxTreeViewer taskSelectionTreeViewer;
	private GradleProject project;
	private GradleTaskCheckStateProvider tasksChecked = new GradleTaskCheckStateProvider(this);
	
	private Text taskOrderText;
	private Button orderButton;

	private Button clearButton;

	private Button refreshButton;
	
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        page.setLayout(layout);
        
        createProjectCombo(page);
        createTaskTree(page);
        
        createTaskList(page);
		
		setControl(page);
	}

	/**
	 * Creates the widgets that display the target order
	 */
	private void createTaskList(Composite parent) {
		Font font= parent.getFont();

		Label label = new Label(parent, SWT.NONE);
		label.setText("Task execution order:");
		label.setFont(font);

		Composite orderComposite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		orderComposite.setLayoutData(gd);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		orderComposite.setLayout(layout);
		orderComposite.setFont(font);

		taskOrderText = new Text(orderComposite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		taskOrderText.setFont(font);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.FILL_VERTICAL);
		gd.heightHint = 40;
		gd.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		taskOrderText.setLayoutData(gd);

		Composite buttonColumn = new Composite(orderComposite, SWT.NONE);
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonColumn.setLayout(layout);
		buttonColumn.setFont(font);
		
		orderButton = createPushButton(buttonColumn, "Order...", null);
		gd = (GridData)orderButton.getLayoutData();
		gd.verticalAlignment = GridData.BEGINNING;
		orderButton.setFont(font);
		orderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleOrderPressed();
			}
		});
		clearButton = createPushButton(buttonColumn, "Clear", null);
		gd = (GridData)orderButton.getLayoutData();
		gd.verticalAlignment = GridData.BEGINNING;
		clearButton.setFont(font);
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleClearPressed();
			}
		});
		updateOrderedTargets();
	}

	/**
	 * Handle the "clear" button which deselects everything.
	 */
	private void handleClearPressed() {
		setChecked(Arrays.asList(new String[0]));
	}

	/**
	 * The target order button has been pressed. Prompt the
	 * user to reorder the selected targets. 
	 */
	private void handleOrderPressed() {
		GradleTaskOrderDialog dialog = new GradleTaskOrderDialog(getShell(), tasksChecked.toArray());
		int ok = dialog.open();
		if (ok == Window.OK) {
			String[] targets = dialog.getTargets();
			setChecked(Arrays.asList(targets));
		}
	}
	
	private void createProjectCombo(Composite _parent) {
		GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
		Composite parent = new Composite(_parent, SWT.NONE);
		parent.setLayout(new GridLayout(3,false));
		
		grabHor.applyTo(parent);
		
		Label label = new Label(parent, SWT.NONE);
		label.setText("Project");
		
		projectCombo = new Combo(parent, SWT.DROP_DOWN|SWT.READ_ONLY);
		List<GradleProject> projects = getGradleProjects();
		String[] items = new String[projects.size()];
		int i = 0;
		for (GradleProject p : projects) {
			items[i++] = p.getName();
		}
		projectCombo.setItems(items);
		
		if (project!=null) {
			projectCombo.setText(project.getName());
		}
		
		projectCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				projectComboChanged();
			}
		});
		
		refreshButton = new Button(parent, SWT.PUSH);
		refreshButton.setText("Refresh");
		refreshButton.setToolTipText("Rebuild the gradle model and refresh the task list");
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				try {
					GradleProject currentProject = project;
					if (currentProject!=null) {
						currentProject.requestGradleModelRefresh();
						taskSelectionTreeViewer.refresh();
					}
				} catch (CoreException e) {
					GradleCore.log(e);
				}
			}
		});
		
		grabHor.align(SWT.RIGHT, SWT.CENTER).applyTo(refreshButton);
	}
	
	private void projectComboChanged() {
		String newProjectName = projectCombo.getText();
		if ("".equals(newProjectName)) {
			setProject(null);
		} else {
			IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProjectName);
			setProject(GradleCore.create(newProject));
		}
	}
	
	private List<GradleProject> getGradleProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<GradleProject> result = new ArrayList<GradleProject>();
		for (IProject p : projects) {
			try {
				if (p.isAccessible() && p.hasNature(GradleNature.NATURE_ID)) {
					result.add(GradleCore.create(p));
				}
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		}
		return result;
	}

	private void createTaskTree(Composite parent) {
		PatternFilter filter = new PatternFilter();
		FilteredTree filteredTree = new FilteredTree(parent, SWT.CHECK | SWT.BORDER, filter, true) {
			@Override
			protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
				return new ContainerCheckedTreeViewer(parent, style);
			}
		};
		taskSelectionTreeViewer = (CheckboxTreeViewer)filteredTree.getViewer();
		filteredTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		//Add multi column support
		Tree tree = taskSelectionTreeViewer.getTree();
		tree.setHeaderVisible(true);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Project/Task");
		TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
		column2.setText("Description");
		column1.pack();
		column2.pack();
        
        taskSelectionTreeViewer.setLabelProvider(new GradleTaskTreeLabelProvider());
        taskSelectionTreeViewer.setContentProvider(new GradleTaskTreeContentProvider(taskSelectionTreeViewer, true));
        taskSelectionTreeViewer.setCheckStateProvider(tasksChecked);
        taskSelectionTreeViewer.addCheckStateListener(tasksChecked);
		
        if (project!=null) {
        	setTreeInput(project);
        }
	}
	
	private GradleProject getTreeInput() {
		if (taskSelectionTreeViewer!=null) {
			return (GradleProject)taskSelectionTreeViewer.getInput();
		}
		return null;
	}

	private void setTreeInput(GradleProject project) {
		if (taskSelectionTreeViewer!=null) {
			taskSelectionTreeViewer.setInput(project);
			Tree tree = taskSelectionTreeViewer.getTree();
			for (TreeColumn col : tree.getColumns()) {
				col.pack();
			}
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		setProject(getContext());
	}

	private void setProject(GradleProject project) {
		//TODO: when restoring from persistent conf, can get non-existent projects...
		//  how to handle that case?
		if (this.project==project) //Don't do anything if project is unchanged
			return;

//		if (this.project!=null) {
//			this.project.removeModelListener(modelListener);
//		}
		this.project = project;
		setChecked(Arrays.asList(new String[0]));
//		if (this.project!=null) {
//			this.project.addModelListener(modelListener);
//		}
		setTreeInput(project);
		if (projectCombo!=null) {
			if (project!=null) {
				projectCombo.setText(project.getName());
			} else {
				projectCombo.deselectAll();
			}
		}
		updateLaunchConfigurationDialog();
	}
	
	@Override
	public void dispose() {
//		if (project!=null && modelListener!=null) {
//			project.removeModelListener(modelListener);
//		}
		super.dispose();
	}

	private GradleProject getContext() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win==null ? null : win.getActivePage();
		
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (!ss.isEmpty()) {
					Object obj = ss.getFirstElement();
					if (obj instanceof IResource) {
						IResource rsrc = (IResource) obj;
						IProject prj = rsrc.getProject();
						if (prj!=null) {
							return GradleCore.create(prj);
						}
					}
				}
			}
			IEditorPart part = page.getActiveEditor();
			if (part != null) {
				IEditorInput input = part.getEditorInput();
				IResource rsrc = (IResource) input.getAdapter(IResource.class);
				if (rsrc!=null) {
					IProject prj = rsrc.getProject();
					if (prj!=null) {
						return GradleCore.create(prj);
					}
				}
			}
		}
		return null;
	}

	public void initializeFrom(ILaunchConfiguration conf) {
		debug(">>> initializing Gradle launch tab");
		try {
			for (Object attName : conf.getAttributes().keySet()) {
				debug(""+attName);
			}
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		debug("<<< initializing Gradle launch tab");
		setProject(GradleLaunchConfigurationDelegate.getProject(conf));
		setChecked(GradleLaunchConfigurationDelegate.getTasks(conf));
	}

	private void setChecked(List<String> tasks) {
		tasksChecked.setChecked(tasks);
		if (taskSelectionTreeViewer!=null) {
			taskSelectionTreeViewer.refresh();
		}
		updateOrderedTargets();
		updateLaunchConfigurationDialog();
	}
	
	@Override
	public void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}
	
	protected void updateOrderedTargets() {
		if (taskOrderText!=null) {
			List<String> tasks = getSelectedTasks();
			StringBuffer s = new StringBuffer();
			boolean comma = false;
			for (String taskPath : tasks) {
				if (comma) {
					s.append(", ");
				}
				s.append(taskPath);
				comma = true;
			}
			taskOrderText.setText(s.toString());
		}
	}

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	@Override
	public boolean canSave() {
		//Don't allow saving until the model is properly initialized. If it isn't, the checkboxes in the tree
		//don't have well defined state.
		return haveGradleModel();
	}
	
	public boolean isValid() {
		return canSave();
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy conf) {
		GradleLaunchConfigurationDelegate.setProject(conf, project);
		GradleLaunchConfigurationDelegate.setTasks(conf, getSelectedTasks());
	}

	/**
	 * Gets tasks selected in the actual GUI widget (rather than from the checked state provider, because the checked state provider
	 * may actually contain task paths strings that don't even exist in the task tree (anymore). Moreover, when we call this we are
	 * about to save the task tree state, and it would be best to weed out non-existent tasks here. Only the tasks that are actually
	 * selected in the GUI tree (these should be real tasks since they are populated from the model) will be returned.
	 */
	private List<String> getSelectedTasks() {
		List<String> result = tasksChecked.getChecked();
		Set<String> validTasks = getValidTasks();
		if (validTasks!=null) {
			result.retainAll(validTasks);
		}
		return result;
	}

	/**
	 * @return THe set of tasks that is valid. May return null if it is not possible at the moment
	 * to determine task validity (typically, this happens if we don;t have a Gradle project model
	 * ready yet. Callers should be ready to deal with that situation.
	 */
	private Set<String> getValidTasks() {
		try {
			GradleProject gp = getTreeInput();
			if (gp!=null) {
				return gp.getAllTasks();
			}
		} catch (FastOperationFailedException e) {
			//Ignore: may happen if gradle model not ready
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return null; // Couldn't determine what is valid
	}

	private boolean haveGradleModel() {
		try {
			return project != null && project.getGradleModel()!=null;
		} catch (FastOperationFailedException e) {
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return false;
	}

	public String getName() {
		return "Gradle Tasks";
	}

}
