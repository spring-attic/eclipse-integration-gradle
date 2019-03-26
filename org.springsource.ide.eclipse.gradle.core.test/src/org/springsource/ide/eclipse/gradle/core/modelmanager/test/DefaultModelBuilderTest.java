/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.core.modelmanager.test;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.modelmanager.BuildResult;
import org.springsource.ide.eclipse.gradle.core.modelmanager.DefaultModelBuilder;
import org.springsource.ide.eclipse.gradle.core.test.GradleTest;
import org.springsource.ide.eclipse.gradle.core.util.Distributions;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;

public class DefaultModelBuilderTest extends GradleTest {

	DefaultModelBuilder builder = new DefaultModelBuilder();
	
	public void testBuildSimpleProjectModel() throws Throwable {
		File projectLoc = extractJavaSample("quickstart");
		GradleProject project = GradleCore.create(projectLoc);
		BuildResult<EclipseProject> result = builder.buildModel(project, EclipseProject.class, new NullProgressMonitor());
		assertSucceeded(result);
		
		EclipseProject model = result.getModel();
		assertEquals("quickstart",  model.getName());
	}
	
	public void testBuildModelWithDifferentReleases() throws Exception {
		class Result {
			int minorVersion;
			Throwable error;
			private int majorVersion;
			public Result(int majorVersion, int minorVersion) {
				this.majorVersion = majorVersion;
				this.minorVersion = minorVersion;
			}
			@Override
			public String toString() {
				return "Release "+majorVersion+"."+minorVersion+" = "+ ((error==null) ? "OK" : ExceptionUtil.getMessage(error));
			}
		}
		String[] versions = {  
				"1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9", "1.10", "1.11", "1.12",
				"2.0", "2.1"
		};
		Result[] results = new Result[versions.length];
		boolean failed = false;
		for (int i = 0; i < results.length; i++) {
			String[] parts = versions[i].split("\\.");
			int majorVersion = Integer.parseInt(parts[0]);
			int minorVersion = Integer.parseInt(parts[1]);
			results[i] = new Result(majorVersion, minorVersion);
			try {
				doModelBuildWithRelease(majorVersion, minorVersion);
			} catch (Throwable e) {
				failed = true;
				e.printStackTrace();
				results[i].error = e;
			}
		}
		if (failed) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < results.length; i++) {
				buf.append(results[i]+"\n");
			}
			throw new Error(buf.toString());
		}
	}
	
	
	private void doModelBuildWithRelease(int majorVersion, int minorVersion) throws Throwable {
		GradleCore.getInstance().getPreferences().setDistribution(Distributions.releaseURI(majorVersion, minorVersion));
		File projectLoc = extractJavaSample("quickstart");
		GradleProject project = GradleCore.create(projectLoc);
		BuildResult<EclipseProject> result = builder.buildModel(project, EclipseProject.class, new NullProgressMonitor());
		assertSucceeded(result);
		
		EclipseProject model = result.getModel();
		assertEquals("quickstart",  model.getName());
	}

	private void assertSucceeded(BuildResult<EclipseProject> result) throws Throwable {
		if (result.isFailed()) {
			throw result.getError();
		}
	}

	public void testCancelationFast() throws Exception {
		File projectLoc = getTestProjectCopy("slowstart");
		GradleProject project = GradleCore.create(projectLoc);
		ModelPromise<EclipseProject> promise = getModelPromise(project, EclipseProject.class);

		//Don't wait, cancel right away. This will tend to cancel job before it even
		// got started and that should be okay.
		promise.cancel();
		
		try {
			EclipseProject model = promise.join();
			assertEquals("quickstart",  model.getName());
			fail("Build should have been canceled");
		} catch (Throwable e) {
			assertTrue(""+e, ExceptionUtil.isCancelation(e));
		}
	}
	
	public void testCancelationSlow() throws Exception {
		File projectLoc = getTestProjectCopy("slowstart");
		GradleProject project = GradleCore.create(projectLoc);
		ModelPromise<EclipseProject> promise = getModelPromise(project, EclipseProject.class);
				
		Thread.sleep(1000); //Wait a bit until build job is actually started.
		promise.cancel();
		
		try {
			EclipseProject model = promise.join();
			assertEquals("quickstart",  model.getName());
			fail("Build should have been canceled");
		} catch (Throwable e) {
			e.printStackTrace();
			assertCancelation(e);
		}
	}
	
	
	private void assertCancelation(Throwable e) {
		if (ExceptionUtil.isCancelation(e)) {
			return;
		}
		StringBuilder msg = new StringBuilder("==== not a cancelation exception ?? ===\n");
		msg.append("class = "+e.getClass().getName()+"\n");
		if (e instanceof CoreException) {
			msg.append("severity = "+((CoreException)e).getStatus().getSeverity()+"\n");
		}
		msg.append("deepestCause = "+ExceptionUtil.getDeepestCause(e)+"\n");
	}

	public <T> ModelPromise<T> getModelPromise(final GradleProject project, final Class<T> type) {
		final ModelPromise<T> promise = new ModelPromise<T>();
		final Job[] job = new Job[1];
		job[0] = new GradleRunnable("Build model ["+type.getSimpleName()+"] for "+project.getDisplayName()) {
			@Override
			public void doit(IProgressMonitor mon) throws Exception {
				promise.setJob(job[0]);
				try {
					promise.apply(builder.buildModel(project, type, mon).get());
				} catch (Throwable e) {
					promise.error(e);
				}
			}
		}.asJob();
		job[0].schedule();
		return promise;
	}
	
	
}
