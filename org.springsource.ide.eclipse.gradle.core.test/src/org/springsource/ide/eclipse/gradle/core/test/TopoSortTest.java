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
package org.springsource.ide.eclipse.gradle.core.test;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springsource.ide.eclipse.gradle.core.util.TopoSort;
import org.springsource.ide.eclipse.gradle.core.util.TopoSort.PartialOrder;

import junit.framework.TestCase;


/**
 * @author Kris De Volder
 */
public class TopoSortTest extends TestCase {

	protected static final Collection<String> NO_ELEMENTS = Arrays.asList();
	
	/**
	 * This map represents the predecessor relationship in the graph. It should be
	 * filled with edges by the test. For any node in the graph there should be
	 * a key in the map, even if the node has no edges associated with it.
	 */
	Map<String, Set<String>> graph = new HashMap<String, Set<String>>();
	
	/**
	 * Try sorting an empty graph.
	 */
	public void testEmpty() {
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false);
	}
	
	/**
	 * Try sorting a simple graph which has some nodes but no edges.
	 */
	public void testNoEdges() {
		nodes("A", "B", "C");
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false);
	}
	
	/**
	 * Try sorting a graph where nodes pred is themselves.
	 */
	public void testSelfCycles() {
		edge("A", "A");
		edge("B", "B");
		edge("C", "C");
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false); //Self cycles are ignored and don't count.
	}
	
	public void testLinearSequence() {
		String previous = null;
		for (int i = 0; i < 10; i++) {
			String node = "node"+i;
			if (previous!=null) {
				edge(node, previous);
			}
			previous = node;
		}
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false);
		sorter.getSorted();
		assertArrayEquals(new Object[] {
				"node0", "node1", "node2", "node3", "node4", 
				"node5", "node6", "node7", "node8", "node9"
		}, sorter.getSorted().toArray());
	}
	
	/**
	 * The algorithm shouldn't care about trivial 'self cycles' so they should have no impact.
	 */
	public void testLinearSequenceWithSelfCycles() {
		String previous = null;
		for (int i = 0; i < 10; i++) {
			String node = "node"+i;
			edge(node, node);
			if (previous!=null) {
				edge(node, previous);
			}
			previous = node;
		}
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false); //Self cycles are ignored and don't count
		sorter.getSorted();
		assertArrayEquals(new Object[] {
				"node0", "node1", "node2", "node3", "node4", 
				"node5", "node6", "node7", "node8", "node9"
		}, sorter.getSorted().toArray());
	}
	
	public void testCycle() {
		for (int i = 0; i < 10; i++) {
			String pred = ""+i;
			String succ = ""+((i+1)%10);
			edge(succ, pred);
		}
	
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		
		//Cycle could be broken anywhere... but if we know the first element in the sort
		//result it determines the other ones.

		assertTrue(sorter.hasCycle());
		List<String> result = sorter.getSorted();
		int first = new Integer(result.get(0));
		String[] expect = new String[getElements().size()];
		for (int i = 0; i < expect.length; i++) {
			expect[i] = ""+((first+i)%expect.length);
		}
		
		assertArrayEquals(expect,  sorter.getSorted().toArray());
		checkResult(sorter, true);
	}
	
	/**
	 * Generates a binary tree and topo sorts as if each node depends on its children. (so we have some
	 * test where there's more than one pred for some nodes.
	 */
	public void testBinaryTree() {
		final int DEPTH = 4;
		genTree("root", DEPTH);
		
		assertEquals(Math.pow(2, 4)-1, (double)getElements().size(), 0.001);
		
		TopoSort<String> sorter = new TopoSort<String>(getElements(), getCompare());
		checkResult(sorter, false);
	}

	private void genTree(String parent, int d) {
		if (d==1) {
			node(parent);
		} else {
			for (String f : new String[] {".l", ".r"}) {
				String child = parent + f;
				edge(parent, child);
				genTree(child, d-1);
			} 
		}
	}

	/**
	 * Declare an edge in the test graph, implicitly declare the end points of
	 * the edge to be nodes in the graph as well.
	 */
	private void edge(String node, String pred) {
		nodes(node, pred);
		Set<String> preds = graph.get(node);
		preds.add(pred);
	}

	/**
	 * Convenience method to declare a number of nodes at once.
	 */
	private void nodes(String... nodes) {
		for (String n : nodes) {
			node(n);
		}
	}

	/**
	 * Ensures that n is a node in the graph, adding it if it doesn't exist yet.
	 */
	private void node(String n) {
		if (graph.get(n)==null) {
			graph.put(n, new HashSet<String>());
		}
	}

	private void checkResult(TopoSort<String> sorter, boolean expectCycle) {
		assertEquals(expectCycle, sorter.hasCycle());
		PartialOrder<String> order = expectCycle ? sorter.getModifiedPartialOrder() : getCompare();
		List<String> result = sorter.getSorted();
		assertEquals(getElements().size(), result.size()); // All elements got sorted?

		//Check each possible pair of element doesn't violate ordering constraint
		for (int i = 0; i < result.size(); i++) {
			String ei = result.get(i);
			for (int j = i+1; j < result.size(); j++) {
				String ej = result.get(j);
				assertFalse(order.getPredecessors(ei).contains(ej));
			}
		}
	}

	private Collection<String> getElements() {
		return graph.keySet();
	}

	private PartialOrder<String> getCompare() {
		return new PartialOrder<String>() {
			public Collection<String> getPredecessors(String o1) {
				Set<String> preds = graph.get(o1);
				assertNotNull(preds);
				return preds;
			}
		};
	}

}
