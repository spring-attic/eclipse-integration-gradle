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
package org.springsource.ide.eclipse.gradle.core.dsld;

import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup;
import org.eclipse.jdt.groovy.search.VariableScope;

/**
 * A TypeLookup that returns 'Object' for anything that gets looked up in it. When this type lookup is activated it will have
 * the effect of suppressing all underlining in the Groovy editor.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class SuppressUnderliningTypeLookup extends AbstractSimplifiedTypeLookup {

	public SuppressUnderliningTypeLookup() {
	}
	
	private boolean enable = false; 
	
	public void initialize(GroovyCompilationUnit unit, VariableScope topLevelScope) {
		enable = GradleCore.getInstance().getPreferences().getGroovyEditorDisableUnderlining();
		if (enable) {
			String name = unit.getElementName();
			enable = name.endsWith(".gradle");
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.groovy.search.AbstractSimplifiedTypeLookup#lookupTypeAndDeclaration(org.eclipse.jdt.groovy.search.ClassNode, java.lang.String, org.eclipse.jdt.groovy.search.VariableScope)
	 */
	@Override
	protected TypeAndDeclaration lookupTypeAndDeclaration(ClassNode declaringType, String name, VariableScope scope) {
		if (enable) {
			return new TypeAndDeclaration(VariableScope.OBJECT_CLASS_NODE, VariableScope.OBJECT_CLASS_NODE);
		}
		return null;
	}

}
