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
package org.springsource.ide.eclipse.gradle.core.util;

import java.util.List;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.internal.ide.misc.FileInfoAttributesMatcher;
import org.eclipse.ui.internal.ide.misc.FileInfoAttributesMatcher.Argument;

/**
 * It is somewhat complicated to figure out how to create FileInfoMatcherDescription objects like
 * the ones created by Project >> Properties >> Resources >> Filters property editor.
 * <p>
 * Therefore we create a bunch of utility methods here to make the task of creating filters programaticaly
 * more straightforward.
 * 
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class ResourceFilterFactory {
	
	private static String OR_ID = "org.eclipse.ui.ide.orFilterMatcher"; //TODO: is there an eclipse constant defining this ID?
	private static String MULTI_ID = FileInfoAttributesMatcher.ID;

	public static FileInfoMatcherDescription or(FileInfoMatcherDescription... children) {
		return new FileInfoMatcherDescription(OR_ID, children);
	}
	
	public static FileInfoMatcherDescription projectRelativePath(IPath path) {
		Argument arg = new Argument(); 
		arg.caseSensitive = true;
		arg.key = FileInfoAttributesMatcher.KEY_PROPJECT_RELATIVE_PATH;
		arg.operator = FileInfoAttributesMatcher.OPERATOR_EQUALS;
		arg.regularExpression = false;
		arg.pattern = path.toString();
		return new FileInfoMatcherDescription(MULTI_ID, FileInfoAttributesMatcher.encodeArguments(arg));
	}

	/**
	 * @param childProjectFilters
	 * @return
	 */
	public static FileInfoMatcherDescription or(List<FileInfoMatcherDescription> children) {
		return or(children.toArray(new FileInfoMatcherDescription[children.size()]));
	}

}
