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

import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.GradleCore;


/**
 * A continuation is a callback function that is called at the completion of some
 * asynchronous computation. When called it may receive some object providing the result of
 * the computation.
 * 
 * @author Kris De Volder
 */
public abstract class Continuation<T> {
	public abstract void apply(T value);
	public void error(Throwable e) {
		GradleCore.log(e);
	}
}
