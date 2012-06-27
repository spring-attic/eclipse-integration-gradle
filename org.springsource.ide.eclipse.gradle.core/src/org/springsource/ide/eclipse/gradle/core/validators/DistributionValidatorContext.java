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
package org.springsource.ide.eclipse.gradle.core.validators;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An abstraction of the context the {@link DistributionValidator} needs in order to work.
 * Typically this will be a {@link DistributionSection} on the Gradle preferences page,
 * but during testing it will be a mock context.
 * 
 * @author Kris De Volder
 */
public interface DistributionValidatorContext {
	URI getDistroInPage() throws URISyntaxException;
}
