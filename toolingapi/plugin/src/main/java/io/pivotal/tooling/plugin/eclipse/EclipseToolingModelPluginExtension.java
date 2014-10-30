package io.pivotal.tooling.plugin.eclipse;

public class EclipseToolingModelPluginExtension {
    private String equivalentBinaryVersion = "latest.integration";

    public String getEquivalentBinaryVersion() {
        return equivalentBinaryVersion;
    }

    public void setEquivalentBinaryVersion(String equivalentBinaryVersion) {
        this.equivalentBinaryVersion = equivalentBinaryVersion;
    }
}
