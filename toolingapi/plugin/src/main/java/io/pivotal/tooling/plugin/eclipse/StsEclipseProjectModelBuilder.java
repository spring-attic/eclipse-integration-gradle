package io.pivotal.tooling.plugin.eclipse;

import io.pivotal.tooling.model.eclipse.StsEclipseProject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.component.DefaultModuleComponentSelector;
import org.gradle.api.internal.artifacts.result.DefaultResolvedArtifactResult;
import org.gradle.api.specs.Specs;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.gradle.plugins.ide.internal.tooling.EclipseModelBuilder;
import org.gradle.plugins.ide.internal.tooling.GradleProjectBuilder;
import org.gradle.runtime.jvm.JvmLibrary;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.util.*;

class StsEclipseProjectModelBuilder implements ToolingModelBuilder {
    private DefaultStsEclipseProject result;
    private Project currentProject;

    private GradleProjectBuilder gradleProjectBuilder = new GradleProjectBuilder();
    private DefaultGradleProject rootGradleProject;

    private EclipseModelBuilder eclipseModelBuilder = new EclipseModelBuilder(gradleProjectBuilder);

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(StsEclipseProject.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        currentProject = project;
        rootGradleProject = gradleProjectBuilder.buildAll(project);
        buildHierarchy(project);
        return result;
    }

    /**
     * @param project
     * @return - A list of all binary dependencies, including transitives of both
     * binary dependencies and project dependencies
     */
    private static List<DefaultStsEclipseExternalDependency> buildExternalDependencies(Project project) {
        boolean hasCompile = false;
        for(Configuration conf : project.getConfigurations())
            if(conf.getName().equals("compile"))
                hasCompile = true;
        if(!hasCompile)
            return Collections.EMPTY_LIST;

        Map<String, DefaultStsEclipseExternalDependency> externalDependenciesById = new HashMap<String, DefaultStsEclipseExternalDependency>();

        List<ComponentIdentifier> binaryDependencies = new ArrayList<ComponentIdentifier>();
        for (DependencyResult dep : project.getConfigurations().getByName("compile").getIncoming().getResolutionResult().getAllDependencies()) {
            if(dep instanceof ResolvedDependencyResult && dep.getRequested() instanceof DefaultModuleComponentSelector)
                binaryDependencies.add(((ResolvedDependencyResult) dep).getSelected().getId());
        }

        List<String> binaryDependenciesAsStrings = new ArrayList<String>();
        for (ComponentIdentifier binaryDependency : binaryDependencies)
            binaryDependenciesAsStrings.add(binaryDependency.toString());

        Set<ComponentArtifactsResult> binaryComponents = project.getDependencies().createArtifactResolutionQuery()
                .forComponents(binaryDependencies)
                .withArtifacts(JvmLibrary.class, SourcesArtifact.class, JavadocArtifact.class)
                .execute()
                .getResolvedComponents();

        for (ResolvedArtifact artifact : project.getConfigurations().getByName("compile").getResolvedConfiguration().getLenientConfiguration().getArtifacts(Specs.SATISFIES_ALL)) {
            ModuleVersionIdentifier id = artifact.getModuleVersion().getId();

            if(binaryDependenciesAsStrings.contains(id.toString())) {
                externalDependenciesById.put(id.toString(), new DefaultStsEclipseExternalDependency()
                        .setFile(artifact.getFile())
                        .setModuleVersion(new DefaultModuleVersionIdentifier(id.getGroup(), id.getName(), id.getVersion())));
            }
        }

        for (ComponentArtifactsResult binaryDependency : binaryComponents) {
            DefaultStsEclipseExternalDependency externalDependency = externalDependenciesById.get(binaryDependency.getId().toString());
            for (ArtifactResult sourcesResult : binaryDependency.getArtifacts(SourcesArtifact.class)) {
                if(sourcesResult instanceof DefaultResolvedArtifactResult)
                    externalDependency.setSource(((DefaultResolvedArtifactResult) sourcesResult).getFile());
            }
            for (ArtifactResult javadocResult : binaryDependency.getArtifacts(JavadocArtifact.class)) {
                if(javadocResult instanceof DefaultResolvedArtifactResult)
                    externalDependency.setJavadoc(((DefaultResolvedArtifactResult) javadocResult).getFile());
            }
        }

        // must create new list because Map.values() is not Serializable
        return new ArrayList<DefaultStsEclipseExternalDependency>(externalDependenciesById.values());
    }

    private DefaultStsEclipseProject buildHierarchy(Project project) {
        List<DefaultStsEclipseProject> children = new ArrayList<DefaultStsEclipseProject>();
        for (Project child : project.getChildProjects().values())
            children.add(buildHierarchy(child));

        List<DefaultStsEclipseExternalDependency> externalDependencies = buildExternalDependencies(project);

        DefaultStsEclipseProject eclipseProject = new DefaultStsEclipseProject(
                eclipseModelBuilder.buildAll(HierarchicalEclipseProject.class.getName(), project),
                rootGradleProject.findByPath(project.getPath()), externalDependencies, children);

        for (DefaultStsEclipseProject child : children)
            child.setParent(eclipseProject);

        if (project == currentProject)
            result = eclipseProject;

        return eclipseProject;
    }
}
