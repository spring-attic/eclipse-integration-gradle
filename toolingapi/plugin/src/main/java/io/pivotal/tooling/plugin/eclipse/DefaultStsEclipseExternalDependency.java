package io.pivotal.tooling.plugin.eclipse;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.tooling.internal.gradle.DefaultGradleModuleVersion;
import org.gradle.tooling.model.GradleModuleVersion;

import java.io.File;
import java.io.Serializable;

public class DefaultStsEclipseExternalDependency implements Serializable {
	private static final long serialVersionUID = 1L;
	
    private File file;
    private File javadoc;
    private File source;
    private GradleModuleVersion moduleVersion;

    public File getFile() {
        return file;
    }

    public File getJavadoc() {
        return javadoc;
    }

    public File getSource() {
        return source;
    }

    public GradleModuleVersion getGradleModuleVersion() {
        return moduleVersion;
    }

    public DefaultStsEclipseExternalDependency setModuleVersion(ModuleVersionIdentifier id) {
        moduleVersion = (id == null)? null : new DefaultGradleModuleVersion(id);
        return this;
    }

    public DefaultStsEclipseExternalDependency setFile(File file) {
        this.file = file;
        return this;
    }

    public DefaultStsEclipseExternalDependency setJavadoc(File javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    public DefaultStsEclipseExternalDependency setSource(File source) {
        this.source = source;
        return this;
    }
}
