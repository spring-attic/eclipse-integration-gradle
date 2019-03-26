/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
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
	
	public static final String INITIAL_COMMENT_DAG_BEFORE = "Test_Before_DAG";
	public static final String DAG_BEFORE_A = "dag_before_A"; //$NON-NLS-N1$
	public static final String DAG_BEFORE_B = "dag_before_B"; //$NON-NLS-N1$
	public static final String DAG_BEFORE_C = "dag_before_C"; //$NON-NLS-N1$
	public static final String DAG_BEFORE_D = "dag_before_D"; //$NON-NLS-N1$
	
	public static final String INITIAL_COMMENT_DAG_AFTER = "Test_After_DAG";
	public static final String DAG_AFTER_A = "dag_after_A"; //$NON-NLS-N1$
	public static final String DAG_AFTER_B = "dag_after_B"; //$NON-NLS-N1$
	public static final String DAG_AFTER_C = "dag_after_C"; //$NON-NLS-N1$
	public static final String DAG_AFTER_D = "dag_after_D"; //$NON-NLS-N1$

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

	public static class DagBeforeA extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_BEFORE;
		}
		@Override
		protected String getAddition() {
			return DAG_BEFORE_A;
		}
	}

	public static class DagBeforeB extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_BEFORE;
		}
		@Override
		protected String getAddition() {
			return DAG_BEFORE_B;
		}
	}
	
	public static class DagBeforeC extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_BEFORE;
		}
		@Override
		protected String getAddition() {
			return DAG_BEFORE_C;
		}
	}
	
	public static class DagBeforeD extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_BEFORE;
		}
		@Override
		protected String getAddition() {
			return DAG_BEFORE_D;
		}
	}

	public static class DagAfterA extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_AFTER;
		}
		@Override
		protected String getAddition() {
			return DAG_AFTER_A;
		}
	}

	public static class DagAfterB extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_AFTER;
		}
		@Override
		protected String getAddition() {
			return DAG_AFTER_B;
		}
	}
	
	public static class DagAfterC extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_AFTER;
		}
		@Override
		protected String getAddition() {
			return DAG_AFTER_C;
		}
	}
	
	public static class DagAfterD extends TestConfigurator {
		@Override
		protected String getPrefix() {
			return INITIAL_COMMENT_DAG_AFTER;
		}
		@Override
		protected String getAddition() {
			return DAG_AFTER_D;
		}
	}
	
}
