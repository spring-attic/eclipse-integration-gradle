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
package org.springsource.ide.eclipse.gradle.core.modelmanager;

import java.io.File;

import org.springsource.ide.eclipse.gradle.core.GradleProject;

/**
 * Object that can be used as key that identifies a model type + project. Implements hashcode and
 * equals so can be used in a HashMap.
 * 
 * @author Kris De Volder
 */
class ModelKey {
	
	private Class<?> type;
	private File projectLoc;
	
	public ModelKey(Class<?> type, GradleProject p) {
		this(type, p.getLocation());
	}
	
	private ModelKey(Class<?> type, File projectLoc) {
		super();
		this.type = type;
		this.projectLoc = projectLoc;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((projectLoc == null) ? 0 : projectLoc.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ModelKey other = (ModelKey) obj;
		if (projectLoc == null) {
			if (other.projectLoc != null)
				return false;
		} else if (!projectLoc.equals(other.projectLoc))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "<"+type.getSimpleName() + ", "+projectLoc + ">";
	}
	
	

}
