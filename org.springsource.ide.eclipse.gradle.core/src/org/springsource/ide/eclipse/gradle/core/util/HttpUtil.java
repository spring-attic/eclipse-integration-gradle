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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Utility method(s) to download the contents of URL.
 * 
 * @author Kris De Volder
 */
public class HttpUtil {

	/**
	 * Downloads data from a given uri and writes it to the output stream. The output stream
	 * is closed at the end of this operation (even if the operation was not succesfull).
	 */
	public static void download(URI uri, OutputStream _out) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(_out);
		try {
			InputStream in = uri.toURL().openStream();
			byte[] buffer = new byte[1024*4]; //A 4k buffer to read in data seems reasonable.
			int bytesRead;
			while ((bytesRead = in.read(buffer))>=0) {
				//0 is probably not a legitimate value for bytesRead... but just to be on the safe side :-)
				if (bytesRead > 0) {
					out.write(buffer, 0, bytesRead);
				}
			}
		} finally {
			out.close();
		}
	}

	/**
	 * Downloads data from a given uri and writes it to a File.
	 */
	public static void download(URI uri, File target) throws FileNotFoundException, IOException {
		download(uri, new FileOutputStream(target, false));
	}

}
