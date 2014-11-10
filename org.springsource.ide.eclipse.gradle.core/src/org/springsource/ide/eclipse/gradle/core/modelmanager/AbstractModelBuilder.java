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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Convenient abstract class to implement {@link ModelBuilder}. 
 * 
 * @author Kris De Volder
 */
public abstract class AbstractModelBuilder implements ModelBuilder {
	
	/**
	 * Note that multiple build request could happen concurrently. A specific builder implementation may or may not 
	 * want to add 'synchronized' modifier when overriding this method. This really depends on whether the build
	 * machinery wants/can support concurrent builds or not.
	 * <p>
	 * Note that the modelmanager makes a 'best effort' attempt to satisfy multiple concurrent build requests 
	 * on the modelmanager efficiently. I.e. if it can be determined that two requests are really asking for
	 * the same model than the build manager will only forward a single build request to the model builder and
	 * attempt to use its result to satisfy both requests. 
	 * <p>
	 * In some scenarios this 'best effort' may fail. So implementors must assume the worst and either 
	 * ensure their implementation allows for concurrent builds or provide a synchronization mechanism.
	 */
	protected abstract <T> T doBuild(GradleProject project, Class<T> type, final IProgressMonitor mon) throws Exception;
	
	public final <T> BuildResult<T> buildModel(GradleProject project, Class<T> type, final IProgressMonitor mon) {
		try {
			return new BuildResult<T>(type, doBuild(project, type, mon));
		} catch (Throwable e) {
			 return new BuildResult<T>(type,e);
		}
	}
	
}
