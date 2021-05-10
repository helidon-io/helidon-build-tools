package io.helidon.lsp.server.model;

import java.util.List;

public class ConfigurationMetadata {

    private List<ConfigurationGroup> groups;
    private List<ConfigurationProperty> properties;
    private List<ConfigurationHint> hints;

    public ConfigurationMetadata() {
    }

    public List<ConfigurationGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ConfigurationGroup> groups) {
        this.groups = groups;
    }

    public List<ConfigurationProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigurationProperty> properties) {
        this.properties = properties;
    }

    public List<ConfigurationHint> getHints() {
        return hints;
    }

    public void setHints(List<ConfigurationHint> hints) {
        this.hints = hints;
    }
}
