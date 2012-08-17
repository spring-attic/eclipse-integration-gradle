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

import java.io.File;
import java.util.Map;

public class MavenCommand extends ExternalCommand {

	public static File mvnHome = null;
	static {	
		//This code is specific to our build server but without,
		//it seems like we cannot correctly execute mvn commands
		//on the build server which has multiple versions of maven installed.
		File f = new File("/opt/java/tools/maven/apache-maven-3.0.3");
		if (f.exists()) {
			mvnHome = f;
		}
	}

	public MavenCommand(String... pieces) {
		super(customize(pieces));
	}

	private static String[] customize(String[] pieces) {
		if (mvnHome!=null) {
			pieces[0] = mvnHome+"/bin/mvn";
		}
		return pieces;
	}

	@Override
	public void configure(ProcessBuilder processBuilder) {
		super.configure(processBuilder);
		Map<String, String> env = processBuilder.environment();
		if (mvnHome!=null) {
			env.put("M2_HOME", mvnHome.toString());
			env.put("MAVEN_HOME", mvnHome.toString());
			env.put("MAVEN2_HOME", mvnHome.toString());
		} else {
			System.out.println("Warning: couldn't find a maven 3.0.3");
		}
	}
	
}
