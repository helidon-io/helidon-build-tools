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
import java.util.Objects;
import java.util.function.Predicate;

import static io.helidon.build.util.FileUtils.assertFile;
import static java.util.Objects.requireNonNull;

/**
 * A project build file that can detect modification.
 */
public class BuildFile {
    private final ProjectDirectory parent;
    private final FileType type;
    private final Path path;
    private volatile FileTime lastModified;

    /**
     * Returns a new build file.
     *
     * @param parent The parent.
     * @param type The type.
     * @param path The file path.
     * @return The file.
     */
    public static BuildFile createBuildFile(ProjectDirectory parent, FileType type, Path path) {
        return new BuildFile(parent, type, path);
    }

    private BuildFile(ProjectDirectory parent, FileType type, Path path) {
        this.parent = requireNonNull(parent);
        this.type = requireNonNull(type);
        this.path = assertFile(path);
        this.lastModified = getLastModifiedTime();
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
     * Tests the given predicate against the path.
     *
     * @param test The test.
     * @return {@code true} if the path matches.
     */
    public boolean matches(Predicate<Path> test) {
        return test.test(path());
    }

    /**
     * Tests the given predicate against the file name.
     *
     * @param test The test.
     * @return {@code true} if the path matches.
     */
    public boolean matchesFileName(Predicate<String> test) {
        return test.test(path().getFileName().toString());
    }

    /**
     * Returns whether or not this file has a different mod time than when this instance was created.
     *
     * @return {@code true} if changed.
     */
    public boolean hasChanged() {
        final FileTime current = getLastModifiedTime();
        return !lastModified.equals(current);
    }

    /**
     * Returns the last modified time.
     *
     * @return The last modified time.
     */
    public long lastModifiedTime() {
        return lastModified.toMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BuildFile that = (BuildFile) o;
        return Objects.equals(path, that.path);
    }

    /**
     * Updates the last modified time.
     */
    public void update() {
        lastModified = getLastModifiedTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "BuildFile{"
                + "type=" + type
                + ", path=" + path
                + '}';
    }

    private FileTime getLastModifiedTime() {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
