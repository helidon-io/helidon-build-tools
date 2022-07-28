package io.helidon.lsp.common;

public class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private String path;

    public Dependency() {
    }

    public Dependency(String groupId, String artifactId, String version, String type, String scope, String path) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
        this.path = path;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public String type() {
        return type;
    }

    public String scope() {
        return scope;
    }

    public String path() {
        return path;
    }
}
