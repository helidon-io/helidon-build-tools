/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.cli.impl;

/**
 * Class ProjectDependency.
 */
class ProjectDependency {

    private String groupId;
    private String artifactId;
    private String version;

    ProjectDependency(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    ProjectDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    String groupId() {
        return groupId;
    }

    void groupId(String groupId) {
        this.groupId = groupId;
    }

    String artifactId() {
        return artifactId;
    }

    void artifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    String version() {
        return version;
    }

    void version(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + (version != null ? ":" + version : "");
    }

    static ProjectDependency fromString(String value) {
        String[] parts = value.split(":");
        return new ProjectDependency(parts[0], parts[1], parts.length > 2 ? parts[2] : null);
    }
}
