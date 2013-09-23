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
package org.springsource.ide.eclipse.gradle.core.util;

/**
 * @author Kris De Volder
 */
public class TimeUtils {

	public static String minutusAndSecondsFromMillis(long millis) {
		return String.format("%d min, %d sec", 
				millis2minutes(millis),
				millis2seconds(millis) - 
				minutes2seconds(millis2minutes(millis))
		);
	}

	private static long millis2seconds(long millis) {
		return millis / 1000;
	}

	private static long minutes2seconds(long minutes) {
		return minutes * 60;
	}

	public static long millis2minutes(long millis) {
		return millis / (1000*60);
	}

}
