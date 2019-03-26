/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.cli.inplace;

import static org.springsource.ide.eclipse.gradle.core.util.JobUtil.NO_RULE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.ui.PlatformUI;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.launch.GradleLaunchConfigurationDelegate;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.RestrictedCapacityStack;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;
import org.springsource.ide.eclipse.gradle.ui.cli.editor.TasksViewer;
import org.springsource.ide.eclipse.gradle.ui.cli.inplace.PopupTable.IItemDescriptor;

/**
 * Dialog to enter Gradle tasks as one would enter them via a command line interface
 * 
 * @author Christian Dupuis
 * @author Andrew Eisenberg
 * @author Andy Clement
 * @author Kris De Volder
 * @author Nieraj Singh
 * @author Alex Boyko
 * 
 * @since 3.5.0
 */
@SuppressWarnings("restriction")
public class ConsoleInplaceDialog {
	
	/**
	 * Currently active instance or null. Only one instance should be active/open at the
	 * same time.
	 */
	private static ConsoleInplaceDialog instance;
	
	private static final RestrictedCapacityStack<CommandEntry> history = new RestrictedCapacityStack<ConsoleInplaceDialog.CommandEntry>();

	public static String title = "Gradle Tasks Prompt";

	/**
	 * Get an instances of GrailsInplaceDialog. This method ensures only one instance 
	 * is open at a time, and returns a reference to the existing instance if one is
	 * already/still open. 
	 */
	public static ConsoleInplaceDialog getInstance(Shell parent) {
		if (instance==null) {
			instance = new ConsoleInplaceDialog(parent);
		}
		return instance;
	}
	

	/**
	 * Dialog constants telling whether this control can be resized or move.
	 */
	public static final String STORE_DISABLE_RESTORE_SIZE = "DISABLE_RESTORE_SIZE"; //$NON-NLS-1$

	public static final String STORE_DISABLE_RESTORE_LOCATION = "DISABLE_RESTORE_LOCATION"; //$NON-NLS-1$

	/**
	 * Dialog store constant for the location's x-coordinate, location's y-coordinate and the size's width and height.
	 */
	private static final String STORE_LOCATION_X = "location.x"; //$NON-NLS-1$

	private static final String STORE_LOCATION_Y = "location.y"; //$NON-NLS-1$

	private static final String STORE_SIZE_WIDTH = "size.width"; //$NON-NLS-1$

	private static final String STORE_SIZE_HEIGHT = "size.height"; //$NON-NLS-1$
	
	private static final String STORE_PINNED_STATE = "pinned";
	
	private static final String STORE_PROJECT = "project";
	
	private GradleProjectIndex index = new GradleProjectIndex();

	/**
	 * The name of the dialog store's section associated with the inplace XReference view.
	 */
	private final String sectionName = ConsoleInplaceDialog.class.getName();

	/**
	 * Fields for text matching and filtering
	 */
	private TasksViewer commandText;

	private Font statusTextFont;

	private IProject selectedProject = null; 
	  // Static is ok: only one instance exists.
	  // make static to remember last selection for new dialog

	/**
	 * Remembers the bounds for this information control.
	 */
	private Rectangle bounds;

	private Rectangle trim;

	/**
	 * Fields for view menu support.
	 */
	private ToolBar toolBar;

	private MenuManager viewMenuManager;

	private Label statusField;

	private boolean isDeactivateListenerActive = false;

	private Composite composite;
	private Composite line1, line2;

	private int shellStyle;

	private Listener deactivateListener;

	private Shell parentShell;

	private Shell dialogShell;

	private Label promptLabel;

	private Combo projectList;

	private Label projectLabel;
	
	private boolean isPinned;

	
	/**
	 * Constructor which takes the parent shell
	 */
	private ConsoleInplaceDialog(Shell parent) {
		parentShell = parent;
		shellStyle = SWT.RESIZE;
		isPinned = getDialogSettings().getBoolean(STORE_PINNED_STATE);
		initializeUI();
	}

