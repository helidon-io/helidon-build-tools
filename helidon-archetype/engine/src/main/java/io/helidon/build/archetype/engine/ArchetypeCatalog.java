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
package io.helidon.build.archetype.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Helidon archetype catalog.
 */
public final class ArchetypeCatalog  {

    /**
     * The current version of the catalog model.
     */
    public static final String MODEL_VERSION = "1.0";

    private final String modelVersion;
    private final String name;
    private final String groupId;
    private final String version;
    private final List<ArchetypeEntry> entries;

    ArchetypeCatalog(String modelVersion, String name, String groupId, String version, List<ArchetypeEntry> entries) {
        this.modelVersion = Objects.requireNonNull(modelVersion, "modelVersion is null");
        this.name = Objects.requireNonNull(name, "id is null");
        this.groupId = Objects.requireNonNull(groupId, "groupId is null");
        this.version = Objects.requireNonNull(version, "version is null");
        this.entries = entries;
    }

    /**
     * Create a archetype descriptor instance from a file.
     *
     * @param file the file
     * @return ArchetypeCatalog
     * @throws IOException on error
     */
    public static ArchetypeCatalog read(Path file) throws IOException {
        return read(Files.newInputStream(file));
    }

    /**
     * Create a archetype descriptor instance from an input stream.
     *
     * @param is input stream
     * @return ArchetypeCatalog
     */
    public static ArchetypeCatalog read(InputStream is) {
        return ArchetypeCatalogReader.read(is);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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

        ArchetypeEntry(String groupId, String artifactId, String version, String name, String title, String summary,
                       String description, List<String> tags) {

            this.name = Objects.requireNonNull(name, "name is null");
            this.groupId = Objects.requireNonNull(groupId, "groupId is null");
            this.artifactId = Objects.requireNonNull(artifactId, "artifactId is null");
            this.version = Objects.requireNonNull(version, "version is null");
            this.title = Objects.requireNonNull(title, "title is null");
            this.summary = Objects.requireNonNull(summary, "summary is null");
            this.description = description;
            this.tags = Objects.requireNonNull(tags, "tags is null");
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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
