/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.util.LinkedList;
import java.util.Map;

import io.helidon.build.common.xml.SimpleXMLParser;

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

    private MavenModel(ModelReader reader) {
        if (reader.hasParent) {
            this.parent = new Parent(reader.parentGroupId, reader.parentArtifactId, reader.parentVersion);
        } else {
            this.parent = null;
        }
        String groupId = isValid(reader.groupId) ? reader.groupId : reader.parentGroupId;
        this.groupId = requireValid(groupId, "groupId is not valid");
        this.artifactId = requireValid(reader.artifactId, "artifactId is not valid");
        String version = isValid(reader.version) ? reader.version : reader.parentVersion;
        this.version = requireValid(version, "version is not valid");
        this.name = reader.name;
        this.description = reader.description;
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
        ModelReader reader = new ModelReader();
        try {
            SimpleXMLParser.parse(is, reader);
            return new MavenModel(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Get the parent POM definition.
     *
     * @return Parent, or {@code null} if the POM does not have a parent.
     */
    public Parent getParent() {
        return parent;
    }

    /**
     * Get the project groupId.
     *
     * @return groupId, never {@code null}
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Get the project artifactId.
     *
     * @return artifactId, never {@code null}
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Get the project version.
     *
     * @return version, never {@code null}
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the project name.
     *
     * @return name, may be {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Get the project description.
     *
     * @return description, may be {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parent POM definition.
     */
    public static class Parent {

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
        public String getGroupId() {
            return groupId;
        }

        /**
         * Get the parent artifactId.
         *
         * @return artifactId
         */
        public String getArtifactId() {
            return artifactId;
        }

        /**
         * Get the parent version.
         *
         * @return version
         */
        public String getVersion() {
            return version;
        }
    }

    private static final class ModelReader implements SimpleXMLParser.Reader {

        private final LinkedList<String> stack = new LinkedList<>();
        private boolean hasParent;
        private String parentGroupId;
        private String parentArtifactId;
        private String parentVersion;
        private String groupId;
        private String artifactId;
        private String version;
        private String name;
        private String description;
        private int mask = 0;

        @Override
        public boolean keepParsing() {
            return mask != (1 << 8) - 1;
        }

        @Override
        public void startElement(String qName, Map<String, String> attributes) {
            String parentQName = stack.peek();
            if (parentQName == null) {
                if (!"project".equals(qName)) {
                    throw new IllegalStateException("Invalid root element '" + qName + "'");
                }
            }
            stack.push(qName);
        }

        @Override
        public void endElement(String name) {
            if ("parent".equals(name)) {
                mask |= 7;
            }
            stack.pop();
        }

        @Override
        public void elementText(String data) {
            if (stack.size() >= 2) {
                String qName = stack.get(0);
                String parentQName = stack.get(1);
                if ("project".equals(parentQName)) {
                    switch (qName) {
                        case "groupId":
                            groupId = data;
                            mask |= (1 << 3);
                            break;
                        case "artifactId":
                            artifactId = data;
                            mask |= (1 << 4);
                            break;
                        case "version":
                            version = data;
                            mask |= (1 << 5);
                            break;
                        case "name":
                            name = data;
                            mask |= (1 << 6);
                            break;
                        case "description":
                            description = data;
                            mask |= (1 << 7);
                            break;
                        default:
                            // do nothing
                    }
                } else if ("parent".equals(parentQName)) {
                    switch (qName) {
                        case "groupId":
                            hasParent = true;
                            parentGroupId = data;
                            mask |= (1 << 1);
                            break;
                        case "artifactId":
                            hasParent = true;
                            parentArtifactId = data;
                            mask |= (1 << 2);
                            break;
                        case "version":
                            hasParent = true;
                            parentVersion = data;
                            mask |= (1 << 3);
                            break;
                        default:
                            // do nothing
                    }
                }
            }
        }
    }
}
