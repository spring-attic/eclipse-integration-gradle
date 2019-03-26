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

public class ObjectUtil {
	
	/**
	 * Same as 'Object.equals' but tolerates null.
	 */
	public static boolean equal(Object o1, Object o2) {
		if (o1==null || o2==null) {
			return o1==o2;
		}
		//neither object is is null.
		return o1.equals(o2);
	}

}
