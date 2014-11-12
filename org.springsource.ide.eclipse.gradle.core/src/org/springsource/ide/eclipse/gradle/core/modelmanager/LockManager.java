/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages locks on a set of strings.
 */
public class LockManager {
	
	private Set<String> locked = new HashSet<String>();
	private boolean worldIsLocked = false;

	/**
	 * Obtain a lock on a given set of keys. All or none of the locks are obtained.
	 * The caller must eventually call 'release' on the returned Lock object.
	 */
	public synchronized Lock lock(Set<String> _keys) {
		//copy keys into an array, just in case clients might mutate the collection after passing it to us.
		final String[] keys = _keys.toArray(new String[_keys.size()]);
		while (!canLock(keys)) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		for (String k : keys) {
			locked.add(k);
		}
		return new Lock() {
			@Override
			public void release() {
				releaseAll(keys);
			}
		};
	}

	private synchronized void releaseAll(String[] keys) {
		for (String k : keys) {
			locked.remove(k);
		}
		notify();
	}

	private boolean canLock(String[] keys) {
		if (worldIsLocked) {
			return false;
		}
		for (String k : keys) {
			if (locked.contains(k)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Lock 'the world'. This lock will only succeed if no other locks are currently held and
	 * once this lock succeeds it needs to be released before any other locks can be obtained.
	 */
	public synchronized Lock lockAll() {
		while (worldIsLocked || !locked.isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		worldIsLocked = true;
		return new Lock() {
			@Override
			public void release() {
				unlockTheWorld();
			}
		};
	}

	private synchronized void unlockTheWorld() {
		worldIsLocked = false;
		notify();
	}
	
}
