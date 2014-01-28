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
import java.util.List;
import java.util.Map;

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
	private static final String ELMT__SECONDARY_TO = "secondaryTo"; //$NON-NLS-1$
	
	private static ProjectConfigurationManager INSTANCE = null;
	
	private List<ProjectConfiguratorDescriptor> descriptors = Collections.emptyList();
	
	public static ProjectConfigurationManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ProjectConfigurationManager();
		}
		return INSTANCE;
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
		
		this.descriptors = new DescriptorsTopoSort(descriptorsMap).getSorted();
	}
	
	public void configure(IProjectConfigurationRequest request, IProgressMonitor monitor) {
		for (ProjectConfiguratorDescriptor descriptor : descriptors) {
			try {
				descriptor.configurator.configure(request, monitor);
			} catch (Exception e) {
				GradleCore.log(e);
			}
		}
	}
	
	private class ProjectConfiguratorDescriptor {
		
		String id;
//		String name;
		String[] secondaryTo;
		IProjectConfigurator configurator;
		
		ProjectConfiguratorDescriptor(IConfigurationElement element) throws CoreException {
			super();
			this.id = element.getAttribute(ATTR__ID);
//			this.name = element.getAttribute(ATTR__NAME);
			this.configurator = (IProjectConfigurator) element.createExecutableExtension(ATTR__CLASS);
			IConfigurationElement[] secondaryElements = element.getChildren(ELMT__SECONDARY_TO);
			this.secondaryTo = new String[secondaryElements.length];
			for (int i = 0; i < secondaryElements.length; i++) {
				this.secondaryTo[i] = secondaryElements[i].getAttribute(ATTR__ID);
			}
		}	
	}
	
	private class DescriptorsTopoSort extends TopoSort<ProjectConfiguratorDescriptor> {
		
		public DescriptorsTopoSort(
				final Map<String, ProjectConfiguratorDescriptor> descriptors) {
			super(descriptors.values(), new PartialOrder<ProjectConfiguratorDescriptor>() {
				@Override
				public Collection<ProjectConfiguratorDescriptor> getPredecessors(
						ProjectConfiguratorDescriptor descriptor) {
					if (descriptor.secondaryTo.length == 0) {
						return Collections.emptyList();
					} else {
						ArrayList<ProjectConfiguratorDescriptor> predecessors = new ArrayList<ProjectConfiguratorDescriptor>(
								descriptor.secondaryTo.length);
						for (String id : descriptor.secondaryTo) {
							ProjectConfiguratorDescriptor predecessor = descriptors.get(id);
							if (predecessor != null) {
								predecessors.add(predecessor);
							}
						}
						return predecessors;
					}
				}
			});
		}

	}
}
