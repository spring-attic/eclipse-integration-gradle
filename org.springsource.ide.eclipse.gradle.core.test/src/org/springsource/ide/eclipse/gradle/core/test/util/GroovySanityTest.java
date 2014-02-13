/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.test.util;

import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.jdt.groovy.integration.LanguageSupportFactory;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springsource.ide.eclipse.gradle.core.test.GradleTest;


/**
 * @author Kris De Volder
 * @since 2.7
 */
public class GroovySanityTest extends GradleTest {
    
    public void testNothing() throws Exception {
        // An empty test to make sure that this suite is never added as an empty suite 
    }
	
	public void testSanity() throws BundleException {
		checkSanity(8); //Currently some of the imported projects expecting Groovy 1.8 or they will fail.
	}

	public static void checkSanity(int expectMinor) throws BundleException {
		Bundle bundle = CompilerUtils.getActiveGroovyBundle();
		assertNotNull("No active Groovy bundle found", bundle);
		assertEquals("Wrong version for groovy bundle: " + bundle.getVersion(), expectMinor, bundle.getVersion().getMinor());
		if (bundle.getState() != Bundle.ACTIVE) {
			bundle.start();
		}
		
		bundle = Platform.getBundle("org.eclipse.jdt.core");
		final String qualifier = bundle.getVersion().getQualifier();
//TODO: the conditions below should be activated... or test will break on non E_3_6 builds... for now leave like this
// to be 100% sure sanity checks are really executed!
//		if (StsTestUtil.isOnBuildSite() && StsTestUtil.ECLIPSE_3_6_OR_LATER) {
			assertTrue("JDT patch not properly installed (org.eclipse.jdt.core version = "+bundle.getVersion()+")", 
					qualifier.contains("xx")|| qualifier.equals("qualifier"));
//		}
		if (bundle.getState() != Bundle.ACTIVE) {
			bundle.start();
		}
		assertTrue("Groovy language support is not active", LanguageSupportFactory.getEventHandler().getClass().getName().endsWith("GroovyEventHandler"));
	}
	
}
