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
package org.springsource.ide.eclipse.gradle.core.launch;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A subclass, who's only purpose is to provide a better toString for debugging, so we can see
 * what is buffered inside a pipe
 * 
 * @author Kris De Volder
 */
public class GradlePipedInputStream extends PipedInputStream {
	
	public GradlePipedInputStream(PipedOutputStream out) throws IOException {
		super(out);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (in<0) {
			//empty
			return "*empty*";
		}
		StringBuffer string = new StringBuffer();
		for (int i = out; i != in; i = (i + 1)%buffer.length) {
			System.out.println("i: "+buffer[i]+" = '"+(char)buffer[i]+"'");
			string.append((char)buffer[i]);
		}
		return "|"+string.toString()+"|";
	}

}
