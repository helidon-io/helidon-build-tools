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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.helidon.build.util.FileUtils;

/**
 * A continuous build project. New instances must have been successfully built.
 */
public class Project {
    private final ProjectDirectory root;
    private final List<BuildFile> buildSystemFiles;
    private final List<File> classPath;
    private final List<String> compilerFlags;
    private final List<Path> dependencies;
    private final List<BuildComponent> components;

    private Project(Builder builder) {
        this.root = builder.root;
        this.buildSystemFiles = builder.buildSystemFiles;
        this.classPath = builder.classpath.stream().map(Path::toFile).collect(Collectors.toList());
        this.compilerFlags = builder.compilerFlags;
        this.dependencies = builder.dependencies;
        this.components = builder.components;
        components.forEach(c -> c.project(this));
    }

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A {@code Project} builder.
     */
    public static class Builder {
        private static final String JAR_FILE_SUFFIX = ".jar";
        private ProjectDirectory root;
        private List<BuildFile> buildSystemFiles;
        private List<String> compilerFlags;
        private List<Path> dependencies;
        private List<BuildComponent> components;
        private Set<Path> classpath;

        private Builder() {
            this.buildSystemFiles = new ArrayList<>();
            this.compilerFlags = new ArrayList<>();
            this.dependencies = new ArrayList<>();
            this.components = new ArrayList<>();
            this.classpath = new LinkedHashSet<>();
        }

        /**
         * Sets the project root directory.
         *
         * @param rootDirectory Thd directory.
         * @return This instance, for chaining.
         */
        public Builder rootDirectory(ProjectDirectory rootDirectory) {
            this.root = rootDirectory;
            return this;
        }

        /**
         * Add a build system file.
         *
         * @param buildSystemFile The file.
         * @return This instance, for chaining.
         */
        public Builder buildSystemFile(BuildFile buildSystemFile) {
            buildSystemFiles.add(buildSystemFile);
            return this;
        }

        /**
         * Add a compiler flag.
         *
         * @param compilerFlag The flag.
         * @return This instance, for chaining.
         */
        public Builder compilerFlags(String compilerFlag) {
            compilerFlags.add(compilerFlag);
            return this;
        }

        /**
         * Add a component.
         *
         * @param component The component.
         * @return This instance, for chaining.
         */
        public Builder component(BuildComponent component) {
            components.add(component);
            return this;
        }

        /**
         * Add a dependency.
         *
         * @param dependency The dependency.
         * @return This instance, for chaining.
         */
        public Builder dependency(Path dependency) {
            dependencies.add(dependency);
            return this;
        }

        /**
         * Returns a new project.
         *
         * @return The project.
         */
        public Project build() {
            if (root == null) {
                throw new IllegalStateException("rootDirectory required");
            }
            assertNotEmpty(buildSystemFiles, "buildSystemFile");
            assertNotEmpty(dependencies, "dependency");
            assertNotEmpty(components, "component");

            components.forEach(component -> {
                if (component.outputRoot().buildType() == BuildType.JavaClasses) {
                    addToClasspath(component.outputRoot().path());
                }
            });
            dependencies.forEach(this::addToClasspath);
            return new Project(this);
        }

        private void assertNotEmpty(Collection<?> collection, String description) {
            if (collection.isEmpty()) {
                throw new IllegalStateException("At least 1 " + description + " is required");
            }
        }

        private void addToClasspath(Path path) {
            if (!classpath.contains(path)) {
                if (Files.isRegularFile(path)) {
                    classpath.add(path);
                } else if (Files.isDirectory(path)) {
                    classpath.add(path);
                    classpath.addAll(FileUtils.listFiles(path, name -> name.endsWith(JAR_FILE_SUFFIX)));
                }
            }
        }
    }

    /**
     * Returns the root directory.
     *
     * @return The root.
     */
    public ProjectDirectory root() {
        return root;
    }

    /**
     * Returns the build system files (e.g. {@code pom.xml}).
     *
     * @return The files.
     */
    public List<BuildFile> buildSystemFiles() {
        return buildSystemFiles;
    }

    /**
     * Returns the project classpath.
     *
     * @return The classpath.
     */
    public List<File> classpath() {
        return classPath;
    }

    /**
     * Returns the compiler flags.
     *
     * @return The flags.
     */
    public List<String> compilerFlags() {
        return compilerFlags;
    }

    /**
     * Returns a list of paths to all external dependencies. A path may point
     * to a directory, in which case all contained jar files should be considered
     * dependencies.
     *
     * @return The paths.
     */
    public List<Path> dependencies() {
        return dependencies;
    }

    /**
     * Returns all components.
     *
     * @return The components.
     */
    public List<BuildComponent> components() {
        return components;
    }

    /**
     * Returns whether or not any build system file has changed.
     *
     * @return {@code true} if any build system file has changed.
     */
    public boolean haveBuildSystemFilesChanged() {
        for (final BuildFile file : buildSystemFiles()) {
            if (file.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of source changes since the last update, if any.
     *
     * @return The changes.
     */
    protected List<BuildRoot.Changes> sourceChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.sourceRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    /** TODO: This doesn't watch dependencies, and it must. It would be nice not to spend any time collecting
     *        info if this will never be used, however, so... maybe make project modal?
     * Returns a list of binary changes since the last update, if any.
     *
     * @return The changes.
     */
    protected List<BuildRoot.Changes> binaryChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.outputRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    /**
     * Returns whether or not all binaries are newer than all sources and no sources have changed.
     *
     * @return {@code true} if up to date, {@code false} if not.
     */
    public boolean isBuildUpToDate() {
        long latestSource = 0;
        long oldestBinary = 0;
        for (final BuildFile file : buildSystemFiles()) {
            if (file.hasChanged()) {
                return false;
            }
            final long lastModified = file.lastModifiedTime();
            if (lastModified > latestSource) {
                latestSource = lastModified;
            }
        }
        for (BuildComponent component : components()) {
            for (final BuildFile file : component.sourceRoot().list()) {
                if (file.hasChanged()) {
                    return false;
                }
                final long lastModified = file.lastModifiedTime();
                if (lastModified > latestSource) {
                    latestSource = lastModified;
                }
            }
            for (final BuildFile file : component.outputRoot().list()) {
                final long lastModified = file.lastModifiedTime();
                if (lastModified > oldestBinary) {
                    oldestBinary = lastModified;
                    if (oldestBinary < latestSource) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Update the project time stamps.
     */
    public void update() {
        components().forEach(BuildComponent::update);
    }

    /**
     * Perform an incremental build for the given changes.
     *
     * @param changes The changes.
     * @param stdOut A consumer for stdout.
     * @param stdErr A consumer for stderr.
     * @throws Exception on error.
     */
    protected void incrementalBuild(List<BuildRoot.Changes> changes,
                                    Consumer<String> stdOut,
                                    Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            for (final BuildRoot.Changes changed : changes) {
                changed.root().component().incrementalBuild(changed, stdOut, stdErr);
            }
        }
    }
}
