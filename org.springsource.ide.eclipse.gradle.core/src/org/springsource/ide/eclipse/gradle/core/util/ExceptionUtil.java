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
package org.springsource.ide.eclipse.gradle.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.gradle.tooling.BuildException;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.InconsistenProjectHierarchyException;


/**
 * Utility methods to convert exceptions into other types of exceptions, status objects etc.
 * 
 * @author Kris De Volder
 */
public class ExceptionUtil {
	
	public static CoreException coreException(int severity, String msg) {
		return coreException(status(severity, msg));
	}

	public static CoreException coreException(IStatus status) {
		Throwable e = status.getException();
		if (e==null) {
			return new CoreException(status);
		} else if (e instanceof CoreException) {
 			return (CoreException) e;
		}
		return new CoreException(status);
	}

	public static CoreException coreException(String msg) {
		return coreException(IStatus.ERROR, msg);
	}

	public static CoreException coreException(Throwable e) {
		if (e instanceof CoreException) {
			return (CoreException) e;
		} else {
			return coreException(status(e));
		}
	}

	public static Throwable getDeepestCause(Throwable e) {
		Throwable cause = e;
		Throwable parent = e.getCause();
		while (parent!=null && parent!=e) {
			cause = parent;
			parent = cause.getCause();
		}
		return cause;
	}

	public static String getMessage(Throwable e) {
		//The message of nested exception is usually more interesting than the one on top.
		Throwable cause = getDeepestCause(e);
		String msg = cause.getMessage();
		if (msg!=null) {
			return msg;
		}
		return e.getMessage();
	}

	/**
	 * This exception is raised when a project is not properly connected to its 
	 * 'root' project, which will result in the inability to fetch a model
	 * for the project.
	 */
	public static InconsistenProjectHierarchyException inconsistentProjectHierachy(GradleProject project) {
		return new InconsistenProjectHierarchyException(status("Gradle project hierarchy is inconsistent for '"+project.getDisplayName()+"'"));
	}

	public static IllegalStateException notImplemented(String string) {
		return new IllegalStateException("Not implemented: "+string);
	}

	public static IStatus status(int severity, String msg) {
		return new Status(severity, GradleCore.PLUGIN_ID, msg);
	}

	public static IStatus status(Throwable e) {
		return status(isCancelation(e)?IStatus.CANCEL:IStatus.ERROR, e);
	}
	
	public static IStatus status(int severity, Throwable e) {
		if (e instanceof CoreException) {
			IStatus status = ((CoreException) e).getStatus();
			if (status!=null && status.getSeverity()==severity) {
				Throwable ee = status.getException();
				if (ee!=null) {
					return status;
				}
			}
		}
		if (e instanceof BuildException) {
			String msg = getMessage(e);
			if (msg.contains("Task 'cleanEclipse' not found")
			|| msg.contains("Task 'eclipse' not found")) {
				e = new Gradle1792BugException(e);
			}
		}
		return new Status(severity, GradleCore.PLUGIN_ID, getMessage(e), e);
	}

	public static IStatus status(String msg) {
		return status(IStatus.ERROR, msg);
	}

	public static final IStatus OK_STATUS = status(IStatus.OK, "");

	public static boolean isCancelation(Throwable e) {
		if (e instanceof CoreException) {
			return ((CoreException) e).getStatus().getSeverity()==IStatus.CANCEL;
		}
		Throwable cause = getDeepestCause(e);
		return cause instanceof OperationCanceledException 
// Warning: these instanceof test don't work, some classloader identity thing?
//			|| cause instanceof org.gradle.api.BuildCancelledException 
//			|| cause instanceof org.gradle.tooling.BuildCancelledException
// so test based on classname:
			|| cause.getClass().getSimpleName().equals("BuildCancelledException")
			|| cause instanceof InterruptedException;
	}
	
}
