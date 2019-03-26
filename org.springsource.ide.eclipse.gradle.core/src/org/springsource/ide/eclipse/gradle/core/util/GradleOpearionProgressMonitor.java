/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.gradle.tooling.CancellationToken;

/**
 * Progress monitor containing the Gradle operation cancellation token
 * 
 * @author aboyko
 *
 */
public class GradleOpearionProgressMonitor extends ProgressMonitorWrapper {
	
	private CancellationToken cancellationToken;
	
	public GradleOpearionProgressMonitor(IProgressMonitor monitor, CancellationToken cancellationToken) {
		super(monitor);
		this.cancellationToken = cancellationToken;
	}

	public CancellationToken getCancellationToken() {
		return cancellationToken;
	}
	
	/**
	 * Find the cancellation 
	 * 
	 * @param monitor the progress monitor
	 * @return
	 */
	public static CancellationToken findCancellationToken(IProgressMonitor monitor) {
		GradleOpearionProgressMonitor gradleOpMon = findGradleOperationMonitor(monitor);
		return gradleOpMon == null ? null : gradleOpMon.getCancellationToken();
	}
	
	private static GradleOpearionProgressMonitor findGradleOperationMonitor(IProgressMonitor m) {
		for (IProgressMonitor itr = m; itr instanceof ProgressMonitorWrapper; itr = ((ProgressMonitorWrapper)itr).getWrappedProgressMonitor()) {
			if (itr instanceof GradleOpearionProgressMonitor) {
				return (GradleOpearionProgressMonitor) itr;
			}
		}
		return null;
	}

}
