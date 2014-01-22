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
package org.springsource.ide.eclipse.gradle.core;

import org.eclipse.core.runtime.Platform;

/**
 * Booleans 'true' and 'false' used to enable debugging text. The 'true' boolean is actually only true under very specific
 * condition, so that the debugging text is automatically turned of when running anywhere else than on Kris's own machine.
 * <p>
 * This is because Kris often forgets to disable debugging code when committing code into the git repo.
 * 
 * @author Kris De Volder
 */
public class Debug {
	
	public static final boolean TRUE = Platform.getLocation()!=null && Platform.getLocation().toString().contains("/kdvolder/");
	public static final boolean FALSE = false;

}
