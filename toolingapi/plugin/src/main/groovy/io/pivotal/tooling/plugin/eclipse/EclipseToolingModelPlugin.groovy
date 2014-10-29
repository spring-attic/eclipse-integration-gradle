package io.pivotal.tooling.plugin.eclipse

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import javax.inject.Inject

class EclipseToolingModelPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry

    @Inject
    public EclipseToolingModelPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry
    }

    @Override
    void apply(Project project) {
        registry.register(new StsEclipseProjectModelBuilder())
    }
}
