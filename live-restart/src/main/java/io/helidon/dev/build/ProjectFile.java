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
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import static io.helidon.build.util.FileUtils.assertFile;

/**
 * A project file.
 */
public class ProjectFile {
    private final ProjectDirectory parent;
    private final FileType type;
    private final Path path;
    private final FileTime lastModified;

    /**
     * Returns a new project file.
     *
     * @param parent The parent.
     * @param type The type.
     * @param path The file path.
     * @return The file.
     */
    public static ProjectFile createProjectFile(ProjectDirectory parent, FileType type, Path path) {
        return new ProjectFile(parent, type, path);
    }

    private ProjectFile(ProjectDirectory parent, FileType type, Path path) {
        this.parent = parent;
        this.type = type;
        this.path = assertFile(path);
        this.lastModified = lastModifiedTime();
    }

    /**
     * Returns the parent directory.
     *
     * @return The parent.
     */
    public ProjectDirectory parent() {
        return parent;
    }

    /**
     * Returns the file type.
     *
     * @return The type.
     */
    public FileType type() {
        return type;
    }

    /**
     * Returns the path.
     *
     * @return The path.
     */
    public Path path() {
        return path;
    }

    /**
     * Returns whether or not this file has a different mod time than when this instance was created.
     *
     * @return {@code true} if changed.
     */
    public boolean hasChanged() {
        final FileTime current = lastModifiedTime();
        return !lastModified.equals(current);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProjectFile that = (ProjectFile) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "ProjectFile{" +
               "type=" + type +
               ", path=" + path +
               '}';
    }

    private FileTime lastModifiedTime() {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
