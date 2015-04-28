/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.GradleClassPathContainer;

/**
 * Helper class to simplify manipulation of a project's class path.
 * @author Kris De Volder
 */
public class ClassPath {
	
	public static boolean isContainerOnClasspath(IJavaProject jp, String containerID) {
		try {
			IClasspathEntry[] entries = jp.getRawClasspath();
			for (IClasspathEntry e : entries) {
				if (e.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
					IPath path = e.getPath();
					if (containerID.equals(path.segment(0))) {
						return true;
					}
				}
			}
		} catch (JavaModelException e) {
			GradleCore.log(e);
		}
		return false;
	}
	
	public boolean DEBUG = false;
	
	private void debug(String msg) {
		if (DEBUG) {
			System.out.println("ClassPath: "+msg);
		}
	}
	
	/**
	 * This array defines the ordering classpath entries based on their kinds.
	 */
	public static final int[] kindOrdering = {
		IClasspathEntry.CPE_SOURCE,
		IClasspathEntry.CPE_LIBRARY,
		IClasspathEntry.CPE_CONTAINER,
		IClasspathEntry.CPE_VARIABLE,
		IClasspathEntry.CPE_PROJECT
	};
	
	/**
	 * Entries, divided-up by category so that categories always maintain their order no matter
	 * what order entries are added in.
	 */
	private Map<Integer, Collection<IClasspathEntry>> entryMap = new HashMap<Integer, Collection<IClasspathEntry>>(kindOrdering.length);

	public class ClasspathEntryComparator implements Comparator<IClasspathEntry> {
		@Override
		public int compare(IClasspathEntry e1, IClasspathEntry e2) {
			Assert.isLegal(e1.getEntryKind()==e2.getEntryKind(), "Only entries with the same kind should be compared");
			String path1 = getCompareString(e1);
			String path2 = getCompareString(e2);
			return path1.compareTo(path2);
		}

		private String getCompareString(IClasspathEntry e) {
			String str = e.getPath().toString();
			if (e.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
				if (str.startsWith(JavaRuntime.JRE_CONTAINER)) {
					/*
					 * Make JRE Container to be the first
					 */
					str = "A";
				} else if (str.startsWith(GradleClassPathContainer.ID)) {
					/*
					 * Make Gradle Dependencies to be the second after the JRE container
					 */
					str = "AA";
				} else if (str.startsWith("GROOVY_")) {
					//STS-3382: DSL support Groovy container should be last entry on classpath
					str = "zzz"+str; 
				}
			}
			return str;
		}
	}
	
	public class ClassnameEntryComparator implements Comparator<IClasspathEntry> {
		@Override
		public int compare(IClasspathEntry e1, IClasspathEntry e2) {
			Assert.isLegal(e1.getEntryKind()==e2.getEntryKind(), "Only entries with the same kind should be compared");
			String name1 = e1.getPath().lastSegment().toLowerCase();
			String name2 = e2.getPath().lastSegment().toLowerCase();
			return name1.compareTo(name2);
		}
	}

	private boolean enablePathSorting; //If true, entries of the same kind will be sorted by classpath.
	private boolean enableNameSorting; //If true, entries of the same kind will be sorted by name.

	/** 
	 * Create a classpath prepopoluated with a set of raw classpath entries.
	 * 
	 * @param project 
	 * @param i
	 */
	public ClassPath(GradleProject project, IClasspathEntry[] rawEntries) {
		this(project);
		addAll(Arrays.asList(rawEntries));
	}
	
	public ClassPath(GradleProject project) {
		this.enablePathSorting = project.getProjectPreferences().getEnableClasspathEntrySorting();
		this.enableNameSorting = project.getProjectPreferences().getEnableClassnameEntrySorting();
	}

	public Collection<IClasspathEntry> createEntrySet(int size) {
		if (enablePathSorting) {
			return new TreeSet<IClasspathEntry>(new ClasspathEntryComparator());
		} if (enableNameSorting) {
			return new TreeSet<IClasspathEntry>(new ClassnameEntryComparator());
		} else {
			return new LinkedHashSet<IClasspathEntry>();
		}
	}

	/**
	 * See STS-2054, it is not sufficient to avoid adding duplicates, sometimes gradle
	 * adds them and it is nice for us to remove them if it does.
	 */
	private void removeDuplicateJREContainers() {
		Collection<IClasspathEntry> entries = getEntries(IClasspathEntry.CPE_CONTAINER);
		Iterator<IClasspathEntry> iterator = entries.iterator();
		IClasspathEntry jreContainer = null;
		while (iterator.hasNext()) {
			IClasspathEntry element = iterator.next();
			if (isJREContainer(element)) {
				if (jreContainer!=null) {
					iterator.remove(); //Already seen a container, so this one is duplicate!
				} else {
					jreContainer = element;
				}
			}
		}
	}
	
