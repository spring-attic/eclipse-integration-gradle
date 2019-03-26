/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
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
		if (strings!=null) {
			String encoded = ArrayEncoder.encode(strings);
			put(key, encoded);
		} else {
			put(key, null);
		}
	}
	
	public void put(String key, boolean enabled) {
		String encoded = ""+enabled;
		put(key, encoded);
	}
	public void put(String key, int deflt) {
		String encoded = ""+deflt;
		put(key, encoded);
	}
	
	public boolean get(String key, boolean deflt) {
		String encoded = get(key, null);
		if (encoded==null) {
			return deflt;
		}
		return Boolean.valueOf(encoded);
	}
	public int get(String key, int deflt) {
		String encoded = get(key, null);
		if (encoded==null) {
			return deflt;
		}
		return Integer.valueOf(encoded);
	}

	public abstract String get(String name, String deflt);
	public abstract void put(String key, String value);

}
