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
package org.springsource.ide.eclipse.gradle.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An instance of this class manages a set of {@link IGradleModelListener}s. It provides methods to
 * add, remove and notify listeners with appropriate thread synchronisation. 
 * <p>
 * The main reason for this class's existence is to split off management of the listeners from
 * the {@link GradleProject} class, so that it can easily use more fine-grained synchronisation t
 * than locking on a GradleProject instance. (We only need to protect against concucrrent modification
 * of the listeners set in this class, so this doesn't have to interfere with other operations on
 * a GradleProject's state.
 * 
 * @author Kris De Volder
 */
class GradleModelListeners {
	
	private Set<IGradleModelListener> listeners = new LinkedHashSet<IGradleModelListener>();

	public synchronized void remove(IGradleModelListener l) {
		listeners.remove(l);
	}

	public synchronized void add(IGradleModelListener l) {
		listeners.add(l);
	}

	public synchronized IGradleModelListener[] toArray() {
		return listeners.toArray(new IGradleModelListener[listeners.size()]);
	}

}
