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
package org.springsource.ide.eclipse.gradle.core.dsld;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;
import org.springsource.ide.eclipse.gradle.core.ClassPath;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleNature;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.NatureUtils;


/**
 * An instance of this class provides DSLD support related functions. So far none of this functionality really has
 * a compile-time dependency on Greclipse and should work regardless of whether Greclipse is installed.
 * <p>
 * However, it doesn't make much sense to enable this functionality without Greclipse installed since it will not
 * do anything usefull without it. (E.g. the items that are added to the classpath are not meaningfull without
 * Greclipse installed.
 * 
 * @author Kris De Volder
 */
public class DSLDSupport {

	public static final String GROOVY_LIBS_CONTAINER = "GROOVY_SUPPORT";
    public static final String GROOVY_DSL_CONTAINER = "GROOVY_DSL_SUPPORT";
	public static final String GROOVY_NATURE = "org.eclipse.jdt.groovy.core.groovyNature";
	static final String ASPECTJ_NATURE = "org.eclipse.ajdt.ui.ajnature";

	private Boolean haveGreclipse = null;
	private static DSLDSupport instance;
	
	/**
	 * Adds the DSLD support to given project if all required preconditions are satisfied. Otherwise... do nothing.
	 */
	public static void maybeAdd(GradleProject p, ErrorHandler eh, IProgressMonitor monitor) {
		monitor.beginTask("Enable DSLD", 1);
		try {
			DSLDSupport enableDSLD = DSLDSupport.getInstance();
			if (enableDSLD!=null && enableDSLD.haveGreclipse() && p.getRefreshPreferences().getEnableDSLD()) {
				try {
					enableDSLD.enableFor(p, true, new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					//Treat as 'less' severe error
					//DSLD support is not crucial for correct operation, so handle error and continue
					eh.handle(e.getStatus().getSeverity(), e);
				}
			}
		} finally {
			monitor.done();
		}
		
	}
	
	/**
	 * Singleton: use getInstance()
	 */
	private DSLDSupport() {
	}

	/**
	 * Checks that all the needed bits and pieces for Gradle DSLD support are present in a given project.
	 */
	public boolean isEnabled(GradleProject gp) {
		try {
			IProject p = gp.getProject();
			if (p!=null) {
				IJavaProject jp = JavaCore.create(p);
				if (GradleNature.hasNature(p) && p.hasNature(JavaCore.NATURE_ID)) {
					return GradleDSLDClasspathContainer.isOnClassPath(jp)
							&& p.hasNature(GROOVY_NATURE);
//							&& ClassPath.isContainerOnClasspath(jp, GROOVY_LIBS_CONTAINER);
				}
			}
		} catch (CoreException e) {
			GradleCore.log(e);
		}
		return false;
	}
	
	public void enableFor(GradleProject gp, boolean enable, IProgressMonitor monitor) throws CoreException {
		if (enable) {
			//enable it: add DSL container and its prerequisites
			monitor.beginTask("Enable Gradle DSL Support", 3);
			try {
				IProject project = gp.getProject();
				IStatus status = verifyPrereqsForEnablement(project);
				if (!status.isOK()) {
					throw ExceptionUtil.coreException(status);
				} else {  //OK status
					NatureUtils.ensure(project, new SubProgressMonitor(monitor, 1), 
							GradleNature.NATURE_ID,
							JavaCore.NATURE_ID,
							GROOVY_NATURE
							);
					GradleDSLDClasspathContainer.addTo(gp.getJavaProject(), new SubProgressMonitor(monitor, 1));
                    ClassPath cp = gp.getClassPath();
					boolean added = false;
					if (!ClassPath.isContainerOnClasspath(gp.getJavaProject(), GROOVY_LIBS_CONTAINER)) {
						cp.add(JavaCore.newContainerEntry(new Path(GROOVY_LIBS_CONTAINER), false));
						added = true;
					}
                    if (!ClassPath.isContainerOnClasspath(gp.getJavaProject(), GROOVY_DSL_CONTAINER)) {
                        cp.add(JavaCore.newContainerEntry(new Path(GROOVY_DSL_CONTAINER), false));
                        added = true;
                    }
					if (added) {
						cp.setOn(gp.getJavaProject(), new SubProgressMonitor(monitor, 1));
					}
				}
			} finally {
				monitor.done();
			}
		} else {
			//disable it: removes the DSLD container. Won't remove Groovy natures and libs, since
			//we can't really know for sure why they were added, could be they are needed for
			//other things than DSLD.
			GradleDSLDClasspathContainer.removeFrom(gp.getJavaProject(), monitor);
		}
		gp.getRefreshPreferences().setEnableDSLD(enable);
	}

//	private void checkPrereqsForEnablement(IProject project) throws CoreException {
//		IStatus status = verifyPrereqsForEnablement(project);
//		if (!status.isOK()) {
//			throw ExceptionUtil.coreException(status);
//		}
//	}

	private IStatus verifyPrereqsForEnablement(IProject project) throws CoreException {
		boolean isAspectJ = project.hasNature(ASPECTJ_NATURE);
		if (isAspectJ) {
			return ExceptionUtil.status(IStatus.WARNING,
					"DSLD support can't be enabled.\n" +
					"Reason: Project '"+project.getName()+"' is an AspectJ project.\n" +
					"Greclipse does not support adding Groovy natures/builders to AspectJ projects.\n");
		}
		return Status.OK_STATUS;
	}

	public static synchronized DSLDSupport getInstance() {
		if (instance==null) {
			instance = new DSLDSupport();
		}
		return instance;
	}

	/**
	 * @return true only if Greclipse is installed. 
	 */
	public boolean haveGreclipse() {
		if (haveGreclipse==null) {
			Bundle greclipseCore = Platform.getBundle("org.codehaus.groovy.eclipse.core");
			haveGreclipse = greclipseCore!=null;
		}
		return haveGreclipse;
	}

}
