package io.pivotal.tooling.model.eclipse;

import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.eclipse.EclipseProjectDependency;

public interface StsEclipseProjectDependency extends EclipseProjectDependency {
    GradleModuleVersion getGradleModuleVersion();
    ExternalDependency getExternalEquivalent();
}
