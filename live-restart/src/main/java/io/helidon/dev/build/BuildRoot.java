/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.helidon.dev.build.BuildFile.createBuildFile;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A project directory that tracks file changes.
 */
public class BuildRoot extends ProjectDirectory {
    private final BuildType type;
    private final FileType fileType;
    private final AtomicReference<Map<Path, BuildFile>> files;
    private final AtomicReference<BuildComponent> component;

    /**
     * Constructor.
     *
     * @param buildType The type.
     * @param directory The directory path.
     */
    BuildRoot(BuildType buildType, Path directory) {
        super(requireNonNull(buildType).directoryType(), requireNonNull(directory));
        this.type = buildType;
        this.fileType = buildType.fileType();
        this.files = new AtomicReference<>(collectFiles());
        this.component = new AtomicReference<>();
    }

    /**
     * Returns a new project directory.
     *
     * @param buildType The type.
     * @param path The directory path.
     */
    public static BuildRoot createBuildRoot(BuildType buildType, Path path) {
        return new BuildRoot(buildType, path);
    }

    /**
     * Returns the build component containing this root.
     *
     * @return The component.
     */
    public BuildComponent component() {
        return requireNonNull(component.get());
    }

    /**
     * Returns the build type.
     *
     * @return The type.
     */
    public BuildType buildType() {
        return type;
    }

    /**
     * Returns the list of files.
     *
     * @return The list.
     */
    public Collection<BuildFile> list() {
        return files.get().values();
    }

    /**
     * Directory changes.
     */
    public static class Changes {
        private final BuildRoot root;
        private final Set<Path> added;
        private final Set<Path> modified;
        private final Set<Path> removed;

        private Changes(BuildRoot root, Set<Path> initialFiles) {
            this.root = root;
            this.added = new HashSet<>();
            this.modified = new HashSet<>();
            this.removed = new HashSet<>(initialFiles);
        }

        /**
         * Returns the build root containing these changes.
         *
         * @return The root.
         */
        public BuildRoot root() {
            return root;
        }

        /**
         * Returns {@code true} if no changes occurred.
         *
         * @return {@code true} if no changes occurred.
         */
        public boolean isEmpty() {
            return size() == 0;
        }

        /**
         * Returns the number of changes.
         *
         * @return The number.
         */
        public int size() {
            return added.size() + modified.size() + removed.size();
        }

        /**
         * Returns the added files.
         *
         * @return The files.
         */
        public Set<Path> added() {
            return added;
        }

        /**
         * Returns the modified files.
         *
         * @return The files.
         */
        public Set<Path> modified() {
            return modified;
        }

        /**
         * Returns the removed files.
         *
         * @return The files.
         */
        public Set<Path> removed() {
            return removed;
        }

        private void update(Path file, BuildFile existing) {
            if (existing == null) {
                added.add(file);
            } else {
                removed.remove(file);
                if (existing.hasChanged()) {
                    modified.add(file);
                }
            }
        }
    }

    /**
     * Tests for any changed files.
     *
     * @return The changes.
     */
    public Changes changes() {
        try {
            final Changes changes = new Changes(this, files.get().keySet());
            final Map<Path, BuildFile> files = this.files.get();
            Files.walk(path())
                 .forEach(file -> {
                     if (fileType.test(file)) {
                         changes.update(file, files.get(file));
                     }
                 });
            return changes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Updates the files list.
     */
    public void update() {
        files.set(collectFiles());
    }

    @Override
    public String toString() {
        return "BuildRoot{" +
               "type=" + directoryType() +
               ", path=" + path() +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildRoot)) return false;
        if (!super.equals(o)) return false;
        final BuildRoot that = (BuildRoot) o;
        return Objects.equals(fileType, that.fileType) &&
               Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fileType, files);
    }

    BuildRoot component(BuildComponent component) {
        this.component.set(component);
        return this;
    }

    private Map<Path, BuildFile> collectFiles() {
        final Map<Path, BuildFile> files = new HashMap<>();
        try {
            Files.walk(path())
                 .forEach(file -> {
                     if (fileType.test(file)) {
                         files.put(file, createBuildFile(this, fileType, file));
                     }
                 });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return unmodifiableMap(files);
    }
}
