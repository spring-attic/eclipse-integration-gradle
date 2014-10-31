package io.pivotal.tooling.plugin.eclipse;

import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProject;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProjectDependency;
import org.gradle.tooling.model.GradleModuleVersion;

import java.io.Serializable;

public class DefaultStsEclipseProjectDependency implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private DefaultEclipseProjectDependency eclipseProjectDependency;
    private GradleModuleVersion gradleModuleVersion;
    private DefaultStsEclipseExternalDependency externalEquivalent;

    public DefaultStsEclipseProjectDependency(DefaultEclipseProjectDependency projectDependency, GradleModuleVersion gradleModuleVersion, DefaultStsEclipseExternalDependency externalEquivalent) {
        this.eclipseProjectDependency = projectDependency;
        this.gradleModuleVersion = gradleModuleVersion;
        this.externalEquivalent = externalEquivalent;
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

    public DefaultStsEclipseExternalDependency getExternalEquivalent() { return externalEquivalent; }
}
