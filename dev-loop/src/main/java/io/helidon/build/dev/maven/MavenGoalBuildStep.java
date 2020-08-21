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

import java.util.function.Consumer;

import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.maven.MavenGoalExecutor.Goal;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;

/**
 * A {@link BuildStep} that executes a single maven goal (in process).
 */
public class MavenGoalBuildStep implements BuildStep {
    private static final BuildGoal RESOURCES_GOAL = new BuildGoal(MavenGoalExecutor.resourcesGoal(), Resources, Resources);
    private static final BuildGoal COMPILE_GOAL = new BuildGoal(MavenGoalExecutor.compileGoal(), JavaSources, JavaClasses);

    /**
     * Returns the {@code resources} goal.
     *
     * @return The goal.
     */
    public static BuildGoal resourcesGoal() {
        return RESOURCES_GOAL;
    }

    /**
     * Returns the {@code compile} goal.
     *
     * @return The goal.
     */
    public static BuildGoal compileGoal() {
        return COMPILE_GOAL;
    }

    private final MavenGoalExecutor executor;
    private final BuildRootType inputType;
    private final BuildRootType outputType;

    private MavenGoalBuildStep(Builder builder) {
        this.executor = builder.executor;
        this.inputType = builder.buildGoal.inputType();
        this.outputType = builder.buildGoal.outputType();
    }

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public BuildRootType inputType() {
        return inputType;
    }

    @Override
    public BuildRootType outputType() {
        return outputType;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes, Consumer<String> stdOut, Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            executor.execute();
        }
    }

    /**
     * A builder.
     */
    public static class Builder {
        private final  MavenGoalExecutor.Builder executorBuilder;
        private MavenGoalExecutor executor;
        private BuildGoal buildGoal;

        Builder() {
            this.executorBuilder = MavenGoalExecutor.builder();
        }

        /**
         * Sets the maven project.
         *
         * @param mavenProject The project.
         * @return This instance, for chaining.
         */
        public Builder mavenProject(MavenProject mavenProject) {
            executorBuilder.mavenProject(mavenProject);
            return this;
        }

        /**
         * Sets the maven session.
         *
         * @param mavenSession The session.
         * @return This instance, for chaining.
         */
        public Builder mavenSession(MavenSession mavenSession) {
            executorBuilder.mavenSession(mavenSession);
            return this;
        }

        /**
         * Sets the plugin manager.
         *
         * @param pluginManager The manager.
         * @return This instance, for chaining.
         */
        public Builder pluginManager(BuildPluginManager pluginManager) {
            executorBuilder.pluginManager(pluginManager);
            return this;
        }

        /**
         * Sets the build goal.
         *
         * @param buildGoal The goal.
         * @return This instance, for chaining.
         */
        public Builder goal(BuildGoal buildGoal) {
            this.buildGoal = buildGoal;
            executorBuilder.goal(buildGoal.goal());
            return this;
        }

        /**
         * Returns the new build step.
         *
         * @return The build step.
         */
        public BuildStep build() {
            executor = executorBuilder.build();
            return new MavenGoalBuildStep(this);
        }
    }

    /**
     * A build goal.
     */
    public static class BuildGoal {
        private final Goal goal;
        private final BuildRootType inputType;
        private final BuildRootType outputType;

        /**
         * Constructor.
         *
         * @param goal The goal to execute.
         * @param inputType The input type.
         * @param outputType The output type.
         */
        private BuildGoal(Goal goal,
                          BuildRootType inputType,
                          BuildRootType outputType) {
            this.goal = goal;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        /**
         * Returns the goal.
         *
         * @return The goal.
         */
        public Goal goal() {
            return goal;
        }

        /**
         * Returns the input type to which this step will apply.
         *
         * @return The type.
         */
        public BuildRootType inputType() {
            return JavaSources;
        }

        /**
         * Returns the output type that this step will produce.
         *
         * @return The type.
         */
        public BuildRootType outputType() {
            return JavaClasses;
        }
    }
}