	/**
	 * Retrieves the classpath entries of a particular kind only.
	 */
	private Collection<IClasspathEntry> getEntries(int kind) {
		Collection<IClasspathEntry> entries = entryMap.get(kind);
		if (entries==null) {
			entries = createEntrySet(0);
			entryMap.put(kind, entries);
		}
		return entries;
	}

	/**
	 * Removes all library entries from this classpath.
	 */
	public void removeLibraryEntries() {
		entryMap.remove(IClasspathEntry.CPE_LIBRARY);
	}
	
	/**
	 * Removes all project entries from this classpath.
	 */
	public void removeProjectEntries() {
		entryMap.remove(IClasspathEntry.CPE_PROJECT);
	}

	public IClasspathEntry[] toArray() {
		ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		for (int kind : kindOrdering) {
			entries.addAll(getEntries(kind));
		}
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	public void add(IClasspathEntry newEntry) {
		int kind = newEntry.getEntryKind();
		Collection<IClasspathEntry> entries = getEntries(kind);
		entries.add(newEntry);
	}

	/**
	 * Make sure that a JRE container is present in the classpath. If one is already there, leave it as is.
	 * Otherwise add a default one.
	 */
	public void ensureJREContainer() {
		removeDuplicateJREContainers(); //STS-2054
		IClasspathEntry currentJREContainer = getContainer(JavaRuntime.JRE_CONTAINER);
		if (currentJREContainer==null) {
			add(JavaRuntime.getDefaultJREContainerEntry());
		}
	}

	/**
	 * Find class path container entry with given container ID. If more than one entry 
	 * exists the first one found will be returned. If no entry is found null will be
	 * returned.
	 * 
	 * @param containerID
	 * @return First matching classpath entry or null if no match.
	 */
	public IClasspathEntry getContainer(String containerID) {
		for (IClasspathEntry e : getEntries(IClasspathEntry.CPE_CONTAINER)) {
			Assert.isLegal(e.getEntryKind()==IClasspathEntry.CPE_CONTAINER);
			if (containerID.equals(e.getPath().segment(0))) {
				return e;
			}
		}
		return null;
	}
	
	public void removeContainer(String containerID) {
		Collection<IClasspathEntry> containers = getEntries(IClasspathEntry.CPE_CONTAINER);
		Iterator<IClasspathEntry> iter = containers.iterator();
		while (iter.hasNext()) {
			IClasspathEntry el = iter.next();
			if (el.getPath().segment(0).equals(containerID)) {
				iter.remove();
			}
		}
	}
	
	private boolean isJREContainer(IClasspathEntry e) {
		return isContainer(e, JavaRuntime.JRE_CONTAINER);
	}

	private boolean isContainer(IClasspathEntry e, String containerID) {
		return e.getEntryKind()==IClasspathEntry.CPE_CONTAINER
				&& containerID.equals(e.getPath().segment(0));
	}

	/**
	 * @param sourceEntries
	 */
	public void addAll(List<IClasspathEntry> toAdd) {
		for (IClasspathEntry e : toAdd) {
			add(e);
		}
	}

	/**
	 * @param javaProject
	 * @param subProgressMonitor
	 */
	public void setOn(IJavaProject javaProject, IProgressMonitor mon) throws JavaModelException {
		mon.beginTask("Setting classpath...", 2);
		try {
			IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();
			mon.worked(1);
			IClasspathEntry[] newClasspath = toArray();
			if (isChanged(oldClasspath, newClasspath)) {
				debug("setRawClasspath...");
				javaProject.setRawClasspath(newClasspath, new SubProgressMonitor(mon, 1));
				debug("setRawClasspath DONE");
			} else {
				debug("Skipping setRawClasspath because old == new");
			}
		} finally {
			mon.done();
		}
	}

	private boolean isChanged(IClasspathEntry[] oldClasspath, IClasspathEntry[] newClasspath) {
		if (oldClasspath.length!=newClasspath.length) 
			return true;
		for (int i = 0; i < newClasspath.length; i++) {
			if (!oldClasspath[i].equals(newClasspath[i])) {
				return true;
			}
		}
		return false;
	}

	public IClasspathEntry[] getSourceFolders() {
		Collection<IClasspathEntry> entries = getEntries(IClasspathEntry.CPE_SOURCE);
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	public IClasspathEntry[] getLibraryEntries() {
		Collection<IClasspathEntry> entries = getEntries(IClasspathEntry.CPE_LIBRARY);
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

}
