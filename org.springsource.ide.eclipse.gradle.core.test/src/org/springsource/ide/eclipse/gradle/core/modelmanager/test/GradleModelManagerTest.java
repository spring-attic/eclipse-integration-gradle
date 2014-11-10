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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.tests.builder.GetResourcesTests;
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
import org.springsource.ide.eclipse.gradle.core.modelmanager.ModelBuilder;
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
		
		Map<GradleProject, ModelPromise<FooHierarchyModel>> models = new HashMap<GradleProject, GradleModelManagerTest.ModelPromise<FooHierarchyModel>>();
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
	 * Note that this can be made to work, but only by locking the world during such builds. 
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
	 * only one model should get built also in the FAILURE.
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
	
	//TODO: check that build can be canceled via progress monitor.
	//TODO: check that canceled build do not get cached as permanent failures
	
	//TODO: when project hierarchy changes model manager recovers (i.e. can associate projects with new 'root' without 
	//  throwing 'InconsistentProjectHierarchyException'.
	//TODO: case where rootproject of a hierarchy changes since last succesful build.
	
	//TODO: test model listeners.
	
	/////////////////////////////////////////////////////////////////////
	
	// no tests below this line, this is all the scaffolding to make the tests work.
	
	/**
	 * Request an asynchronously built model, returns a 'promise' of the model.
	 */
	public <T> ModelPromise<T> getModelPromise(final GradleProject project, final Class<T> type) {
		final ModelPromise<T> promise = new ModelPromise<T>();
		GradleRunnable modelRequest = new GradleRunnable("Build model ["+type.getSimpleName()+"] for "+project.getDisplayName()) {
			@Override
			public void doit(IProgressMonitor mon) throws Exception {
				promise.setMonitor(mon);
				try {
					promise.apply(mgr.getModel(project, type, mon));
				} catch (Throwable e) {
					promise.error(e);
				}
			}
		};
		JobUtil.schedule(JobUtil.NO_RULE, modelRequest);
		return promise;
	}

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
			File parentLoc = loc.getParentFile();
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
			// TODO Auto-generated constructor stub
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
		 * Enables 'build time' simulation. This makes the 'build' request
		 * sleep for some time before returning the build
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

	public class ModelPromise<T> extends JoinableContinuation<T> {
		
		private IProgressMonitor monitor = null;
		private boolean canceled = false;

		public ModelPromise() {
		}
		
		public void setMonitor(IProgressMonitor mon) {
			this.monitor = mon;
			if (canceled) {
				monitor.setCanceled(true);
			}
		}

		public void cancel() {
			if (monitor!=null) {
				monitor.setCanceled(true);
			} else {
				this.canceled = true;
			}
		}
	}

	
	
}
