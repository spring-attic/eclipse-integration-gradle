import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import io.pivotal.tooling.model.eclipse.StsEclipseProject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.ExternalDependency;
import org.junit.BeforeClass;
import org.junit.Test;

public class StsEclipseProjectTest {
    static StsEclipseProject root;
    static ProjectConnection connection;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        GradleConnector connector = GradleConnector.newConnector();

        connector.forProjectDirectory(file("projects/multiproject"));
        connection = connector.connect();

        ModelBuilder<StsEclipseProject> customModelBuilder = connection.model(StsEclipseProject.class);
        customModelBuilder.setJvmArguments(
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiRepo=" + file("../../org.springsource.ide.eclipse.gradle.toolingapi/lib").getAbsolutePath(),
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiEquivalentBinaryVersion=latest.integration"
        );
        customModelBuilder.withArguments("--init-script", file("projects/init.gradle").getAbsolutePath());

        root = customModelBuilder.get();
    }

    @Test
    public void eachProjectContainsAPointerToTheRoot() {
        assertEquals(root, project("a").getRoot());
        assertEquals(root, project("b").getRoot());
        assertEquals(root, root.getRoot());
    }

    @Test
    public void projectWithNoJavaPluginHasEmptyClasspath() {
    		assertTrue(root.getClasspath().isEmpty());
    }

    @Test
    public void rootProjectContainsResolvedHierarchyOfAllChildProjects() {
        List<String> projectNames = new ArrayList<String>();
        for (StsEclipseProject project : root.getChildren())
			projectNames.add(project.getGradleProject().getName());
        
        assertThat(projectNames, hasItems("a", "b", "minus", "plus"));
    }

    @Test
    public void allBinaryTransitivesAreDiscovered() {
    		StsEclipseProject a = project("a");
    		StsEclipseProject b = project("b");

    		assertEquals(0, a.getChildren().size());
    		assertEquals(7, a.getClasspath().size());

    		List<String> moduleNames = new ArrayList<String>();
    		for (ExternalDependency dependency : a.getClasspath())
    			moduleNames.add(dependency.getGradleModuleVersion().getName());

    		assertThat(moduleNames, hasItems("guava"));
    		assertEquals(0, b.getChildren().size());

    		moduleNames.clear();
    		for (ExternalDependency dependency : b.getClasspath())
    			moduleNames.add(dependency.getGradleModuleVersion().getName());
    		
    		assertEquals(0, b.getChildren().size());
    		assertEquals(8, b.getClasspath().size());
    		assertThat(moduleNames, hasItems("jackson-dataformat-xml"));
    }

    @Test
    public void externalEquivalentsOfProjectReferencesAreDiscoverable() {
        // Publish a binary form of both 'a' and 'b' to Maven Local
        connection.newBuild().forTasks("publish").run();
        StsEclipseProject a = project("a");

        ExternalDependency depWithExternalEquivalent = a.getProjectDependencies().iterator().next().getExternalEquivalent();

        assertEquals("b-1.0.jar", depWithExternalEquivalent.getFile().getName());
        assertEquals("b-1.0-sources.jar", depWithExternalEquivalent.getSource().getName());
    }

    @Test
    public void projectDependenciesContainReferenceToTheirProjectsGradleModuleVersion() {
    		assertEquals("b", project("a").getProjectDependencies().iterator().next().getGradleModuleVersion().getName());
    }

    @Test
    public void canDetermineIfProjectHasPluginApplied() {
        StsEclipseProject a = project("a");
        StsEclipseProject b = project("b");

        assertTrue(a.hasPlugin(JavaPlugin.class));
        assertTrue(a.hasPlugin(MavenPublishPlugin.class));
    		assertTrue(b.hasPlugin(JavaPlugin.class));
		assertTrue(b.hasPlugin(MavenPublishPlugin.class));
        assertFalse(root.hasPlugin(JavaPlugin.class));
    }

    @Test
    public void minusConfigurationsResultsInResolutionOfAllConfigurationsExceptForThoseSpecified() {
        StsEclipseProject minus = project("minus");
        assertEquals(2, minus.getClasspath().size());

        List<String> moduleNames = new ArrayList<String>();
        for (ExternalDependency dep : minus.getClasspath())
            moduleNames.add(dep.getGradleModuleVersion().getName());

        assertThat(moduleNames, hasItems("hamcrest-core", "junit"));
    }

    @Test
    public void plusConfigurationsResultsInResolutionOfAdditionalConfigurations() {
        assertEquals(2, project("plus").getClasspath().size());
    }

    @Test
    public void projectHasListOfPublications() {
    		assertEquals("a", project("a").getPublications().getAt(0).getName());
    }

    @Test
    public void projectHasName() {
        assertEquals("a", project("a").getName());
    }

    @Test
    public void projectDependencyCyclesAreResolvable() throws URISyntaxException {
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(file("projects/multiproject-cycle"));

        ModelBuilder<StsEclipseProject> customModelBuilder = connector.connect().model(StsEclipseProject.class);
        customModelBuilder.setJvmArguments(
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiRepo=" + file("../../org.springsource.ide.eclipse.gradle.toolingapi/lib").getAbsolutePath(),
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiEquivalentBinaryVersion=latest.integration"
        );
        customModelBuilder.withArguments("--init-script", file("projects/init.gradle").getAbsolutePath());

        StsEclipseProject project = customModelBuilder.get().getChildren().iterator().next();
        assertEquals(1, project.getProjectDependencies().size());
    }

    StsEclipseProject project(String name) {
        for (StsEclipseProject project : root.getChildren())
            if(project.getGradleProject().getName().equals(name))
                return project;
        return null;
    }

    static File file(String path) {
        return new File(System.getProperty("user.dir"), path);
    }
}
