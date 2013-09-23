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
package org.springsource.ide.eclipse.gradle.ui.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Base class containing common functionality useful to implement label providers.
 * 
 * @author Kris De Volder
 */
public abstract class GradleLabelProvider extends BaseLabelProvider {

	protected Image getImage(String location) {
		InputStream imageStream = getClass().getClassLoader().getResourceAsStream(location);
		try {
			return new Image(null, imageStream);
		} finally {
			try {
				imageStream.close();
			} catch (IOException e) {
			}
		}
	}

}
