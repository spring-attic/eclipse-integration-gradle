package io.pivotal.tooling.plugin.eclipse;

import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseLinkedResource;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseProject;
import org.gradle.plugins.ide.internal.tooling.eclipse.DefaultEclipseSourceDirectory;
import org.gradle.tooling.internal.gradle.DefaultGradleModuleVersion;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class DefaultStsEclipseProject implements Serializable {
	private static final long serialVersionUID = 1L;
	
    private DefaultGradleProject<?> gradleProject;
    private DefaultEclipseProject hierarchicalEclipseProject;
    private List<DefaultStsEclipseExternalDependency> classpath;
    private DefaultStsEclipseProject parent;
    private List<DefaultStsEclipseProject> children;
    private List<String> plugins;
    private List<DefaultStsEclipseProjectDependency> projectDependencies;
    private DefaultStsEclipseProject root;
    private List<DefaultGradleModuleVersion> publications;

    public List<DefaultStsEclipseExternalDependency> getClasspath() { return classpath; }

    public DefaultGradleProject<?> getGradleProject() { return gradleProject; }

    public DefaultStsEclipseProject getParent() {
        return parent;
    }

    public List<DefaultStsEclipseProject> getChildren() {
        return children;
    }

    public List<DefaultStsEclipseProjectDependency> getProjectDependencies() { return projectDependencies; }

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

    public boolean hasPlugin(Class<?> pluginClass) { return plugins.contains(pluginClass.getName()); }

    public DefaultStsEclipseProject getRoot() { return root; }

    public List<DefaultGradleModuleVersion> getPublications() { return publications; }

    public DefaultEclipseProject getHierarchicalEclipseProject() { return hierarchicalEclipseProject; }

    public DefaultStsEclipseProject setGradleProject(DefaultGradleProject<?> gradleProject) {
        this.gradleProject = gradleProject;
        return this;
    }

    public DefaultStsEclipseProject setHierarchicalEclipseProject(DefaultEclipseProject hierarchicalEclipseProject) {
        this.hierarchicalEclipseProject = hierarchicalEclipseProject;
        return this;
    }

    public DefaultStsEclipseProject setClasspath(List<DefaultStsEclipseExternalDependency> classpath) {
        this.classpath = classpath;
        return this;
    }

    public DefaultStsEclipseProject setChildren(List<DefaultStsEclipseProject> children) {
        this.children = children;
        return this;
    }

    public DefaultStsEclipseProject setPlugins(List<String> plugins) {
        this.plugins = plugins;
        return this;
    }

    public DefaultStsEclipseProject setProjectDependencies(List<DefaultStsEclipseProjectDependency> projectDependencies) {
        this.projectDependencies = projectDependencies;
        return this;
    }

    public DefaultStsEclipseProject setRoot(DefaultStsEclipseProject root) {
        this.root = root;
        return this;
    }

    public DefaultStsEclipseProject setPublications(List<DefaultGradleModuleVersion> publications) {
        this.publications = publications;
        return this;
    }
}
