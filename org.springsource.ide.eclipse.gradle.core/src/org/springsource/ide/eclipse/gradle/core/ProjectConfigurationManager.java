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
package org.springsource.ide.eclipse.gradle.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurationRequest;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurator;
import org.springsource.ide.eclipse.gradle.core.util.TopoSort;

/**
 * Gathers contributed project configurators and keeps them in topological
 * order. This manager object is able to configure projects via
 * {@link #configure(IProjectConfigurationRequest, IProgressMonitor)} method
 * 
 * @author Alex Boyko
 * 
 */
public final class ProjectConfigurationManager {
	
	private static final String EXTPT_ID = "projectConfigurators"; //$NON-NLS-1$
	private static final String ATTR__ID = "id"; //$NON-NLS-1$
	private static final String ATTR__CLASS = "class"; //$NON-NLS-1$
//	private static final String ATTR__NAME = "name"; //$NON-NLS-1$
	private static final String ATTR__AFTER = "after"; //$NON-NLS-1$
	private static final String ATTR__BEFORE = "before"; //$NON-NLS-1$
	
	private static final String IDS_SEPARATOR = ","; //$NON-NLS-1$
	
	private static ProjectConfigurationManager INSTANCE = null;
	
	private IProjectConfigurator[] configurators = new IProjectConfigurator[0];
	
	public static ProjectConfigurationManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ProjectConfigurationManager();
		}
		return INSTANCE;
	}
	
	private static List<String> extractIds(String idStr) {
		if (idStr == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(idStr, IDS_SEPARATOR);
		while (tokenizer.hasMoreElements()) {
			String id = tokenizer.nextToken().trim();
			if (!id.isEmpty()) {
				list.add(id);
			}
		}
		return list;
	}
	
	private ProjectConfigurationManager() {
		super();
		initializeFromExtensions();
	}
	
	private void initializeFromExtensions() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(GradleCore.PLUGIN_ID, EXTPT_ID);
		final Map<String, ProjectConfiguratorDescriptor> descriptorsMap = new HashMap<String, ProjectConfiguratorDescriptor>();
		for (IConfigurationElement element : elements) {
			try {
				ProjectConfiguratorDescriptor descriptor = new ProjectConfiguratorDescriptor(element);
				if (descriptorsMap.containsKey(descriptor.id)) {
					GradleCore.warn("Duplicate project configurators for id " + descriptor.id);
				} else {
					descriptorsMap.put(descriptor.id, descriptor);
				}
			} catch (CoreException e) {
				GradleCore.log(e);
			}
		}
		
		// Topologically sort project configurator descriptors.
		DescriptorsTopoSort topoSort = new DescriptorsTopoSort(createPredecessorsMap(descriptorsMap));
		List<ProjectConfiguratorDescriptor> descriptors = topoSort.getSorted();
		if (topoSort.hasCycle()) {
			GradleCore
					.log("Cycle detected in the graph constructed from contributed Gradle Project Configurators. Gradle projects may not be configured appropriately.");
		}
		
		this.configurators = new IProjectConfigurator[descriptors.size()];
		for (int i = 0; i < descriptors.size(); i++) {
			this.configurators[i] = descriptors.get(i).configurator;
		}
	}
	
	private static Map<ProjectConfiguratorDescriptor, Set<ProjectConfiguratorDescriptor>> createPredecessorsMap(Map<String, ProjectConfiguratorDescriptor> descriptors) {
		Map<ProjectConfiguratorDescriptor, Set<ProjectConfiguratorDescriptor>> predecessorsMap = new HashMap<ProjectConfiguratorDescriptor, Set<ProjectConfiguratorDescriptor>>();
		for (ProjectConfiguratorDescriptor descriptor : descriptors.values()) {
			
			// Handle "after" ids
			Set<ProjectConfiguratorDescriptor> predecessors = predecessorsMap.get(descriptor);
			if (predecessors == null) {
				predecessors = new HashSet<ProjectConfiguratorDescriptor>();
				predecessorsMap.put(descriptor, predecessors);
			}
			for (String afterId : descriptor.after) {
				ProjectConfiguratorDescriptor after = descriptors.get(afterId);
				if (after != null) {
					predecessors.add(after);
				}
			}
			
			// Handle "before" ids
			for (String beforeId : descriptor.before) {
				ProjectConfiguratorDescriptor before = descriptors.get(beforeId);
				if (before != null) {
					predecessors = predecessorsMap.get(before);
					if (predecessors == null) {
						predecessors = new HashSet<ProjectConfiguratorDescriptor>();
						predecessorsMap.put(before, predecessors);
					}
					predecessors.add(descriptor);
				}
			}
		}
		return predecessorsMap;
	}
	
	public void configure(IProjectConfigurationRequest request, IProgressMonitor monitor) {
		for (IProjectConfigurator configurator : this.configurators) {
			try {
				configurator.configure(request, monitor);
			} catch (Exception e) {
				GradleCore.log(e);
			}
		}
	}
	
	private class ProjectConfiguratorDescriptor {
		
		String id;
//		String name;
		List<String> before;
		List<String> after;
		IProjectConfigurator configurator;
		
		ProjectConfiguratorDescriptor(IConfigurationElement element) throws CoreException {
			super();
			this.id = element.getAttribute(ATTR__ID);
//			this.name = element.getAttribute(ATTR__NAME);
			this.configurator = (IProjectConfigurator) element.createExecutableExtension(ATTR__CLASS);			
			this.before = extractIds(element.getAttribute(ATTR__BEFORE));
			this.after = extractIds(element.getAttribute(ATTR__AFTER));
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ProjectConfiguratorDescriptor) {
				ProjectConfiguratorDescriptor other = (ProjectConfiguratorDescriptor) obj;
				return id.equals(other.id);
			}
			return false;
		}
		
		
	}
	
	private class DescriptorsTopoSort extends TopoSort<ProjectConfiguratorDescriptor> {
		
		public DescriptorsTopoSort(
				final Map<ProjectConfiguratorDescriptor, Set<ProjectConfiguratorDescriptor>> predecessorsMap) {
			super(predecessorsMap.keySet(), new PartialOrder<ProjectConfiguratorDescriptor>() {
				@Override
				public Collection<ProjectConfiguratorDescriptor> getPredecessors(
						ProjectConfiguratorDescriptor descriptor) {
					return predecessorsMap.get(descriptor);
				}
			});
		}

	}
}
