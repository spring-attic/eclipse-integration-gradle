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

import org.springsource.ide.eclipse.gradle.core.util.ArgumentsParser;


import junit.framework.TestCase;

/**
 * @author Kris De Volder
 */
public class ArgumentsParserTests extends TestCase {
	
	public void testSimpleCase() {
		doTest("abc kiklao blah", 
			//=> parses into:
			"abc", 
			"kiklao", 
			"blah");
	}
	
	public void testEmpty() {
		doTest(""
			//=> parses into:
			);
	}
	
	public void testLeadingWhitespace() {
		doTest("   abc def ghi",
				"abc", "def", "ghi");
	}
	
	public void testTrailingWhitespace() {
		doTest("   abc def ghi   ",
				"abc", "def", "ghi");
	}
	
	public void testLongerWhitespace() {
		doTest("   abc 		\ndef\n\n \tghi   ",
				"abc", "def", "ghi");
	}
	
	public void testWindowsyPaths() {
		doTest("\"C:\\Programs and Documents\\foo\" \ndef\n\n \tghi   ",
				"C:\\Programs and Documents\\foo", "def", "ghi");
	}
	
	public void testEmbeddedQuotes() {
		doTest("\"foo\\\"bar\" def ghi",
				"foo\"bar", "def", "ghi");
	}
	
	public void testEmptyString() {
		doTest("\"\"", 
				"");
	}

	public void testSeveralEmptyStrings() {
		doTest("\"\" \"\" \"\"", 
				"", "", "");
	}
	
	public void testOpenQuotesButNoCloseQuotes() {
		doTest("\"hello world!",
				"hello world!");
	}
	
	public void testOpenQuotesButNoCloseQuotesEndingWithBackslash() {
		doTest("\"hello world\\",
				"hello world\\");
	}
	
	public void testOnlyEscapeDoubleQuotes() {
		doTest("\"foo\\bar \" bork snort",
				"foo\\bar ", "bork", "snort");
	}
	
	public void testBackslashOutsideOfQuotes() {
		doTest("  foo\\bar  bork\\snort   ",
				"foo\\bar", "bork\\snort");
	}
	
	public void testBackslashAtTokenEnd() {
		doTest("  C:\\users\\jeff\\ bork",
				"C:\\users\\jeff\\", "bork");
	}

	public void testBackslashAtLineEnd() {
		doTest("  C:\\users\\jeff\\",
				"C:\\users\\jeff\\");
	}

	public void testQuoteInToken() {
		doTest("  bork\" and more\" yeah",
				"bork and more", "yeah");
	}
	
	public void testOnlyEscapeDoubleQuotesInToken() {
		doTest("  bork\\\"bork bark\\bark",
				"bork\"bork", "bark\\bark");
	}

	public void testDontLoseTrailinBackslashInToken() {
		doTest(" bork\\bark\\",
				"bork\\bark\\");
	}
	
	public void testNull() {
		doTest(null);
	}
	
	private void doTest(String input, String... parsed) {
		String[] actuals = ArgumentsParser.parseArguments(input);
		assertArrayEquals(parsed, actuals);
	}

}
