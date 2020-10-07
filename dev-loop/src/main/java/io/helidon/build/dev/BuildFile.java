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

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

import io.helidon.build.util.FileUtils;

import static io.helidon.build.util.FileUtils.assertFile;
import static java.util.Objects.requireNonNull;

/**
 * A project build file that can detect modification.
 */
public class BuildFile implements FileChangeAware {
    private final ProjectDirectory parent;
    private final Path path;
    private volatile FileTime lastModified;

    /**
     * Returns a new build file.
     *
     * @param parent The parent.
     * @param path The file path.
     * @return The file.
     */
    public static BuildFile createBuildFile(ProjectDirectory parent, Path path) {
        return new BuildFile(parent, path);
    }

    private BuildFile(ProjectDirectory parent, Path path) {
        this.parent = requireNonNull(parent);
        this.path = assertFile(path);
        this.lastModified = FileUtils.lastModifiedTime(path);
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
     * Returns the path.
     *
     * @return The path.
     */
    public Path path() {
        return path;
    }

    @Override
    public Optional<FileTime> changedTime() {
        return FileUtils.newerThan(path, lastModified);
    }

    /**
     * Tests whether or not this file has a modified time that is more recent than the base time.
     *
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is newer.
     */
    public Optional<FileTime> changedTimeIfNewerThan(FileTime baseTime) {
        return FileUtils.newerThan(path, baseTime);
    }

    /**
     * Tests whether or not this file has a modified time that is more recent than the base time.
     *
     * @param baseTime The base time. May be {@code null}.
     * @return {@code true} if base time is {@code null} or change time is older.
     */
    public Optional<FileTime> changedTimeIfOlderThan(FileTime baseTime) {
        return FileUtils.olderThan(path, baseTime);
    }

    /**
     * Returns the last modified time.
     *
     * @return The last modified time.
     */
    public FileTime lastModifiedTime() {
        return lastModified;
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
        lastModified = FileUtils.lastModifiedTime(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "BuildFile{"
               + "path=" + path
               + '}';
    }
}
