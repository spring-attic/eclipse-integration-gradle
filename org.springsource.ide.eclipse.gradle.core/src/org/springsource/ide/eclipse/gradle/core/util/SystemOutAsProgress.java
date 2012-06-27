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
///******************************************************************************************
// * Copyright (c) 2011 SpringSource, a division of VMware, Inc. All rights reserved.
// ******************************************************************************************/
//package org.springsource.ide.eclipse.gradle.core.util;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintStream;
//
//import org.eclipse.core.runtime.Assert;
//import org.eclipse.core.runtime.IProgressMonitor;
//
///**
// * Helper class to turn system.out output into subtasks for a progress monitor. 
// * Any line of text sent to system out will be forwarded as a subtask title to
// * the progress monitor.
// * <p>
// * Typical usage:
// * 
// * <code>
// *   IProgressMonitor monitor = ...
// *   monitor.beginTask(...);
// *   try {
// *      SystemOutAsProgress.startCapture(monitor);
// *      try {
// *         ... call somehing that produces output on system out
// *      } finally {
// *          SystemOutAsProgress.endCapture(monitor);
// *      }
// *   } finally {
// *      monitor.done();
// *   }
// *   ...
// *   
// * </code>
// * 
// * @author Kris De Volder
// */
//public class SystemOutAsProgress extends OutputStream {
//	
//	private static final boolean ENABLED = false;
//	
//	private static PrintStream System_out = null;
//	
//	private static SystemOutAsProgress activeInstance = null;
//	
//	/**
//	 * We only create one instance and keep reusing it. This is not just for efficiency, but also because
//	 * some stuff might be caching the instance
//	 */
//	private static SystemOutAsProgress reusableInstance = null;
//
//	/**
//	 * @param monitor
//	 */
//	public synchronized static void startCapture(IProgressMonitor monitor) {
//		if (ENABLED) {
//			System_out = System.out;
//			Assert.isLegal(activeInstance==null, "At most one SystemOutAsProgress capture can be active at any time");
//			activeInstance = getInstance(monitor);
//			System.setOut(new PrintStream(activeInstance));
//		}
//	}
//
//	private static SystemOutAsProgress getInstance(IProgressMonitor monitor) {
//		if (reusableInstance==null) {
//			reusableInstance = new SystemOutAsProgress();
//		}
//		reusableInstance.init(monitor);
//		return reusableInstance;
//	}
//
//	public synchronized static void endCapture() {
//		if (ENABLED) {
//			Assert.isLegal(activeInstance!=null, "No SystemOutAsProgress capture is currently active!");
//			activeInstance.dispose();
//			activeInstance = null;
//			System.setOut(System_out);
//			//Note: not setting System_out to null because if someone has cached our instance they might still be sending us
//			//text and we will keep sending it to System_out
//		}
//	}
//
//	private void dispose() {
//		monitor = null; //Disables the instance, can still be reused!
//	}
//
//	private void init(IProgressMonitor monitor) {
//		this.line = new StringBuffer();
//		this.monitor = monitor;
//	}
//	
//	private IProgressMonitor monitor;
//	StringBuffer line = null;
//
//	@Override
//	public void write(int b) throws IOException {
//		System_out.write(b);
//		IProgressMonitor monitor = this.monitor; // thread safe without synchronisation
//		if (monitor!=null) {
//			char c = (char) b;
//			if (c=='\n') {
//				monitor.subTask(line.toString());
//				line = new StringBuffer();
//			} else if (c=='\r') {
//				//Skip (windoze)
//			} else {
//				line.append(c);
//			}
//		}
//	}
//
//}
