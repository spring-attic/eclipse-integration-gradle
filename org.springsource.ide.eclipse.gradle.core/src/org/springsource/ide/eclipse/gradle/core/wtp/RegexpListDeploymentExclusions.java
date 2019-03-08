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
package org.springsource.ide.eclipse.gradle.core.wtp;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A concrete implementation of {@link DeploymentExclusions} based on a list of
 * regexps.
 * 
 * @author Kris De Volder
 */
public class RegexpListDeploymentExclusions extends DeploymentExclusions {

	private static final Pattern MATCH_ANYTHING = Pattern.compile(".*"); //pattern that matches anything.

	/**
	 * The 'source code' of the regexps as Strings.
	 */
	private String[] sourceExps;

	/**
	 * A 'compiled' matcher created from the sources.
	 */
	private Pattern compiled;

	/**
	 * Create an instance of {@link RegexpListDeploymentExclusions}. This does not verify the 
	 * syntactic correctness of the provided regexps. To explicitly verify them call the
	 * verify method.
	 */
	public RegexpListDeploymentExclusions(String... regexps) {
		if (regexps==null) {
			regexps = new String[0];
		}
		this.sourceExps = regexps;
	}
	
	public RegexpListDeploymentExclusions(List<String> regexps) {
		this(regexps.toArray(new String[regexps.size()]));
	}

	public String[] getSourceExps() {
		return sourceExps;
	}

	/**
	 * Verifies syntactic correctness of the source exps.
	 */
	public void verify() throws PatternSyntaxException {
		compile();
	}

	private void compile() {
		if (sourceExps.length==0) {
			compiled = MATCH_ANYTHING;
		} else {
			//Build a combined regexp String
			StringBuilder combined = new StringBuilder();
			boolean addedOne = false;
			for (String regexp : sourceExps) {
				Pattern.compile(regexp); // We don't need it compiled separately, but we do this to get more specific error message if compile fails.
				if (addedOne) {
					combined.append("|"); 
				}
				combined.append("("+regexp+")");
				addedOne = true;
			}
			//Compile combined expression
			compiled = Pattern.compile(combined.toString());
		}
	}

	@Override
	public boolean shouldExclude(String jarFileName) {
		ensureCompiled();
		return compiled.matcher(jarFileName).matches();
	}
	
	private void ensureCompiled() {
		if (compiled==null) {
			compile();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("RegexpList {\n");
		for (String exp : sourceExps) {
			result.append("   ");
			result.append(exp);
			result.append("\n");
		}
		result.append("}");
		return result.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sourceExps);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegexpListDeploymentExclusions other = (RegexpListDeploymentExclusions) obj;
		if (!Arrays.equals(sourceExps, other.sourceExps))
			return false;
		return true;
	}

	
}
