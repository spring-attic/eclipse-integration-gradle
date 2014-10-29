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
import org.gradle.plugins.ide.internal.tooling.EclipseModelBuilder
import org.gradle.plugins.ide.internal.tooling.GradleProjectBuilder
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseExternalDependency
import org.gradle.runtime.jvm.JvmLibrary
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject
import org.gradle.tooling.provider.model.ToolingModelBuilder

class StsEclipseProjectModelBuilder implements ToolingModelBuilder {
    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(StsEclipseProject.name)
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        def externalDependencies = buildExternalDependencies(project)
        def gradleProjectBuilder = new GradleProjectBuilder()
        def rootGradleProject = gradleProjectBuilder.buildAll(project)
        def eclipseModelBuilder = new EclipseModelBuilder(gradleProjectBuilder)

        return new DefaultStsEclipseProject(eclipseModelBuilder.buildAll(HierarchicalEclipseProject.name, project),
                rootGradleProject.findByPath(project.getPath()), externalDependencies)
    }

    /**
     * @param project
     * @return - A list of all binary dependencies, including transitives of both
     * binary dependencies and project dependencies
     */
    private static def buildExternalDependencies(Project project) {
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

        project.configurations.create('projectReferences') {

        }

        project.configurations.projectReferences.resolvedConfiguration.lenientConfiguration.

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
}
