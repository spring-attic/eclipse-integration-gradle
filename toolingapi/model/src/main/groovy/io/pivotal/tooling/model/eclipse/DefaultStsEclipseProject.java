package io.pivotal.tooling.model.eclipse;

import org.gradle.plugins.ide.internal.tooling.eclipse.*;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class DefaultStsEclipseProject implements Serializable {
    DefaultGradleProject<?> gradleProject;
    DefaultEclipseProject hierarchicalEclipseProject;
    List<DefaultEclipseExternalDependency> classpath;

    public DefaultStsEclipseProject(DefaultEclipseProject hierarchicalEclipseProject,
                                    DefaultGradleProject<?> gradleProject,
                                    List<DefaultEclipseExternalDependency> classpath) {
        this.hierarchicalEclipseProject = hierarchicalEclipseProject;
        this.gradleProject = gradleProject;
        this.classpath = classpath;
    }

    public Collection<DefaultEclipseExternalDependency> getClasspath() { return classpath; }

    public DefaultGradleProject<?> getGradleProject() { return gradleProject; }

    public DefaultEclipseProject getParent() { return hierarchicalEclipseProject.getParent(); }

    public List<DefaultEclipseProject> getChildren() { return hierarchicalEclipseProject.getChildren(); }

    public Iterable<? extends DefaultEclipseProjectDependency> getProjectDependencies() {
        return hierarchicalEclipseProject.getProjectDependencies();
    }

    public Iterable<? extends DefaultEclipseSourceDirectory> getSourceDirectories() {
        return hierarchicalEclipseProject.getSourceDirectories();
    }

    public File getProjectDirectory() {
        return hierarchicalEclipseProject.getProjectDirectory();
    }

    public Iterable<? extends DefaultEclipseLinkedResource> getLinkedResources() {
        return hierarchicalEclipseProject.getLinkedResources();
    }
}
