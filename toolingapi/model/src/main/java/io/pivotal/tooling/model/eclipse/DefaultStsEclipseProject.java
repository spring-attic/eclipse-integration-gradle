package io.pivotal.tooling.model.eclipse;

import org.gradle.plugins.ide.internal.tooling.eclipse.*;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class DefaultStsEclipseProject implements Serializable {
    private DefaultGradleProject<?> gradleProject;
    private DefaultEclipseProject hierarchicalEclipseProject;
    private List<DefaultEclipseExternalDependency> classpath;
    private DefaultStsEclipseProject parent;
    private List<DefaultStsEclipseProject> children;

    public DefaultStsEclipseProject(DefaultEclipseProject hierarchicalEclipseProject,
                                    DefaultGradleProject<?> gradleProject,
                                    List<DefaultEclipseExternalDependency> classpath,
                                    List<DefaultStsEclipseProject> children) {
        this.hierarchicalEclipseProject = hierarchicalEclipseProject;
        this.gradleProject = gradleProject;
        this.classpath = classpath;
        this.children = children;
    }

    public Collection<DefaultEclipseExternalDependency> getClasspath() { return classpath; }

    public DefaultGradleProject<?> getGradleProject() { return gradleProject; }

    public DefaultStsEclipseProject getParent() {
        return parent;
    }

    public List<DefaultStsEclipseProject> getChildren() {
        return children;
    }

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

    public void setParent(DefaultStsEclipseProject parent) {
        this.parent = parent;
    }
}
