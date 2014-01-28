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
package org.springsource.ide.eclipse.gradle.core.test.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurationRequest;
import org.springsource.ide.eclipse.gradle.core.api.IProjectConfigurator;

/**
 * Project configurators used in unit tests
 * 
 * @author Alex Boyko
 *
 */
public class TestProjectConfigurators {
	
	public static final String DELIMITER = ":";

	public static final String INITIAL_COMMENT_SINGLE = "Test_Single";
	public static final String SINGLE_CONF = "singleConfig";
	
	public static final String INITIAL_COMMENT_TREE = "Test_Tree";
	public static final String TREE_CONF1 = "treeConfig-1";
	public static final String TREE_CONF2 = "treeConfig-2";
	
	public static final String INITIAL_COMMENT_DAG = "Test_DAG";
	public static final String DAG_CONF_A = "dagConfigA";
	public static final String DAG_CONF_B = "dagConfigB";
	public static final String DAG_CONF_C = "dagConfigC";
	public static final String DAG_CONF_D = "dagConfigD";
	public static final String DAG_CONF_E = "dagConfigE";
	
	public static abstract class TestConfigurator implements IProjectConfigurator {
		
		abstract protected String getPrefix();
		
		abstract protected String getAddition();
		
		@Override
		public void configure(IProjectConfigurationRequest request,
				IProgressMonitor monitor) throws Exception {
			IProject project = request.getProject();
			IProjectDescription description = request.getProject().getDescription();
			if (description.getComment().startsWith(getPrefix())) {
				description.setComment(description.getComment() + DELIMITER + getAddition());
				project.setDescription(description, monitor);
			}
		}
	}
	
	public static class SingleConf extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_SINGLE;
		}
		@Override
		protected String getAddition() {
			return SINGLE_CONF;
		}
	}
	
	public static class TreeConf1 extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_TREE;
		}
		@Override
		protected String getAddition() {
			return TREE_CONF1;
		}
	}
	
	public static class TreeConf2 extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_TREE;
		}
		@Override
		protected String getAddition() {
			return TREE_CONF2;
		}
	}

	public static class DagConfA extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG;
		}
		@Override
		protected String getAddition() {
			return DAG_CONF_A;
		}
	}

	public static class DagConfB extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG;
		}
		@Override
		protected String getAddition() {
			return DAG_CONF_B;
		}
	}

	public static class DagConfC extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG;
		}
		@Override
		protected String getAddition() {
			return DAG_CONF_C;
		}
	}

	public static class DagConfD extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG;
		}
		@Override
		protected String getAddition() {
			return DAG_CONF_D;
		}
	}

	public static class DagConfE extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG;
		}
		@Override
		protected String getAddition() {
			return DAG_CONF_E;
		}
	}

}
