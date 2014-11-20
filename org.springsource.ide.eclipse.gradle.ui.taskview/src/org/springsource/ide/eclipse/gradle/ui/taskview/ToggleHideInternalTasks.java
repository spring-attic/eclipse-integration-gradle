package org.springsource.ide.eclipse.gradle.ui.taskview;

import org.eclipse.jface.action.Action;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;

public class ToggleHideInternalTasks extends Action {

	private GradleTasksView owner;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleHideInternalTasks(GradleTasksView owner, boolean on) {
		super(null, AS_CHECK_BOX);
		this.owner = owner;
		setChecked(on);
		setDescription("Hide Internal Tasks");
		setToolTipText("Hides internal tasks (tasks with null 'group' property");
		setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_FILTER_INTERNAL_TASKS));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		owner.setHideInternalTasks(isChecked());
	}
}
