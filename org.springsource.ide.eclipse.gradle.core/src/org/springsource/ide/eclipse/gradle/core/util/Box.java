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

/**
 * Simple mutable 'box' containing a single value.
 * <p>
 * Common use for this box is to provide a 'mutable' variable
 * that needs to be declared final because it is used from within
 * a nested inner class.
 * 
 * @author Kris De Volder
 */
public class Box<T> {
	private T v;
	public Box(T ini) {
		this.v = ini;
	}
	public T get() {
		return v;
	}
	public void set(T v) {
		this.v = v;
	}
	@Override
	public String toString() {
		return ""+v;
	}
}
