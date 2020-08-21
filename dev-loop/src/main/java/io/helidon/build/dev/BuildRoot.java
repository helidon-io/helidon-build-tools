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

package io.helidon.build.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static io.helidon.build.util.FileUtils.newerThan;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A project directory that tracks file changes.
 */
public class BuildRoot extends ProjectDirectory implements Iterable<BuildFile> {
    private final BuildRootType type;
    private final BiPredicate<Path, Path> fileType;
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
     * @return The build root.
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
    public static class Changes implements FileChangeAware {
        private final BuildRoot root;
        private final Set<Path> added;
        private final Set<Path> modified;
        private final Set<Path> removed;
        private FileTime changedTime;

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

        @Override
        public Optional<FileTime> changedTime() {
            return Optional.of(changedTime);
        }

        private void update(Path file, BuildFile existing) {
            FileTime lastModified = null;
            if (existing == null) {

                // We didn't know about this one last time, so it is an addition

                lastModified = lastModifiedTime(file);
                added.add(file);

            } else {

                // We knew about it last time, so take it out of our removed list

                removed.remove(file);

                // Has it changed?

                final Optional<FileTime> changedTime = existing.changedTime();
                if (changedTime.isPresent()) {

                    // Yes, so it is modified

                    lastModified = changedTime.get();
                    modified.add(file);
                }
            }

            // Keep the most recent changed time

            if (lastModified != null && newerThan(lastModified, changedTime)) {
                changedTime = lastModified;
            }
        }
    }

    /**
     * Tests for any changed files.
     *
     * @return The changes.
     */
    public Changes changes() {
        final Changes changes = new Changes(this, files.get().keySet());
        final Map<Path, BuildFile> files = this.files.get();
        final Path root = path();
        try (Stream<Path> stream = Files.walk(path())) {
            stream.forEach(file -> {
                if (fileType.test(file, root)) {
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
        return "BuildRoot{"
               + "directoryType=" + directoryType()
               + ", fileType=" + fileType
               + ", path=" + path()
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildRoot)) return false;
        if (!super.equals(o)) return false;
        final BuildRoot that = (BuildRoot) o;
        return Objects.equals(fileType, that.fileType)
               && Objects.equals(files, that.files);
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
        final Path root = path();
        try (Stream<Path> stream = Files.walk(path())) {
            stream.forEach(file -> {
                if (fileType.test(file, root)) {
                    files.put(file, createBuildFile(this, fileType, file));
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return unmodifiableMap(files);
    }
}
