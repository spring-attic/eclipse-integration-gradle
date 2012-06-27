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
package org.springsource.ide.eclipse.gradle.core.preferences;

import org.springsource.ide.eclipse.gradle.core.util.ArrayEncoder;

/**
 * @author Kris De Volder
 */
public abstract class AbstractGradlePreferences {

	public String[] getStrings(String key, String[] defaultValue) {
		String encoded = get(key, null);
		if (encoded==null) return defaultValue;
		return ArrayEncoder.decode(encoded);
	}

	public void putStrings(String key, String[] strings) {
		String encoded = ArrayEncoder.encode(strings);
		put(key, encoded);
	}
	
	public void put(String key, boolean enabled) {
		String encoded = ""+enabled;
		put(key, encoded);
	}
	
	public boolean get(String key, boolean deflt) {
		String encoded = get(key, null);
		if (encoded==null) {
			return deflt;
		}
		return Boolean.valueOf(encoded);
	}

	public abstract String get(String name, String deflt);
	public abstract void put(String key, String value);

}
