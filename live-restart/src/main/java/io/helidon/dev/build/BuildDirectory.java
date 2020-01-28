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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class BuildDirectory extends ProjectDirectory {
    private final List<FileType> fileTypes;
    private final AtomicReference<Map<Path, BuildFile>> files;

    /**
     * Constructor.
     *
     * @param type The directory type.
     * @param directory The directory path.
     * @param fileTypes The file types to include when listing files.
     */
    BuildDirectory(DirectoryType type, Path directory, List<FileType> fileTypes) {
        super(type, directory);
        if (requireNonNull(fileTypes).isEmpty()) {
            throw new IllegalArgumentException("Must have at least 1 file type");
        }
        this.fileTypes = fileTypes;
        this.files = new AtomicReference<>(collectFiles());
    }

    /**
     * Returns a new project directory.
     *
     * @param type The directory type.
     * @param path The directory path.
     * @param fileTypes The file types to include when listing files.
     */
    public static BuildDirectory createBuildDirectory(DirectoryType type, Path path, FileType... fileTypes) {
        return new BuildDirectory(type, path, Arrays.asList(fileTypes));
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
        private final Set<Path> added;
        private final Set<Path> modified;
        private final Set<Path> removed;

        private Changes(Set<Path> initialFiles) {
            this.added = new HashSet<>();
            this.modified = new HashSet<>();
            this.removed = new HashSet<>(initialFiles);
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
            final Changes changes = new Changes(files.get().keySet());
            final Map<Path, BuildFile> files = this.files.get();
            Files.walk(path())
                 .forEach(file -> {
                     for (final FileType type : fileTypes) {
                         if (type.test(file)) {
                             changes.update(file, files.get(file));
                             break;
                         }
                     }
                 });
            return changes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Updates and returns the files list.
     *
     * @return The updated files.
     */
    public Collection<BuildFile> update() {
        files.set(collectFiles());
        return list();
    }

    @Override
    public String toString() {
        return "BuildDirectory{" +
               "type=" + type() +
               ", path=" + path() +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildDirectory)) return false;
        if (!super.equals(o)) return false;
        final BuildDirectory that = (BuildDirectory) o;
        return Objects.equals(fileTypes, that.fileTypes) &&
               Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fileTypes, files);
    }

    private Map<Path, BuildFile> collectFiles() {
        final Map<Path, BuildFile> files = new HashMap<>();
        if (!fileTypes.isEmpty()) {
            try {
                Files.walk(path())
                     .forEach(file -> {
                         for (final FileType type : fileTypes) {
                             if (type.test(file)) {
                                 files.put(file, createBuildFile(this, type, file));
                                 break;
                             }
                         }
                     });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return unmodifiableMap(files);
    }
}
