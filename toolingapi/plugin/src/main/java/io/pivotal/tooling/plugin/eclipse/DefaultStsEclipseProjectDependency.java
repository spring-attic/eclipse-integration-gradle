package io.pivotal.tooling.plugin.eclipse;

import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProject;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProjectDependency;
import org.gradle.tooling.model.GradleModuleVersion;

import java.io.Serializable;

public class DefaultStsEclipseProjectDependency implements Serializable {
    private DefaultEclipseProjectDependency eclipseProjectDependency;
    private GradleModuleVersion gradleModuleVersion;

    public DefaultStsEclipseProjectDependency(DefaultEclipseProjectDependency projectDependency, GradleModuleVersion gradleModuleVersion) {
        this.eclipseProjectDependency = projectDependency;
        this.gradleModuleVersion = gradleModuleVersion;
    }

    public GradleModuleVersion getGradleModuleVersion() {
        return gradleModuleVersion;
    }

    public DefaultEclipseProject getTargetProject() {
        return eclipseProjectDependency.getTargetProject();
    }

    public String getPath() {
        return eclipseProjectDependency.getPath();
    }
}
