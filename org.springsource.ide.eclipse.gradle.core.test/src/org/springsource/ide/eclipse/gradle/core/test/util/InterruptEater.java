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

import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * For some reason when running individual tests as Junit Plugin tests in Eclipse. 
 * The test fails because testing Thread is being 'interrupted'. This can cause 
 * any pending IO or wait style blocking operations to throw Interrupted exception
 * causing test to fail. 
 * <p>
 * I assume this is because the JUnit runner may be already letting Eclipse enter
 * into 'shutting down' stage while the test is still in progress.
 * <p>
 * Ironically bad code that just loops and swallows interrupt exceptions or
 * doesn't check for interrupts has no problem with this. But some well 
 * designed code (such as for example apache FileUtils will do 'the right thing'
 * and actually propagate an interrupted exception when their thread is
 * interupted.
 * <p>
 * To deal with this, InteruptEater wraps some bit of work 
 * inside an 'interrupt eater'. The interrupt eater will catch any InterruptedExceptions and
 * keep re-trying the bit of code until it isn't interrupted anymore.
 * <p>
 * Should be no need to put this in a 'production' test. But it is useful
 * for running certain individual tests inside of Eclipse IDE.
 * 
 * @author Kris De Volder
 */
public abstract class InterruptEater {
	
	public InterruptEater() throws Throwable {
		start();
	}

	private void start() throws Throwable {
		boolean interrupted;
		do {
			interrupted = false;
			try {
				run();
			} catch (Throwable _e) {
				//actual exception may be wrapped several levels deep
				Throwable e = ExceptionUtil.getDeepestCause(_e);
				if (e instanceof InterruptedException) {
					interrupted = true;
					Thread.interrupted(); //clears interrupt status
				} else {
					throw _e;
				}
			}
		} while (interrupted);
	}

	protected abstract void run() throws Exception;

}
