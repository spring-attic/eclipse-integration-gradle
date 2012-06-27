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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kris De Volder
 */
public class KillGradleDaemons {

	public static void killem() throws IOException, InterruptedException {
		ExternalCommand cmd = new ExternalCommand("jps");
		File workingDir = new File(System.getProperty("user.home"));
		ExternalProcess process = new ExternalProcess(workingDir, cmd, true);
		System.out.println(process.getOut());
		BufferedReader result = new BufferedReader(new StringReader(process.getOut()));
		String line;
		Pattern pat = Pattern.compile("^([0-9]+) (GradleDaemon)$");
		while ((line = result.readLine())!=null) {
			Matcher matcher = pat.matcher(line);
			if (matcher.matches()) {
				String pid = matcher.group(1);
				System.out.println("Killing pid = "+pid+" line: "+line);
				ExternalCommand kill = new ExternalCommand("kill", "-9", pid);
				new ExternalProcess(workingDir, kill);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		killem();
	}
	
}
