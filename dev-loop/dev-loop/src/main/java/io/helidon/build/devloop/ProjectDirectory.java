/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import io.helidon.build.common.FileUtils;

import static io.helidon.build.common.FileUtils.requireDirectory;
import static java.util.Objects.requireNonNull;

/**
 * A project directory.
 */
public class ProjectDirectory {
    private final DirectoryType type;
    private final Path directory;

    /**
     * Constructor.
     *
     * @param type The directory type.
     * @param directory The directory path.
     */
    ProjectDirectory(DirectoryType type, Path directory) {
        this.type = requireNonNull(type);
        this.directory = requireDirectory(directory);
    }

    /**
     * Returns a new project directory.
     *
     * @param type The directory type.
     * @param path The directory path.
     * @return The project directory.
     */
    public static ProjectDirectory createProjectDirectory(DirectoryType type, Path path) {
        return new ProjectDirectory(type, path);
    }

    /**
     * Returns the directory type.
     *
     * @return The type.
     */
    public DirectoryType directoryType() {
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
        FileUtils.deleteDirectory(directory);
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

    @Override
    public String toString() {
        return "ProjectDirectory{"
                + "type=" + type
                + ", path=" + directory
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectDirectory)) return false;
        final ProjectDirectory that = (ProjectDirectory) o;
        return type == that.type
                && Objects.equals(directory, that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, directory);
    }
}
