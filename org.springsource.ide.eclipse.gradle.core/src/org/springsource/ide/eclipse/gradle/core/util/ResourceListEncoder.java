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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;

/**
 * Utility class providing methods to encode and decode a list of IResources into a single String.
 * 
 * @author Kris De Volder
 */
public class ResourceListEncoder {

	/**
	 * Helper method to encode a list of IFile and IFolder into a single String.
	 */
	public static String encode(boolean projectRelative, List<IResource> resources) {
		Assert.isLegal(projectRelative); // Only implemented for project relative so far, can generalize implementation if needed.
		String[] paths = new String[resources.size()];
		IProject project = null;
		for (int i = 0; i < paths.length; i++) {
			IResource rsrc = resources.get(i);
			if (project==null) {
				project = rsrc.getProject();
			}
			Assert.isLegal(project.equals(rsrc.getProject()));
			if (rsrc instanceof IFile) {
				paths[i] = rsrc.getProjectRelativePath().toString();
			} else if (rsrc instanceof IFolder) {
				paths[i] = rsrc.getProjectRelativePath().toString();
				if (!paths[i].endsWith("/")) {
					paths[i] = paths[i]+"/";
				}
			} else {
				throw new IllegalArgumentException("Only IFile or IFolder are allowed: "+rsrc);
			}
		}
		return ArrayEncoder.encode(paths);
	}

	/**
	 * Decode a list of IResources from a previously encoded String.
	 * @param project  null if absolute encoding, project if project relative encoding was used.
	 * @param encoded  String to be decoded.
	 */
	public static List<IResource> decode(IProject project, String encoded) {
		Assert.isNotNull(project); // not null means project relative encoding, which is the only mode supported for now.
		String[] decoded = ArrayEncoder.decode(encoded);
		IResource[] resources = new IResource[decoded.length];
		for (int i = 0; i < resources.length; i++) {
			if (decoded[i].endsWith("/")) {
				resources[i] = project.getFolder(new Path(decoded[i]));
			} else {
				resources[i] = project.getFile(new Path(decoded[i]));
			}
		}
		return Arrays.asList(resources);
	}

}
