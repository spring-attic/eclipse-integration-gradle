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
package org.springsource.ide.eclipse.gradle.core.test.util;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Progress monitor that dumps progress info onto system out. Useful for testing
 * code to get some idea of what is going on.
 * 
 * @author Kris De Volder
 */
public class LoggingProgressMonitor implements IProgressMonitor {

	public void beginTask(String name, int totalWork) {
		System.out.println("begin task: "+name+" work = "+totalWork);
	}

	public void done() {
		System.out.println("\ndone");
	}

	public void internalWorked(double work) {
		System.out.print('.');
	}

	public boolean isCanceled() {
		return false;
	}

	public void setCanceled(boolean value) {
	}

	public void setTaskName(String name) {
		System.out.println("task = "+name);
	}

	public void subTask(String name) {
		System.out.println("subTask = "+name);
	}

	public void worked(int work) {
		System.out.print('.');
	}

}
