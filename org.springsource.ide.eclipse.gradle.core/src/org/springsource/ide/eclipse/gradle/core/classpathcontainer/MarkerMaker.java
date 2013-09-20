/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.classpathcontainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.slf4j.Marker;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.BusyStatus;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.Joinable;


/**
 * Class responsible for creating a certain type of marker on a GradleProject.
 * <p>
 * It is assumed that all markers added to this MarkerMaker replace any
 * existing markers of the same type.
 * 
 * Usage:
 * 
 * void computeSomestuff() {
 *     MarkerMaker markers = new MarkerMaker(project, MARKER_ID);
 *     try {
 *     ...
 *       if (detectedAnError) {
 *          addError(...error message...); //queues up a marker to add
 *       }
 *     } finally {
 *        markers.schedule(); //schedules job that will actually add the markers (and remove old markers)
 *     }
 *  }
 * 
 * @author Kris De Volder
 */
public class MarkerMaker extends GradleRunnable implements Joinable<Void> {
	
	public static BusyStatus busy = new BusyStatus(MarkerMaker.class.getName());
	
	private final class AddMarker extends GradleRunnable {
		private final String msg;
		private final int severity;

		private AddMarker(String jobName, String msg, int severity) {
			super(jobName);
			this.msg = msg;
			this.severity = severity;
		}

		@Override
		public void doit(IProgressMonitor mon) throws Exception {
			IProject p = gp.getProject();
			if (p!=null) {
				IMarker m = p.createMarker(markerType);
				//m.setAttribute(IMarker.LINE_NUMBER, line);
				m.setAttribute(IMarker.MESSAGE, msg);
				m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				m.setAttribute(IMarker.SEVERITY, severity);
			}
		}
	}

	private GradleProject gp;
	private List<GradleRunnable> work = new ArrayList<GradleRunnable>();
	private String markerType;

	public MarkerMaker(GradleProject project, String markerType) {
		super("Updating markers for "+project.getDisplayName());
		this.gp = project;
		this.markerType = markerType;
		busy.start();
	}
	
	@Override
	public void doit(IProgressMonitor mon) throws Exception {
		try {
			mon.beginTask(jobName, 1+work.size());
			IProject project = gp.getProject();
			if (project!=null) { //Safeguard against project has been deleted
				project.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
				mon.worked(1);
				for (GradleRunnable w : work) {
					w.run(new SubProgressMonitor(mon, 1));
				}
			}
		} finally {
			mon.done();
			busy.stop();
		}
	}

	public void schedule() {
		JobUtil.schedule(this);
	}

	public void reportWarning(final String msg) {
		work.add(new AddMarker("add warning", msg, IMarker.SEVERITY_WARNING));
	}

	public void reportError(String msg) {
		work.add(new AddMarker("add warning", msg, IMarker.SEVERITY_ERROR));
	}

	public Void join() throws Exception {
		return null;
	}
	
}
