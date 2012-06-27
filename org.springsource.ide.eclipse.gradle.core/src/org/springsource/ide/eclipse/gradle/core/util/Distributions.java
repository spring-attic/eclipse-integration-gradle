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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.Assert;

/**
 * Constants representing different Gradle distributions.
 * 
 * @author Kris De Volder
 */
public class Distributions {

	public static final URI M3_URI;
	public static final URI M4_URI;
	public static final URI M5_URI;
	public static final URI M6_URI;
	public static final URI M7_URI;
	public static final URI M8_URI;
	public static final URI M9_URI;
	
	private static final int LO = 3; // Distributions range from M3
	private static final int HI = 9; // 					to M9
	
	static {
		try {
			M3_URI = milestoneURI(3);
			M4_URI = milestoneURI(4);
			M5_URI = milestoneURI(5);
			M6_URI = milestoneURI(6);
			M7_URI = milestoneURI(7);
			M8_URI = milestoneURI(8);
			M9_URI = milestoneURI(9);
		} catch (URISyntaxException e) {
			throw new Error(e); // Shouldn't happen if the URI's above are ok.
		}
	}

	public static URI[] all;
	static {
		try {
			all = new URI[HI-LO+1];
			for (int i = 0; i < all.length; i++) {
				all[i] = milestoneURI(i+LO);
			}
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

	public static URI milestoneURI(int num) throws URISyntaxException {
		Assert.isLegal(num>=3 && num<=9);
		if (num<9) {
			return new URI("http://repo.gradle.org/gradle/distributions/gradle-1.0-milestone-"+num+"-bin.zip");
		} else {
			return new URI("http://services.gradle.org/distributions/gradle-1.0-milestone-"+num+"-bin.zip");
		}
	}
	
}
