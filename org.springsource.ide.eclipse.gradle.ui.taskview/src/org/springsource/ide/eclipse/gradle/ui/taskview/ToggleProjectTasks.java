package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.jface.action.Action;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;

public class ToggleProjectTasks extends Action {
	
	private GradleTasksView owner;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleProjectTasks(GradleTasksView owner, boolean on) {
		super(null, AS_CHECK_BOX);
		this.owner = owner;
		setChecked(on);
		setDescription("Display Local Tasks");
		setToolTipText("Displays tasks only defined in this project excluding subprojects");
		setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_PROJECT_FOLDER));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		owner.setDisplayProjectLocalTasks(isChecked());
	}
	

}
