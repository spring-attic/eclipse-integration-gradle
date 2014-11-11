/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.dsld.DSLDSupport;
import org.springsource.ide.eclipse.gradle.core.preferences.GradleImportPreferences;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation;
import org.springsource.ide.eclipse.gradle.core.wizards.PrecomputedProjectMapper;
import org.springsource.ide.eclipse.gradle.core.wizards.GradleImportOperation.MissingProjectDependencyException;
import org.springsource.ide.eclipse.gradle.core.wizards.PrecomputedProjectMapper.NameClashException;
import org.springsource.ide.eclipse.gradle.ui.util.UIJobUtil;


/**
 * @author Kris De Volder
 */
public class GradleImportWizardPageOne extends WizardPage {
	
	private static final ImageDescriptor WIZBAN_IMAGE = ImageDescriptor.createFromURL(
			GradleImportWizardPageOne.class.getClassLoader().getResource("icons/gradle-import-wizban.png"));

	/**
	 * Key to store / retrieve root folder input history from GradlePreferences.
	 */
	private static final String ROOT_FOLDER_HISTORY_KEY = GradleImportWizardPageOne.class.getName()+".RF_HIST";
	private static final String[] DEFAULT_ROOT_FOLDER_HISTORY = new String[] {};
	private static final int MAX_ROOT_FOLDER_HISTORY = 10;
	
	private Combo rootFolderText;
	private Button browseButton;
	private CheckboxTreeViewer projectSelectionTree;
	private Button enableDependencyManagementCheckbox;
	private Button autoSelectSubtreeCheckBox;
	private Button createRecourceFiltersCheckbox;
	private Button useHierarchicalNamesCheckbox;
	
	private TaskRunLine runBeforeLine;
	private TaskRunLine runAfterLine;
	
	private PrecomputedProjectMapper projectMapping = null;

	private WorkingSetGroup workingSetGroup;

	private Group optionsGroup;

	private Button loadModelButton;

	/**
	 * Tracks the state of the project tree viewer.
	 */
	private boolean isTreePopulated = false;
	
	private DefaultsSetter defaultsSetter = new DefaultsSetter();

	private Button enableDSLDCheckbox;

	public GradleImportWizardPageOne() {
		super("gradleImportWizardPage1", "Import Gradle Project", WIZBAN_IMAGE);
	}

	public void createControl(Composite parent) {
		
		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        layout.marginWidth = 1;
        page.setLayout(layout);
       
        createRootFolderSection(page);
        createProjectSelectionGroup(page);
        createOptionsGroup(page);
        createWorkingSetGroup(page);
        
        setControl(page);
        checkPageComplete();
        checkRootFolder();
	}
	
	private void createWorkingSetGroup(Composite page) {
		this.workingSetGroup = new WorkingSetGroup();
		Control control = workingSetGroup.createControl(optionsGroup, page);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(control);
	}

