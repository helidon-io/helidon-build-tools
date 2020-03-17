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
package io.helidon.build.util;

/**
 * Class ProjectDependency.
 */
public class ProjectDependency {

    private String groupId;
    private String artifactId;
    private String version;

    /**
     * Constructor.
     *
     * @param groupId The group ID.
     * @param artifactId The artifact ID.
     */
    public ProjectDependency(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    /**
     * Constructor.
     *
     * @param groupId The group ID.
     * @param artifactId The artifact ID.
     * @param version The version.
     */
    public ProjectDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Returns the group ID.
     *
     * @return The group ID.
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Sets the group ID.
     *
     * @param groupId The group ID.
     */
    public void groupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the artifact ID.
     *
     * @return The artifact ID.
     */
    public String artifactId() {
        return artifactId;
    }

    /**
     * Sets the artifact ID.
     *
     * @param artifactId The artifact ID.
     */
    public void artifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Gets the version.
     *
     * @return The version.
     */
    public String version() {
        return version;
    }

    /**
     * Sets the version.
     *
     * @param version The version.
     */
    public void version(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + (version != null ? ":" + version : "");
    }

    /**
     * Parse dependency from string.
     *
     * @param value String representation.
     * @return The dependency.
     */
    public static ProjectDependency fromString(String value) {
        String[] parts = value.split(":");
        return new ProjectDependency(parts[0], parts[1], parts.length > 2 ? parts[2] : null);
    }
}
