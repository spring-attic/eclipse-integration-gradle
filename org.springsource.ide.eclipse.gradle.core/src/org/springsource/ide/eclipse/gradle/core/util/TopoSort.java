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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that implements a simple topological sort algorithm. The algorithm should be able to
 * handle cycles. (I.e. still produce some kind of sorting order even the partial order provided
 * isn't a true partial ordering.
 * <p>
 * The algorithm assumes that the partial ordering comes from a finite DAG of elements. 
 * where it is possible and efficient to ask for a collection of all predecessors of a given element.
 * <p>
 * Though the algortihm is mainly intended to sort things that are expected to be DAGs (i.e.
 * without cycles) it should gracefully deal with cycles and still return some sorting order
 * that mostly respect the predecessor relationship (some edges in cycles may be ignored 
 * arbitrarily to make the graph into a DAG).
 * 
 * @author Kris De Volder
 */
public class TopoSort<T> {

	/**
	 * This provides a view on another partial order allowing it to be modified by 
	 * removing egdes (this used to cut cycles in the original graph).
	 */
	public static class ModifyablePartialOrder<T> implements PartialOrder<T> {
		
		private PartialOrder<T> original;
		private Map<T, Set<T>> removed = null;
		
		public ModifyablePartialOrder(PartialOrder<T> original) {
			this.original = original;
		}

		public Collection<T> getPredecessors(T o1) {
			Collection<T> preds = original.getPredecessors(o1);
			if (removed!=null) {
				Set<T> removedPreds = removed.get(o1);
				if (removedPreds!=null) {
					preds = new HashSet<T>(preds);
					preds.removeAll(removedPreds);
				}
			}
			return preds;
		}
		
		public void removePred(T node, T pred) {
			if (removed==null) {
				removed = new HashMap<T, Set<T>>();
			}
			Set<T> removedPreds = removed.get(node);
			if (removedPreds==null) {
				removedPreds = new HashSet<T>();
				removed.put(node, removedPreds);
			}
			removedPreds.add(pred);
		}
		
		/**
		 * @return true if at least one edge has been removed.
		 */
		public boolean isModified() {
			return removed!=null;
		}

	}

	public interface PartialOrder<T> {
		Collection<T> getPredecessors(T o1);
	}

	private HashSet<T> elementsToSort;
	private ModifyablePartialOrder<T> graph;
	private ArrayList<T> sortedElements;

	/**
	 * Create a TopoSort for given collection of elements and partial ordering.
	 * The collection of elements should not contain any duplicates!
	 */
	public TopoSort(Collection<T> elements, PartialOrder<T> compare) {
		this.elementsToSort = new LinkedHashSet<T>(elements);
		this.graph = new ModifyablePartialOrder<T>(compare);
	}
	
	public List<T> getSorted() {
		ensureSorted();
		return sortedElements;
	}

	private synchronized void ensureSorted() {
		if (sortedElements==null) {
			sort();
		}
	}

	private void sort() {
		sortedElements = new ArrayList<T>();
		while (!elementsToSort.isEmpty()) {
			T element = chase(getElement(elementsToSort), new HashSet<T>());
			elementsToSort.remove(element);
			sortedElements.add(element);
		}
	}

	/**
	 * Chase down a path following pred relationship until we find an element that has no more pred in the elementsToSort.
	 */
	private T chase(T element, HashSet<T> seen) {
		//TODO: optimise: use while loop instead of recursion
		T pred = getPred(element, seen);
		if (pred==null) {
			return element;
		} else {
			seen.add(element);
			return chase(pred, seen);
		}
	}

	/**
	 * Get a pred of given element, limited to preds that are still in elementsToSort
	 * and are not excluded because of cycle detection. If cycle detection is used
	 * to exclude a pred... the hasCycles flag will be set.
	 */
	private T getPred(T element, HashSet<T> seen) {
		Collection<T> preds = graph.getPredecessors(element);
		for (T p : preds) {
			if (!p.equals(element) && elementsToSort.contains(p)) {
				if (seen.contains(p)) {
					graph.removePred(element, p);
				} else {
					return p; //Found a good one to chase!
				}
			}
		}
		return null; //No acceptable pred found
	}

	/**
	 * Get some element from a set. We don't care which one as long as it is in the set.
	 */
	private T getElement(HashSet<T> set) {
		Iterator<T> iter = set.iterator();
		return iter.next();
	}

	public boolean hasCycle() {
		ensureSorted();
		return graph.isModified();
	}

	/**
	 * @return The 'edited' graph/partial order. It may have been modified from the original in order to cut cycles.
	 */
	public ModifyablePartialOrder<T> getModifiedPartialOrder() {
		ensureSorted();
		return graph;
	}

	/**
	 * For debugging purposes dump out info about this Sorter and its result.
	 */
	public void dump() {
		
	}

}