	/**
     *	Create the import source selection widget
     */
    protected void createProjectSelectionGroup(Composite parent) {
        
        //Just create with a dummy root.
        projectSelectionTree = new CheckboxTreeViewer(parent, SWT.BORDER);
        projectSelectionTree.addFilter(new ExistingGradleProjectFilter());
        projectSelectionTree.setSorter(new ViewerSorter());
        
        Tree tree = projectSelectionTree.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		tree.setLayout(new GridLayout(2, true));

		//Multi column stuff
		tree.setHeaderVisible(true);
        TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
        column1.setText("Project");
        TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
        column2.setText("Description");
        column1.pack();
        column2.pack();
        
        projectSelectionTree.setContentProvider(new GradleProjectTreeContentProvider());
        projectSelectionTree.setLabelProvider(new GradleProjectTreeLabelProviderWithDescription(true));
        
        projectSelectionTree.setInput(null); // pass a non-null that will be ignored
        
        projectSelectionTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (autoSelectSubtreeCheckBox.getSelection()) {
					boolean select = event.getChecked();
					projectSelectionTree.setSubtreeChecked(event.getElement(), select);
				}
				checkPageComplete();
			}
		});

        GridDataFactory.fillDefaults().grab(true, true).minSize(0, 200).applyTo(projectSelectionTree.getControl());
        
        autoSelectSubtreeCheckBox = new Button(parent, SWT.CHECK);
        autoSelectSubtreeCheckBox.setSelection(true);
        autoSelectSubtreeCheckBox.setText("Auto-select subprojects");
        autoSelectSubtreeCheckBox.setToolTipText("When this option is selected, clicking a project in the tree will select/deselect not just the project, but also all its subprojects");

        Composite buttonRow = new Composite(parent, SWT.NONE);
        buttonRow.setLayout(new GridLayout(3, true));
        
        Button selectAllButton = new Button(buttonRow, SWT.PUSH);
        selectAllButton.setText("Select All");
        
        Button deselectAllButton = new Button(buttonRow, SWT.PUSH);
        deselectAllButton.setText("Deselect All");
        
        Button selectRequired = new Button(buttonRow, SWT.PUSH);
        selectRequired.setText("Select Required");
        selectRequired.setToolTipText("Select all projects that are required as dependencies of the projects you have already selected");
        
        GridDataFactory grabHor = GridDataFactory.fillDefaults().grab(true, false);
        grabHor.applyTo(selectAllButton);
        grabHor.applyTo(deselectAllButton);
        
        selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllProjects(true);
			}
		});
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllProjects(false);
			}
		});
        selectRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectRequiredProjects();
			}
		});
    }
    
	private void createOptionsGroup(Composite parent) {
		
		optionsGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		optionsGroup.setText("Import options");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(optionsGroup);
		optionsGroup.setLayout(new GridLayout(2, true));
		
		runBeforeLine = new TaskRunLine("Run before", 
				"Execute a list of Gradle tasks BEFORE importing projects.\n" +
				"Note that BUG GRADLE-1792 prevents checking whether eclipse tasks " +
				"are defined by a project. You MUST ensure the eclipse plugin " +
				"is applied to EVERY imported project to run tasks 'eclipse'" +
				"and 'cleanEclipse'", this, optionsGroup);
		runBeforeLine.setEnabled(GradleImportOperation.determineDefaultDoBefore(null));
		runBeforeLine.setTasks(GradleImportOperation.DEFAULT_BEFORE_TASKS);
		
		runAfterLine = new TaskRunLine("Run after", 
				"Execute a list of Gradle tasks AFTER importing projects.\n" +
				"The tasks are only executed on projects that define it.", 
				this, optionsGroup);
		runAfterLine.setEnabled(GradleImportOperation.DEFAULT_DO_AFTER_TASKS);
		runAfterLine.setTasks(GradleImportOperation.DEFAULT_AFTER_TASKS);

		enableDependencyManagementCheckbox = new Button(optionsGroup, SWT.CHECK);
		enableDependencyManagementCheckbox.setText("Enable dependency management");
		enableDependencyManagementCheckbox.setToolTipText("If enabled: adds the 'Gradle Dependencies' classpath entry to " +
				"the project's classpath; lets STS manage project and jar dependencies by querying the tooling API.\n" +
				"If not enabled: STS will only make minimal modifications to configuration files generated by the Gradle " +
				"eclipse plugin and won't attempt to manage the dependencies for you; dependencies can still be 'refreshed'" +
				"with the 'Refresh All' command, but this will be done by re-running the eclipse plugin tasks.");
		enableDependencyManagementCheckbox.setSelection(GradleImportOperation.DEFAULT_ENABLE_DEPENDENCY_MANAGEMENT);
		
		enableDSLDCheckbox =  new Button(optionsGroup, SWT.CHECK);
		enableDSLDCheckbox.setText("Enable DSL Support");
		enableDSLDCheckbox.setToolTipText("Enable Gradle DSL editor support (requires Greclipse)");
		if (DSLDSupport.getInstance().haveGreclipse()) {
			enableDSLDCheckbox.setSelection(GradleImportOperation.DEFAULT_ENABLE_DSLD);
		} else {
			enableDSLDCheckbox.setSelection(false);
			enableDSLDCheckbox.setEnabled(false);
		}
		
		createRecourceFiltersCheckbox = new Button(optionsGroup, SWT.CHECK);
		createRecourceFiltersCheckbox.setText("Create resource filters");
		createRecourceFiltersCheckbox.setToolTipText("Check this option to add resource filters that remove subprojects " +
				"from imported projects in the workspace. Any pre-existing resource filters will be deleted. " +
				"Uncheck this box to leave existing resource filters intact.");
		createRecourceFiltersCheckbox.setSelection(GradleImportOperation.DEFAULT_ADD_RESOURCE_FILTERS);
		
		useHierarchicalNamesCheckbox = new Button(optionsGroup, SWT.CHECK);
		useHierarchicalNamesCheckbox.setText("Use hierarchical project names");
		useHierarchicalNamesCheckbox.setToolTipText("Check this to include parent project's name in child project's name.");
		useHierarchicalNamesCheckbox.setSelection(GradleImportOperation.DEFAULT_USE_HIERARCHICAL_NAMES);
		
		useHierarchicalNamesCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});
		
		runBeforeLine.addListener(new TaskRunLine.Listener() {
			public void fire() {
				checkPageComplete();
			}
		});
		
		enableDependencyManagementCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPageComplete();
			}
		});
	}
	
	private boolean getEnableDSLD() {
		return enableDSLDCheckbox.getSelection();
	}

	private void updateProjectMapping() throws NameClashException, CoreException {
		projectMapping = null;
		projectMapping = GradleImportOperation.createProjectMapping(getUseHierarchicalNames(), getAllProjects());
	}

	private Collection<HierarchicalEclipseProject> getAllProjects() {
		//TODO: cache this. It should only change when root folder changes
		Object input = projectSelectionTree.getInput();
		if (input instanceof List<?>) {
			HierarchicalEclipseProject rootModel = (HierarchicalEclipseProject)((List<?>) input).get(0); 
			return GradleProjectUtil.getAllProjects(rootModel);
		} 
		return new ArrayList<HierarchicalEclipseProject>();
	}

	private void selectAllProjects(boolean select) {
		Object input = projectSelectionTree.getInput();
		if (input!=null) {
			for (Object rootElement : (List<?>)input) {
				projectSelectionTree.setSubtreeChecked(rootElement, select);
			}
		}
		checkPageComplete();
	}
	
	private void selectRequiredProjects() {
		HashSet<HierarchicalEclipseProject> selected = new HashSet<HierarchicalEclipseProject>(getSelectedProjects());
		int initialSize = selected.size();
		for (HierarchicalEclipseProject p : getSelectedProjects()) {
			for (EclipseProjectDependency dep : p.getProjectDependencies()) {
				HierarchicalEclipseProject depProj = dep.getTargetProject();
				selectRequiredProjects(selected, depProj);
			}
		}
		if (initialSize!=selected.size()) {
			projectSelectionTree.setCheckedElements(selected.toArray()); 
			// setCheckedElements doesn't fire check state events so we must call:
			checkPageComplete();
		}
	}
	
	
	private void selectRequiredProjects(HashSet<HierarchicalEclipseProject> selected, HierarchicalEclipseProject p) {
		if (!selected.contains(p)) {
			selected.add(p);
			for (EclipseProjectDependency dep : p.getProjectDependencies()) {
				selectRequiredProjects(selected, dep.getTargetProject());
			}
		}
	}

	protected void checkPageComplete() {
		GradleImportOperation operation = createOperation();
		setErrorMessage(null);
		setMessage(null, IStatus.WARNING);
		if (!isTreePopulated) {
			setErrorMessage("You must click 'Build Model' before you can select projects to import");
		} else if (!hasSelectedProjects()) {
			setErrorMessage("At least one project must be selected to import");
		} else {
			try {
				updateProjectMapping();
				GradleImportOperation candidateOp = createOperation();
				candidateOp.verify();
				operation = candidateOp;
				workingSetGroup.setQuickWorkingSetName(getRootProjectName());
			} catch (MissingProjectDependencyException e) {
				//Expected exceptions should not be logged...
				setErrorMessage(e.getMessage());
			} catch (CoreException e) {
				setErrorMessage(e.getMessage());
				GradleCore.log(e);
			}
		}
		if (getErrorMessage()==null) {
			boolean isDoingEclipseTask = operation.isDoingEclipseTask();
			if (isDoingEclipseTask && getUseHierarchicalNames()) {
				setMessage("The 'hierarchical names' option is incompatible with the 'eclipse' task. It may cause problems" +
						" because Eclipse and Gradle use different names to refer to nested projects.", IStatus.WARNING);
			}
			if (!operation.getEnableDependencyManagement() && (!operation.isDoingEclipseTask())) {
				setMessage("Dependency management is disabled so you probably want to enable running the 'eclipse' task before import to configure the classpath.", IStatus.WARNING);
			}
		}
		setPageComplete(getErrorMessage()==null);
	}

	private String getRootProjectName() {
		HierarchicalEclipseProject rp = getRootProject();
		if (rp!=null) {
			return rp.getName();
		}
		return null;
	}

	private HierarchicalEclipseProject getRootProject() {
		Object input = projectSelectionTree.getInput();
		if (input instanceof List<?>) {
			HierarchicalEclipseProject rootModel = (HierarchicalEclipseProject)((List<?>) input).get(0); 
			return rootModel;
		}
		return null;
	}

	private boolean hasSelectedProjects() {
		if (projectSelectionTree!=null) {
			Object[] selectedProjects = projectSelectionTree.getCheckedElements();
			return selectedProjects!=null && selectedProjects.length>0;
		}
		return false;
	}

	private void createRootFolderSection(Composite page) {
        GridDataFactory grabHorizontal = GridDataFactory.fillDefaults().grab(true, false);

		GridLayout layout = new GridLayout(4, false);
		Composite composite = new Composite(page, SWT.NONE);
        composite.setLayout(layout);
        
		Label label = new Label(composite, SWT.NONE);
		label.setText("Root folder:");
        rootFolderText = new Combo(composite, SWT.DROP_DOWN);

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		
        browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//Button clicked
				File file = openFileDialog(rootFolderText.getText());
				if (file!=null) {
					rootFolderText.setText(file.toString());
//					populateProjectTree(file);
				}
			}
		});
        
        loadModelButton = new Button(composite, SWT.PUSH);
        loadModelButton.setText("Build Model");
        loadModelButton.setToolTipText("Ask Gradle to construct a project/subproject model. " +
        		"This model is needed to populate the project selection area below. " +
        		"Building the model may take some time.");
        loadModelButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
    			File rf = getRootFolder();
    			if (rf!=null && rf.exists()) {
    				populateProjectTree(rf);
    			}
        	}
		});
        
        String[] rootFolderHistory = getRootFolderHistory();
        if (rootFolderHistory.length>0) {
        	rootFolderText.setItems(rootFolderHistory);
        	rootFolderText.select(rootFolderHistory.length-1);
        }
        
        rootFolderText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				clearProjectTree();
				checkRootFolder();
			}
		});
        
        grabHorizontal.applyTo(composite);
        grabHorizontal.applyTo(rootFolderText);
	}
	
	/**
	 * Enable / disable the load model button based on whether root folder is selected.
	 */
	private void updateLoadButton() {
		if (loadModelButton!=null) {
			File rf = getRootFolder();
			loadModelButton.setEnabled(rf!=null && rf.exists() && rf.isDirectory());
		}
	}

	private String[] getRootFolderHistory() {
		return GradleCore.getInstance().getPreferences().getStrings(ROOT_FOLDER_HISTORY_KEY, DEFAULT_ROOT_FOLDER_HISTORY);
	}

	private void checkRootFolder() {
		final File rf = getRootFolder();
		setPageComplete(false);
		if (rf==null) {
			setErrorMessage("Specify the root folder of your Gradle project to import");
		} else if (!rf.exists()) {
			setErrorMessage("The root folder doesn't exist");
		} else if (!hasBuildFile(rf)) {
			setErrorMessage("Could not find 'build.gradle' in the root folder. Is it a Gradle project?");
		} else {
			checkPageComplete();
		}
		updateLoadButton();
	}

	private boolean hasBuildFile(File rf) {
		File buildFile = new File(rf, "build.gradle");
		return buildFile.exists();
	}

	private void clearProjectTree() {
		defaultsSetter.rootModelChanged(null);
		isTreePopulated = false;
		projectSelectionTree.setInput(null);
	}

	private void populateProjectTree(final File rf) {
		try {
			UIJobUtil.withProgressDialog(getShell(), new GradleRunnable("Creating Gradle model") {
				//TODO: This runnable isn't protected by a scheduling rule. That can be a problem!
				@Override
				public void doit(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
					GradleProject selectedProject = GradleCore.create(rf);
					selectedProject.invalidateGradleModel(); // TODO: with better invalidation from Gradle, this may not be necessary
					selectedProject.getSkeletalGradleModel(monitor); //forces model to be built

					try {
						
						//Careful... make sure to get the model for the rootproject even if a subproject was selected!
						//See https://issuetracker.springsource.com/browse/STS-3456
						final HierarchicalEclipseProject rootModel = selectedProject.getRootProject().getSkeletalGradleModel();
						rootFolderText.getDisplay().asyncExec(new Runnable() {
							public void run() {
								//This call must execute in the UI thread.
								projectSelectionTree.setInput(Arrays.asList(rootModel));
								isTreePopulated = true;
	//							if (!hasSelectedProjects()) {
	//								selectAllProjects(true);
	//							}
								// Expand the root node, also helps for the "pack" calls below to have correct column size. 
								projectSelectionTree.setExpandedElements(new Object[] { rootModel }); 
								// Allocate column widths based on contents.
								projectSelectionTree.getTree().getColumn(0).pack();
								projectSelectionTree.getTree().getColumn(1).pack();
								defaultsSetter.rootModelChanged(rootModel);
								checkPageComplete();
							}
						});
					} catch (FastOperationFailedException e) {
						//This shouldn't happen since model was already built beforehand
						throw ExceptionUtil.coreException(e);
					}
				}
			});
		} catch (Exception e) {
			//TODO: some errors are expected... so should be handled nicer
			setErrorMessage("Unexpected error: see error log for details");
			GradleCore.log(e);
		}
	};

	
	private File openFileDialog(String initialSelection) {
		DirectoryDialog fileDialog = new DirectoryDialog(getShell(),  SWT.OPEN);
		fileDialog.setFilterPath(initialSelection);
		String file = fileDialog.open();
		if (file!=null) {
			return new File(file);
		}
		return null;
	}

	public File getRootFolder() {
		String text = rootFolderText.getText();
		if (text!=null && !("".equals(text.trim()))) {
			return new File(text);
		}
		return null;
	}

	/**
	 * @return All the projects selected for importing by the user, never null, but may be an empty list.
	 */
	public List<HierarchicalEclipseProject> getSelectedProjects() {
		if (projectSelectionTree!=null) {
			Object[] selected = projectSelectionTree.getCheckedElements();
			ArrayList<HierarchicalEclipseProject> asList = new ArrayList<HierarchicalEclipseProject>(selected.length);
			for (Object object : selected) {
				asList.add((HierarchicalEclipseProject) object);
			}
			return asList;
		}
		return Arrays.asList(new HierarchicalEclipseProject[0]);
	}
	
	/**
	 * Selects given set of GradleProjects in the project selection tree (if the tree
	 * currently contains them, any elements not currently in the tree will be ignored).
	 */
	public void setSelectedProjects(GradleProject[] projects) {
		if (projectSelectionTree!=null) {
			List<HierarchicalEclipseProject> toBeChecked =
					new ArrayList<HierarchicalEclipseProject>(projects.length); 
			for (GradleProject p : projects) {
				try {
					HierarchicalEclipseProject hp = p.getSkeletalGradleModel();
					toBeChecked.add(hp);
				} catch (Exception e) {
					//No model for that project ignore it
				}
			}
			projectSelectionTree.setCheckedElements(toBeChecked.toArray());
		}
	}

	public boolean getCreateResourceFilters() {
		return createRecourceFiltersCheckbox.getSelection();
	}

	private void setCreateResourceFilters(boolean enable) {
		createRecourceFiltersCheckbox.setSelection(enable);
	}
	
	private boolean getUseHierarchicalNames() {
		return useHierarchicalNamesCheckbox.getSelection();
	}

	public PrecomputedProjectMapper getProjectMapping() {
		return projectMapping;
	}

	/**
	 * Called by our wizard when its about to finish (i.e. user has pressed finish button).
	 */
	public void wizardAboutToFinish() {
		addRootFolderToHistory();
		HierarchicalEclipseProject project = getRootProject();
		if (project!=null) {
			defaultsSetter.saveDefaults(GradleCore.create(project));
		}
	}

	/**
	 * Ensure that the current contents of the root folder text box is added to the history.
	 */
	private void addRootFolderToHistory() {
		String[] history = getRootFolderHistory();
		LinkedHashSet<String> historySet = new LinkedHashSet<String>(Arrays.asList(history));
		if (historySet.size()>=MAX_ROOT_FOLDER_HISTORY) {
			historySet.remove(history[0]);
		}
		String selectedFolder = rootFolderText.getText();
		historySet.remove(selectedFolder);
		historySet.add(selectedFolder);
		GradleCore.getInstance().getPreferences().putStrings(ROOT_FOLDER_HISTORY_KEY, historySet.toArray(new String[historySet.size()]));
	}

	/**
	 * Creates an import operation based on all the settings in the wizard page UI.
	 */
	public GradleImportOperation createOperation() {
		GradleImportOperation operation = new GradleImportOperation(getSelectedProjects(), getCreateResourceFilters(), getProjectMapping());
		operation.setWorkingSets(workingSetGroup.getSelectedWorkingSets());
		operation.setQuickWorkingSet(workingSetGroup.getQuickWorkingSetName());
		operation.setDoBeforeTasks(runBeforeLine.isEnabled());
		operation.setBeforeTasks(runBeforeLine.getTasks());
		operation.setDoAfterTasks(runAfterLine.isEnabled());
		operation.setAfterTasks(runAfterLine.getTasks());
		operation.setEnableDependencyManagement(enableDependencyManagementCheckbox.getSelection());
		operation.setEnableDSLD(getEnableDSLD());
		return operation;
	}

	/**
	 * An instance of this class is responsible for saving and restoring the state of the Wizard's UI
	 * widgetry based on preferences stored in relation to the selected project.
	 * 
	 * @author Kris De Volder
	 */
	public class DefaultsSetter {

		GradleProject currentProject = null;

		/**
		 * Called by the wizard each time the rootModel is set.
		 */
		public void rootModelChanged(HierarchicalEclipseProject newModel) {
			if (currentProject!=null) {
				saveDefaults(currentProject);
			}
			if (newModel==null) {
				//model and tree just got cleared
				currentProject = null;
			} else {
				//model and tree just got set
				GradleProject newProject = newModel==null?null:GradleCore.create(newModel);
				restoreDefaults(newProject);
				currentProject = newProject;
			}
		}

		/**
		 * Saves the state of prefs currently in the UI widgets in project prefs for
		 * given project.
		 */
		public void saveDefaults(GradleProject project) {
			GradleImportPreferences store = project.getImportPreferences();
			
			store.setSelectedProjects(getSelectedProjects());
			//TODO: naming scheme (linked resources pref?)
			
//			store.setQuickWorkingSetEnabled(workingSetGroup.getQuickWorkingSetEnabled());
			store.setDoBeforeTasks(runBeforeLine.isEnabled());
			store.setBeforeTasks(runBeforeLine.getTasks());
			store.setDoAfterTasks(runAfterLine.isEnabled());
			store.setAfterTasks(runAfterLine.getTasks());
			store.setEnableDependencyManagement(enableDependencyManagementCheckbox.getSelection());
			store.setEnableDSLD(getEnableDSLD());
			store.setAddResourceFilters(getCreateResourceFilters());
		}

		/**
		 * Reads from given project's preferences and initializes the widgets to their
		 * default settings.
		 */
		public void restoreDefaults(GradleProject newProject) {
			GradleImportPreferences store = newProject.getImportPreferences();

			setSelectedProjects(store.getSelectedProjects());
//			workingSetGroup.setQuickWorkingSetEnabled(store.getQuickWorkingSetEnabled());
			runBeforeLine.setEnabled(store.getDoBeforeTasks());
			runBeforeLine.setTasks(store.getBeforeTasks());
			runAfterLine.setEnabled(store.getDoAfterTasks());
			runAfterLine.setTasks(store.getAfterTasks());
			enableDependencyManagementCheckbox.setSelection(store.getEnableDependencyManagement());
			if (enableDSLDCheckbox.isEnabled()) {
				enableDSLDCheckbox.setSelection(store.getEnableDSLD());
			}
			setCreateResourceFilters(store.getAddResourceFilters());
		}

	}
}
