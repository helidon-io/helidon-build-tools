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
    private static final String DEFAULT_FULL_BUILD_PHASE = "process-classes";

    private String fullBuildPhase;
    private int maxBuildFailures;
    private IncrementalBuildConfig incrementalBuild;

    /**
     * Constructor.
     */
    public DevLoopBuildConfig() {
        this.fullBuildPhase = DEFAULT_FULL_BUILD_PHASE;
        this.maxBuildFailures = Integer.MAX_VALUE;
        this.incrementalBuild = new IncrementalBuildConfig();
    }

    /**
     * Resolves the Maven goals.
     *
     * @param resolver The resolver.
     * @throws Exception If an error occurs.
     */
    public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
        resolver.assertValidPhase(fullBuildPhase);
        incrementalBuild.resolve(resolver);
    }

    /**
     * Returns the validated full build phase.
     *
     * @return The phase.
     */
    public String fullBuildPhase() {
        return fullBuildPhase;
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
     * Returns the incremental build config.
     *
     * @return The config.
     */
    public IncrementalBuildConfig incrementalBuild() {
        return incrementalBuild;
    }

    /**
     * Sets the full build phase.
     *
     * @param fullBuildPhase The phase.
     */
    public void setFullBuildPhase(String fullBuildPhase) {
        this.fullBuildPhase = fullBuildPhase;
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
     * Sets the maximum number of build failures allowed before the dev loop should exit.
     *
     * @param maxBuildFailures The count.
     */
    public void setMaxBuildFailures(int maxBuildFailures) {
        this.maxBuildFailures = maxBuildFailures;
    }

    @Override
    public String toString() {
        return "DevLoopBuildConfig{"
               + "fullBuildPhase=" + fullBuildPhase
               + ", maxBuildFailures=" + maxBuildFailures
               + ", incrementalBuild=" + incrementalBuild
               + '}';
    }

    /**
     * Incremental build configuration.
     */
    public static class IncrementalBuildConfig {
        private static final String DEFAULT_RESOURCES_GOAL = "resources:resources";
        private static final String DEFAULT_JAVA_SOURCES_GOAL = "compiler:compile";

        private List<String> resourceGoals;
        private List<String> javaSourceGoals;
        private List<MavenGoal> resolvedResourceGoals;
        private List<MavenGoal> resolvedJavaSourceGoals;
        private List<CustomDirectoryConfig> customDirectories;

        /**
         * Constructor.
         */
        public IncrementalBuildConfig() {
            this.resourceGoals = List.of(DEFAULT_RESOURCES_GOAL);
            this.javaSourceGoals = List.of(DEFAULT_JAVA_SOURCES_GOAL);
        }

        /**
         * Resolves the Maven goals.
         *
         * @param resolver The resolver.
         * @throws Exception If an error occurs.
         */
        public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
            resolvedResourceGoals = resolver.resolve(resourceGoals, new ArrayList<>());
            resolvedJavaSourceGoals = resolver.resolve(javaSourceGoals, new ArrayList<>());
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
         * Returns the custom directory configurations.
         *
         * @return The configurations.
         */
        public List<CustomDirectoryConfig> customDirectories() {
            return customDirectories == null ? emptyList() : customDirectories;
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

        @Override
        public String toString() {
            return "IncrementalBuild{"
                   + "resourceGoals=" + resourceGoals
                   + ", javaSourceGoals=" + javaSourceGoals
                   + ", customDirectories=" + customDirectories
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
             * Resolves the Maven goals.
             *
             * @param resolver The resolver.
             * @throws Exception If an error occurs.
             */
            public void resolve(MavenGoalReferenceResolver resolver) throws Exception {
                if (path.isAbsolute()) {
                    throw new IllegalArgumentException(path + " must be relative");
                }
                this.resolvedGoals = resolver.resolve(goals, new ArrayList<>());
                this.resolvedIncludes = PathFilters.matches(toList(includes), toList(excludes));
            }

            private static List<String> toList(String list) {
                return list == null ? emptyList() : Arrays.asList(list.split(","));
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
