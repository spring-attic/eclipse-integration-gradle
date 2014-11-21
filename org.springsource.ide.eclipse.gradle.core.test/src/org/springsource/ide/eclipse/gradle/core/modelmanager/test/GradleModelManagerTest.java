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
package org.springsource.ide.eclipse.gradle.core.modelmanager.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.eclipse.EclipseLinkedResource;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;
import org.springsource.ide.eclipse.gradle.core.GradleCore;
import org.springsource.ide.eclipse.gradle.core.GradleProject;
import org.springsource.ide.eclipse.gradle.core.classpathcontainer.FastOperationFailedException;
import org.springsource.ide.eclipse.gradle.core.modelmanager.AbstractModelBuilder;
import org.springsource.ide.eclipse.gradle.core.modelmanager.GradleModelManager;
import org.springsource.ide.eclipse.gradle.core.modelmanager.IGradleModelListener;
import org.springsource.ide.eclipse.gradle.core.test.GradleTest;
import org.springsource.ide.eclipse.gradle.core.test.util.TestUtils;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;
import org.springsource.ide.eclipse.gradle.core.util.GradleRunnable;
import org.springsource.ide.eclipse.gradle.core.util.JobUtil;
import org.springsource.ide.eclipse.gradle.core.util.JoinableContinuation;

public class GradleModelManagerTest extends GradleTest {
	
	public void testGetFastModelWithoutCache() throws Exception {
		GradleProject project = project("animal/mamal/cow");
		try {
			mgr.getModel(project, FooModel.class);
			fail("Should have thrown FastOperationFailedException");
		} catch (FastOperationFailedException e) {
			//OK!
		}
		assertEquals(0, builder.totalBuilds());
	}
	
