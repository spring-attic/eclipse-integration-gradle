/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helper class to make it easier manipulate a list of String that represent
 * 'program arguments'. It helps in mixing together user declared program 
 * arguments list and arguments computed by the tools.
 * 
 * @author Kris De Volder
 */
public class ArgumentsCustomizerHelper {
	
	private ArrayList<String> args;

	private boolean hasUserProvidedArguments = false;
	private boolean hasToolingProvidedArguments = false;
	
	private static final String ARG_SETTINGS_FILE_1 = "-c";
	private static final String ARG_SETTINGS_FILE_2 = "--settings-file";

	/**
	 * Create an ArgumentsCustomizerHelper with a list of user-provided arguments.
	 * User provided arguments may be null, meaning that the user did not provide
	 * an arguments list. The null case is considered distinct from the case
	 * where user explicitly provided an empty argument list.
	 * 
	 * @param userProvidedArguments
	 */
	public ArgumentsCustomizerHelper(String[] userProvidedArguments) {
		this.args = new ArrayList<String>();
		if (userProvidedArguments!=null) {
			hasUserProvidedArguments = true;
			args.addAll(Arrays.asList(userProvidedArguments));
		}
	}
	
	/**
	 * Add a '--settings-file' argument but only if there's not one there already.
	 */
	public void addSettingsFile(File settingsPath) {
		for (String a : args) {
			if (a.equals(ARG_SETTINGS_FILE_1)|| a.equals(ARG_SETTINGS_FILE_2)) {
				return; //already has 'settings' argument so ignore added argument.
			}
		}
		add("-c");
		add(settingsPath.toString());
	}
	
	/**
	 * Add a '--init-script' argument. This argument is added unconditionally (because
	 * multiple '--init-script' arguments are allowed).
	 */
	public void addInitScript(File initScript) {
		add("--init-script");
		add(initScript.toString());
	}

	public void add(String string) {
		args.add(string);
		hasToolingProvidedArguments = true;
	}
	
	public boolean hasUserProvidedArguments() {
		return hasUserProvidedArguments;
	}

	public boolean hasArguments() {
		return hasUserProvidedArguments || hasToolingProvidedArguments;
	}

	public String[] getArguments() {
		return args.toArray(new String[args.size()]);
	}


}
