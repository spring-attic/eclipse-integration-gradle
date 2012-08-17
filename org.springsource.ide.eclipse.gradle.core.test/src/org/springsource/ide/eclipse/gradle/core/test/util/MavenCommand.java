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

	public static String mavenHome = null;
	static final String[] mavenHomes = {
		//A list of places to check for a suitable maven installation
		"/opt/java/tools/maven/apache-maven-3.0.3",
		"/home/kdvolder/Applications/apache-maven-3.0.3"
	};
	
	static {	
		for (String path : mavenHomes) {
			if (new File(path).isDirectory()) {
				mavenHome = path;
				break;
			}
		}
	}

	public MavenCommand(String... pieces) {
		super(customize(pieces));
	}

	private static String[] customize(String[] pieces) {
		if (mavenHome!=null) {
			pieces[0] = mavenHome+"/bin/mvn";
		}
		return pieces;
	}

	@Override
	public void configure(ProcessBuilder processBuilder) {
		super.configure(processBuilder);
		Map<String, String> env = processBuilder.environment();
		if (mavenHome!=null) {
			env.put("HOME", System.getProperty("user.home"));
			env.put("M2_HOME", mavenHome.toString());
			env.put("MAVEN_HOME", mavenHome.toString());
			env.put("MAVEN2_HOME", mavenHome.toString());
		} else {
			System.out.println("Warning: couldn't find a maven 3.0.3");
		}
	}
	
}
