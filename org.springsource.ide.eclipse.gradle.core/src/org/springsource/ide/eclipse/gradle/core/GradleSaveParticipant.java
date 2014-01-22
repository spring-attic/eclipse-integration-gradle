/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * This save participant maintains a per-project key value store. The keys are Strings and the
 * values must be serialisible objects. The key value store is persisted during full workspace
 * save operations.  
 * <p>
 * Also, key value pairs associated with projects that no longer exist in the workspace will
 * be deleted during the periodic save operation.
 *  
 * @author Kris De Volder
 */
public class GradleSaveParticipant implements ISaveParticipant {

	public static class ProjectStore extends HashMap<String, Serializable> {
		private static final long serialVersionUID = 1L;
	}

	private static final boolean DEBUG = false; // (""+Platform.getLocation()).contains("kdvolder");

	private static GradleSaveParticipant instance;
	
	private Map<String, ProjectStore> store = null;

	/**
	 * Singleton: use getInstance
	 */
	private GradleSaveParticipant() {
	}

	public void doneSaving(ISaveContext context) {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		if (context.getKind()==ISaveContext.FULL_SAVE) {
			save();
		}
	}

	private void save() {
		debug("Saving gradle workspace state...");
		File file = getSaveFile();
		if (file!=null) {
			debug("file = "+file);
			if (file.exists()) {
				debug(file + " exists. Deleting it");
				file.delete();
			}
			ObjectOutputStream out = null;
			try {
				out = new ObjectOutputStream(new FileOutputStream(file));
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (Iterator<Entry<String, ProjectStore>> iterator = store.entrySet().iterator(); iterator.hasNext();) {
					Entry<String, ProjectStore> entry = (Entry<String, ProjectStore>) iterator.next();
					IProject project = root.getProject(entry.getKey());
					if (!project.exists()) {
						iterator.remove();
					}
				}
				out.writeObject(store);
				debug("Saving gradle workspace state DONE");
			} catch (FileNotFoundException e) {
				GradleCore.log(e);
			} catch (IOException e) {
				GradleCore.log(e);
			} finally {
				if (out!=null) {
					try {
						out.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			GradleCore.warn("Gradle workspace state couldn't be saved. No state location.");
		}
	}

	private static void debug(String string) {
		if (DEBUG) {
			System.out.println("GradleSaveParticipant: "+string);
		}
	}

	/**
	 * Called when the gradleCore plugin is starting. 
	 */
	public static synchronized GradleSaveParticipant getInstance() {
		if (instance==null) {
			debug("Starting gradle save participant");
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			instance = new GradleSaveParticipant();
			try {
				ISavedState savedState = ws.addSaveParticipant(GradleCore.PLUGIN_ID, instance);
//				if (savedState!=null) {
					instance.restore();
//				}
			} catch (CoreException e) {
				GradleCore.log(e);
			}
			
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private synchronized void restore() {
		debug("Restoring gradle workspace state...");
		File file = getSaveFile();
		if (file!=null) {
			//Should really never be null, but aparantly it does happen after a workspace crash.
			//It should be ok to skip restoring the state, there's nothing essential in this state just some cached.
			debug("file = "+file);
			Exception e = null;
			ObjectInputStream in = null;
			try {
				if (file!=null && file.exists()) {
					in = new ObjectInputStream(new FileInputStream(file));
					store = (Map<String, ProjectStore>) in.readObject();
					debug("Restoring gradle workspace state...OK");
				} else {
					debug("Not restroring state... save file not found: "+file);
				}
			} catch (IOException _e) {
				e = _e;
			} catch (ClassNotFoundException _e) {
				e = _e;
			} finally {
				if (in!=null) {
					try {
						in.close();
					} catch (IOException e1) {
					}
				}
			}
			if (e!=null) {
				debug("Restoring gradle workspace state...FAILED: "+e.getMessage());
				//File corrupt?
				GradleCore.log(e);
				if (file!=null && file.exists()) {
					file.delete();
					debug("Deleting save file: "+file);
				}
			}
		}
	}
	
	private File getSaveFile() {
		GradleCore instance = GradleCore.getInstance();
		if (instance!=null) {
			IPath stateLoc = instance.getStateLocation();
			if (stateLoc!=null) {
				IPath saveLoc = stateLoc.append("gradleWSState");
				return saveLoc.toFile();
			}
		}
		return null;
	}

	/**
	 * Insert a key value pair into a persisted store. This store is saved whenever there
	 * is a full workspace save. Key value pairs are associated with an Eclipse project and
	 * upon each save, keys will be removed when projects no longer exist in the workspace.
	 */
	public synchronized void put(IProject project, String key, Serializable value) {
		if (store==null) {
			store = new HashMap<String, GradleSaveParticipant.ProjectStore>();
		}
		ProjectStore projectStore = store.get(project.getName());
		if (projectStore==null) {
			projectStore = new ProjectStore();
			store.put(project.getName(), projectStore);
		}
		projectStore.put(key, value);
	}

	/**
	 * Retrieve a persisted value from the persistent store. This may return null if
	 * no such value was persisted or if it was deleted.
	 */
	public Serializable get(IProject project, String key) {
		if (store!=null) {
			ProjectStore projectStore = store.get(project.getName());
			if (projectStore!=null) {
				return projectStore.get(key);
			}
		}
		return null;
	}

}
