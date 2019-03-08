/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Linked list based implementation of a capacity restricted stack/queue
 * 
 * @author Alex Boyko
 *
 * @param <E>
 */
public class RestrictedCapacityStack<E> extends LinkedList<E> {
	
	private static final long serialVersionUID = 7642692882661680882L;

	private static final int DEFAULT_CAPACITY = 30;
	
	private int maxCapacity = DEFAULT_CAPACITY;
	
	public RestrictedCapacityStack() {
		super();
	}
	
	public RestrictedCapacityStack(int maxCapacity) {
		this();
		this.maxCapacity = maxCapacity;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		boolean added = super.addAll(index, c);
		adjustSize();
		return added;
	}

	@Override
	public void add(int index, E element) {
		super.add(index, element);
		adjustSize();
	}

	@Override
	public boolean add(E e) {
		boolean added = super.add(e);
		if (added) {
			adjustSize();
		}
		return added;
	}
	
	final protected void adjustSize() {
		while (size() > maxCapacity) {
			removeLast();
		}
	}

}
