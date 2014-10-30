package io.pivotal.tooling.plugin.eclipse

import io.pivotal.tooling.model.eclipse.DefaultStsEclipseProject
import io.pivotal.tooling.model.eclipse.StsEclipseProject
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.component.DefaultModuleComponentSelector
import org.gradle.api.specs.Specs
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.internal.tooling.EclipseModelBuilder
import org.gradle.plugins.ide.internal.tooling.GradleProjectBuilder
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseExternalDependency
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProject
import org.gradle.runtime.jvm.JvmLibrary
import org.gradle.tooling.internal.gradle.DefaultGradleProject
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.util.GUtil

class StsEclipseProjectModelBuilder implements ToolingModelBuilder {
    private DefaultStsEclipseProject result
    private Project currentProject

    def gradleProjectBuilder = new GradleProjectBuilder()
    DefaultGradleProject rootGradleProject

    def eclipseModelBuilder = new EclipseModelBuilder(gradleProjectBuilder)

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(StsEclipseProject.name)
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        currentProject = project
        rootGradleProject = gradleProjectBuilder.buildAll(project)
        buildHierarchy(project)
        return result
    }

    /**
     * @param project
     * @return - A list of all binary dependencies, including transitives of both
     * binary dependencies and project dependencies
     */
    private static def buildExternalDependencies(Project project) {
        if(!project.configurations.collect { it.name }.contains('compile'))
            return []

        def externalDependenciesById = [:]

        def binaryDependencies = project.configurations.compile.incoming.resolutionResult.allDependencies.inject([]) { mods, dep ->
            if(dep instanceof ResolvedDependencyResult && dep.requested instanceof DefaultModuleComponentSelector)
                mods += dep.selected.id
            return mods
        }

        def binaryComponents = project.dependencies.createArtifactResolutionQuery()
                .forComponents(*binaryDependencies)
                .withArtifacts(JvmLibrary.class, *[SourcesArtifact, JavadocArtifact])
                .execute()
                .getResolvedComponents()

        def binaryDependenciesAsStrings = binaryDependencies*.toString()

        // set the compile jar
        project.configurations.compile.resolvedConfiguration.lenientConfiguration.getArtifacts(Specs.SATISFIES_ALL).each {
            def id = it.moduleVersion.id

            if(binaryDependenciesAsStrings.contains(id.toString())) {
                externalDependenciesById[id.toString()] = [
                    file: it.file,
                    moduleVersion: new DefaultModuleVersionIdentifier(id.group, id.name, id.version)
                ]
            }
        }

        binaryComponents.each { binaryDependency ->
            def externalDependency = externalDependenciesById[binaryDependency.id.toString()]

            // set the sources jar
            binaryDependency.getArtifacts(SourcesArtifact).each { sourcesResult ->
                externalDependency.sourceFile = sourcesResult.file
            }

            // set the javadoc jar
            binaryDependency.getArtifacts(JavadocArtifact).each { javadocResult ->
                externalDependency.javadocFile = javadocResult.file
            }
        }

        return externalDependenciesById.values().collect {
            new DefaultEclipseExternalDependency(it.file, it.sourceFile, it.javadocFile, it.moduleVersion)
        }
    }

    private DefaultStsEclipseProject buildHierarchy(Project project) {
        List<DefaultStsEclipseProject> children = new ArrayList<DefaultStsEclipseProject>()
        for (Project child : project.getChildProjects().values())
            children.add(buildHierarchy(child))

        println 'building hierarchy for: ' + project.name

        def externalDependencies = buildExternalDependencies(project)
        def eclipseProject = new DefaultStsEclipseProject(eclipseModelBuilder.buildAll(HierarchicalEclipseProject.name, project),
                rootGradleProject.findByPath(project.getPath()), externalDependencies, children)

        for (DefaultStsEclipseProject child : children)
            child.setParent(eclipseProject)

        if (project == currentProject)
            result = eclipseProject

        return eclipseProject
    }
}
