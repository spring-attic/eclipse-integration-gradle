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
package org.springsource.ide.eclipse.gradle.core.util.expression;

/**
 * A concrete implementation of LiveExpression that represents a single storage cell in
 * which a value of a given type can be stored. 
 * <p>
 * Can be used in Mockup UIs for testing. Here a LiveVariable can take the place of
 * an input field in the GUI. Core functionality can then be tested against the
 * headless Mock in which it is easy to set values.
 * 
 * @author Kris De Volder
 */
public class LiveVariable<T> extends LiveExpression<T> {

	public LiveVariable(T initialValue) {
		super(initialValue);
	}
	
	public LiveVariable() {
		this(null);
	}

	private T storedValue;

	@Override
	protected T compute() {
		return storedValue;
	}

	public void setValue(T v) {
		storedValue = v;
		refresh();
	}

}