	/**
	 * Open the dialog
	 */
	public void open() {
		if (dialogShell==null) {
			initializeUI();
		}
		dialogShell.open();
		dialogShell.forceActive();
	}

	private void initializeUI() {
		if (dialogShell != null) {
			close();
		}
		
		createContents();
		createShell();
		
		createComposites();
		
		// creates the drop down menu and creates the actions
		commandText = createCommandText(line1);
		createViewMenu(line1);
		
		createProjectList(line2);
		createStatusField(line2);
		
		// set the tab order
		line1.setTabList(new Control[] { commandText.getSourceViewer().getControl() });
		composite.setTabList(new Control[] { line1 });

		setInfoSystemColor();
		addListenersToShell();
		initializeBounds();
		restoreProject();
	}

    private void createShell() {
		// Create the shell
		dialogShell = new Shell(parentShell, shellStyle);
		dialogShell.setText(title);

		// To handle "ESC" case
		dialogShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent event) {
				event.doit = false; // don't close now
				dispose();
			}
		});

		Display display = dialogShell.getDisplay();
		dialogShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		int border = ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : 1;
		dialogShell.setLayout(new BorderFillLayout(border));

	}

	private void createComposites() {
		// Composite for filter text and tree
		composite = new Composite(dialogShell, SWT.RESIZE);
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		line1 = new Composite(composite, SWT.FILL);
		layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		line1.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(line1);

		createHorizontalSeparator(composite);
		
		line2 = new Composite(composite, SWT.FILL);
		layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		line2.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(line2);
	}

	private void createHorizontalSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void setInfoSystemColor() {
		Display display = dialogShell.getDisplay();

		// set the foreground colour
		commandText.getSourceViewer().getControl().setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		promptLabel.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		composite.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		line1.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		toolBar.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		statusField.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		line2.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

		// set the background colour
		commandText.getSourceViewer().getControl().setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		promptLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		composite.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		line1.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		toolBar.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		statusField.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		line2.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		projectLabel.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		//projectList.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	// --------------------- adding listeners ---------------------------

	private void addListenersToShell() {
		dialogShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (statusTextFont!=null) 
					statusTextFont.dispose();
				
				close();
				// dialogShell = null;
				composite = null;
				commandText = null;
				statusTextFont = null;
			}
		});

		deactivateListener = new Listener() {
			public void handleEvent(Event event) {
				if (isDeactivateListenerActive && !isPinned())
					dispose();
			}
		};

		dialogShell.addListener(SWT.Deactivate, deactivateListener);
		isDeactivateListenerActive = true;
		dialogShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellActivated(ShellEvent e) {
				if (e.widget == dialogShell && dialogShell.getShells().length == 0) {
					isDeactivateListenerActive = true;
					refreshProjects(); // force refresh of projects list
				}
			}
		});

		dialogShell.addControlListener(new ControlAdapter() {
			@Override
			public void controlMoved(ControlEvent e) {
				bounds = dialogShell.getBounds();
				if (trim != null) {
					Point location = composite.getLocation();
					bounds.x = bounds.x - trim.x + location.x;
					bounds.y = bounds.y - trim.y + location.y;
				}

			}

			@Override
			public void controlResized(ControlEvent e) {
				bounds = dialogShell.getBounds();
				if (trim != null) {
					Point location = composite.getLocation();
					bounds.x = bounds.x - trim.x + location.x;
					bounds.y = bounds.y - trim.y + location.y;
				}
			}
		});
	}

	// --------------------- creating and filling the menu
	// ---------------------------

	private void createViewMenu(Composite parent) {
		toolBar = new ToolBar(parent, SWT.FLAT);
		ToolItem viewMenuButton = new ToolItem(toolBar, SWT.PUSH, 0);

		GridData data = new GridData();
		data.horizontalAlignment = GridData.END;
		data.verticalAlignment = GridData.BEGINNING;
		toolBar.setLayoutData(data);

		viewMenuButton.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ELCL_VIEW_MENU));
		viewMenuButton.setDisabledImage(JavaPluginImages.get(JavaPluginImages.IMG_DLCL_VIEW_MENU));
		viewMenuButton.setToolTipText("Menu");
		viewMenuButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});
	}

	private void showViewMenu() {
		isDeactivateListenerActive = false;

		Menu aMenu = getViewMenuManager().createContextMenu(dialogShell);
		dialogShell.setMenu(aMenu);

		Rectangle bounds = toolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = dialogShell.toDisplay(topLeft);
		aMenu.setLocation(topLeft.x, topLeft.y);
		aMenu.addMenuListener(new MenuListener() {

			public void menuHidden(MenuEvent e) {
				isDeactivateListenerActive = true;
			}

			public void menuShown(MenuEvent e) {
			}
		});
		aMenu.setVisible(true);
	}

	private MenuManager getViewMenuManager() {
		if (viewMenuManager == null) {
			viewMenuManager = new MenuManager();
			fillViewMenu(viewMenuManager);
		}
		return viewMenuManager;
	}

	private void fillViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new GroupMarker("SystemMenuStart")); //$NON-NLS-1$
		viewMenu.add(new MoveAction());
		viewMenu.add(new ResizeAction());
		viewMenu.add(new PinDownAction());
		viewMenu.add(new RememberBoundsAction());
		viewMenu.add(new Separator("SystemMenuEnd")); //$NON-NLS-1$
	}

	// --------------------- creating and handling the project selection list
	
	private IProject getSelectedProject() {
		return selectedProject;
	}

	private String getSelectedProjectName() {
		if (selectedProject==null) 
			return null;
		else
			return selectedProject.getName();
	}
	
	private void createProjectList(Composite parent) {
		projectLabel = new Label(parent, SWT.FILL);
		projectLabel.setText("Project: ");
		smallFont(projectLabel);
		
		projectList = new Combo(parent, SWT.READ_ONLY);
		smallFont(projectList);
		GridDataFactory.swtDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(projectList);
		projectList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				handleProjectSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				handleProjectSelection();
			}
			private void handleProjectSelection() {
				if (!projectList.getText().equals(getSelectedProjectName())) {
					setSelectedProject(projectList.getText());
				}
			}
		});
		refreshProjects(); // force refresh of created projectList
	}

	private String[] getGradleProjectNames() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		java.util.List<String> names = new ArrayList<String>();
		for (IProject project : projects) {
			// Test if the selected project has Gradle nature
			if (isValidProject(project)) {
				names.add(project.getName());
			}
		}
		if (selectedProject!=null && !isValidProject(selectedProject)) {
			names.add(selectedProject.getName()); // make sure selected project is always added! or it won't show in the dropdown!
		}
		return names.toArray(new String[names.size()]);
	}

	private boolean isValidProject(IProject project) {
		try {
			return project!=null && project.hasNature(GradleNature.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	// ----------------- creating and filling the status field ----------

	private void createStatusField(Composite parent) {
		// Status field label
		statusField = new Label(parent, SWT.RIGHT);
		refreshStatusField();
		GridDataFactory.swtDefaults().grab(false, false).align(SWT.RIGHT, SWT.CENTER).applyTo(statusField);
		smallFont(statusField);
	}
	
	private String getStatusMessage() {
		if (selectedProject==null)
			return "Select a Gradle Project";
		if (!isValidProject(selectedProject))
			if (!selectedProject.exists()) 
				return "Selected project does not exist";
			else if (!selectedProject.isOpen())
				return "Selected project is not open";
			else {
				return "Selected project is not a Gradle project"; 
			}
		if (isPinned())
			return "Pinned: Press 'Esc' to close";
		else
			return "Type Gradle tasks and press 'Enter'";
	}

	private void refreshStatusField() {
		if (statusField!=null) {
			statusField.setText(getStatusMessage());
			statusField.getParent().layout();
		}
	}

	private void smallFont(Control widget) {
		if (statusTextFont==null) {
			Font font = widget.getFont();
			Display display = widget.getDisplay();
			FontData[] fontDatas = font.getFontData();
			for (FontData element : fontDatas) {
				element.setHeight(element.getHeight() * 9 / 10);
			}
			statusTextFont = new Font(display, fontDatas);
		}
		widget.setFont(statusTextFont);
	}
	
	// ----------- all to do with setting the bounds of the dialog -------------

	/**
	 * Initialize the shell's bounds.
	 */
	private void initializeBounds() {
		// if we don't remember the dialog bounds then reset
		// to be the defaults (behaves like inplace outline view)
		Rectangle oldBounds = restoreBounds();
		if (oldBounds != null) {
			Rectangle defaultBounds = getDefaultBounds();
			// Only use oldBounds if they are at least as large as the default
			if (oldBounds.width < defaultBounds.width) {
				oldBounds.width = defaultBounds.width;
				oldBounds.x = defaultBounds.x;
			}
			if (oldBounds.height < defaultBounds.height) {
				oldBounds.height = defaultBounds.height;
				oldBounds.y = defaultBounds.y;
			}
			dialogShell.setBounds(oldBounds);
			return;
		}
		dialogShell.setBounds(getDefaultBounds());
	}

	public Rectangle getDefaultBounds() {
		GC gc = new GC(composite);
		gc.setFont(composite.getFont());
		int width = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();

		Point size = dialogShell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Point location = getDefaultLocation(size);
		size.x = Math.max(size.x, 65*width); // At least space for 60 something chars
		return new Rectangle(location.x, location.y, size.x, size.y);
	}

	private Point getDefaultLocation(Point initialSize) {
		Rectangle monitorBounds = getMonitorBounds();
		Point centerPoint;
		if (parentShell != null) {
			centerPoint = Geometry.centerPoint(parentShell.getBounds());
		}
		else {
			centerPoint = Geometry.centerPoint(monitorBounds);
		}

		return new Point(centerPoint.x - (initialSize.x / 2), Math.max(monitorBounds.y, Math.min(centerPoint.y
				- (initialSize.y * 2 / 3), monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	private Rectangle getMonitorBounds() {
		Monitor monitor = dialogShell.getDisplay().getPrimaryMonitor();
		if (parentShell != null) {
			monitor = parentShell.getMonitor();
		}

		Rectangle monitorBounds = monitor.getClientArea();
		return monitorBounds;
	}

	/**
	 * @return A rectangle that is considered "valid" for the placement of command prompt window with
	 * restored location. 
	 */
	private Rectangle getValidBounds() {
		if (parentShell!=null) {
			return parentShell.getBounds();
		} else {
			return getMonitorBounds();
		}
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings settings = GradleUI.getDefault().getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings = GradleUI.getDefault().getDialogSettings().addNewSection(sectionName);

		return settings;
	}

	private void storeBounds() {
		IDialogSettings dialogSettings = getDialogSettings();

		boolean controlRestoresSize = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_SIZE);
		boolean controlRestoresLocation = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_LOCATION);

		if (bounds == null)
			return;

		if (controlRestoresSize) {
			dialogSettings.put(STORE_SIZE_WIDTH, bounds.width);
			dialogSettings.put(STORE_SIZE_HEIGHT, bounds.height);
		}
		if (controlRestoresLocation) {
			dialogSettings.put(STORE_LOCATION_X, bounds.x);
			dialogSettings.put(STORE_LOCATION_Y, bounds.y);
		}
	}

	private Rectangle restoreBounds() {

		IDialogSettings dialogSettings = getDialogSettings();

		boolean controlRestoresSize = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_SIZE);
		boolean controlRestoresLocation = !dialogSettings.getBoolean(STORE_DISABLE_RESTORE_LOCATION);

		Rectangle bounds = new Rectangle(-1, -1, -1, -1);

		if (controlRestoresSize) {
			try {
				bounds.width = dialogSettings.getInt(STORE_SIZE_WIDTH);
				bounds.height = dialogSettings.getInt(STORE_SIZE_HEIGHT);
			}
			catch (NumberFormatException ex) {
				bounds.width = -1;
				bounds.height = -1;
			}
		}

		if (controlRestoresLocation) {
			try {
				bounds.x = dialogSettings.getInt(STORE_LOCATION_X);
				bounds.y = dialogSettings.getInt(STORE_LOCATION_Y);
			}
			catch (NumberFormatException ex) {
				bounds.x = -1;
				bounds.y = -1;
			}
		}

		// sanity check
		if (bounds.x == -1 && bounds.y == -1 && bounds.width == -1 && bounds.height == -1) {
			return null;
		}

		if (!isValid(bounds)) {
			return null;
		} else {
			return bounds;
		}		
	}

	/**
	 * When restoring window coordinates persisted in preferences, this validity check is
	 * used to determine whether coordinates look reasonable enough to be reused 
	 * (to avoid having the prompt show up on the other, possibly turned off montitor) 
	 * <p>
	 * This implementation checks whether the bounds fall completely within the bounds
	 * of the 'parentShell' which is typically the Eclipse workbench window.
	 */
	private boolean isValid(Rectangle bounds) {
		Rectangle validBounds = getValidBounds();
		if (validBounds!=null && validBounds.intersects(bounds)) {
			//Must fall completely inside the valid bounds
			Rectangle intersection = validBounds.intersection(bounds);
			return intersection.width==bounds.width && intersection.height == bounds.height;
		}
		return false;
	}

	// ----------- all to do with filtering text

	private TasksViewer createCommandText(Composite parent) {
		promptLabel = new Label(parent, SWT.NONE);
		promptLabel.setText("Tasks:");
		GridData data = new GridData();
		data.verticalAlignment = GridData.CENTER;
		promptLabel.setLayoutData(data);

		commandText = new TasksViewer(parent, index, true);
		commandText.setDocument(new Document());
		
		FontData fontData = commandText.getSourceViewer().getTextWidget().getFont().getFontData()[0];
		fontData.setStyle(SWT.BOLD);
		promptLabel.setFont(new Font(parent.getDisplay(), fontData));

		data = new GridData(GridData.FILL_HORIZONTAL);
		GC gc = new GC(parent);
		gc.setFont(promptLabel.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		data.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.CENTER;
		commandText.getSourceViewer().getControl().setLayoutData(data);
		
		commandText.getSourceViewer().appendVerifyKeyListener(new VerifyKeyListener() {
			
			@Override
			public void verifyKey(VerifyEvent e) {
				if (e.character == SWT.ESC) { // ESC
					dispose();
					return;
				}

				if (e.doit && e.keyCode == 0x0D && isDeactivateListenerActive) { // return
					e.doit = false;
					IProject project = getSelectedProject();
					if (!isValidProject(project)) {
						openInformationMessage("Invalid Project ", getStatusMessage());
					} else {
						String taskStr = commandText.getSourceViewer().getDocument()
								.get();
						history.push(new CommandEntry(taskStr, project
								.getName()));
						final ILaunchConfiguration conf = GradleLaunchConfigurationDelegate
								.createDefault(
										GradleCore.create(project),
										taskStr, false);
						JobUtil.schedule(NO_RULE, new GradleRunnable(
								taskStr) {
							@Override
							public void doit(IProgressMonitor mon)
									throws Exception {
								conf.launch("run", mon, false, true);
							}
						});
						commandText.getSourceViewer().getDocument().set("");

						if (!isPinned()) dispose();
					}
					return;
				}
				
				if (e.doit && e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					showHistoryPopup();
					return;
				}
			}
			
		});
		
		commandText.getSourceViewer().getContentAssistantFacade().addCompletionListener(new ICompletionListener() {
			
			@Override
			public void assistSessionStarted(ContentAssistEvent event) {
				isDeactivateListenerActive = false;
			}
			
			@Override
			public void assistSessionEnded(ContentAssistEvent event) {
				isDeactivateListenerActive = true;
				dialogShell.setActive();
			}

			@Override
			public void selectionChanged(ICompletionProposal proposal,
					boolean smartToggle) {
				// nothing
			}
		});
		
		return commandText;
	}

	private void openInformationMessage(String title, String message) {
		Shell shell = dialogShell;
		if (shell.isDisposed())
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		isDeactivateListenerActive = false;
		try {
			MessageDialog.openInformation(shell, title, message);
		}
		finally {
			isDeactivateListenerActive = true;
		}
	}
	
	private boolean isPinned() {
		return isPinned;
	}

	private void setPinned(boolean pinit) {
		this.isPinned = pinit;
		refreshStatusField();
		getDialogSettings().put(STORE_PINNED_STATE, pinit); // Future dialogs start in same pinned state
	}

	/**
	 * Static inner class which sets the layout for the inplace view. Without this, the inplace view will not be
	 * populated.
	 * 
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl
	 */
	private static class BorderFillLayout extends Layout {

		/** The border widths. */
		final int fBorderSize;

		/**
		 * Creates a fill layout with a border.
		 */
		public BorderFillLayout(int borderSize) {
			if (borderSize < 0)
				throw new IllegalArgumentException();
			fBorderSize = borderSize;
		}

		/**
		 * Returns the border size.
		 */
		@SuppressWarnings("unused")
		public int getBorderSize() {
			return fBorderSize;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

			Control[] children = composite.getChildren();
			Point minSize = new Point(0, 0);

			if (children != null) {
				for (Control element : children) {
					Point size = element.computeSize(wHint, hHint, flushCache);
					minSize.x = Math.max(minSize.x, size.x);
					minSize.y = Math.max(minSize.y, size.y);
				}
			}

			minSize.x += fBorderSize * 2 + 3;
			minSize.y += fBorderSize * 2;

			return minSize;
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {

			Control[] children = composite.getChildren();
			Point minSize = new Point(composite.getClientArea().width, composite.getClientArea().height);

			if (children != null) {
				for (Control child : children) {
					child.setSize(minSize.x - fBorderSize * 2, minSize.y - fBorderSize * 2);
					child.setLocation(fBorderSize, fBorderSize);
				}
			}
		}
	}

	// ---------- shuts down the dialog ---------------

	/**
	 * Close the dialog
	 */
	public void close() {
		storeBounds();
		storeProject();
		toolBar = null;
		viewMenuManager = null;
	}

	public void dispose() {
		instance = null;
		if (commandText != null) {
			commandText.dispose();
		}
		if (promptLabel != null) {
			// dispose the label's font because it was created explicitly
			if (promptLabel.getFont().isDisposed()) {
				promptLabel.getFont().dispose();
			}
			promptLabel.dispose();
		}
		if (dialogShell != null) {
			if (!dialogShell.isDisposed())
				dialogShell.dispose();
			dialogShell = null;
			parentShell = null;
			composite = null;
		}
	}
	
	private void storeProject() {
		if (selectedProject != null) {
			getDialogSettings().put(STORE_PROJECT, selectedProject.getName());
		}
	}
	
	private void restoreProject() {
		if (selectedProject == null) {
			String projectName = getDialogSettings().get(STORE_PROJECT);
			if (projectName!=null) {
				setSelectedProject(projectName);
			}
		}
	}

	// ------------------ moving actions --------------------------

	/**
	 * Move action for the dialog.
	 */
	private class MoveAction extends Action {

		MoveAction() {
			super("&Move", IAction.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			performTrackerAction(SWT.NONE);
			isDeactivateListenerActive = true;
		}

	}

	/**
	 * Remember bounds action for the dialog.
	 */
	private class RememberBoundsAction extends Action {

		RememberBoundsAction() {
			super("Remember Size and &Location", IAction.AS_CHECK_BOX);
			setChecked(!getDialogSettings().getBoolean(STORE_DISABLE_RESTORE_LOCATION));
		}

		@Override
		public void run() {
			IDialogSettings settings = getDialogSettings();

			boolean newValue = !isChecked();
			// store new value
			settings.put(STORE_DISABLE_RESTORE_LOCATION, newValue);
			settings.put(STORE_DISABLE_RESTORE_SIZE, newValue);

			isDeactivateListenerActive = true;
		}
	}

	/**
	 * Resize action for the dialog.
	 */
	private class ResizeAction extends Action {

		ResizeAction() {
			super("&Resize", IAction.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			performTrackerAction(SWT.RESIZE);
			isDeactivateListenerActive = true;
		}

	}

	/**
	 * Resize action for the dialog.
	 */
	private class PinDownAction extends Action {

		PinDownAction() {
			super("&Pin", IAction.AS_CHECK_BOX);
			setChecked(isPinned());
		}

		@Override
		public void run() {
			setPinned(isChecked());
		}

	}
	
	/**
	 * Perform the requested tracker action (resize or move).
	 * 
	 * @param style The track style (resize or move).
	 */
	private void performTrackerAction(int style) {
		Tracker tracker = new Tracker(dialogShell.getDisplay(), style);
		tracker.setStippled(true);
		Rectangle[] r = new Rectangle[] { dialogShell.getBounds() };
		tracker.setRectangles(r);
		isDeactivateListenerActive = false;
		if (tracker.open()) {
			dialogShell.setBounds(tracker.getRectangles()[0]);
			isDeactivateListenerActive = true;
		}
	}

	// -------------------- all to do with the contents of the view  ------------------

	private void createContents() {
	}

	public void setSelectedProject(IProject project) {
		index.setProject(project == null || !project.exists() ? null : GradleCore.create(project));
		selectedProject = project;
		refreshProjects();
	}

	/**
	 * Refresh the list of gradle projects and put them into the projectList widget.
	 */
	private void refreshProjects() {
		if (projectList!=null) {
			String[] projectNames = getGradleProjectNames();
			projectList.setItems(projectNames);
			if (getSelectedProject()==null) {
				projectList.setText("");
			}
			if (getSelectedProject()!=null) {
				projectList.setText(selectedProject.getName());
			}
			projectList.getParent().layout();
			refreshStatusField();
		}
	}

	private void setSelectedProject(String projectName) {
		setSelectedProject(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
	}
	
	public boolean isOpen() {
		return dialogShell != null;
	}

	public static void closeIfNotPinned() {
		if (instance!=null && !instance.isPinned())
			instance.dispose();
	}
	
	private void showHistoryPopup() {
		if (history.isEmpty()) {
			return;
		}
		PopupTable<CommandEntry> popup = new PopupTable<CommandEntry>(dialogShell, SWT.ON_TOP);
		List<CommandEntryDescriptor> descriptors = new ArrayList<CommandEntryDescriptor>(history.size());
		for (CommandEntry commandEntry : history) {
			descriptors.add(new CommandEntryDescriptor(commandEntry));
		}
		popup.setData(descriptors);
		isDeactivateListenerActive = false;
		SourceViewer viewer = commandText.getSourceViewer();
		Control viewerControl = viewer.getControl();
		IDocument document = viewer.getDocument();
		CommandEntry commandEntry = popup.open(viewerControl
				.getDisplay()
				.map(viewerControl.getParent(), null,
						viewerControl.getBounds()));
		dialogShell.setFocus();
		isDeactivateListenerActive = true;
		if (commandEntry != null) {
			try {
				document.replace(0, document.getLength(), commandEntry.command);
				viewer.setSelectedRange(commandEntry.command.length(), 0);
			} catch (BadLocationException e) {
				// ignore
			}
		}
	}
	
	private class CommandEntryDescriptor implements IItemDescriptor<CommandEntry> {
		
		CommandEntry commandEntry;
		StyledString styledString;
		
		public CommandEntryDescriptor(CommandEntry commandEntry) {
			this.commandEntry = commandEntry;
			this.styledString = new StyledString();
			this.styledString.append(this.commandEntry.command);
			this.styledString.append(' ');
			this.styledString.append("[" + this.commandEntry.project + "]", StyledString.DECORATIONS_STYLER);
		}
		
		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getDisplayString() {
			return styledString.getString();
		}

		@Override
		public StyledString getStyledDisplayString() {
			return styledString;
		}

		@Override
		public CommandEntry getData() {
			return commandEntry;
		}
		
	}
	
	private class CommandEntry {
		String project;
		String command;
		public CommandEntry(String command, String project) {
			this.command = command;
			this.project = project;
		}
	}

}
