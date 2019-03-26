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
import java.nio.charset.Charset;

import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * @author Kris De Volder
 */
public class GradleStreamsProxy implements IStreamsProxy {

	/**
	 * A pipe has an in and and out stream, what's written to the out can then
	 * be read from the in. Usually one thread is reading the in and another
	 * is writing the out.
	 * 
	 * @author Kris De Volder
	 */
	public class Pipe {
		PipedInputStream in;
		PipedOutputStream out;
		public Pipe() {
			this.out = new PipedOutputStream();
			try {
				this.in = new GradlePipedInputStream(out);
			} catch (IOException e) {
				GradleCore.log(e);
			}
		}
	}

	private static final String ENCODING = Charset.defaultCharset().name();
	
	private final Pipe in;
	
//	private final Pipe out;
//	private final Pipe err;
	
	private final GradleOutputStreamMonitor outMon;
	private final GradleOutputStreamMonitor errMon;
	
	@SuppressWarnings("unused")
	private GradleProcess gradleProcess;

	/**
	 * @param gradleProcess
	 */
	public GradleStreamsProxy(GradleProcess gradleProcess) {
		this.gradleProcess = gradleProcess;
		
		outMon = new GradleOutputStreamMonitor(ENCODING);
		errMon = new GradleOutputStreamMonitor(ENCODING);
		
		in = new Pipe();
//		out = new Pipe();
//		err = new Pipe();
		
		gradleProcess.setInput(in.in);
		gradleProcess.setOutput(outMon.getOutputStream());
		gradleProcess.setError(errMon.getOutputStream());
		
	}

	public IStreamMonitor getErrorStreamMonitor() {
		return errMon;
	}

	public IStreamMonitor getOutputStreamMonitor() {
		return outMon;	
	}

	public void write(String input) throws IOException {
		in.out.write(input.getBytes(ENCODING));
	}

	public void close() {
		//TODO: do we need to do something to close the output streams?
//		close(out.out);
//		close(err.out);
	}

//	private void close(PipedOutputStream out) {
//		try {
//			out.close();
//		} catch (IOException e) {
//			GradleCore.log(e);
//		}
//	}

	
}
