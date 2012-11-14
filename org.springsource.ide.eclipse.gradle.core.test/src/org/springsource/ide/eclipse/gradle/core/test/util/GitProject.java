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
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Assert;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager;


/**
 * Utility class for fetching a 'clean' checked out git project from some particular git repo
 * and with some particular commit hash.
 * 
 * @author Kris De Volder
 */
public class GitProject {
	
	private String projectName;
	private URI repo;
	private String checkout;
	private boolean recursive = false;
	
	private ExternalCommand gitCommand;
	private ExternalCommand checkoutCommand;
	private ExternalCommand resetCommand;
	private ExternalCommand cleanCommand;
	private ExternalCommand pullCommand;

	public GitProject(String projectName, URI repo, String checkout) {
		Assert.isNotNull(projectName);
		Assert.isNotNull(repo);
		Assert.isNotNull(checkout);
		this.projectName = projectName;
		this.repo = repo;
		this.checkout = checkout;
	}
	
	public GitProject setRecursive(boolean recursice) {
		this.recursive = recursice;
		this.gitCommand = null;
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("GitProject {\n");
		
		buf.append("   ");
		buf.append(getCloneCommand());
		buf.append("\n");
		
		buf.append("   ");
		buf.append(getCheckoutCommand());
		buf.append("\n");
		
		buf.append("   ");
		buf.append(getResetCommand());
		buf.append("\n");
		
		buf.append("   ");
		buf.append(getCleanCommand());
		buf.append("\n");
		
		buf.append("}\n");
		
		return buf.toString();
	}

	private ExternalCommand getResetCommand() {
		if (resetCommand==null) {
			resetCommand = new ExternalCommand("git", "reset", "--hard", checkoutRef());
		}
		return resetCommand;
	}

	private String checkoutRef() {
		if (checkout==null) {
			return "HEAD";
		} else {
			return checkout;
		}
	}

	private ExternalCommand getCheckoutCommand() {
		if (checkoutCommand==null) {
			checkoutCommand = new ExternalCommand("git", "checkout", checkout);
		}
		return checkoutCommand;
	}

	private ExternalCommand getPullCommand() {
	    if (pullCommand==null) {
	        pullCommand = new ExternalCommand("git", "pull", "-f", "origin", checkout);
	    }
	    return pullCommand;
	}
	
	private ExternalCommand getCloneCommand() {
		if (gitCommand==null) {
			gitCommand = new ExternalCommand("git", "clone", recursive?"--recursive":null, repo.toString());
		}
		return gitCommand;
	}

	public File checkout() throws IOException, InterruptedException {
		File gitDir = new File(DownloadManager.getDefault().getCacheDir(), "gitprojects");
		File projectDir = new File(gitDir, projectName);
		if (projectDir.exists()) {
			try {
				System.out.println("Reusing cached test project: '"+projectName+"'");
				System.out.println("     ..............location: "+projectDir);
				getCheckoutCommand().exec(projectDir);
				getResetCommand().exec(projectDir);
				getCleanCommand().exec(projectDir);
				return projectDir;
			} catch (Throwable e) {
				e.printStackTrace();
				//Something is wrong with the git project in the cache...
				FileUtils.deleteDirectory(projectDir);
			}
		}

		//Either project clone didn't exist yet, or something failed trying to use it
		System.out.println("Cloning test project: '"+projectName+"'");
		System.out.println("     Target location: "+projectDir);
		
		FileUtils.forceMkdir(gitDir);
		getCloneCommand().exec(gitDir);
		getCheckoutCommand().exec(projectDir);
		return projectDir;
	}

	public File forcePull() throws IOException, InterruptedException {
	    File projectDir = checkout();
	    getPullCommand().exec(projectDir);
	    return projectDir;
	}
	
	private ExternalCommand getCleanCommand() {
		if (cleanCommand==null) {
			cleanCommand = new ExternalCommand("git", "clean", "-f", "-x", "-d");
		}
		return cleanCommand;
	}

}
