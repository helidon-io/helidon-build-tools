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
import java.util.Set;

import io.helidon.build.util.FileUtils;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.dev.build.ProjectFile.createProjectFile;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A project directory.
 */
public class ProjectDirectory {
    private final DirectoryType type;
    private final Path directory;
    private final List<FileType> fileTypes;
    private volatile Map<Path, ProjectFile> files;
    private volatile boolean hasDeletions;

    /**
     * Constructor.
     *
     * @param type The directory type.
     * @param directory The directory path.
     * @param fileTypes The file types to include when listing files.
     */
    ProjectDirectory(DirectoryType type, Path directory, List<FileType> fileTypes) {
        this.type = type;
        this.directory = assertDir(directory);
        this.fileTypes = requireNonNull(fileTypes);
        this.files = collectFiles();
    }

    /**
     * Returns a new project directory.
     *
     * @param type The directory type.
     * @param path The directory path.
     * @param fileTypes The file types to include when listing files.
     */
    public static ProjectDirectory createProjectDirectory(DirectoryType type, Path path, FileType... fileTypes) {
        return new ProjectDirectory(type, path, Arrays.asList(fileTypes));
    }

    /**
     * Returns the directory type.
     *
     * @return The type.
     */
    public DirectoryType type() {
        return type;
    }

    /**
     * Returns the directory path.
     *
     * @return The path.
     */
    public Path path() {
        return directory;
    }

    /**
     * Delete the contents of this directory.
     *
     * @throws UncheckedIOException if an error occurs.
     */
    public void clean() {
        try {
            FileUtils.deleteDirectoryContent(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete this directory and all its contents.
     *
     * @throws UncheckedIOException if an error occurs.
     */
    public void delete() {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether or not this directory is empty.
     *
     * @return {@code true} if empty.
     */
    public boolean isEmpty() {
        if (Files.isDirectory(directory)) {
            final String[] paths = directory.toFile().list();
            return paths == null || paths.length == 0;
        } else {
            return true;
        }
    }

    /**
     * Returns the list of files.
     *
     * @return The list.
     */
    public Collection<ProjectFile> list() {
        return files.values();
    }

    /**
     * Tests for any changed files.
     *
     * @return The (possibly empty) set of changed files or {@code null} if none. An empty set
     * will be returned when the only changes were deletions.
     */
    public Set<Path> changes() {
        try {
            final Set<Path> removed = new HashSet<>(files.keySet());
            final Set<Path> addedOrModified = new HashSet<>();
            Files.walk(directory)
                 .forEach(file -> {
                     for (final FileType type : fileTypes) {
                         if (type.test(file)) {
                             final ProjectFile existing = files.get(file);
                             if (existing == null) {
                                 // New
                                 addedOrModified.add(file);
                             } else {
                                 // Existing
                                 removed.remove(file);
                                 if (existing.hasChanged()) {
                                     // Modified
                                     addedOrModified.add(file);
                                 }
                             }
                             break;
                         }
                     }
                 });
            if (removed.isEmpty() && addedOrModified.isEmpty()) {
                return null;
            } else {
                return addedOrModified;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Updates and returns the files list.
     *
     * @return The updated files.
     */
    public Collection<ProjectFile> update() {
        files = collectFiles();
        return list();
    }

    @Override
    public String toString() {
        return "ProjectDirectory{" +
               "type=" + type +
               ", path=" + directory +
               '}';
    }

    private Map<Path, ProjectFile> collectFiles() {
        final Map<Path, ProjectFile> files = new HashMap<>();
        if (!fileTypes.isEmpty()) {
            try {
                Files.walk(directory)
                     .forEach(file -> {
                         for (final FileType type : fileTypes) {
                             if (type.test(file)) {
                                 files.put(file, createProjectFile(this, type, file));
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
