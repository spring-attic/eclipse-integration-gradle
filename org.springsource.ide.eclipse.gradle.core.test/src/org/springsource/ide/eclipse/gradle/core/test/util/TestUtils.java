/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;

/**
 * @author Kris De Volder
 */
public class TestUtils {

	public static File createTempDirectory() throws IOException {
		final File temp;
		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
		if(!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}
		if(!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return temp;
	}
	
	public static File createTempDirectory(String name) throws IOException {
		File tmpParent = createTempDirectory();
		File temp = new File(tmpParent, name);
		if(!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return temp;
	}

	public static void assertNoErrors(IProject project) throws CoreException {
		setAutoBuilding(false);
		project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForManualBuild();
		waitForAutoBuild();

		IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		for (IMarker problem : problems) {
			if (problem.getAttribute(IMarker.SEVERITY, 0) >= IMarker.SEVERITY_ERROR) {
				Assert.fail("Expecting no problems but found: " + markerMessage(problem));
			}
		}
	}
	
	public static String markerMessage(IMarker m) throws CoreException {
		StringBuffer msg = new StringBuffer("Marker {\n");
		final Map attributes = m.getAttributes();
		IResource rsrc = m.getResource();
		msg.append("   rsrc = " + (rsrc == null ? "unknown" : rsrc.getFullPath() + "\n"));
		for (Object atrName : attributes.keySet()) {
			msg.append("   " + atrName + " = " + attributes.get(atrName) + "\n");
		}
		msg.append("}");
		if (rsrc != null) {
			if (rsrc.getType() == IResource.FILE) {
				IFile file = (IFile) rsrc;
				if (isGroovyOrJava(file)) {
					InputStream content = file.getContents();
					if (content != null) {
						try {
							msg.append(">>>>>>>>> " + file.getFullPath() + "\n");
							BufferedReader reader = new BufferedReader(new InputStreamReader(content));
							String line = reader.readLine();
							int lineNumber = 1;
							while (line != null) {
								msg.append(String.format("%3d", lineNumber++) + ": " + line);
								line = reader.readLine();
							}
						}
						catch (IOException e) {
							msg.append("error reading file: (" + e.getClass().getName() + ") " + e.getMessage());
						}
						finally {
							msg.append("<<<<<<<<< " + file.getFullPath() + "\n");
							if (content != null) {
								try {
									content.close();
								}
								catch (IOException e) {
								}
							}
						}
					}
				}
			}
		}
		return msg.toString();
		// return m.getAttribute(IMarker.MESSAGE, "") + " line: " +
		// m.getAttribute(IMarker.LINE_NUMBER, "unknown")
		// + " location: " + m.getAttribute(IMarker.LOCATION, "unknown");
	}
	
	public static void setAutoBuilding(boolean enabled) throws CoreException {
		IWorkspaceDescription wsd = ResourcesPlugin.getWorkspace().getDescription();
		if (!wsd.isAutoBuilding() == enabled) {
			wsd.setAutoBuilding(enabled);
			ResourcesPlugin.getWorkspace().setDescription(wsd);
		}
	}

	public static void waitForJobFamily(Object jobFamily) {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(jobFamily, null);
				wasInterrupted = false;
			}
			catch (OperationCanceledException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}
	
	public static void waitForManualBuild() {
		waitForJobFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
	}
	
	public static void waitForAutoBuild() {
		waitForJobFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
	}
	
	/**
	 * Allows Display to process events, so UI can make progress. Tests running
	 * in the UI thread may need to call this to avoid UI deadlocks.
	 * <p>
	 * For convenience, it is allowed to call this method from a non UI thread,
	 * but such calls have no effect.
	 */
	public static void waitForDisplay() {
		if (inUIThread()) {
			while (Display.getDefault().readAndDispatch()) {
				// do nothing
			}
		}
	}
	
	public static boolean inUIThread() {
		return Display.getDefault().getThread() == Thread.currentThread();
	}
	
	private static boolean isGroovyOrJava(IFile file) {
		String ext = file.getFileExtension();
		return "groovy".equals(ext) || "java".equals(ext);
	}
	
	public static StringBuffer getStackDumps() {
		StringBuffer sb = new StringBuffer();
		Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
			sb.append(entry.getKey().toString());
			sb.append("\n");
			for (StackTraceElement element : entry.getValue()) {
				sb.append("  ");
				sb.append(element.toString());
				sb.append("\n");
			}
			sb.append("\n");
		}
		return sb;
	}
}
