/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Copied from eclipe org.eclipse.debug.core.DebugPlugin.ArgumentParser 
 * It was modified to remove the double escaping that it did for windows platform in Eclipse. This escaping is presumed to
 * be an idiosynchracy of Eclipse's JAva runtime implementation on Windows. The Gradle API doesn't require this kind
 * of special treatment based on platform. (or does it?)
 * 
 * @author Kris De Volder
 */
public class ArgumentsParser {

	private String fArgs;
	private int fIndex= 0;
	private int ch= -1;

	public ArgumentsParser(String args) {
		this.fArgs= args;
	}

	public String[] parseArguments() {
		List<String> v= new ArrayList<String>();

		ch= getNext();
		while (ch > 0) {
			if (Character.isWhitespace((char)ch)) {
				ch= getNext();
			} else {
				if (ch == '"') {
					StringBuffer buf = new StringBuffer();
					buf.append(parseString());
					v.add(buf.toString());
				} else {
					v.add(parseToken());
				}
			}
		}

		String[] result= new String[v.size()];
		v.toArray(result);
		return result;
	}

	private int getNext() {
		if (fIndex < fArgs.length())
			return fArgs.charAt(fIndex++);
		return -1;
	}

	private String parseString() {
		ch= getNext();
		if (ch == '"') {
			ch= getNext();
			return ""; //$NON-NLS-1$
		}
		StringBuffer buf= new StringBuffer();
		while (ch > 0 && ch != '"') {
			if (ch == '\\') {
				ch= getNext();
				if (ch != '"') {           // Only escape double quotes
					buf.append('\\');
				}
			}
			if (ch > 0) {
				buf.append((char)ch);
				ch= getNext();
			}
		}
		ch= getNext();
		return buf.toString();
	}

	private String parseToken() {
		StringBuffer buf= new StringBuffer();

		while (ch > 0 && !Character.isWhitespace((char)ch)) {
			if (ch == '\\') {
				ch= getNext();
				if (Character.isWhitespace((char)ch)) {
					// end of token, don't lose trailing backslash
					buf.append('\\');
					return buf.toString();
				}
				if (ch > 0) {
					if (ch != '"') {           // Only escape double quotes
						buf.append('\\');
					}
					buf.append((char)ch);
					ch= getNext();
				} else if (ch == -1) {     // Don't lose a trailing backslash
					buf.append('\\');
				}
			} else if (ch == '"') {
				buf.append(parseString());
			} else {
				buf.append((char)ch);
				ch= getNext();
			}
		}
		return buf.toString();
	}

	/**
	 * Parses the given command line into separate arguments that can be passed to
	 * <code>DebugPlugin.exec(String[], File)</code>. Embedded quotes and slashes
	 * are escaped.
	 * 
	 * @param args command line arguments as a single string
	 * @return individual arguments
	 * @since 3.1
	 */
	public static String[] parseArguments(String args) {
		if (args == null)
			return new String[0];
		ArgumentsParser parser= new ArgumentsParser(args);
		String[] res= parser.parseArguments();

		return res;
	}	

}
