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
package org.springsource.ide.eclipse.gradle.core.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Christian Dupuis
 * @author Kris De Volder
 * @since 2.0
 */
public class ZipFileUtil {

	/**
	 * This Zip utility doesn't know how to extract file permission correctly
	 * from zip files (does Java ZipEntry even provide this info? It seems
	 * not!).
	 * <p>
	 * To remedy this problem, clients can provide their own logic for setting
	 * permissions they may care about. A subclass of PermissionSetter is used
	 * to determine the permission setting logic.
	 * 
	 * @author Kris De Volder
	 * 
	 * @since 2.8
	 */
	public static abstract class PermissionSetter {

		/**
		 * Called after a file was succesfully extracted from the zip archive.
		 * @throws IOException
		 */
		public abstract void fileUnzipped(ZipEntry entry, File entryFile) throws IOException;

		/**
		 * A permission setter that does nothing.
		 */
		public static final PermissionSetter NULL = new PermissionSetter() {
			@Override
			public void fileUnzipped(ZipEntry entry, File entryFile) {
				// Do nothing
			}
		};

		/**
		 * Creates an executable permission setter based on a list of file
		 * extensions.
		 * <p>
		 * Any file ending with the extension will be made executable.
		 */
		public static PermissionSetter executableExtensions(final String... exts) {
			if (OsUtils.isWindows()) {
				// It may be ok to do nothing for windows? (note: not tested!)
				return NULL;
			}
			else {
				return new PermissionSetter() {
					@Override
					public void fileUnzipped(ZipEntry entry, File entryFile) throws IOException {
						String name = entryFile.toString();
						for (String ext : exts) {
							if (name.endsWith(ext)) {
								// This only works with Java 6:
								entryFile.setExecutable(true);
							}
						}
					}
				};
			}
		}

	}

	private static final int BUFFER_SIZE = 512 * 1024;

	public static void unzip(URL source, File targetFile) throws IOException {
		unzip(source, targetFile, null);
	}

	public static void unzip(URL source, File targetFile, String prefix) throws IOException {
		unzip(source, targetFile, prefix, PermissionSetter.NULL);
	}

	public static void unzip(URL source, File targetFile, String prefix, PermissionSetter permsetter) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		ZipInputStream zipIn = new ZipInputStream(source.openStream());
		try {
			ZipEntry entry;
			Path normalizedTargetFilePath = targetFile.toPath().normalize();
			while ((entry = zipIn.getNextEntry()) != null) {
				String name = entry.getName();
				if (prefix != null) {
					if (name.startsWith(prefix)) {
						name = name.substring(prefix.length());
						if (name.length() > 1) {
							// cut off separator
							name = name.substring(1);
						}
					} else {
						continue;
					}
				}

				File entryFile = new File(targetFile, name);
				/*
				 * Ensure the outputdir + name doesn't leave the outputdir.
				 */
				if (!entryFile.toPath().normalize().startsWith(normalizedTargetFilePath)) {
					throw new ZipException(
							"The file " + name + " is trying to leave the target output directory of " + targetFile);
				}
				if (entry.isDirectory()) {
					entryFile.mkdirs();
				}
				else {												
					entryFile.getParentFile().mkdirs();
					FileOutputStream out = new FileOutputStream(entryFile);
					try {
						int len;
						while ((len = zipIn.read(buffer)) >= 0) {
							out.write(buffer, 0, len);
						}
					}
					finally {
						out.close();
					}
					long modTime = entry.getTime();
					if (modTime > 0) {
						entryFile.setLastModified(modTime);
					}
					permsetter.fileUnzipped(entry, entryFile);
				}
			}
		}
		finally {
			zipIn.close();
		}
	}

}
