import io.pivotal.tooling.model.eclipse.StsEclipseProject
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import spock.lang.Specification

class StsEclipseProjectSpec extends Specification {
    def 'all binary transitive dependencies are discovered'() {
        when:
        def connector = GradleConnector.newConnector()
        connector.forProjectDirectory(new File(getClass().getResource('a').toURI()))
        def connection = connector.connect()

        ModelBuilder<StsEclipseProject> customModelBuilder = connection.model(StsEclipseProject.class)
        customModelBuilder.withArguments("--init-script", new File(getClass().getResource('init.gradle').toURI()).absolutePath)
        StsEclipseProject model = customModelBuilder.get()

        then:
        model.classpath.size() == 3
        model.classpath.collect { it.gradleModuleVersion.name }.sort() == ['commons-logging', 'guava', 'spring-core']
        model.gradleProject.name == 'a'
    }
}
