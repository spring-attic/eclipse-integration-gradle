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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * Default implementation of ModelBuilder. Delegates to Gradle tooling API.
 * 
 * @author Kris De Volder
 */
public class DefaultModelBuilder extends AbstractModelBuilder {
	
	@Override
	public <T> T doBuild(GradleProject project, Class<T> type, IProgressMonitor mon) throws CoreException {
		mon.beginTask(jobName(project, type), 10);
		try {
			return ToolinApiUtils.buildModel(project, type, new SubProgressMonitor(mon, 9));
		} catch (CoreException e) {
			if (ExceptionUtil.getDeepestCause(e).getClass().getName().equals("java.lang.InterruptedException")) {
				//WTF: someone throws this spurriously aborting the build... don't want that, so try again and
				// don't fail my tests because of this!
				return ToolinApiUtils.buildModel(project, type, new SubProgressMonitor(mon, 1));
			}
			throw e;
		}
		finally {
			mon.done();
		}
	}
	
	public static <T> String jobName(GradleProject project, Class<T> requiredType) {
		return "Build '"+requiredType.getSimpleName()+"' model for '"+project.getDisplayName();
	}

}
