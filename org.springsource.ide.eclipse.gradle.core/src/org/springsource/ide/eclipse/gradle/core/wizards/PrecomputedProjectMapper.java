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
package org.springsource.ide.eclipse.gradle.core.wizards;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.IProjectMapper;


/**
 * Abstract base class for project mapper that is precomputed for a given list of projects. Subclasses may provide
 * different implementation that compute the mapping in a different way (i.e. we have two implementations, one of
 * which is more suitable for nested hierarchical projects where parent project names are included in the sub-project's
 * name, and another where only the name of the project itself is used.
 * <p>
 * This base class implementation includes checking for name clashes introduced by the mapping scheme, so subclass can
 * just focus on generating the mappings.
 *  
 * @author Kris De Volder
 */
public abstract class PrecomputedProjectMapper implements IProjectMapper {
	
	//TODO: ensure consistent use of getAbsolutePath and getCanonicalPath everywhere (propopse: find all reference to getCanonical path or file
	// and replaces with getAbsolute

	/**
	 * Exception thrown when the precomputed mapping results in a name clash, where two Gradle project
	 * models end up being mapped onto the same IProject
	 */
	public static class NameClashException extends CoreException {

		private static final long serialVersionUID = 1L;
		
		public NameClashException(HierarchicalEclipseProject model1, HierarchicalEclipseProject model2, IProject project) {
			super(new Status(IStatus.ERROR, GradleCore.PLUGIN_ID, "name clash: "+GradleProject.getHierarchicalName(model1)+" and "+GradleProject.getHierarchicalName(model2)+" both get mapped to "+project.getName()));
		}

	}
	
	private Map<String, IProject> map = new HashMap<String, IProject>();
	
	/**
	 * Inverse mapping, this mapping is only used during initialisation and set to null afterwards.
	 */
	private Map<IProject, HierarchicalEclipseProject> inverseMap = new HashMap<IProject, HierarchicalEclipseProject>();

	/**
	 * Create a precomputed mapper for a given set of model instances. The mapping will be precomputed
	 * as the instance is created. If a problem, such as a name clash, is detected an exception
	 * will be raised. 
	 */
	public PrecomputedProjectMapper(Collection<HierarchicalEclipseProject> collection) throws CoreException, NameClashException {
		for (HierarchicalEclipseProject p : collection) {
			IProject target = internalComputeMapping(p);
			addMapping(p, target);
		}
		//inverseMap = null; //No longer needed now!
	}

	/**
	 * To support importing additional projects after a "partial" import, and not get messed up if
	 * the user decides to use a different name mapping scheme than used in the original import (
	 * or custom renamed projects etc...) we make sure that, no matter what the name mapping always
	 * takes existing projects into account first and only uses the computed name mapping if
	 * the project doesn't exist yet in the workspace.
	 */
	private IProject internalComputeMapping(HierarchicalEclipseProject p) throws CoreException {
		IProject existing = GradleCore.create(p).getProject();
		if (existing!=null) {
			return existing; 
		} else {
			return computeMapping(p);
		}
	}

	/**
	 * @return Gradlemodel that is mapped to given eclipse project or null, if no mapping is
	 * associated with the eclipse project yet.
	 */
	protected HierarchicalEclipseProject inverseGet(IProject target) {
		Assert.isLegal(inverseMap!=null, "inverse mapping is only maintained during initialization");
		return inverseMap.get(target);
	}

	/**
	 * A subclass must implement this method to define how a given GradleModel will be 
	 * mapped onto an IProject instance. 
	 */
	protected abstract IProject computeMapping(HierarchicalEclipseProject p) throws CoreException;
	
	private void addMapping(HierarchicalEclipseProject from, IProject to) throws NameClashException {
		HierarchicalEclipseProject existingFrom = inverseGet(to);
		if (existingFrom!=null) {
			throw new NameClashException(existingFrom, from, to);
		} 
		
		map.put(from.getProjectDirectory().getAbsolutePath(), to);
		inverseMap.put(to, from);
	}

	
	
	public final IProject get(HierarchicalEclipseProject target) {
		String projectKey = target.getProjectDirectory().getAbsolutePath();
		IProject result = map.get(projectKey);
		if (result==null) {
			Assert.isLegal(false, 
					"No project name mapping was defined for "+target+"\n" +
							"Project key is: '"+projectKey+"'\n"+
							"The name map is: \n"+
							dumpMap());
		}
		return result;
	}

	/**
	 * Dump the contents of the name -> project map into a string for debugging 
	 * purposes.
	 */
	private String dumpMap() {
		StringBuilder dump = new StringBuilder();
		for (String k : map.keySet()) {
			dump.append("  '"+k+"' => "+map.get(k)+"\n");
		}
		return dump.toString();
	}

	public Collection<HierarchicalEclipseProject> getAllProjects() {
		return inverseMap.values();
	}

}
