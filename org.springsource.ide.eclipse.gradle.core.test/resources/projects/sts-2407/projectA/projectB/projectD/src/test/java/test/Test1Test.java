/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package test;

import static org.junit.Assert.*;

import org.jmock.api.Action;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.internal.ExpectationCollector;
import org.junit.Test;

public class Test1Test {

	@Test
	public void test() {
		ExpectationBuilder builder = new ExpectationBuilder() {
			
			@Override
			public void buildExpectations(Action defaultAction,
					ExpectationCollector collector) {
				// TODO Auto-generated method stub
				
			}
		};
		Test1 test = new Test1();
		fail("Not yet implemented");
	}

}
