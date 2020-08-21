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
import io.helidon.build.util.PathPredicates;

import static java.util.Collections.emptyList;

/**
 * Configuration beans for the {@link DevLoop} build lifecycle.
 * <p></p>
 * Example pom declaration:
 * <pre>
 *     &lt;profiles>
 *         &lt;profile>
 *             &lt;id>helidon-cli&lt;/id>
 *             &lt;activation>
 *                 &lt;property>
 *                     &lt;name>helidon.cli&lt;/name>
 *                     &lt;value>true&lt;/value>
 *                 &lt;/property>
 *             &lt;/activation>
 *             &lt;build>
 *                 &lt;plugins>
 *                     &lt;plugin>
 *                         &lt;groupId>io.helidon.build-tools&lt;/groupId>
 *                         &lt;artifactId>helidon-cli-maven-plugin&lt;/artifactId>
 *                         &lt;extensions>true&lt;/extensions>
 *                         &lt;executions>
 *                             &lt;execution>
 *                                 &lt;id>default-cli&lt;/id> &lt;!-- must use this id! -->
 *                                 &lt;goals>
 *                                     &lt;goal>dev&lt;/goal>
 *                                 &lt;/goals>
 *                                 &lt;configuration>
 *
 *                                    &lt;!-- NOTE: changes to this configuration will NOT be noticed during execution of
 *                                    the helidon dev command -->
 *
 *                                    &lt;devLoop>
 *                                        &lt;!-- Phase used when a full build is required; defaults to process-classes -->
 *                                         &lt;fullBuildPhase>process-test-classes&lt;/fullBuildPhase>
 *                                         &lt;incrementalBuild>
 *
 *                                             &lt;!-- directories/includes/excludes from maven-resources-plugin config -->
 *                                             &lt;resourceGoals>
 *                                                 &lt;goal>resources:copy&lt;/goal>
 *                                             &lt;/resourceGoals>
 *
 *                                             &lt;!-- directories/includes/excludes from maven-compiler-plugin config -->
 *                                             &lt;javaSourceGoals>
 *                                                 &lt;goal>process-my-sources&lt;/goal>
 *                                             &lt;/javaSourceGoals>
 *
 *                                             &lt;customDirectories>
 *                                                 &lt;directory>
 *                                                     &lt;path>src/etc1&lt;/path>
 *                                                     &lt;includes>**&#47;*.foo,**&#47;*.bar&lt;/includes>
 *                                                     &lt;excludes />
 *                                                     &lt;goals>
 *                                                         &lt;goal>my-custom-goal-1&lt;/goal>
 *                                                         &lt;goal>my-custom-goal-2&lt;/goal>
 *                                                     &lt;/goals>
 *                                                 &lt;/directory>
 *                                                 &lt;directory>
 *                                                     &lt;path>src/etc2&lt;/path>
 *                                                     &lt;includes>**&#47;*.bar&lt;/includes>
 *                                                     &lt;excludes>**&#47;*.foo&lt;/includes>
 *                                                     &lt;goals>
 *                                                         &lt;goal>my-custom-goal-X&lt;/goal>
 *                                                     &lt;/goals>
 *                                                 &lt;/directory>
 *                                             &lt;/customDirectories>
 *                                         &lt;/incrementalBuild>
 *                                     &lt;/devLoop>
 *                                 &lt;/configuration>
 *                             &lt;/execution>
 *                         &lt;/executions>
 *                     &lt;/plugin>
 *                 &lt;/plugins>
 *             &lt;/build>
 *         &lt;/profile>
 *     &lt;/profiles>
 * </pre>
 */
public class DevLoopBuildConfig {
    private static final String DEFAULT_FULL_BUILD_PHASE = "process-classes";

    private String fullBuildPhase;
    private IncrementalBuild incrementalBuild;

    /**
     * Constructor.
     */
    public DevLoopBuildConfig() {
        this.fullBuildPhase = DEFAULT_FULL_BUILD_PHASE;
        this.incrementalBuild = new IncrementalBuild();
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
     * Returns the incremental build config.
     *
     * @return The config.
     */
    public IncrementalBuild incrementalBuild() {
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
    public void setIncrementalBuild(IncrementalBuild incrementalBuild) {
        this.incrementalBuild = incrementalBuild;
    }

    @Override
    public String toString() {
        return "DevLoopBuildConfig{"
               + "fullBuildPhase=" + fullBuildPhase
               + ", incrementalBuild=" + incrementalBuild
               + '}';
    }

    /**
     * Incremental build configuration.
     */
    public static class IncrementalBuild {
        private static final String DEFAULT_RESOURCES_GOAL = "resources:resources";
        private static final String DEFAULT_JAVA_SOURCES_GOAL = "compiler:compile";

        private List<String> resourceGoals;
        private List<String> javaSourceGoals;
        private List<MavenGoal> resolvedResourceGoals;
        private List<MavenGoal> resolvedJavaSourceGoals;
        private List<CustomDirectory> customDirectories;

        /**
         * Constructor.
         */
        public IncrementalBuild() {
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
            this.resolvedResourceGoals = resolver.resolve(resourceGoals, new ArrayList<>());
            this.resolvedJavaSourceGoals = resolver.resolve(javaSourceGoals, new ArrayList<>());
            for (CustomDirectory directory : customDirectories()) {
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
        public List<CustomDirectory> customDirectories() {
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
        public void setCustomDirectories(List<CustomDirectory> customDirectories) {
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
        public static class CustomDirectory {
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
                this.resolvedIncludes = PathPredicates.matches(toList(includes), toList(excludes));
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
