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

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.build.dev.util.ConsumerPrintStream;
import io.helidon.build.util.Log;

import static io.helidon.build.dev.DirectoryType.Depencencies;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.FileUtils.listFiles;
import static java.util.Objects.requireNonNull;

/**
 * A continuous build project. New instances must have been successfully built.
 */
public class Project {
    private static final String JAR_FILE_SUFFIX = ".jar";
    private final ProjectDirectory root;
    private final List<BuildFile> buildSystemFiles;
    private final List<File> classPath;
    private final List<String> compilerFlags;
    private final List<Path> dependencyPaths;
    private final List<BuildFile> dependencies;
    private final List<BuildComponent> components;
    private final String mainClassName;
    private final Map<Path, ProjectDirectory> parents;

    private Project(Builder builder) {
        this.root = builder.root;
        this.buildSystemFiles = builder.buildSystemFiles;
        this.classPath = new ArrayList<>();
        this.compilerFlags = builder.compilerFlags;
        this.dependencyPaths = builder.dependencyPaths;
        this.dependencies = builder.dependencies;
        this.components = builder.components;
        this.mainClassName = builder.mainClassName;
        this.parents = new HashMap<>();
        components.forEach(c -> c.project(this));
        updateDependencies();
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
        private ProjectDirectory root;
        private List<BuildFile> buildSystemFiles;
        private List<String> compilerFlags;
        private List<Path> dependencyPaths;
        private List<BuildFile> dependencies;
        private List<BuildComponent> components;
        private String mainClassName;

        private Builder() {
            this.buildSystemFiles = new ArrayList<>();
            this.compilerFlags = new ArrayList<>();
            this.dependencyPaths = new ArrayList<>();
            this.dependencies = new ArrayList<>();
            this.components = new ArrayList<>();
        }

        /**
         * Sets the project root directory.
         *
         * @param rootDirectory Thd directory.
         * @return This instance, for chaining.
         */
        public Builder rootDirectory(ProjectDirectory rootDirectory) {
            this.root = requireNonNull(rootDirectory);
            return this;
        }

        /**
         * Add a build system file.
         *
         * @param buildSystemFile The file.
         * @return This instance, for chaining.
         */
        public Builder buildSystemFile(BuildFile buildSystemFile) {
            buildSystemFiles.add(requireNonNull(buildSystemFile));
            return this;
        }

        /**
         * Add a compiler flag.
         *
         * @param compilerFlag The flag.
         * @return This instance, for chaining.
         */
        public Builder compilerFlags(String compilerFlag) {
            compilerFlags.add(requireNonNull(compilerFlag));
            return this;
        }

        /**
         * Add a component.
         *
         * @param component The component.
         * @return This instance, for chaining.
         */
        public Builder component(BuildComponent component) {
            components.add(requireNonNull(component));
            return this;
        }

        /**
         * Add a dependency.
         *
         * @param dependency The dependency.
         * @return This instance, for chaining.
         */
        public Builder dependency(Path dependency) {
            dependencyPaths.add(requireNonNull(dependency));
            return this;
        }

        /**
         * Sets the main class name.
         *
         * @param mainClassName The name.
         * @return This instance, for chaining.
         */
        public Builder mainClassName(String mainClassName) {
            this.mainClassName = requireNonNull(mainClassName);
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
            if (mainClassName == null) {
                throw new IllegalStateException("mainClassName required");
            }
            assertNotEmpty(buildSystemFiles, "buildSystemFile");
            assertNotEmpty(dependencyPaths, "dependency");
            assertNotEmpty(components, "component");
            return new Project(this);
        }

        private void assertNotEmpty(Collection<?> collection, String description) {
            if (collection.isEmpty()) {
                throw new IllegalStateException("At least 1 " + description + " is required");
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
     * Returns a list of all external dependencies.
     *
     * @return The paths.
     */
    public List<BuildFile> dependencies() {
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
     * Returns the main class name.
     *
     * @return The name.
     */
    public String mainClassName() {
        return mainClassName;
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
    public List<BuildRoot.Changes> sourceChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.sourceRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    /**
     * Returns whether or not any binaries have changed.
     *
     * @return The changes.
     */
    public boolean hasBinaryChanges() {
        for (final BuildComponent component : components()) {
            final BuildRoot.Changes changes = component.outputRoot().changes();
            if (!changes.isEmpty()) {
                return true;
            }
        }
        for (BuildFile dependency : dependencies()) {
            if (dependency.hasChanged()) {
                return true;
            }
        }
        return false;
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
        if (oldestBinary == 0) {
            return false;   // Not yet built.
        }
        for (final BuildFile file : dependencies()) {
            final long lastModified = file.lastModifiedTime();
            if (lastModified > oldestBinary) {
                oldestBinary = lastModified;
                if (oldestBinary < latestSource) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Update the project time stamps.
     *
     * @param updateDepenencies {@code true} if dependencies should be updated.
     */
    public void update(boolean updateDepenencies) {
        components().forEach(BuildComponent::update);
        if (updateDepenencies) {
            updateDependencies();
        }
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
            final long startTime = System.currentTimeMillis();
            final PrintStream origOut = System.out;
            final PrintStream origErr = System.err;
            try {
                System.setOut(ConsumerPrintStream.newStream(stdOut));
                System.setErr(ConsumerPrintStream.newStream(stdErr));
                for (final BuildRoot.Changes changed : changes) {
                    changed.root().component().incrementalBuild(changed, stdOut, stdErr);
                }
            } finally {
                System.setOut(origOut);
                System.setErr(origErr);
            }
            final long elapsedTime = System.currentTimeMillis() - startTime;
            final float elapsedSeconds = elapsedTime / 1000F;
            Log.info("Build completed in %.1f seconds", elapsedSeconds);
        }
    }

    private void updateDependencies() {

        // Build/rebuild dependencies

        dependencies.clear();
        dependencyPaths.forEach(this::addDependency);

        // Build/rebuild classPath, weeding out any duplicates

        final Set<Path> paths = new LinkedHashSet<>();
        components.forEach(component -> {
            if (component.outputRoot().buildType() == BuildRootType.JavaClasses) {
                paths.add(component.outputRoot().path());
            }
        });
        dependencies.forEach(dependency -> paths.add(dependency.path()));

        classPath.clear();
        paths.forEach(path -> classPath.add(path.toFile()));
    }

    private void addDependency(Path path) {
        if (FileType.Jar.test(path)) {
            dependencies.add(toJar(path));
        } else if (Files.isDirectory(path)) {
            for (Path file : listFiles(path, name -> name.endsWith(JAR_FILE_SUFFIX))) {
                addDependency(file);
            }
        }
    }

    private BuildFile toJar(Path path) {
        final Path parent = path.getParent();
        final ProjectDirectory parentDir = parents.computeIfAbsent(parent, p -> createProjectDirectory(Depencencies, parent));
        return BuildFile.createBuildFile(parentDir, FileType.Jar, path);
    }
}
