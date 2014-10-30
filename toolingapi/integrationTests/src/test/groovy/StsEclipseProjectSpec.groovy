import io.pivotal.tooling.model.eclipse.StsEclipseProject
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import spock.lang.Specification

class StsEclipseProjectSpec extends Specification {
    def 'all binary transitive dependencies are discovered'() {
        setup:
        def connector = GradleConnector.newConnector()
        connector.forProjectDirectory(new File(getClass().getResource('.').toURI()))
        def connection = connector.connect()

        ModelBuilder<StsEclipseProject> customModelBuilder = connection.model(StsEclipseProject.class)
        customModelBuilder.withArguments("--init-script", new File(getClass().getResource('init.gradle').toURI()).absolutePath)

        when:
        StsEclipseProject model = customModelBuilder.get()

        then:
        model.gradleProject.name == 'test'
        model.children.collect { it.gradleProject.name }.sort() == ['a','b']
        model.classpath.empty

        when:
        def a = model.children.find { it.gradleProject.name == 'a' }

        then:
        a.children.size() == 0
        a.parent == model
        a.classpath.size() == 7 // 1 first order dependency and 6 transitives through project reference 'b'
        a.classpath.collect { it.gradleModuleVersion.name }.contains('guava') // our first order dep

        when:
        def b = model.children.find { it.gradleProject.name == 'b' }

        then:
        b.children.size() == 0
        b.parent == model
        b.classpath.size() == 6 // 1 first order dependency and 5 transitives through 'jackson-dataformat-xml'
        b.classpath.collect { it.gradleModuleVersion.name }.contains('jackson-dataformat-xml')
    }
}
