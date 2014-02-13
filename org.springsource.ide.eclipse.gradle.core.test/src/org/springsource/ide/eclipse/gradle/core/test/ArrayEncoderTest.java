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

import org.springsource.ide.eclipse.gradle.core.util.ArrayEncoder;

import junit.framework.TestCase;


/**
 * @author Kris De Volder
 */
public class ArrayEncoderTest extends TestCase {
	
	public void testEmptyArray() {
		encodeDecode(new String[0]);
	}
	
	public void testSimpleArray() {
		encodeDecode(new String[] { "Hello", "abc", "nice one" });
	}
	
	public void testArrayWithEmptyString() {
		encodeDecode(new String[] { "" });
	}

	public void testWithEscapeChars() {
		encodeDecode(new String[] { "C:\\world\\banger", "D:\\Hyper\\Visors\\", "\\\\\\\\\\" });
	}
	
	public void testWithSeparatorChars() {
		encodeDecode(new String[] { ";", ";;;;;;;;;;;;;;", "\\;", "" });
	}
	
	private void encodeDecode(String[] original) {
		String encoded = ArrayEncoder.encode(original);
		String[] decoded = ArrayEncoder.decode(encoded);
		assertArrayEquals(original, decoded);
	}

}
