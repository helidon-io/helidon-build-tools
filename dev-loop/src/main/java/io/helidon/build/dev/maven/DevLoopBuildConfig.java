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
package io.helidon.build.dev.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import io.helidon.build.dev.mode.DevLoop;
import io.helidon.build.util.PathFilters;

import org.apache.maven.plugin.MojoExecutionException;

import static java.util.Collections.emptyList;

/**
 * Configuration beans for the {@link DevLoop} build lifecycle.
 */
public class DevLoopBuildConfig {
    private FullBuildConfig fullBuild;
    private IncrementalBuildConfig incrementalBuild;
    private int maxApplicationFailures;

    /**
     * Constructor.
     */
    public DevLoopBuildConfig() {
        this.fullBuild = new FullBuildConfig();
        this.incrementalBuild = new IncrementalBuildConfig();
        this.maxApplicationFailures = Integer.MAX_VALUE;
    }

    /**
     * Validate the configuration.
     *
     * @throws MojoExecutionException If invalid.
     */
    public void validate() throws MojoExecutionException {
        assertNonNull(fullBuild, "fullBuild required: " + this);
        assertNonNull(incrementalBuild, "incrementalBuild required: " + this);
        fullBuild.validate();
        incrementalBuild.validate();
        if (maxApplicationFailures < 0) {
            throw new MojoExecutionException("maxApplicationFailures cannot be negative: " + this);
        }
    }

