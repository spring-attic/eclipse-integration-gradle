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
package org.springsource.ide.eclipse.gradle.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;
import org.springsource.ide.eclipse.gradle.ui.cli.editor.TasksViewer;


/**
 * @author Kris De Volder
 * @author Alex Boyko
 */
public class GradleLaunchTasksTab extends AbstractLaunchConfigurationTab {

	private static final boolean DEBUG = false;
	
	private static final String CTRL_SPACE_LABEL = "<Ctrl> + <Space>";
	private static final String EDITOR_INFO_LABEL = "Type tasks in the editor below. Use " + CTRL_SPACE_LABEL + " to activate content assistant.";
	
	private Combo projectCombo;
	private GradleProject project;
	private GradleProjectIndex tasksIndex= new GradleProjectIndex();
	
	private TasksViewer tasksViewer;

	private Button refreshButton;
	
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        page.setLayout(layout);
        
        createProjectCombo(page);
        createTaskEditor(page);

		setControl(page);
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
				String newProjectName = projectCombo.getText();
				if ("".equals(newProjectName)) {
					setProject(null);
					setTasksDocument("");
				} else {
					IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProjectName);
					setProject(GradleCore.create(newProject));
					setTasksDocument(tasksViewer.getSourceViewer().getDocument().get());
				}
			}
		});
		
		refreshButton = new Button(parent, SWT.PUSH);
		refreshButton.setText("Refresh");
		refreshButton.setToolTipText("Rebuild the gradle model and refresh the task list");
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {

				GradleProject currentProject = project;
				if (currentProject != null) {
					try {
						project.requestGradleModelRefresh();
					} catch (CoreException e) {
						// ignore
					}
					tasksIndex.setProject(project);
				}

			}
		});
		
		grabHor.align(SWT.RIGHT, SWT.CENTER).applyTo(refreshButton);
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
	
	private void createTaskEditor(final Composite parent) {
		
		StyledText styledText = new StyledText(parent, SWT.READ_ONLY | SWT.WRAP);
		styledText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		styledText.setBackground(parent.getBackground());
		styledText.setText(EDITOR_INFO_LABEL);
		styledText.setStyleRange(new StyleRange(EDITOR_INFO_LABEL
				.indexOf(CTRL_SPACE_LABEL), CTRL_SPACE_LABEL.length(),
				styledText.getForeground(), styledText.getBackground(),
				SWT.BOLD | SWT.ITALIC));
		

		tasksViewer = new TasksViewer(parent, tasksIndex, false);
		
		tasksViewer.getSourceViewer().getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		setTasksDocument("");
	}
	
	
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		setProject(getContext());
		setTasksDocument("");
	}

	private void setProject(GradleProject project) {
		//TODO: when restoring from persistent conf, can get non-existent projects...
		//  how to handle that case?
		if (this.project==project) //Don't do anything if project is unchanged
			return;

		this.project = project;
		tasksIndex.setProject(project);
		if (projectCombo!=null) {
			if (project!=null) {
				projectCombo.setText(project.getName());
			} else {
				projectCombo.deselectAll();
			}
		}
		updateLaunchConfigurationDialog();
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
		setTasksDocument(GradleLaunchConfigurationDelegate.getTasks(conf));
	}

	private void setTasksDocument(String tasks) {
		Document document = new Document(tasks);
		document.addDocumentListener(new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				GradleLaunchTasksTab.this.updateLaunchConfigurationDialog();
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// empty
			}
		});
		tasksViewer.setDocument(document);
	}
	
	@Override
	public void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
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
		GradleLaunchConfigurationDelegate.setTasks(conf, tasksViewer.getSourceViewer().getDocument().get());
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

	@Override
	public void dispose() {
		tasksViewer.dispose();
		super.dispose();
	}

}
