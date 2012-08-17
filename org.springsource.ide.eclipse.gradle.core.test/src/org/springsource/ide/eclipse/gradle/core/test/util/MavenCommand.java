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

import java.util.Map;

public class MavenCommand extends ExternalCommand {

	public MavenCommand(String... pieces) {
		super(pieces);
	}

	@Override
	public void configure(ProcessBuilder processBuilder) {
		super.configure(processBuilder);
		Map<String, String> env = processBuilder.environment();
		env.remove("M2_HOME"); //See http://askubuntu.com/questions/41017/maven-exits-after-trying-to-run-it
			//Apparantly having this set causes maven commands to fail on the build servers.
	}
	
}
