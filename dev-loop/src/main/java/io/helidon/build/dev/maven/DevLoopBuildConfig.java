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
import java.util.function.BiPredicate;

import io.helidon.build.dev.mode.DevLoop;
import io.helidon.build.util.PathFilters;

import static java.util.Collections.emptyList;

/**
 * Configuration beans for the {@link DevLoop} build lifecycle.
 */
public class DevLoopBuildConfig {
    private FullBuildConfig fullBuild;
    private IncrementalBuildConfig incrementalBuild;

    /**
     * Constructor.
     */
    public DevLoopBuildConfig() {
        this.fullBuild = new FullBuildConfig();
        this.incrementalBuild = new IncrementalBuildConfig();
    }

    /**
     * Finalize the configuration.
     *
     * @param resolver The resolver.
     * @throws Exception If an error occurs.
     */
    public void finish(MavenGoalReferenceResolver resolver) throws Exception {
        fullBuild.finish(resolver);
        incrementalBuild.finish(resolver);
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

    @Override
    public String toString() {
        return "DevLoopBuildConfig{"
               + "fullBuild=" + fullBuild
               + ", incrementalBuild=" + incrementalBuild
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
         * Finalize the configuration.
         *
         * @param resolver The resolver.
         * @throws Exception If an error occurs.
         */
        public void finish(MavenGoalReferenceResolver resolver) throws Exception {
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
            return "FullBuildConfig{"
                   + "phase='" + phase + '\''
                   + ", maxBuildFailures=" + maxBuildFailures
                   + '}';
        }
    }

    /**
     * Incremental build configuration.
     */
    public static class IncrementalBuildConfig {
        private static final List<String> DEFAULT_RESOURCES_GOAL = List.of("resources:resources");
        private static final List<String> DEFAULT_JAVA_SOURCES_GOAL = List.of("compiler:compile");

        private List<String> resourceGoals;
        private List<String> javaSourceGoals;
        private List<MavenGoal> resolvedResourceGoals;
        private List<MavenGoal> resolvedJavaSourceGoals;
        private List<CustomDirectoryConfig> customDirectories;
        private int maxBuildFailures;

        /**
         * Constructor.
         */
        public IncrementalBuildConfig() {
            this.resourceGoals = DEFAULT_RESOURCES_GOAL;
            this.javaSourceGoals = DEFAULT_JAVA_SOURCES_GOAL;
        }

        /**
         * Finalize the configuration.
         *
         * @param resolver The resolver.
         * @throws Exception If an error occurs.
         */
        public void finish(MavenGoalReferenceResolver resolver) throws Exception {
            if (resourceGoals.isEmpty()) {
                resourceGoals = DEFAULT_RESOURCES_GOAL;
            }
            if (javaSourceGoals.isEmpty()) {
                javaSourceGoals = DEFAULT_JAVA_SOURCES_GOAL;
            }
            resolvedResourceGoals = resolver.resolve(resourceGoals, new ArrayList<>());
            resolvedJavaSourceGoals = resolver.resolve(javaSourceGoals, new ArrayList<>());
            for (CustomDirectoryConfig directory : customDirectories()) {
                directory.finish(resolver);
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
         * Returns the custom directory configurations.
         *
         * @return The configurations.
         */
        public List<CustomDirectoryConfig> customDirectories() {
            return customDirectories == null ? emptyList() : customDirectories;
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
            this.resourceGoals = resourceGoals;
        }

        /**
         * Sets the Java source goals.
         *
         * @param javaSourceGoals The goals.
         */
        public void setJavaSourceGoals(List<String> javaSourceGoals) {
            this.javaSourceGoals = javaSourceGoals;
        }

        /**
         * Sets the custom directory configurations.
         *
         * @param customDirectories The configurations.
         */
        public void setCustomDirectories(List<CustomDirectoryConfig> customDirectories) {
            this.customDirectories = customDirectories;
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
            return "IncrementalBuild{"
                   + "resourceGoals=" + resourceGoals
                   + ", javaSourceGoals=" + javaSourceGoals
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
            private List<String> goals;
            private List<MavenGoal> resolvedGoals;
            private BiPredicate<Path, Path> resolvedIncludes;

            /**
             * Constructor.
             */
            public CustomDirectoryConfig() {
                this.includes = "";
                this.excludes = "";
            }

            /**
             * Finalize the configuration.
             *
             * @param resolver The resolver.
             * @throws Exception If an error occurs.
             */
            public void finish(MavenGoalReferenceResolver resolver) throws Exception {
                if (path.isAbsolute()) {
                    throw new IllegalArgumentException(path + " must be relative");
                }
                this.resolvedGoals = resolver.resolve(goals, new ArrayList<>());
                this.resolvedIncludes = PathFilters.matches(toList(includes), toList(excludes));
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
                return resolvedIncludes;
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
                this.goals = goals;
            }

            @Override
            public String toString() {
                return "CustomDirectory{"
                       + "path='" + path + '\''
                       + ", includes='" + includes + '\''
                       + ", excludes='" + excludes + '\''
                       + ", goals=" + goals
                       + '}';
            }
        }
    }
}
