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
package org.springsource.ide.eclipse.gradle.core.test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.util.Distributions;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager;
import org.springsource.ide.eclipse.gradle.core.util.ZipFileUtil;
import org.springsource.ide.eclipse.gradle.core.util.DownloadManager.DownloadRequestor;
import org.springsource.ide.eclipse.gradle.core.util.expression.LiveExpression;
import org.springsource.ide.eclipse.gradle.core.util.expression.ValueListener;
import org.springsource.ide.eclipse.gradle.core.validators.DistributionValidator;
import org.springsource.ide.eclipse.gradle.core.validators.DistributionValidatorContext;
import org.springsource.ide.eclipse.gradle.core.validators.ValidationResult;


/**
 * @author Kris De Volder
 */
public class DistributionValidatorTest extends GradleTest {
	
	public class UnzippedDistribution implements DownloadRequestor {
		File location = null;

		/**
		 * This method is called by the download manager once the zip file is available
		 */
		public void exec(File zipFile) throws Exception {
			File unzipped = TestUtils.createTempDirectory("gradle-distro");
			ZipFileUtil.unzip(zipFile.toURI().toURL(), unzipped);
			//Assume that the unzipped dir contains exactly one subdirectory. This should be the
			//gradle distribution.
			for (File subDir : unzipped.listFiles()) {
				if (location==null) {
					location = subDir;
				} else {
					throw new RuntimeException("Distribution zip has mutliple subdirs or files: "+zipFile);
				}
			}
		}
	}

	/**
	 * Mock context that provides the info that needs to be validated.
	 */
	class MockContext implements DistributionValidatorContext {
		private String uriTxt = null;
		public MockContext(String distro) {
			if (distro!=null) {
				this.uriTxt = distro;
			}
		}
		public URI getDistroInPage() throws URISyntaxException {
			if (uriTxt!=null) {
				return new URI(uriTxt);
			}
			return null;
		}
	}
	
	public class MockListener implements ValueListener<ValidationResult> {
		private ValidationResult lastResult = null;

		public void gotValue(LiveExpression<ValidationResult> exp, ValidationResult value) {
			lastResult = value;
		}
		public ValidationResult getResult() {
			return lastResult;
		}
	}
	
	public static void assertOK(ValidationResult result) {
		assertNotNull("No validation result", result);
		assertEquals("ValidationResult not OK", IStatus.OK, result.status);
	}
	public static void assertError(ValidationResult result, String msg) {
		assertNotNull("No validation result", result);
		assertEquals(IStatus.ERROR, result.status);
		assertContains(msg, result.msg);
	}
	public static void assertWarning(ValidationResult result, String msg) {
		assertNotNull("No validation result", result);
		assertEquals(IStatus.WARNING, result.status);
		assertContains(msg, result.msg);
	}
		
	public void testNull() throws Exception {
		File distro = null;
		ValidationResult result = doValidation(distro);
		assertOK(result);
	}
	
	public void testNoExist() throws Exception {
		File distro = new File("/bogus/no-exist");
		ValidationResult result = doValidation(distro);
		assertError(result, "'/bogus/no-exist' does not exist");
	}
	
	public void testNoDirectory() throws Exception {
		File distro = File.createTempFile("who-cares", "txt");
		ValidationResult result = doValidation(distro);
		assertError(result, "'"+distro+"' is not a directory");
	}
	
	public void testEmptyDirectory() throws Exception {
		File distro = TestUtils.createTempDirectory("empty");
		ValidationResult result = doValidation(distro);
		assertError(result, "'"+ distro +"' doesn't look like a valid Gradle installation");
	}
	
	public void testValidDistributionZipURIs() throws Exception {
		//Note: all the uris tested here are 'remote' URIs. We don't actually validate such URIs beyond syntactic correctness.
		for (URI distro : Distributions.all) {
			ValidationResult result = doValidation(distro);
			assertOK(result);
		}
	}
	
	public void testBadURI() throws Exception {
		String distro = "http:://///foo asas";
		ValidationResult result = doValidation(distro.toString());
		assertError(result, "URI syntax error");
	}
	
	public void testValidDistrosUnzipped() throws Exception {
//		URI uri = Distributions.M7_URI;
		for (URI uri : Distributions.all) {
			UnzippedDistribution distro = new UnzippedDistribution();
			DownloadManager.getDefault().doWithDownload(uri, distro);
			
			ValidationResult result = doValidation(distro.location);
			assertOK(result);
		}
	}

	private ValidationResult doValidation(String distro) {
		MockContext context = new MockContext(distro);
		DistributionValidator validator = new DistributionValidator(context);
		MockListener listener = new MockListener();
		validator.addListener(listener);
		validator.refresh();
		ValidationResult result1 = listener.getResult();
		ValidationResult result2 = validator.getValue();
		assertEquals("Both ways of obtaining validation result should be consistent", result1, result2);
		return result1;
	}
	/**
	 * Do a single validation.
	 */
	private ValidationResult doValidation(File distro) {
		return doValidation(distro!=null ? distro.toURI() : null);
	}
	private ValidationResult doValidation(URI uri) {
		return doValidation(uri!=null ? uri.toString() : null);
	}
	
}
