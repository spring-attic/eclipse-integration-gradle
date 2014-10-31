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
                "-Dorg.springsource.ide.eclipse.gradle.toolingApiRepo=" + new File('../org.springsource.ide.eclipse.gradle.toolingapi/lib').getAbsolutePath(),
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

    def 'a project that does not have the java plugin applied has an empty classpath'() {
        when:
        root // only root's subprojects have the java plugin applied

        then:
        root.classpath.empty
    }

    def 'the root project contains a fully resolved hierarchy of all child projects'() {
        when:
        def a = project('a')
        def b = project('b')

        then:
        root.gradleProject.name == 'test'
        root.children.collect { it.gradleProject.name }.sort() == ['a','b','minus','plus']

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
        b.classpath.size() == 8 // 1 first order dependency and 7 transitives through 'jackson-dataformat-xml' (including one test dep)
        b.classpath.collect { it.gradleModuleVersion.name }.contains('jackson-dataformat-xml')
    }

    def 'external equivalents of project references are discoverable'() {
        when:
        // Publish a binary form of both 'a' and 'b' to Maven Local
        connection.newBuild().forTasks("publish").run()
        def a = project('a')

        def depWithExternalEquivalent = a.projectDependencies[0].externalEquivalent

        then:
        depWithExternalEquivalent.file.name == 'b-1.0.jar'
        depWithExternalEquivalent.source.name == 'b-1.0-sources.jar'
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

    def 'minusConfigurations results in resolution of all configurations except for those specified'() {
        when:
        def minus = project('minus')

        then:
        minus.classpath.size() == 2
        minus.classpath.collect { it.gradleModuleVersion.name }.sort() == ['hamcrest-core','junit']
    }

    def 'plusConfigurations results in resolution of additional configurations'() {
        when:
        def plus = project('plus')

        then:
        plus.classpath.size() == 2
    }

    def 'project has a list of publications'() {
        when:
        def a = project('a')

        then:
        a.publications.collect { it.name } == ['a']
    }

    def project(String name) { root.children.find { it.gradleProject.name == name } }
}
