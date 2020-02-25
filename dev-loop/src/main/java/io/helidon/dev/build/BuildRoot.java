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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.helidon.dev.build.BuildFile.createBuildFile;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A project directory that tracks file changes.
 */
public class BuildRoot extends ProjectDirectory implements Iterable<BuildFile> {
    private final BuildRootType type;
    private final FileType fileType;
    private final AtomicReference<Map<Path, BuildFile>> files;
    private final AtomicReference<BuildComponent> component;

    /**
     * Constructor.
     *
     * @param type The type.
     * @param directory The directory path.
     */
    BuildRoot(BuildRootType type, Path directory) {
        super(requireNonNull(type).directoryType(), requireNonNull(directory));
        this.type = type;
        this.fileType = type.fileType();
        this.files = new AtomicReference<>(collectFiles());
        this.component = new AtomicReference<>();
    }

    /**
     * Returns a new project directory.
     *
     * @param type The type.
     * @param path The directory path.
     */
    public static BuildRoot createBuildRoot(BuildRootType type, Path path) {
        return new BuildRoot(type, path);
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
    public BuildRootType buildType() {
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
     * Returns the stream of files.
     *
     * @return The stream.
     */
    public Stream<BuildFile> stream() {
        return list().stream();
    }

    /**
     * Returns the first file whose path matches the given filter.
     *
     * @param filter The filter.
     * @return The build file.
     * @throws NoSuchElementException If not found.
     */
    public BuildFile findFirst(Predicate<Path> filter) {
        return stream().filter(file -> filter.test(file.path()))
                       .findFirst()
                       .orElseThrow(() -> new NoSuchElementException("No match found in " + path()));
    }

    /**
     * Returns the first file whose file name matches the given filter.
     *
     * @param filter The filter.
     * @return The build file.
     * @throws NoSuchElementException If not found.
     */
    public BuildFile findFirstNamed(Predicate<String> filter) {
        return findFirst(path -> filter.test(path.getFileName().toString()));
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Iterator<BuildFile> iterator() {
        return list().iterator();
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

        /**
         * Returns the added or modified files.
         *
         * @return The files.
         */
        public Set<Path> addedOrModified() {
            final Set<Path> result = new HashSet<>(added.size() + modified.size());
            result.addAll(added());
            result.addAll(modified());
            return result;
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
               "directoryType=" + directoryType() +
               ", fileType=" + fileType +
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
