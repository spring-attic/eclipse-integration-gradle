package io.pivotal.tooling.plugin.eclipse;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import javax.inject.Inject;

class EclipseToolingModelPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry;

    @Inject
    public EclipseToolingModelPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().create("eclipseToolingModel", EclipseToolingModelPluginExtension.class);
        registry.register(new StsEclipseProjectModelBuilder());
    }
}
