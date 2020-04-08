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

import java.util.Map;
import java.util.function.Consumer;

import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.maven.MavenGoalExecutor.Goal;
import io.helidon.build.util.Log;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import static io.helidon.build.dev.BuildRootType.JavaClasses;
import static io.helidon.build.dev.BuildRootType.JavaSources;
import static io.helidon.build.dev.BuildRootType.Resources;
import static java.util.Objects.requireNonNull;

/**
 * A {@link BuildStep} that executes a single maven goal (in process).
 */
public class MavenGoalBuildStep implements BuildStep {

    /**
     * The {@code resources} goal.
     */
    public static BuildGoal RESOURCES_GOAL = new BuildGoal(MavenGoalExecutor.RESOURCES_GOAL, Resources, Resources);

    /**
     * The {@code compile} goal.
     */
    public static BuildGoal COMPILE_GOAL = new BuildGoal(MavenGoalExecutor.COMPILE_GOAL, JavaSources, JavaClasses);

    private final MavenGoalExecutor executor;
    private final BuildRootType inputType;
    private final BuildRootType outputType;

    private MavenGoalBuildStep(Builder builder) {
        this.executor = builder.executor;
        this.inputType = builder.inputType;
        this.outputType = builder.outputType;
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
        private MavenGoalExecutor.Builder executorBuilder;
        private MavenGoalExecutor executor;
        private BuildRootType inputType;
        private BuildRootType outputType;

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
         * Sets the goal.
         *
         * @param goal The goal.
         * @return This instance, for chaining.
         */
        public Builder goal(Goal goal) {
            executorBuilder.goal(goal);
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