    /**
     * Resolve goal references.
     *
     * @param resolver The resolver.
     * @throws Exception If an error occurs.
     */
    public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
        fullBuild.resolve(resolver);
        incrementalBuild.resolve(resolver);
    }

    /**
     * Returns the full build config.
     *
     * @return The config.
     */
    public FullBuildConfig fullBuild() {
        return fullBuild;
    }


    /**
     * Returns the incremental build config.
     *
     * @return The config.
     */
    public IncrementalBuildConfig incrementalBuild() {
        return incrementalBuild;
    }

    /**
     * Returns the maximum number of application failures allowed before the dev loop should exit.
     *
     * @return The maximum.
     */
    public int maxApplicationFailures() {
        return maxApplicationFailures;
    }

    /**
     * Sets the full build config.
     *
     * @param fullBuild The config.
     */
    public void setFullBuild(FullBuildConfig fullBuild) {
        this.fullBuild = fullBuild;
    }

    /**
     * Sets the incremental build config.
     *
     * @param incrementalBuild The config.
     */
    public void setIncrementalBuild(IncrementalBuildConfig incrementalBuild) {
        this.incrementalBuild = incrementalBuild;
    }

    /**
     * Sets the maximum number of application failures allowed before the dev loop should exit.
     *
     * @param maxApplicationFailures The count.
     */
    public void setMaxApplicationFailures(int maxApplicationFailures) {
        this.maxApplicationFailures = maxApplicationFailures;
    }

    @Override
    public String toString() {
        return "devLoop {"
               + "fullBuild=" + fullBuild
               + ", incrementalBuild=" + incrementalBuild
               + ", maxApplicationFailures=" + maxApplicationFailures
               + '}';
    }

    /**
     * Full build configuration.
     */
    public static class FullBuildConfig {
        private static final String DEFAULT_FULL_BUILD_PHASE = "process-classes";

        private String phase;
        private int maxBuildFailures;

        /**
         * Constructor.
         */
        public FullBuildConfig() {
            this.phase = DEFAULT_FULL_BUILD_PHASE;
            this.maxBuildFailures = Integer.MAX_VALUE;
        }

        /**
         * Validate the configuration.
         *
         * @throws MojoExecutionException If invalid.
         */
        public void validate() throws MojoExecutionException {
            if (maxBuildFailures < 0) {
                throw new MojoExecutionException("maxBuildFailures cannot be negative: " + this);
            }
        }

        /**
         * Resolve goal references.
         *
         * @param resolver The resolver.
         * @throws Exception If an error occurs.
         */
        public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
            resolver.assertValidPhase(phase);
        }

        /**
         * Returns the validated full build phase.
         *
         * @return The phase.
         */
        public String phase() {
            return phase;
        }

        /**
         * Returns the maximum number of build failures allowed before the dev loop should exit.
         *
         * @return The maximum.
         */
        public int maxBuildFailures() {
            return maxBuildFailures;
        }

        /**
         * Sets the full build phase.
         *
         * @param phase The phase.
         */
        public void setPhase(String phase) {
            this.phase = phase;
        }

        /**
         * Sets the maximum number of full build failures allowed before the dev loop should exit.
         *
         * @param maxBuildFailures The count.
         */
        public void setMaxBuildFailures(int maxBuildFailures) {
            this.maxBuildFailures = maxBuildFailures;
        }

        @Override
        public String toString() {
            return "fullBuild {"
                   + "phase='" + phase + '\''
                   + ", maxBuildFailures=" + maxBuildFailures
                   + '}';
        }
    }

    /**
     * Incremental build configuration.
     */
    public static class IncrementalBuildConfig {
        private static final List<String> DEFAULT_RESOURCES_GOALS = List.of("resources:resources");
        private static final List<String> DEFAULT_JAVA_SOURCES_GOALS = List.of("compiler:compile");

        private List<String> unresolvedResourceGoals;
        private List<String> unresolvedJavaSourceGoals;
        private List<MavenGoal> resolvedResourceGoals;
        private List<MavenGoal> resolvedJavaSourceGoals;
        private List<CustomDirectoryConfig> customDirectories;
        private int maxBuildFailures;

        /**
         * Constructor.
         */
        public IncrementalBuildConfig() {
            this.unresolvedResourceGoals = DEFAULT_RESOURCES_GOALS;
            this.unresolvedJavaSourceGoals = DEFAULT_JAVA_SOURCES_GOALS;
            this.customDirectories = emptyList();
            this.maxBuildFailures = Integer.MAX_VALUE;
        }

        /**
         * Validate the configuration.
         *
         * @throws MojoExecutionException If invalid.
         */
        public void validate() throws MojoExecutionException {
            if (maxBuildFailures < 0) {
                throw new MojoExecutionException("maxBuildFailures cannot be negative: " + this);
            }
            assertNonNull(unresolvedResourceGoals, "resourceGoals cannot be null: " + this);
            assertNonNull(unresolvedJavaSourceGoals, "javaSourceGoals cannot be null: " + this);
            for (CustomDirectoryConfig custom : customDirectories) {
                custom.validate();
            }
        }

        /**
         * Resolve goal references.
         *
         * @param resolver The resolver.
         * @throws Exception If an error occurs.
         */
        public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
            if (unresolvedResourceGoals.isEmpty()) {
                unresolvedResourceGoals = DEFAULT_RESOURCES_GOALS;
            }
            if (unresolvedJavaSourceGoals.isEmpty()) {
                unresolvedJavaSourceGoals = DEFAULT_JAVA_SOURCES_GOALS;
            }
            resolvedResourceGoals = resolver.resolve(unresolvedResourceGoals, new ArrayList<>());
            resolvedJavaSourceGoals = resolver.resolve(unresolvedJavaSourceGoals, new ArrayList<>());
            for (CustomDirectoryConfig directory : customDirectories()) {
                directory.resolve(resolver);
            }
        }

        /**
         * Returns the resolved resource goals.
         *
         * @return The goals.
         */
        public List<MavenGoal> resourceGoals() {
            return resolvedResourceGoals;
        }

        /**
         * Returns the resolved Java source goals.
         *
         * @return The goals.
         */
        public List<MavenGoal> javaSourceGoals() {
            return resolvedJavaSourceGoals;
        }

        /**
         * Returns the unresolved resource goals.
         *
         * @return The goals.
         */
        public List<String> unresolvedResourceGoals() {
            return unresolvedResourceGoals;
        }

        /**
         * Returns the unresolved Java source goals.
         *
         * @return The goals.
         */
        public List<String> unresolvedJavaSourceGoals() {
            return unresolvedJavaSourceGoals;
        }

        /**
         * Returns the custom directory configurations.
         *
         * @return The configurations.
         */
        public List<CustomDirectoryConfig> customDirectories() {
            return customDirectories;
        }

        /**
         * Returns the maximum number of build failures allowed before the dev loop should exit.
         *
         * @return The maximum.
         */
        public int maxBuildFailures() {
            return maxBuildFailures;
        }

        /**
         * Sets the resource goals.
         *
         * @param resourceGoals The goals.
         */
        public void setResourceGoals(List<String> resourceGoals) {
            this.unresolvedResourceGoals = distinct(resourceGoals);
        }

        /**
         * Sets the Java source goals.
         *
         * @param javaSourceGoals The goals.
         */
        public void setJavaSourceGoals(List<String> javaSourceGoals) {
            this.unresolvedJavaSourceGoals = distinct(javaSourceGoals);
        }

        /**
         * Sets the custom directory configurations.
         *
         * @param customDirectories The configurations.
         */
        public void setCustomDirectories(List<CustomDirectoryConfig> customDirectories) {
            this.customDirectories = distinct(customDirectories);
        }

        /**
         * Sets the maximum number of full build failures allowed before the dev loop should exit.
         *
         * @param maxBuildFailures The count.
         */
        public void setMaxBuildFailures(int maxBuildFailures) {
            this.maxBuildFailures = maxBuildFailures;
        }

        @Override
        public String toString() {
            return "incrementalBuild {"
                   + "resourceGoals=" + unresolvedResourceGoals
                   + ", javaSourceGoals=" + unresolvedJavaSourceGoals
                   + ", customDirectories=" + customDirectories
                   + ", maxBuildFailures=" + maxBuildFailures
                   + '}';
        }

        /**
         * Custom directory configuration.
         */
        public static class CustomDirectoryConfig {
            private Path path;
            private String includes;
            private String excludes;
            private List<String> unresolvedGoals;
            private List<MavenGoal> resolvedGoals;
            private BiPredicate<Path, Path> mappedIncludes;

            /**
             * Constructor.
             */
            public CustomDirectoryConfig() {
                this.includes = "";
                this.excludes = "";
            }

            /**
             * Validate the configuration.
             *
             * @throws MojoExecutionException If invalid.
             */
            public void validate() throws MojoExecutionException {
                assertNonNull(path, "path is required: " + this);
                assertNotEmpty(unresolvedGoals, "one or more goals are required: " + this);
                mappedIncludes = PathFilters.matches(toList(includes), toList(excludes));
                if (mappedIncludes == PathFilters.matchesNone()) {
                    throw new MojoExecutionException("includes + excludes will not match any file: " + this);
                }
            }

            /**
             * Resolve goal references.
             *
             * @param resolver The resolver.
             * @throws Exception If an error occurs.
             */
            public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
                if (path.isAbsolute()) {
                    throw new IllegalArgumentException(path + " must be relative: " + this);
                }
                this.resolvedGoals = resolver.resolve(unresolvedGoals, new ArrayList<>());
            }

            private static List<String> toList(String list) {
                return (list == null || list.isEmpty()) ? emptyList() : Arrays.asList(list.split(","));
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
             * Returns the includes and excludes as a single predicate.
             *
             * @return The predicate.
             */
            public BiPredicate<Path, Path> includes() {
                return mappedIncludes;
            }

            /**
             * Returns the resolved goals.
             *
             * @return The goals.
             */
            public List<MavenGoal> goals() {
                return resolvedGoals;
            }

            /**
             * Returns the unresolved goals.
             *
             * @return The goals.
             */
            public List<String> unresolvedGoals() {
                return unresolvedGoals;
            }

            /**
             * Sets the path.
             *
             * @param path The path.
             */
            public void setPath(String path) {
                this.path = Path.of(path);
            }

            /**
             * Sets the includes.
             *
             * @param includes The includes.
             */
            public void setIncludes(String includes) {
                this.includes = includes;
            }

            /**
             * Sets the excludes.
             *
             * @param excludes The excludes.
             */
            public void setExcludes(String excludes) {
                this.excludes = excludes;
            }

            /**
             * Sets the goals.
             *
             * @param goals The goals.
             */
            public void setGoals(List<String> goals) {
                this.unresolvedGoals = distinct(goals);
            }

            @Override
            public String toString() {
                return "customDirectory {"
                       + "path='" + path + '\''
                       + ", includes='" + includes + '\''
                       + ", excludes='" + excludes + '\''
                       + ", goals=" + unresolvedGoals
                       + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                final CustomDirectoryConfig that = (CustomDirectoryConfig) o;
                return path.equals(that.path)
                       && includes.equals(that.includes)
                       && excludes.equals(that.excludes)
                       && unresolvedGoals.equals(that.unresolvedGoals);
            }

            @Override
            public int hashCode() {
                return Objects.hash(path, includes, excludes, unresolvedGoals);
            }
        }
    }

    private static void assertNonNull(Object object, String errorMessage) throws MojoExecutionException {
        if (object == null) {
            throw new MojoExecutionException(errorMessage);
        }
    }

    private static void assertNotEmpty(List<?> list, String errorMessage) throws MojoExecutionException {
        if (list == null || list.isEmpty()) {
            throw new MojoExecutionException(errorMessage);
        }
    }

    private static <T> List<T> distinct(List<T> list) {
        return list.stream()
                   .distinct()
                   .collect(Collectors.toList());
    }
}
