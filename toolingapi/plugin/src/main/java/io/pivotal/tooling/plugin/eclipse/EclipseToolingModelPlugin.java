package io.pivotal.tooling.plugin.eclipse;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import javax.inject.Inject;

class EclipseToolingModelPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry;
    private final ProjectPublicationRegistry publicationRegistry;

    @Inject
    public EclipseToolingModelPlugin(ToolingModelBuilderRegistry registry, ProjectPublicationRegistry publicationRegistry) {
        this.registry = registry;
        this.publicationRegistry = publicationRegistry;
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().create("eclipseToolingModel", EclipseToolingModelPluginExtension.class);
        registry.register(new StsEclipseProjectModelBuilder(publicationRegistry));
    }
}
