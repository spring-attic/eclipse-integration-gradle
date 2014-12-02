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
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getPath() == null) ? 0 : getPath().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultStsEclipseProjectDependency other = (DefaultStsEclipseProjectDependency) obj;
		if (getPath() == null) {
			if (other.getPath() != null)
				return false;
		} else if (!getPath().equals(other.getPath()))
			return false;
		return true;
	}
}
