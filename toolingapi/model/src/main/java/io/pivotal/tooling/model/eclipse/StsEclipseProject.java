package io.pivotal.tooling.model.eclipse;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

public interface StsEclipseProject extends HierarchicalEclipseProject {
    /**
     * {@inheritDoc}
     */
    StsEclipseProject getParent();

    /**
     * {@inheritDoc}
     */
    DomainObjectSet<? extends StsEclipseProject> getChildren();

    /**
     * The gradle project that is associated with this project.
     * Typically, a single Eclipse project corresponds to a single gradle project.
     * <p>
     * See {@link org.gradle.tooling.model.HasGradleProject}
     *
     * @return associated gradle project
     */
    GradleProject getGradleProject();

    /**
     * Returns the external dependencies which make up the classpath of this project.
     * The set includes ALL binary transitive dependencies, including those that are derived from
     * project dependencies.
     */
    DomainObjectSet<ExternalDependency> getClasspath();

    ExternalDependency getExternalEquivalent();

    boolean hasPlugin(Class<?> type);
}
