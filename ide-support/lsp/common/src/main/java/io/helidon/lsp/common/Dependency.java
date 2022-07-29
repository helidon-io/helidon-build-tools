/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.common;

/**
 * Information about maven dependency.
 */
public class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private String path;

    /**
     * Create a new instance.
     */
    public Dependency() {
    }

    /**
     * Create a new instance.
     *
     * @param groupId    groupId
     * @param artifactId artifactId
     * @param version    version
     * @param type       type
     * @param scope      scope
     * @param path       path
     */
    public Dependency(String groupId, String artifactId, String version, String type, String scope, String path) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
        this.path = path;
    }

    /**
     * Get groupId.
     *
     * @return groupId.
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Get artifactId.
     *
     * @return artifactId.
     */
    public String artifactId() {
        return artifactId;
    }

    /**
     * Get version.
     *
     * @return version.
     */
    public String version() {
        return version;
    }

    /**
     * Get type (jar, etc.).
     *
     * @return type.
     */
    public String type() {
        return type;
    }

    /**
     * Get scope.
     *
     * @return scope.
     */
    public String scope() {
        return scope;
    }

    /**
     * Get path to the file.
     *
     * @return path to the file.
     */
    public String path() {
        return path;
    }
}
