/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.helidon.build.common.Strings.isValid;
import static io.helidon.build.common.Strings.requireValid;

/**
 * Pom file utilities.
 */
public final class MavenModel {

    private final Parent parent;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String name;
    private final String description;
    private final String packaging;

    private MavenModel(Builder builder) {
        if (isValid(builder.parentGroupId) && isValid(builder.parentArtifactId) && isValid(builder.parentVersion)) {
            this.parent = new Parent(builder.parentGroupId, builder.parentArtifactId, builder.parentVersion);
        } else {
            this.parent = null;
        }
        this.groupId = requireValid(isValid(builder.groupId) ? builder.groupId : builder.parentGroupId, "groupId is not valid");
        this.artifactId = requireValid(builder.artifactId, "artifactId is not valid");
        this.version = requireValid(isValid(builder.version) ? builder.version : builder.parentVersion, "version is not valid");
        this.packaging = requireValid(isValid(builder.packaging) ? builder.packaging : "jar", "packaging is not valid");
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Read a model.
     *
     * @param pomFile The pom file.
     * @return The model.
     * @throws RuntimeException on error.
     */
    public static MavenModel read(Path pomFile) {
        try {
            return read(Files.newInputStream(pomFile));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Read a model.
     *
     * @param is input stream
     * @return The model.
     * @throws RuntimeException on error.
     */
    public static MavenModel read(InputStream is) {
        try (MavenModelReader reader = new MavenModelReader(is)) {
            return reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the parent POM definition.
     *
     * @return Parent, or {@code null} if the POM does not have a parent.
     */
    public Parent parent() {
        return parent;
    }

    /**
     * Get the project groupId.
     *
     * @return groupId, never {@code null}
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Get the project artifactId.
     *
     * @return artifactId, never {@code null}
     */
    public String artifactId() {
        return artifactId;
    }

    /**
     * Get the project version.
     *
     * @return version, never {@code null}
     */
    public String version() {
        return version;
    }

    /**
     * Get the project name.
     *
     * @return name, may be {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Get the project description.
     *
     * @return description, may be {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Get the project packaging.
     *
     * @return packaging, never {@code null}
     */
    public String packaging() {
        return packaging;
    }

    /**
     * Parent POM definition.
     */
    public static final class Parent {

        private final String groupId;
        private final String artifactId;
        private final String version;

        private Parent(String groupId, String artifactId, String version) {
            this.groupId = requireValid(groupId, "parent groupId is not valid");
            this.artifactId = requireValid(artifactId, "parent artifactId is not valid");
            this.version = requireValid(version, "parent version is not valid");
        }

        /**
         * Get the parent groupId.
         *
         * @return groupId
         */
        public String groupId() {
            return groupId;
        }

        /**
         * Get the parent artifactId.
         *
         * @return artifactId
         */
        public String artifactId() {
            return artifactId;
        }

        /**
         * Get the parent version.
         *
         * @return version
         */
        public String version() {
            return version;
        }
    }

    /**
     * MavenModel builder.
     */
    public static final class Builder {
        private String parentGroupId;
        private String parentArtifactId;
        private String parentVersion;
        private String groupId;
        private String artifactId;
        private String version;
        private String name;
        private String description;
        private String packaging;

        /**
         * Set the parent groupId.
         *
         * @param parentGroupId groupId
         * @return this builder
         */
        public Builder parentGroupId(String parentGroupId) {
            this.parentGroupId = parentGroupId;
            return this;
        }

        /**
         * Set the parent artifactId.
         *
         * @param parentArtifactId artifactId
         * @return this builder
         */
        public Builder parentArtifactId(String parentArtifactId) {
            this.parentArtifactId = parentArtifactId;
            return this;
        }

        /**
         * Set the parent version.
         *
         * @param parentVersion version
         * @return this builder
         */
        public Builder parentVersion(String parentVersion) {
            this.parentVersion = parentVersion;
            return this;
        }

        /**
         * Set the groupId.
         *
         * @param groupId groupId
         * @return this builder
         */
        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        /**
         * Set the artifactId.
         *
         * @param artifactId artifactId
         * @return this builder
         */
        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        /**
         * Set the version.
         *
         * @param version version
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Set the packaging.
         *
         * @param packaging packaging
         * @return this builder
         */
        public Builder packaging(String packaging) {
            this.packaging = packaging;
            return this;
        }

        /**
         * Set the name.
         *
         * @param name name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Build the instance.
         *
         * @return MavenModel
         */
        public MavenModel build() {
            return new MavenModel(this);
        }
    }
}
