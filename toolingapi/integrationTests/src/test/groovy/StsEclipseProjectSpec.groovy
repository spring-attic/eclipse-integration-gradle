import io.pivotal.tooling.model.eclipse.StsEclipseProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.ProjectConnection
import spock.lang.Shared
import spock.lang.Specification

class StsEclipseProjectSpec extends Specification {
    @Shared StsEclipseProject root
    @Shared ProjectConnection connection

    def setupSpec() {
        def connector = GradleConnector.newConnector()

        connector.forProjectDirectory(new File(getClass().getResource('.').toURI()))
        connection = connector.connect()

        ModelBuilder<StsEclipseProject> customModelBuilder = connection.model(StsEclipseProject.class)
        customModelBuilder.setJvmArguments(
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiRepo=" + new File('repo').getAbsolutePath(),
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiEquivalentBinaryVersion=latest.integration"
        )
        customModelBuilder.withArguments("--init-script", new File(getClass().getResource('init.gradle').toURI()).absolutePath)

        root = customModelBuilder.get()
    }

    def 'each project contains a pointer to the root project'() {
        when:
        def a = project('a')
        def b = project('b')

        then:
        a.root == root
        b.root == root
        root.root == root
    }

    def 'the root project contains a fully resolved hierarchy of all child projects'() {
        when:
        def a = project('a')
        def b = project('b')

        then:
        root.gradleProject.name == 'test'
        root.children.collect { it.gradleProject.name }.sort() == ['a','b']
        root.classpath.empty

        a.parent == root
        b.parent == root
    }

    def 'all binary transitive dependencies are discovered'() {
        when:
        def a = project('a')
        def b = project('b')

        then:
        a.children.size() == 0
        a.classpath.size() == 7 // 1 first order dependency and 6 transitives through project reference 'b'
        a.classpath.collect { it.gradleModuleVersion.name }.contains('guava') // our first order dep

        then:
        b.children.size() == 0
        b.classpath.size() == 6 // 1 first order dependency and 5 transitives through 'jackson-dataformat-xml'
        b.classpath.collect { it.gradleModuleVersion.name }.contains('jackson-dataformat-xml')
    }

    def 'external equivalents of project references are discoverable'() {
        when:
        // Publish a binary form of both 'a' and 'b' to Maven Local
        connection.newBuild().forTasks("publish").run()
        def a = project('a')
        def b = project('b')

        then:
        a.externalEquivalent?.getFile()?.name == 'a-1.0.jar'
        a.externalEquivalent?.getSource()?.name == 'a-1.0-sources.jar'
        b.externalEquivalent?.getFile()?.name == 'b-1.0.jar'
        b.externalEquivalent?.getSource()?.name == 'b-1.0-sources.jar'
    }

    def 'project dependencies contain a reference to their project\'s gradle module version'() {
        when:
        def a = project('a')

        then:
        a.projectDependencies[0].gradleModuleVersion.name == 'b'
    }

    def 'can determine if a project has a particular plugin applied'() {
        when:
        def a = project('a')
        def b = project('b')

        then:
        a.hasPlugin(JavaPlugin)
        a.hasPlugin(MavenPublishPlugin)
        b.hasPlugin(JavaPlugin)
        b.hasPlugin(MavenPublishPlugin)
        !root.hasPlugin(JavaPlugin)
    }

    def project(String name) { root.children.find { it.gradleProject.name == name } }
}
