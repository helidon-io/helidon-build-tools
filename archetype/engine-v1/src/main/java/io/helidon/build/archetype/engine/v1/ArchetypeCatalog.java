/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.helidon.build.common.Lists;
import io.helidon.build.common.xml.XMLElement;

/**
 * Helidon archetype catalog.
 */
public final class ArchetypeCatalog {

    /**
     * The current version of the catalog model.
     */
    public static final String MODEL_VERSION = "1.0";

    private final String modelVersion;
    private final String name;
    private final String groupId;
    private final String version;
    private final List<ArchetypeEntry> entries;

    ArchetypeCatalog(XMLElement elt) {
        if (!"archetype-catalog".equals(elt.name())) {
            throw new IllegalArgumentException("Invalid root element: %s" + elt.name());
        }
        this.modelVersion = elt.attribute("modelVersion");
        this.name = elt.attribute("name");
        this.groupId = elt.attribute("groupId");
        this.version = elt.attribute("version");
        this.entries = Lists.map(elt.children("archetype"), it -> new ArchetypeEntry(it, groupId, version));
    }

    /**
     * Create an archetype descriptor instance from a file.
     *
     * @param file the file
     * @return ArchetypeCatalog
     * @throws IOException on error
     */
    public static ArchetypeCatalog read(Path file) throws IOException {
        return read(Files.newInputStream(file));
    }

    /**
     * Create an archetype descriptor instance from an input stream.
     *
     * @param is input stream
     * @return ArchetypeCatalog
     */
    public static ArchetypeCatalog read(InputStream is) {
        try {
            XMLElement element = XMLElement.parse(is);
            return new ArchetypeCatalog(element);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the catalog model version.
     *
     * @return model version, never {@code null}
     */
    public String modelVersion() {
        return modelVersion;
    }

    /**
     * Get the catalog name.
     * The name is informative only.
     *
     * @return name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Get the catalog groupId.
     *
     * @return groupId, never {@code null}
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Get the catalog version.
     *
     * @return version, never {@code null}
     */
    public String version() {
        return version;
    }

    /**
     * Get the catalog entries.
     *
     * @return entries, never {@code null}
     */
    public List<ArchetypeEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArchetypeCatalog that = (ArchetypeCatalog) o;
        return modelVersion.equals(that.modelVersion)
               && name.equals(that.name)
               && groupId.equals(that.groupId)
               && version.equals(that.version)
               && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelVersion, name, groupId, version, entries);
    }

    @Override
    public String toString() {
        return "ArchetypeCatalog{"
               + "id='" + name + '\''
               + ", groupId='" + groupId + '\''
               + ", version='" + version + '\''
               + ", modelVersion='" + modelVersion + '\''
               + '}';
    }

    /**
     * Archetype entry.
     */
    public static final class ArchetypeEntry {

        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String name;
        private final String title;
        private final String summary;
        private final String description;
        private final List<String> tags;

        ArchetypeEntry(XMLElement elt, String defaultGroupId, String defaultVersion) {
            groupId = elt.attribute("groupId", defaultGroupId);
            artifactId = elt.attribute("artifactId");
            version = elt.attribute("version", defaultVersion);
            name = elt.attribute("name");
            title = elt.attribute("title");
            summary = elt.attribute("summary");
            description = elt.attribute("description", null);
            tags = elt.attributeList("tags", ",");
        }

        /**
         * Get the archetype groupId.
         *
         * @return groupId, never {@code null}
         */
        public String groupId() {
            return groupId;
        }

        /**
         * Get the archetype artifactId.
         *
         * @return artifactId, never {@code null}
         */
        public String artifactId() {
            return artifactId;
        }

        /**
         * Get the archetype version.
         *
         * @return version, never {@code null}
         */
        public String version() {
            return version;
        }

        /**
         * Get the archetype name.
         *
         * @return id, never {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * Get the archetype name.
         *
         * @return name, never {@code null}
         */
        public String title() {
            return title;
        }

        /**
         * Get the archetype summary.
         *
         * @return summary, never {@code null}
         */
        public String summary() {
            return summary;
        }

        /**
         * Get the archetype description.
         *
         * @return optional, never {@code null}
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Get the archetype tags.
         *
         * @return tags, never {@code null}
         */
        public List<String> tags() {
            return tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArchetypeEntry that = (ArchetypeEntry) o;
            return groupId.equals(that.groupId)
                   && artifactId.equals(that.artifactId)
                   && version.equals(that.version)
                   && name.equals(that.name)
                   && title.equals(that.title)
                   && Objects.equals(summary, that.summary)
                   && description.equals(that.description)
                   && tags.equals(that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, version, name, title, summary, description, tags);
        }

        @Override
        public String toString() {
            return "ArchetypeEntry{"
                   + "groupId='" + groupId + '\''
                   + ", artifactId='" + artifactId + '\''
                   + ", version='" + version + '\''
                   + ", name='" + name + '\''
                   + ", title='" + title + '\''
                   + ", summary='" + summary + '\''
                   + ", description='" + description + '\''
                   + '}';
        }
    }
}
