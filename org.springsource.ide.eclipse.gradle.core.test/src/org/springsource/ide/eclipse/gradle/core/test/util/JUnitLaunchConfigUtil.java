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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitMigrationDelegate;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabels;

/**
 * Code copied and modified slighly from org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut.
 * <p>
 * The code had to be copied because the method containing the code we want to call is protected.
 * <p>
 * Allows the creation of a JUnit launch configuration that is equivalent to what is created by the
 * "Run As >> Junit test" menu.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class JUnitLaunchConfigUtil {
	
	private static final String EMPTY_STRING = "";
	
	private static ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	

	public static ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		final String testName;
		final String mainTypeQualifiedName;
		final String containerHandleId;

		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaElement.PACKAGE_FRAGMENT: {
				String name= JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_FULLY_QUALIFIED);
				containerHandleId= element.getHandleIdentifier();
				mainTypeQualifiedName= EMPTY_STRING;
				testName= name.substring(name.lastIndexOf(IPath.SEPARATOR) + 1);
			}
			break;
			case IJavaElement.TYPE: {
				containerHandleId= EMPTY_STRING;
				mainTypeQualifiedName= ((IType) element).getFullyQualifiedName('.'); // don't replace, fix for binary inner types
				testName= element.getElementName();
			}
			break;
			case IJavaElement.METHOD: {
				IMethod method= (IMethod) element;
				containerHandleId= EMPTY_STRING;
				mainTypeQualifiedName= method.getDeclaringType().getFullyQualifiedName('.');
				testName= method.getDeclaringType().getElementName() + '.' + method.getElementName();
			}
			break;
			default:
				throw new IllegalArgumentException("Invalid element type to create a launch configuration: " + element.getClass().getName()); //$NON-NLS-1$
		}

		String testKindId= TestKindRegistry.getContainerTestKindId(element);

		ILaunchConfigurationType configType= getLaunchManager().getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION);
		ILaunchConfigurationWorkingCopy wc= configType.newInstance(null, getLaunchManager().generateLaunchConfigurationName(testName));

		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeQualifiedName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, element.getJavaProject().getElementName());
		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING, false);
		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, containerHandleId);
		wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, testKindId);
		JUnitMigrationDelegate.mapResources(wc);
		AssertionVMArg.setArgDefault(wc);
		if (element instanceof IMethod) {
			wc.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, element.getElementName()); // only set for methods
		}
		return wc;
	}
	

}
