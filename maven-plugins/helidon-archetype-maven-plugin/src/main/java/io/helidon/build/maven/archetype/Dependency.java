/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

/**
 * Information about schema that describes helidon archetype xml file.
 */
public class Dependency {

    /**
     * groupId of the artifact that contains the schema file.
     */
    private String groupId;
    /**
     * artifactId of the artifact that contains the schema file.
     */
    private String artifactId;
    /**
     * version of the artifact that contains the schema file.
     */
    private String version;
    /**
     * type of the schema file.
     */
    private String type;
    /**
     * classifier for the schema file.
     */
    private String classifier;

    /**
     * Create a new instance.
     */
    public Dependency() {
    }

    /**
     * Get groupId.
     *
     * @return groupId
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Set groupId.
     */
    public void groupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Get artifactId.
     *
     * @return artifactId
     */
    public String artifactId() {
        return artifactId;
    }

    /**
     * Set artifactId.
     *
     * @param artifactId artifactId
     */
    public void artifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Get version.
     *
     * @return version
     */
    public String version() {
        return version;
    }

    /**
     * Set version.
     *
     * @param version version
     */
    public void version(String version) {
        this.version = version;
    }

    /**
     * Get type.
     *
     * @return type
     */
    public String type() {
        return type;
    }

    /**
     * Set type.
     *
     * @param type type
     */
    public void type(String type) {
        this.type = type;
    }

    /**
     * Get classifier.
     *
     * @return classifier
     */
    public String classifier() {
        return classifier;
    }

    /**
     * Set classifier.
     *
     * @param classifier classifier
     */
    public void classifier(String classifier) {
        this.classifier = classifier;
    }
}
