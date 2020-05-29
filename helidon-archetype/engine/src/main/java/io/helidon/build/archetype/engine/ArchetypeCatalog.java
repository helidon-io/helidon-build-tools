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

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Helidon archetype catalog.
 */
public final class ArchetypeCatalog  {

    private final String groupId;
    private final String version;
    private final List<ArchetypeEntry> entries;

    ArchetypeCatalog(String groupId, String version, List<ArchetypeEntry> entries) {
        this.groupId = Objects.requireNonNull(groupId, "groupId is null");
        this.version = Objects.requireNonNull(version, "version is null");
        this.entries = entries;
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
        return groupId.equals(that.groupId)
                && version.equals(that.version)
                && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, version, entries);
    }

    @Override
    public String toString() {
        return "ArchetypeCatalog{"
                + "groupId='" + groupId + '\''
                + ", version='" + version + '\''
                + '}';
    }

    /**
     * Archetype entry.
     */
    public static final class ArchetypeEntry {

        private final String id;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String name;
        private final String description;
        private final List<String> tags;

        ArchetypeEntry(String id, String groupId, String artifactId, String version, String name, String description,
                       List<String> tags) {

            this.id = Objects.requireNonNull(id, "id is null");
            this.groupId = Objects.requireNonNull(groupId, "groupId is null");
            this.artifactId = Objects.requireNonNull(artifactId, "artifactId is null");
            this.version = Objects.requireNonNull(version, "version is null");
            this.name = Objects.requireNonNull(name, "name is null");
            this.description = Objects.requireNonNull(description, "description is null");
            this.tags = Objects.requireNonNull(tags, "tags is null");
        }

        /**
         * Get the archetype id.
         * This is <strong>NOT</strong> a unique ID, multiple archetype may have the same id.
         *
         * @return id, never {@code null}
         */
        public String id() {
            return id;
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
         * @return name, never {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * Get the archetype description.
         *
         * @return description, never {@code null}
         */
        public String description() {
            return description;
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
            return id.equals(that.id)
                    && groupId.equals(that.groupId)
                    && artifactId.equals(that.artifactId)
                    && version.equals(that.version)
                    && name.equals(that.name)
                    && description.equals(that.description)
                    && tags.equals(that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, groupId, artifactId, version, name, description, tags);
        }

        @Override
        public String toString() {
            return "ArchetypeEntry{"
                    + "id='" + id + '\''
                    + "groupId='" + groupId + '\''
                    + ", artifactId='" + artifactId + '\''
                    + ", version='" + version + '\''
                    + ", name='" + name + '\''
                    + ", description='" + description + '\''
                    + '}';
        }
    }
}
