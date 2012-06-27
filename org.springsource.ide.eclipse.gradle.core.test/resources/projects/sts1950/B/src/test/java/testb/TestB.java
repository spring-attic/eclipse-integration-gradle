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
package testb;

import org.junit.Test;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class TestB {

	@Test
	public void test()
	{
		final EventList<String> list = new BasicEventList<String>(1);
		list.add("test ok!");
		System.out.println(list.get(0));
	}
}
