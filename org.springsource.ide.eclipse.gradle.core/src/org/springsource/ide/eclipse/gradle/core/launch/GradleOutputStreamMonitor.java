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
package org.springsource.ide.eclipse.gradle.core.launch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * Need to create a subclass otherwise we can't actually start this monitor's thread.
 * 
 * @author Kris De Volder
 */
public class GradleOutputStreamMonitor implements IStreamMonitor {

	private String encoding;
	
	private ByteArrayOutputStream contents = new ByteArrayOutputStream();
	private Set<IStreamListener> listeners = new HashSet<IStreamListener>();

	private OutputStream out = new OutputStream() {
		
		@Override
		public void write(int b) throws IOException {
			write(new byte[] {(byte)b});
		}
		
		public void write(byte[] b, int off, int len) throws IOException {
			contents.write(b, off, len);
			String appended = new String(b, off, len, encoding);
			fireAppend(appended);
		}

	};

	private void fireAppend(String appended) {
		for (IStreamListener l : listeners) {
			l.streamAppended(appended, this);
		}
	};
	
	public GradleOutputStreamMonitor(String encoding) {
		this.encoding = encoding;
	}

	public synchronized void addListener(IStreamListener l) {
		listeners.add(l);
	}

	public String getContents() {
		try {
			return contents.toString(encoding);
		} catch (UnsupportedEncodingException e) {
			GradleCore.log(e);
			return contents.toString(); //Best effort, use default encoding
		}
	}

	public synchronized void removeListener(IStreamListener l) {
		listeners.remove(l);
	}

	public OutputStream getOutputStream() {
		return out;
	}

}
