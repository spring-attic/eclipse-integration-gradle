/*******************************************************************************
 * Copyright (c) 2012 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.test.util;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.springsource.ide.eclipse.gradle.core.util.ErrorHandler;


/**
 * Resource listener, mostly intended for testing purposes, to listen for
 * the addition/creation of resources in the workspace.
 * <p>
 * While active, the listener will remember all added resources and can
 * later be queried to test whether resources that were expected to
 * be added to workspace where indeed added.
 * 
 * @author Kris De Volder
 */
public class AddedResourceListener implements IResourceChangeListener {
	
	private HashSet<IResource> added = new HashSet<IResource>();
	
	/**
	 * This error handler is used to avoid loosing errors in the listener.
	 * When the listener is disposed, it will rethrow at least one error 
	 * if some exceptions got caught in the listener.
	 */
	private ErrorHandler eh = new ErrorHandler.KeepFirst();

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if ((delta.getKind()&IResourceDelta.ADDED)!=0) {
						IResource addedResource = delta.getResource();
						if (addedResource!=null) {
							System.out.println("added: "+addedResource);
							added.add(addedResource);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			eh.handleError(e);
		}
	}

	/**
	 * Create an register a FileCreationListener
	 */
	public static AddedResourceListener create() {
		AddedResourceListener listener = new AddedResourceListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		return listener;
	}

	/**
	 * Deregister this listener from the workspace. 
	 * @throws CoreException (if problems happened while listenening).
	 */
	public void dispose() throws CoreException {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		eh.rethrowAsCore();
	}

	/**
	 * @return whether a given resource was added to the workspace during the listeners lifetime.
	 */
	public boolean wasAdded(IResource theFile) {
		return added.contains(theFile);
	}

}
