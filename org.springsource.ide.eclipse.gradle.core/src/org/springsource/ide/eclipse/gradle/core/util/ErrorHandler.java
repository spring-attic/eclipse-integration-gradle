/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * In contexts where we wish to collect certain errors and continue processing rather than abort
 * immediately, an instance of this class is passed in as an argument.
 * <p>
 * Concrete implementation can handle the errors in different ways (most commonly loggin them
 * to some type of error log).
 * 
 * @author Kris De Volder
 */
public abstract class ErrorHandler {
	
	/**
	 * @author Kris De Volder
	 */
	public static abstract class FilterDuplicates extends ErrorHandler {
		
		private Set<String> messagesSeen = new HashSet<String>();
		
		@Override
		final protected void internalHandle(int severity, Throwable e) {
			String msg = e.getMessage();
			if (msg==null || !messagesSeen.contains(msg)) {
				if (msg!=null) {
					messagesSeen.add(msg);
				}
				noDuplicateHandle(severity, e);
			}
		}

		protected abstract void noDuplicateHandle(int severity, Throwable e);

	}

	/**
	 * Error handler that logs all exceptions into the Eclipse error log and 
	 * rethrows the first Error (warnings are only logged).
	 */
	private static class LogToEclipseErrorLog extends FilterDuplicates {

		@Override
		protected void noDuplicateHandle(int severity, Throwable e) {
			GradleCore.log(ExceptionUtil.status(severity, e));
		}

	}

	private IStatus firstError = null;

	public void handle(int severity, Throwable e) {
		if (firstError==null && severity>=IStatus.ERROR) {
			firstError = ExceptionUtil.status(severity, e);
		}
		internalHandle(severity, e);
	}
	
	/**
	 * Convenient shortcut for handling an exception with severity 'error'.
	 */
	public void handleError(Throwable e) {
		handle(IStatus.ERROR, e);
	}
	
	public boolean hasErrors() {
		return firstError!=null;
	}
	
	public void rethrowAsCore() throws CoreException {
		if (hasErrors()) {
			throw ExceptionUtil.coreException(firstError);
		}
	}
	
	/**
	 * Error handler that records all errors so they can be retrieved and reported
	 * later on.
	 */
	private static class RecordAll extends FilterDuplicates {
		
		private List<IStatus> recorded = new ArrayList<IStatus>();
		
		@Override
		public void rethrowAsCore() throws CoreException {
			if (hasErrors()) {
				if (recorded.size()==1) {
					throw ExceptionUtil.coreException(recorded.get(0));
				} else {
					StringBuffer msg = new StringBuffer("Multiple problems: \n");
					IStatus[] children = new IStatus[recorded.size()];
					int i = 0;
					for (IStatus e : recorded) {
						if (i<3) {
							msg.append(e.getMessage()+"\n");
						} else if (i==3) {
							msg.append("...more errors...");
						}
						children[i++] = e;
					}
					
					IStatus status = new MultiStatus(GradleCore.PLUGIN_ID, 666, children, msg.toString(), null);
					throw new CoreException(status);
				}
			}
		}
		
		@Override
		protected void noDuplicateHandle(int severity, Throwable e) {
			recorded.add(ExceptionUtil.status(severity, e));
		}
//
//		public boolean hasErrorsOrWarnings() {
//			return !recorded.isEmpty();
//		}
	}
	
	/**
	 * Default implementation to use in context where one really only cares about the first error.
	 * All other errors are silently discarded.
	 */
	public static class KeepFirst extends ErrorHandler {
		@Override
		protected void internalHandle(int severity, Throwable e) {
		}
	};
	
	/**
	 * Error handler for use in (most) unit tests. Any error/warning raised is rethrown immediately as an unchecked exception
	 */
	public static class Test extends ErrorHandler {
		
		private int rethrowSeverity; //Determines severity threshold for re-throwing. Anything lower is ignored.
		
		/**
		 * Rethrows anything with severity 'warning' or worse. Ignores any other exceptions.
		 */
		public Test() {
			this(IStatus.WARNING);
		}

		/**
		 * Create a test error handler that will immediately rethrow any received exception
		 * with severity greater or equal to given severity and ignore any other exceptions.
		 */
		public Test(int rethrowSeverity) {
			this.rethrowSeverity = rethrowSeverity;
		}

		@Override
		protected void internalHandle(int severity, Throwable e) {
			if (severity>=rethrowSeverity) {
				if (e instanceof Error) {
					throw (Error)e;
				} else {
					throw new Error(e);
				}
			}
		}

	}
	
	protected abstract void internalHandle(int severity, Throwable e);

	@Override
	public final String toString() {
		if (!hasErrors()) {
			return "*no-errors*";
		} else {
			return firstError.toString();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/// Methods below are to keep error handler policy decisions all together in one place.
	
	/**
	 * @return ErrorHandler used by "Refresh Source Folders" menu command.
	 */
	public static ErrorHandler forRefreshSourceFolders() {
		//return new RecordAll();
		return new LogToEclipseErrorLog();
	}

	/**
	 * @return ErrorHandler used by "Refresh All" menu command.
	 */
	public static ErrorHandler forRefreshAll() {
		//return new RecordAll();
		return new LogToEclipseErrorLog();
	}

	/**
	 * @return ErrorHandler used by import wizard when it performs the import operation.
	 */
	public static ErrorHandler forImportWizard() {
		return new RecordAll();
		//return new LogToEclipseErrorLog();
	}

	public static ErrorHandler forEnableDisableDSLD() {
		return new LogToEclipseErrorLog();
	}
	
}
