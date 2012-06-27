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
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.util.ArrayList;

/**
 * Encapsulates information about an 'external' command that can be run through the OS. 
 * <p>
 * This is a simplistic implementation. A more sophisticate implementation should allow for
 * different OS's (commands may return different information depending on the OS).
 * 
 * @author Kris De Volder
 */
public class ExternalCommand {

	private final String[] command;

	public ExternalCommand(String... command) {
		ArrayList<String> pieces = new ArrayList<String>(command.length);
		for (String piece : command) {
			if (piece!=null) {
				pieces.add(piece);
			}
		}
		this.command = pieces.toArray(new String[pieces.size()]);
	}
	
	public String[] getProgramAndArgs() {
		return command;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String piece : command) {
			if (!first) {
				buf.append(" ");
			}
			buf.append(piece);
			first = false;
		}
		return buf.toString();
	}

}