	public void testGetSlowModel() throws Exception {
		GradleProject project = project("animal/mamal/cow");
		FooModel model = mgr.getModel(project, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(cow)", model.getFoo());
		
		//fast get should be able to fetch from cache now.
		model = mgr.getModel(project, FooModel.class); 
		assertEquals("Foo(cow)", model.getFoo());
		
		//slow get should also be able to fetch from cache now.
		model = mgr.getModel(project, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(cow)", model.getFoo());
		
		
		//Check build counts should only be one, for the first slow request.
		assertEquals(1, builder.count(project, FooModel.class));
		assertEquals(1, builder.totalBuilds());
	}
	
	public void testInvalidateForProject() throws Exception {
		GradleProject animal = project("animal");
		mgr.invalidate(animal); //invalidate should do nothing rather than throw NPE
		
		FooHierarchyModel model = mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		assertNotNull(model.getFoo());
		
		mgr.invalidate(animal);
		try {
			mgr.getModel(animal, FooHierarchyModel.class);
			fail("Should have thrown FastOperationFailedException");
		} catch (FastOperationFailedException e) {
			//ok!
		}
	}
		
	public void testFailuresCached() throws Exception {
		GradleProject project = project("animal/mamal/dog");
		
		BadProjectException badProjectException = new BadProjectException("animal/mamal/dog");
		builder.addError(FooModel.class, project, badProjectException);

		//Before caching should fail without trying a build
		try {
			mgr.getModel(project, FooModel.class);
			fail("Should have thrown FastOperationFailedException");
		} catch (FastOperationFailedException e) {
			//ok!
		}
		assertEquals(0, builder.totalBuilds());

		//A 'slow' get should attempt a single build and fail
		try {
			mgr.getModel(project, FooModel.class, new NullProgressMonitor());
			fail("Should have thrown BadProjectException");
		} catch (CoreException e) {
			assertEquals(badProjectException, ExceptionUtil.getDeepestCause(e));
		}
		assertEquals(1, builder.totalBuilds());
		
		//Subsequent fast ...
		try {
			mgr.getModel(project, FooModel.class);
			fail("Should have thrown BadProjectException");
		} catch (CoreException e) {
			assertEquals(badProjectException, ExceptionUtil.getDeepestCause(e));
		}
		assertEquals(1, builder.totalBuilds());
		
		//... and slow requests should fail without further build attempts.
		try {
			mgr.getModel(project, FooModel.class, new NullProgressMonitor());
			fail("Should have thrown BadProjectException");
		} catch (CoreException e) {
			assertEquals(badProjectException, ExceptionUtil.getDeepestCause(e));
		}
		assertEquals(1, builder.totalBuilds());
	}
	
	public void testBuildMultipleModels() throws Exception {
		GradleProject project = project("animal/mamal/cow");
		
		FooModel foo = mgr.getModel(project, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(cow)", foo.getFoo());
		
		BarModel bar = mgr.getModel(project, BarModel.class, new NullProgressMonitor());
		assertEquals("Bar(cow)", bar.getBar());
		
		foo = mgr.getModel(project, FooModel.class);
		assertEquals("Foo(cow)", foo.getFoo());
		
		bar = mgr.getModel(project, BarModel.class);
		assertEquals("Bar(cow)", bar.getBar());
		
		assertEquals(1, builder.count(project, FooModel.class));
		assertEquals(1, builder.count(project, BarModel.class));
		assertEquals(2, builder.totalBuilds());
	}
	
	public void testBuildsOnMultipleProjects() throws Exception {
		GradleProject cow = project("animal/mamal/cow");
		GradleProject dog = project("animal/mamal/dog");
		
		FooModel dogFoo = mgr.getModel(dog, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(dog)", dogFoo.getFoo());
		FooModel cowFoo = mgr.getModel(cow, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(cow)", cowFoo.getFoo());
		
		dogFoo = mgr.getModel(dog, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(dog)", dogFoo.getFoo());
		cowFoo = mgr.getModel(cow, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(cow)", cowFoo.getFoo());
	}
	
	public void testGroupedBuildOnRoot() throws Exception {
		GradleProject animal = project("animal");
		
		FooHierarchyModel foo = mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		
		System.out.println(">>>>> children of animal");
		for (HierarchicalEclipseProject c : foo.getChildren()) {
			System.out.println(c);
		}
		System.out.println("<<<<< children of animal");
		
		assertEquals(2,foo.getChildren().size()); //2 children: mamal and bird
		
		// now that we built the root project all submodels in the hierarchy should be populated as well.
		File animalPath = project("animal").getLocation();
		for (GradleProject subproject : testProjects()) {
			if (subproject.getLocation().toString().startsWith(animalPath.toString())) {
				FooHierarchyModel submodel = mgr.getModel(subproject, FooHierarchyModel.class);
				assertEquals("Foo("+subproject.getLocation().getName()+")", submodel.getFoo());
			}
		}
		
		for (GradleProject project : testProjects()) {
			assertEquals(project==animal?1:0, builder.count(project, FooHierarchyModel.class));
		}
		assertEquals(1, builder.totalBuilds());
	}
	
	public void testGroupedBuildOnSubProject() throws Exception {
		doMamalBuild(1);
		mgr.invalidate();
		builder.reset();
		doMamalBuild(2);
	}

	private void doMamalBuild(int iteration) throws CoreException, FastOperationFailedException {
		GradleProject mamal = project("animal/mamal");
		
		FooHierarchyModel foo = mgr.getModel(mamal, FooHierarchyModel.class, new NullProgressMonitor());
		
		System.out.println(">>>>> children of mamal");
		for (HierarchicalEclipseProject c : foo.getChildren()) {
			System.out.println(c);
		}
		System.out.println("<<<<< children of mamal");
		
		assertEquals(2,foo.getChildren().size()); //2 children: cow and dog
		
		// now that we built the mamal project all related models in the hierarchy should be populated as well.
		File animalPath = project("animal").getLocation();
		for (GradleProject project : testProjects()) {
			if (project.getLocation().toString().startsWith(animalPath.toString())) {
				FooHierarchyModel model = mgr.getModel(project, FooHierarchyModel.class);
				assertEquals("Foo("+project.getLocation().getName()+")", model.getFoo());
			} else {
				try {
					mgr.getModel(project, FooHierarchyModel.class);
					fail("Should have failed, model not in cache");
				} catch (FastOperationFailedException e) {
					//ok
				}
			}
		}
		
		GradleProject rootProject = project("animal");
		builder.dump();
		for (GradleProject project : testProjects()) {
			if (iteration==1) {
				//This is the first build so impossible to determine root, this means 
				// model will be built via the reference project even though that is not the usual case, 
				// it is unavoidable here.
				assertEquals(project.getDisplayName(), project==mamal?1:0, builder.count(project, FooHierarchyModel.class));
			} else {
				assertEquals(project.getDisplayName(), project==rootProject?1:0, builder.count(project, FooHierarchyModel.class));
			}
		}
		assertEquals(1, builder.totalBuilds());
	}
	
	
	public void testSubtypeSubsumesSupertype() throws Exception {
		GradleProject project = project("animal/mamal");
		{
			FooHierarchyModel model = mgr.getModel(project, FooHierarchyModel.class, new NullProgressMonitor());
			assertEquals("Foo("+project.getLocation().getName()+")", model.getFoo());
		}
		assertEquals(1, builder.totalBuilds());
		
		// since we built FooHierarchyModel which extends HierarchicalEclipseProject we should not need to
		// build HierarchicalEclipseProject
		
		{
			HierarchicalEclipseProject model = mgr.getModel(project, HierarchicalEclipseProject.class, new NullProgressMonitor());
			assertEquals("mamal", model.getName());
		}
		assertEquals(1, builder.totalBuilds()); //build count unchanged, used from cache.
	}
	
	
	public void testSubtypeSubsumesSupertype2() throws Exception {
		//Like other variant of this test but models requested in different (wrong order).
		GradleProject project = project("animal/mamal");
		
		{	//Supertype model
			HierarchicalEclipseProject model = mgr.getModel(project, HierarchicalEclipseProject.class, new NullProgressMonitor());
			assertEquals("mamal", model.getName());
		}
		assertEquals(1, builder.totalBuilds()); //build count unchanged, used from cache.
		
		{	//Subtype model
			FooHierarchyModel model = mgr.getModel(project, FooHierarchyModel.class, new NullProgressMonitor());
			assertEquals("Foo("+project.getLocation().getName()+")", model.getFoo());
		}
		assertEquals(2, builder.totalBuilds());
				
	}
	
	public void testGroupedBuildFailure() throws Exception {
		GradleProject animal = project("animal");
		//We need at least one succesful model build (to be able to determine grouping)
		mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		mgr.invalidate();
		builder.reset();

		//Introduce error in all projects in the "animal" group.
		ArrayList<GradleProject> animalProjects = new ArrayList<GradleProject>();
		for (GradleProject project : testProjects()) {
			if (animal.equals(project.getRootProjectMaybe())) {
				builder.addError(FooHierarchyModel.class, project, new BadProjectException("BAD!"));
				animalProjects.add(project);
			}
		}
		assertEquals(7, animalProjects.size());
		
		for (GradleProject project : animalProjects) {
			try {
				mgr.getModel(project, FooHierarchyModel.class, new NullProgressMonitor());
			} catch (Exception e) {
				assertEquals("BAD!", ExceptionUtil.getDeepestCause(e).getMessage());
			}
		}

		//Should have failed as one, so only one build from 7 attempts to fetch the model.
		assertEquals(1, builder.totalBuilds());
		
	}
	
	/**
	 * When many request for a different models in a hierarchy come in quick succession 
	 * only one model should get built and all request should be satisfied by models from
	 * grouped model build.
	 */
	public void testSimultaneousGroupedBuildRequests() throws Exception {
		GradleProject animal = project("animal");
		
		//We need at least one succesfull model build (to be able to determine grouping)
		mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		mgr.invalidate();
		builder.reset();
		
		builder.setBuildDuration(1000); //
		
		Map<GradleProject, ModelPromise<FooHierarchyModel>> models = new HashMap<GradleProject, ModelPromise<FooHierarchyModel>>();
		for (GradleProject project : testProjects()) {
			if (animal==project.getRootProjectMaybe()) {
				models.put(project, (getModelPromise(project, FooHierarchyModel.class)));
			}
		}
		assertEquals(7, models.size());
		
		//Wait for all the request to finish and check that the models look sane
		for (GradleProject project : models.keySet()) {
			JoinableContinuation<FooHierarchyModel> promise = models.get(project);
			FooHierarchyModel model = promise.join();
			assertEquals("Foo("+project.getLocation().getName()+")", model.getFoo());
			
			//also check whether models are in the cache now
			FooHierarchyModel model2 = mgr.getModel(project, FooHierarchyModel.class);
			assertEquals(model, model2);
		}
		
		assertEquals(1, builder.totalBuilds());
	}
		
	/**
	 * Variant of previous test, with a 'cold' start. I.e. without information about build families.
	 * Note that this can be made to work, but only by locking the whole world during such builds.
	 */
	public void testSimultaneousGroupedBuildRequestsColdStart() throws Exception {
		GradleProject animal = project("animal");
		builder.setBuildDuration(1000);
		
		Map<GradleProject, JoinableContinuation<FooHierarchyModel>> models = new HashMap<GradleProject, JoinableContinuation<FooHierarchyModel>>();
		for (GradleProject project : testProjects()) {
			if (project.getLocation().toString().startsWith(animal.getLocation().toString())) {
				models.put(project, (getModelPromise(project, FooHierarchyModel.class)));
			}
		}
		assertEquals(7, models.size());
		
		//Wait for all the request to finish and check that the models look sane
		for (GradleProject project : models.keySet()) {
			JoinableContinuation<FooHierarchyModel> promise = models.get(project);
			FooHierarchyModel model = promise.join();
			assertEquals("Foo("+project.getLocation().getName()+")", model.getFoo());
			
			//also check whether models are in the cache now
			FooHierarchyModel model2 = mgr.getModel(project, FooHierarchyModel.class);
			assertEquals(model, model2);
		}
		
		assertEquals(1, builder.totalBuilds());
	}
	
	/**
	 * When many request for a different models in a hierarchy come in quick succession 
	 * only one model should get built also in the FAILURE case.
	 */
	public void testSimultaneousGroupedBuildRequestsFailure() throws Exception { 
		
		GradleProject animal = project("animal");
		
		//We need at least one succesfull model build (to be able to determine grouping)
		//Note that the 'cold start' case for this is missing for exactly this reason. 
		//It simply can not be implemented because there is no way to get the family info
		//unless there is or has been some kind of model sometime in the past.
		mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		mgr.invalidate();
		builder.reset();
		
		builder.setBuildDuration(1000);
		
		//Break the build script:
		for (GradleProject project : testProjects()) {
			if (animal==project.getRootProjectMaybe()) {
				builder.addError(FooHierarchyModel.class, project, new BadProjectException("BAD!"));
			}
		}
		
		Map<GradleProject, JoinableContinuation<FooHierarchyModel>> promises = new HashMap<GradleProject, JoinableContinuation<FooHierarchyModel>>();
		for (GradleProject project : testProjects()) {
			if (animal==project.getRootProjectMaybe()) {
				promises.put(project, (getModelPromise(project, FooHierarchyModel.class)));
			}
		}
		assertEquals(7, promises.size());
		
		//Wait for all the request to finish and check that all are failures
		for (GradleProject project : promises.keySet()) {
			JoinableContinuation<FooHierarchyModel> promise = promises.get(project);
			try {
				promise.join();
				fail("Should have thrown");
			} catch (Throwable e) {
				assertEquals("BAD!", ExceptionUtil.getDeepestCause(e).getMessage());
			}
		}
		
		assertEquals(1, builder.totalBuilds());
	}
		
	/**
	 *  When multiple request for a single model come in quick succession only one 
	 *  model should get built...
	 */
	public void testSimultaneousSingleBuildRequests() throws Exception {
		GradleProject animal = project("animal");
		
		//Cold start is ok since families are not involved in this test
		

		ArrayList<JoinableContinuation<FooModel>> promises = new ArrayList<JoinableContinuation<FooModel>>();
		for (int i = 0; i < 10; i++) {
			promises.add(getModelPromise(animal, FooModel.class));
		}
		assertEquals(10, promises.size());
		
		FooModel model = null;
		for (JoinableContinuation<FooModel> promise : promises) {
			model = promise.join();
			assertEquals("Foo(animal)", model.getFoo());
		}
		assertEquals(1, builder.totalBuilds());
		
		//also check whether models is now in cache
		FooModel model2 = mgr.getModel(animal, FooModel.class);
		assertEquals(model, model2);
		
		assertEquals(1, builder.totalBuilds());
	}
	
	/**
	 *  When multiple request for a single model come in quick succession only one 
	 *  build should happen even if this build fails.
	 */
	public void testSimultaneousSingleBuildRequestFailure() throws Exception {
		GradleProject animal = project("animal");
		
		//Cold start is ok since families are not involved in this test
		builder.addError(FooModel.class, animal, new BadProjectException("BAD!"));

		ArrayList<JoinableContinuation<FooModel>> promises = new ArrayList<JoinableContinuation<FooModel>>();
		for (int i = 0; i < 10; i++) {
			promises.add(getModelPromise(animal, FooModel.class));
		}
		assertEquals(10, promises.size());
		
		for (JoinableContinuation<FooModel> promise : promises) {
			try {
				promise.join();
				fail("Should have thrown");
			} catch (Throwable e) {
				assertEquals("BAD!", ExceptionUtil.getDeepestCause(e).getMessage());
			}
		}
		
		//also check whether failure is now in cache
		try {
			mgr.getModel(animal, FooModel.class);
			fail("Should have thrown");
		} catch (Throwable e) {
			assertEquals("BAD!", ExceptionUtil.getDeepestCause(e).getMessage());
		}
	}
	
	public void testCancelation() throws Exception {
		GradleProject project = project("animal");
		builder.setBuildDuration(1000); //give us some time to cancel a build.
		
		ModelPromise<FooModel> promise = getModelPromise(project, FooModel.class);
		promise.cancel();
		try {
			promise.join();
			fail("Should have been canceled");
		} catch (Throwable e) {
			assertTrue("Expected cancelation but got: "+e, ExceptionUtil.isCancelation(e));
		}
	}
	
	public void testNoCachingCanceledBuild() throws Exception {
		GradleProject project = project("animal");
		builder.setBuildDuration(1000); //give us some time to cancel a build.
		
		ModelPromise<FooModel> promise = getModelPromise(project, FooModel.class);
		promise.cancel();
		try {
			promise.join();
			fail("Should have been canceled");
		} catch (Throwable e) {
			assertTrue("Expected cancelation but got: "+e, ExceptionUtil.isCancelation(e));
		}

		FooModel model = mgr.getModel(project, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(animal)", model.getFoo());
	}
	
	//TODO: the next two tests are disabled. They are not passing as cancelation is not
	// propagating amongst operations that are blocking one another. 
	// Not yet sure how to handle this, should see what is really required .w.r.t to
	// how this will be used from the Eclipse Gradle tooling UI (i.e. where cancelation is
	// triggered from. Waiting to resolve this conundrum until after we wired things
	// to the UI.
	
	/** 
	 * When mulptiple concurrent requests wait for the same model build operation,
	 * canceling the build cancels all the requests.
	 * Case 1: single model provider
	 */
	public void DISABLED_testCancelingConcurrentSingleBuild() throws Exception {
		GradleProject animal = project("animal");
		builder.setBuildDuration(1000); //give us some time to cancel a build.
		
		ArrayList<ModelPromise<FooModel>> promises = new ArrayList<ModelPromise<FooModel>>();
		for (int i = 0; i < 10; i++) {
			promises.add(getModelPromise(animal, FooModel.class));
		}
		assertEquals(10, promises.size());
		
		promises.get(9).cancel(); //Canceling one should cancel all because it cancels the
								  // the model build they all depend on.
		for (ModelPromise<FooModel> promise : promises) {
			try {
				promise.join();
				fail("Should have thrown");
			} catch (Throwable e) {
				assertTrue(""+e, ExceptionUtil.isCancelation(e));
			}
		}
		
		assertTrue(builder.totalBuilds()<=1); //Depending on how fast things get canceled builder may not even get called.
		builder.reset();
		
		//also check that failures because of cancelation are *not* cached.
		FooModel model = mgr.getModel(animal, FooModel.class, new NullProgressMonitor());
		assertEquals("Foo(animal)", model.getFoo());
		
		assertEquals(1,  builder.totalBuilds());
		
	}
	
	/** 
	 * When multiple concurrent requests wait for the same model build operation,
	 * canceling the build cancels all the requests.
	 * Case 2: grouped model provider
	 */
	public void DISABLED_testCancelingConcurrentGroupedBuild() throws Exception {
		GradleProject animal = project("animal");
		builder.setBuildDuration(1000); //give us some time to cancel a build.
		
		//no cold start... need build family for this
		mgr.getModel(animal, FooHierarchyModel.class, new NullProgressMonitor());
		assertEquals(1, builder.totalBuilds());
		builder.reset();
		mgr.invalidate();
		
		ArrayList<GradleProject> projects = new ArrayList<GradleProject>();
		for (GradleProject p : testProjects()) {
			if (p.getRootProjectMaybe()==animal) {
				projects.add(p);
			}
		}
		assertEquals(7, projects.size());
		
		ArrayList<ModelPromise<FooHierarchyModel>> promises = new ArrayList<ModelPromise<FooHierarchyModel>>();
		for (GradleProject p : projects) {
			promises.add(getModelPromise(p, FooHierarchyModel.class));
		}
		
		promises.get(6).cancel(); //Cancel last one 
		
		for (ModelPromise<FooHierarchyModel> promise : promises) {
			try {
				promise.join();
				fail("Should have been canceled");
			} catch (Throwable e) {
				assertTrue(""+e, ExceptionUtil.isCancelation(e));
			}
		}
	}
	
	/**
	 * Test that Grouped model provider is able to cope with misprediction of 
	 * build family. (I.e. when user changes their build scripts in such a way
	 * that project hierachy changes... then predicted family will be incorrect
	 * as it is going to still be based on the old project hierarchy.
	 * <p>
	 * In such cases we will allow for sub-optimal build scheduling so that
	 * maybe too many builds will occur, but no exceptions such as 'InconsistentProjectHierarchy'
	 * should be propagated to model requestors.
	 */
	public void testChangingProjectHierarchy() throws Exception {
		//Ensure that we have family info for all projects.
		List<GradleProject> projects = testProjects();
		for (GradleProject p : projects) {
			mgr.getModel(p, FooHierarchyModel.class, new NullProgressMonitor());
		}
		
		assertEquals(2, builder.totalBuilds());
		builder.reset();
		mgr.invalidate();
		
		//From now on birds are people rather than animals
		changeParent(project("animal/bird"), project("people"));
		
		//The most likely thing to break is building model for a 'focus project' that has
		// moved to another family... because the wrong family model will be built for it
		// and then the build strategy won't produce the focus project's model in that build.
		
		FooHierarchyModel model = mgr.getModel(project("animal/bird/penguin"), FooHierarchyModel.class, new NullProgressMonitor());
		assertEquals("Foo(penguin)", model.getFoo());
		model = mgr.getModel(project("animal/bird"), FooHierarchyModel.class);
		assertEquals(project("people"), GradleCore.create(model.getParent())); //birds are now people... right?
		
		assertEquals(2, builder.totalBuilds());
		//1: unnecesary build caused by misprediction:
		assertEquals(1, builder.count(project("animal"), FooHierarchyModel.class));
		//2: recovery build done without informaton about root, so builds via focus project itself.
		assertEquals(1, builder.count(project("animal/bird/penguin"), FooHierarchyModel.class)); //one recoverbuild not via focus project lacking

		builder.reset();
		mgr.invalidate();
		
		//Test that family info for all projects has now been recovered (should be because both families have been
		// built since the change).
		
		for (GradleProject p : projects) {
			model = mgr.getModel(p, FooHierarchyModel.class, new NullProgressMonitor());
			assertEquals("Foo("+p.getLocation().getName()+")", model.getFoo());
		}

		assertEquals(2, builder.totalBuilds());
		assertEquals(1, builder.count(project("animal"), FooHierarchyModel.class));
		assertEquals(1, builder.count(project("people"), FooHierarchyModel.class));
		
	}
	
	public void testChangingProjectHierarchyConccurrent() throws Exception {
		System.out.println("==== testChangingProjectHierarchyConccurrent ====");
		List<GradleProject> projects = testProjects();
		for (GradleProject p : projects) {
			mgr.getModel(p, FooHierarchyModel.class, new NullProgressMonitor());
		}
		
		assertEquals(2, builder.totalBuilds());
		builder.reset();
		mgr.invalidate();
		
		//From now on birds are people rather than animals
		System.out.println("birds are now people");
		changeParent(project("animal/bird"), project("people"));
		
		ArrayList<ModelPromise<FooHierarchyModel>> promises = new ArrayList<ModelPromise<FooHierarchyModel>>();
		for (GradleProject p : projects) {
			promises.add(getModelPromise(p, FooHierarchyModel.class));
		}
		
		for (ModelPromise<FooHierarchyModel> promise : promises) {
			promise.join();
		}
		
		//This next condition is 'tricky'. In current implementation, based on how
		// it works one can reason out that all scenarios including recovery / repair of
		// broken family data will still only build each model once, even in the misprediction
		// case.
		
		builder.dump();
		assertTrue(builder.totalBuilds()<=3);

	}
	
	/**
	 * Test that explicitly asks for models in a 'bad' order which triggers an extraneous
	 * build in buggy implementation of grouped model builder.
	 */
	public void testChangingProjectHierarchyBadSequence() throws Exception {
		List<GradleProject> projects = testProjects();
		for (GradleProject p : projects) {
			mgr.getModel(p, FooHierarchyModel.class, new NullProgressMonitor());
		}
		
		assertEquals(2, builder.totalBuilds());
		builder.reset();
		mgr.invalidate();
		
		//From now on birds are people rather than animals
		changeParent(project("animal/bird"), project("people"));
		
		// sequence of model requests case which exposes a bug in how
		// inaccurate family info is corrected (if this test passes, then
		// the bug is fixed :-)
		
		ArrayList<ModelPromise<FooHierarchyModel>> promises = new ArrayList<ModelPromise<FooHierarchyModel>>();
		
		// To reliably cause the bad sequencing we need builds to slow enough 
		builder.setBuildDuration(1000);
		promises.add(getModelPromise(project("animal/bird"), FooHierarchyModel.class));
		Thread.sleep(100); 
		// "animal" build should now be underway. But it won't produce 'bird' model.

		//These builds will be blocked until 'bird' build completes. Then they too will fail
		// with inconsistent project hierarchy exception, UNLESS their memmbership infos are
		// flushed properly by the failed animal build
		promises.add(getModelPromise(project("animal/bird/penguin"), FooHierarchyModel.class));
		promises.add(getModelPromise(project("animal/bird/swallow"), FooHierarchyModel.class));
		
		// At time around 1000ms the bird build finishes but has InconsistentProjectHierarchy.
		// It will retry... at the same time penguin and swallow unblock and will try to build.
		// Race condition: 
		//   - if 'penguin' or 'swallow' win, then extraneous builds result (in bug case)
		//   - If bird wins => ok.
		
		mgr.sleepBetweenRetries(100); //Causes bird retry to be slow so it will loose the race.
		
		for (ModelPromise<FooHierarchyModel> promise : promises) {
			promise.join();
		}
		
		builder.dump();
		
		assertEquals(2, builder.totalBuilds());
	}
	
	public void testChangingProjectHierachyOrphansMarkedOnNextBuild() throws Exception {
		List<GradleProject> projects = testProjects();
		for (GradleProject p : projects) {
			mgr.getModel(p, FooHierarchyModel.class, new NullProgressMonitor());
		}
		
		assertEquals(2, builder.totalBuilds());
		builder.reset();
		mgr.invalidate();
		
		//From now on birds are people rather than animals
		changeParent(project("animal/bird"), project("people"));
		
		//If a 'animal' model is built...
		mgr.getModel(project("animal"), FooHierarchyModel.class, new NullProgressMonitor());
		//.. then 'birds' should become marked as as orphans
		assertNull(project("animal/bird").getRootProjectMaybe());
		assertNull(project("animal/bird/penguin").getRootProjectMaybe());
		assertNull(project("animal/bird/swallow").getRootProjectMaybe());
	}
	
	public void testModelListenersAll() throws Exception {
		Class<?>[] types = {
				FooModel.class,
				FooHierarchyModel.class,
				BarModel.class
		};
		
		System.out.println("==== testModelListeners ====");
		
		List<GradleProject> projects = testProjects();
		List<ModelPromise<?>> promises = new ArrayList<ModelPromise<?>>();
		List<Expector> expectors = new ArrayList<Expector>();
		
		System.out.println("Attaching listeners...");
		for (GradleProject project : projects) {
			for (Class<?> type : types) {
				Expector listener = new Expector(project, type, 1);
				expectors.add(listener);
				mgr.addListener(project, listener);
			}
		}
		
		System.out.println("Building models....");
		for (GradleProject project : projects) {
			for (Class<?> type : types) {
				promises.add(getModelPromise(project, type));
			}
		}
		for (ModelPromise<?> promise : promises) {
			promise.join();
		}

		builder.reset();
		mgr.invalidate(); //include this in expectations, we do not
		                  // expect clearing caches to update listeners (listeners only get
						  // called when actual models are added to the cache.
		
		System.out.println("Checking expectations...");
		for (Expector expector : expectors) {
			expector.assertExpectations();
		}
		
		//Remove some listeners and check that they behave as expected if
		// we do more model builds.
	
		System.out.println("==== testModelListeners phase 2 ====");
		
		System.out.println("Remove some listeners...");
		for (Expector expector : expectors) {
			if (removableListener(expector.expectProject, expector.expectType)) {
				System.out.println("Remove listener: "+expector);
				mgr.removeListener(expector.expectProject, expector);
				expector.reset(0); //shouldn't get any more events for removed listeners
			} else {
				expector.reset(); // other listeners should get same events as before
			}
		}
		
		System.out.println("Rebuilding models...");
		promises = new ArrayList<ModelPromise<?>>();
		for (GradleProject project : projects) {
			for (Class<?> type : types) {
				promises.add(getModelPromise(project, type));
			}
		}
		for (ModelPromise<?> promise : promises) {
			promise.join();
		}

		System.out.println("Checking expectations...");
		for (Expector expector : expectors) {
			expector.assertExpectations();
		}
		
	}
	
	/**
	 * Somewhat arbitrary criteria to pick some listeners to remove in the
	 * preceding test.
	 */
	private boolean removableListener(GradleProject p, Class<?> type) {
		return p.getLocation().getName().contains("a") && type==FooModel.class;
	}
	
	
	
	public void testModelListenersSparse() throws Exception {
		Class<?>[] types = {
				FooModel.class,
				FooHierarchyModel.class,
				BarModel.class
		};
		
		System.out.println("==== testModelListenersSparse ====");
		
		List<GradleProject> projects = testProjects();
		List<ModelPromise<?>> promises = new ArrayList<ModelPromise<?>>();
		List<Expector> expectors = new ArrayList<Expector>();
				
		{
			System.out.println("Attaching listeners...");
			
			Expector expector = new Expector(project("animal/bird"), FooModel.class, 1);
			mgr.addListener(expector.expectProject, expector);
			expectors.add(expector);
			
			expector = new Expector(project("people/mary"), BarModel.class, 1);
			mgr.addListener(expector.expectProject, expector);
			expectors.add(expector);
		}
		
		System.out.println("Building models....");
		for (GradleProject project : projects) {
			for (Class<?> type : types) {
				promises.add(getModelPromise(project, type));
			}
		}
		for (ModelPromise<?> promise : promises) {
			promise.join();
		}
		
		System.out.println("Checking expectations...");
		for (Expector expector : expectors) {
			expector.assertExpectations();
		}
		
		//Remove some listeners and check that they behave as expected if
		// we do more model builds.
	
		System.out.println("==== testModelListeners phase 2 ====");
		
		System.out.println("Remove some listeners...");
		for (Expector expector : expectors) {
			if (expector.expectType==FooModel.class) {
				System.out.println("Remove listener: "+expector);
				mgr.removeListener(expector.expectProject, expector);
				expector.reset(0); //shouldn't get any more events for removed listeners
			} else {
				expector.reset(); // other listeners should get same events as before
			}
		}
		
		System.out.println("Rebuilding models...");
		
		builder.reset();
		mgr.invalidate(); 
		
		promises = new ArrayList<ModelPromise<?>>();
		for (GradleProject project : projects) {
			for (Class<?> type : types) {
				promises.add(getModelPromise(project, type));
			}
		}
		for (ModelPromise<?> promise : promises) {
			promise.join();
		}

		System.out.println("Checking expectations...");
		for (Expector expector : expectors) {
			expector.assertExpectations();
		}
		
	}
	
	public void testListenersFailure() throws Exception {
		builder.addError(FooModel.class, project("animal/bird/penguin"), new BadProjectException("Penguin can't fly"));
		
		Expector expectSuccess = new Expector(project("animal/bird/swallow"), FooModel.class, 1);
		Expector expectFailure = new Expector(project("animal/bird/penguin"), FooModel.class, 0); //Failures don't produce model change events

		mgr.addListener(expectSuccess.expectProject, expectSuccess);
		mgr.addListener(expectFailure.expectProject, expectFailure);

		try {
			mgr.getModel(project("animal/bird/penguin"), FooModel.class, new NullProgressMonitor());
			fail("Should have failed");
		} catch (Throwable e) {
			assertEquals("Penguin can't fly", ExceptionUtil.getDeepestCause(e).getMessage());
		}
		mgr.getModel(project("animal/bird/penguin"), BarModel.class, new NullProgressMonitor());
		
		mgr.getModel(project("animal/bird/swallow"), FooModel.class, new NullProgressMonitor());
		mgr.getModel(project("animal/bird/swallow"), BarModel.class, new NullProgressMonitor());
	}
	
	//TODO: if make 'slow request' which starts a build, then a concurrent fast request...
	// the fast request should fail fast and not block during the build.
		
	//TODO: effects of project deletion on grouped model builds?
	
	//TODO: effects of project addition on grouped model builds?
			
	/////////////////////////////////////////////////////////////////////
	
	// no tests below this line, this is all the scaffolding to make the tests work.

	private File PROJECT_FOLDER;
	
	/**
	 * Test data that generates the mock projects in a hierarchy.
	 */
	private static String[] TEST_PROJECT_FOLDERS = {
			"animal/mamal/cow",
			"animal/mamal/dog",
			"animal/bird/swallow",
			"animal/bird/penguin",
			"people/john",
			"people/mary",
			"people/jeff"
	};
	
	/**
	 * Move a given project to a different parent in the project hierarchy.
	 */
	private void changeParent(GradleProject project, GradleProject parent) {
		parentOverrides.put(project.getLocation(), parent.getLocation());
	}

	/**
	 * Keeps track of 'moved' projects in the hierarchy.
	 */
	private Map<File, File> parentOverrides = new HashMap<File,File>();
	
	/**
	 * Request an asynchronously built model, returns a 'promise' of the model.
	 */
	public <T> ModelPromise<T> getModelPromise(final GradleProject project, final Class<T> type) {
		final ModelPromise<T> promise = new ModelPromise<T>();
		final Job[] job = new Job[1];
		job[0] = new GradleRunnable("Build model ["+type.getSimpleName()+"] for "+project.getDisplayName()) {
			@Override
			public void doit(IProgressMonitor mon) throws Exception {
				try {
					promise.setJob(job[0]);
					promise.apply(mgr.getModel(project, type, mon));
				} catch (Throwable e) {
					promise.error(e);
				}
			}
		}.asJob();
		job[0].schedule();
		return promise;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		PROJECT_FOLDER = TestUtils.createTempDirectory();
		for (String path : TEST_PROJECT_FOLDERS) {
			new File(PROJECT_FOLDER, path).mkdirs();
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (PROJECT_FOLDER!=null) {
			FileUtils.deleteDirectory(PROJECT_FOLDER);
		}
	}
	
	/**
	 * Fetch list of all the test projects.
	 */
	public List<GradleProject> testProjects() {
		return testProjects(PROJECT_FOLDER, new ArrayList<GradleProject>());
	}

	/**
	 * Fetch list of test projects in a given folder. The folder itself is
	 * not included as a project.
	 */
	private List<GradleProject> testProjects(File root, List<GradleProject> projects) {
		for (File child : root.listFiles()) {
			if (child.isDirectory() && !child.getName().startsWith(".")) {
				projects.add(GradleCore.create(child));
				testProjects(child, projects);
			}
		}
		return projects;
	}
	
	
	public interface FooModel {
		String getFoo();
	}
	
	public static abstract class MockModel {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((loc == null) ? 0 : loc.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MockModel other = (MockModel) obj;
			if (loc == null) {
				if (other.loc != null)
					return false;
			} else if (!loc.equals(other.loc))
				return false;
			return true;
		}

		protected File loc;

		public MockModel(File loc) {
			this.loc = loc;
		}
	}
	
	public abstract class MockHierarchyModel extends MockModel implements HierarchicalEclipseProject {

		public MockHierarchyModel(File loc) {
			super(loc);
		}
		
		protected abstract HierarchicalEclipseProject create(File loc);

		@Override
		public String getName() {
			return loc.getName();
		}

		@Override
		public String getDescription() {
			return getClass().getSimpleName()+" "+getName();
		}

		@Override
		public HierarchicalEclipseProject getParent() {
			File parentLoc = parentOverrides.get(loc);
			if (parentLoc==null) {
				parentLoc = loc.getParentFile();
			}
			if (parentLoc.equals(PROJECT_FOLDER)) {
				return null;
			}
			return create(parentLoc);
		}

		@Override
		public DomainObjectSet<? extends HierarchicalEclipseProject> getChildren() {
			ArrayList<HierarchicalEclipseProject> children = new ArrayList<HierarchicalEclipseProject>();
			for (File childLoc : loc.listFiles()) {
				if (childLoc.isDirectory() && !childLoc.getName().startsWith(".")) {
					File changedParent = parentOverrides.get(childLoc);
					if (changedParent==null) { //Exclude children that where moved out of me
						children.add(create(childLoc));
					}
				}
			}
			//Add children that were moved into me
			for (Entry<File, File> e : parentOverrides.entrySet()) {
				File parentLoc = e.getValue();
				File childLoc = e.getKey();
				if (parentLoc.equals(loc)) {
					children.add(create(childLoc));
				}
			}
			return new ImmutableDomainObjectSet<HierarchicalEclipseProject>(children);
		}

		@Override
		public DomainObjectSet<? extends EclipseProjectDependency> getProjectDependencies() {
			return null;
		}

		@Override
		public DomainObjectSet<? extends EclipseSourceDirectory> getSourceDirectories() {
			return null;
		}

		@Override
		public DomainObjectSet<? extends EclipseLinkedResource> getLinkedResources() throws UnsupportedMethodException {
			return null;
		}

		@Override
		public File getProjectDirectory() {
			return loc;
		}
		
	}
	
	public class FooHierarchyModelImpl extends MockHierarchyModel implements FooHierarchyModel {

		public FooHierarchyModelImpl(File loc) {
			super(loc);
		}
	
		@Override
		public String getFoo() {
			return "Foo("+loc.getName()+")";
		}

		@Override
		protected FooHierarchyModel create(File loc) {
			return new FooHierarchyModelImpl(loc);
		}
		
		@Override
		public String toString() {
			return "FooHierarchyModel("+loc.toString().substring(PROJECT_FOLDER.toString().length())+")";
		}
	}
	
	public class VanillaHierarchyModel extends MockHierarchyModel {
		public VanillaHierarchyModel(File loc) {
			super(loc);
		}

		@Override
		protected HierarchicalEclipseProject create(File loc) {
			return new VanillaHierarchyModel(loc);
		}

	}

	
	
	public class FooModelImpl extends MockModel implements FooModel {
		public FooModelImpl(File loc) {
			super(loc);
		}

		@Override
		public String getFoo() {
			return "Foo("+loc.getName()+")";
		}
	}
	
	public class BarModelImpl extends MockModel implements BarModel {
		public BarModelImpl(File loc) {
			super(loc);
		}

		@Override
		public String getBar() {
			return "Bar("+loc.getName()+")";
		}
		
	}

	public interface BarModel {
		String getBar();
	}
	
	public interface FooHierarchyModel extends HierarchicalEclipseProject {
		String getFoo();
	}
	
	public interface BarHierarchyModel extends HierarchicalEclipseProject {
		String getBar();
	}
	
	public class MockModelBuilder extends AbstractModelBuilder {
		
		Map<String, Integer> buildCounters = new HashMap<String, Integer>();
		Map<String, Throwable> errors = new HashMap<String, Throwable>();
		
		private String modelKey(GradleProject project, Class<?> type) {
			return project.getLocation()+"::"+type.getSimpleName();
		}
		
		/**
		 * Enables 'build time' simulation. This makes the build
		 * sleep for some time before returning the builder
		 * result.
		 */
		public void setBuildDuration(int duration) {
			buildTime = duration;
		}

		public void reset() {
			buildCounters = new HashMap<String, Integer>();
			errors = new HashMap<String, Throwable>();
		}

		private long buildTime = 0; //to simulate that builds are 'slow'. Disabled by default. Must
		  // be turned on by tests that require it.
		
		private synchronized void incrementBuildCount(GradleProject project, Class<?> type) {
			String key = modelKey(project, type);
			Integer current = buildCounters.get(key);
			if (current==null) {
				current = 0;
			}
			buildCounters.put(key, current+1);
		}
		
		/**
		 * Make it so that when given project is being built it will throw an Exception rather than
		 * produce a model.
		 */
		public void addError(Class<?> type, GradleProject project, Throwable error) {
			errors.put(modelKey(project, type), error);
		}

		@Override
		protected <T> T doBuild(GradleProject project, Class<T> requiredType, IProgressMonitor monitor) throws CoreException {
			System.out.println(">> building "+project.getLocation().getName()+"::"+requiredType.getSimpleName());
			incrementBuildCount(project, requiredType);
			monitor.beginTask("Building of type "+requiredType.getSimpleName()+" for '"+project.getDisplayName()+"'", 1);
			try {
				simulateBuildTime(monitor);
				checkError(project, requiredType);
				File loc = project.getLocation();
				if (loc.isDirectory() && !loc.getName().startsWith(".")) {
					return createModel(loc, requiredType);
				}
				throw ExceptionUtil.coreException("No such project: "+project.getLocation());
			} finally {
				System.out.println("<< building "+project.getLocation().getName()+"::"+requiredType.getSimpleName());
				monitor.done();
			}
		}

		private void simulateBuildTime(IProgressMonitor monitor) {
			if (buildTime>0) {
				long start = System.currentTimeMillis();
				long end = start + buildTime;
				while ( end > System.currentTimeMillis()) {
					JobUtil.checkCanceled(monitor);
					try {
						Thread.sleep(buildTime/10);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		private void checkError(GradleProject p, Class<?> t) throws CoreException {
			Throwable error = errors.get(modelKey(p, t));
			if (error!=null) {
				throw ExceptionUtil.coreException(error);
			}
		}

		public synchronized int totalBuilds() {
			int sum = 0;
			for (Integer ct : buildCounters.values()) {
				sum += ct;
			}
			return sum;
		}

		public synchronized int count(GradleProject project, Class<?> type) {
			String key = modelKey(project, type);
			Integer count = buildCounters.get(key);
			if (count!=null) {
				return count;
			}
			return 0;
		}
		
		public void dump() {
			System.out.println("==== Build Count Summary =====");
			for (Entry<String, Integer> e : buildCounters.entrySet()) {
				System.out.println(e.getValue()+"  "+e.getKey());
			}
			System.out.println("------------------------------");
			System.out.println("total: "+totalBuilds());
			System.out.println("==============================");
		}
	}

	private MockModelBuilder builder = new MockModelBuilder();
	private GradleModelManager mgr = new GradleModelManager(builder);
	
	@SuppressWarnings("unchecked")
	public <T> T createModel(File loc, Class<T> type) throws CoreException {
		if (type.equals(FooModel.class)) {
			return (T) new FooModelImpl(loc);
		} else if (type.equals(BarModel.class)) {
			return (T) new BarModelImpl(loc);
		} else if (type.equals(FooHierarchyModel.class)) {
			return (T) new FooHierarchyModelImpl(loc);
		} else if (type.equals(HierarchicalEclipseProject.class)) {
			return (T) new VanillaHierarchyModel(loc);
		}
		throw ExceptionUtil.coreException("Unkown model type: "+type);
	}

	private GradleProject project(String loc) {
		return GradleCore.create(new File(PROJECT_FOLDER, loc));
	}

	class BadProjectException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadProjectException(String string) {
			super(string);
		}
	}

	/**
	 * Listener for testing, it 'expects' a certain number of events and keeps
	 * track of whether its expectations are met. At the end of the test
	 * sequence the test should call 'assertExpectations' to verify whether
	 * the expectations are met at that point in time.
	 */
	private static class Expector implements IGradleModelListener {

		final private Class<?> expectType;
		private int expectCount;
		final private GradleProject expectProject;
		
		public Expector(GradleProject expectProject, Class<?> expectType, int expectCount) {
			super();
			this.expectProject = expectProject;
			this.expectType = expectType;
			this.expectCount = expectCount;
		}

		public void reset(int newExpectCount) {
			this.expectCount = newExpectCount;
			reset();
		}

		/**
		 * Reset so we can verify the same expectations all over again.
		 */
		public void reset() {
			actualCount = 0;
		}

		//Counts number of times listener is called
		private int actualCount = 0;
		
		//Things that can be checked right away will result in message being built up in here.
		private StringBuilder errors = null; 
		
		@Override
		public synchronized <T> void modelChanged(GradleProject project, Class<T> type, T model) {
			System.out.println("modelChange: "+project.getLocation().getName() + "  "+type.getSimpleName());
			if (expectProject!=null && project!=expectProject) {
				error("Unexpected project: "+project.getDisplayName());
			}
			if (expectType!=null && !expectType.equals(type)) {
				//Not an error, listeners are attached per-project, not per-type. So we will get events for
				// other types and should just ignore those events.
				return;
			}
			if (!type.isAssignableFrom(model.getClass())) {
				error("Bad model: "+model);
			}
			actualCount++;
		}

		private synchronized void error(String string) {
			if (errors==null) {
				errors = new StringBuilder();
			}
			errors.append(string);
			errors.append("\n");
		}
		
		public void assertExpectations() throws AssertionFailedError {
			String me = "Listener ["+expectProject.getLocation().getName()+", "+expectType.getSimpleName()+"]";
			if (errors!=null) {
				fail(me+":\n"+errors);
			}
			assertEquals(me+" calls",
					expectCount, actualCount);
		}
		
		@Override
		public String toString() {
			return "[" + expectProject.getLocation().getName()+", "+expectType.getSimpleName()+"]";
		}
		
	}


	
}
