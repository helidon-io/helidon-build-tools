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

import io.helidon.build.util.Log;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import static java.util.Objects.requireNonNull;

/**
 * In process executor for a single maven goal.
 */
public class MavenGoalExecutor {
    private static final String PLUGINS_GROUP = "org.apache.maven.plugins";
    private static final String RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final Goal RESOURCES_GOAL = new Goal("resources", PLUGINS_GROUP, RESOURCES_PLUGIN);
    private static final Goal COMPILE_GOAL = new Goal("compile", PLUGINS_GROUP, COMPILER_PLUGIN);
    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    /**
     * Returns the {@code resources} goal.
     *
     * @return The goal.
     */
    public static Goal resourcesGoal() {
        return RESOURCES_GOAL;
    }

    /**
     * Returns the {@code compile} goal.
     *
     * @return The goal.
     */
    public static Goal compileGoal() {
        return COMPILE_GOAL;
    }

    private final ExecutionEnvironment executionEnvironment;
    private final Goal goal;
    private final Plugin plugin;
    private final Xpp3Dom config;

    private MavenGoalExecutor(Builder builder) {
        this.executionEnvironment = builder.executionEnvironment;
        this.goal = builder.goal;
        this.plugin = executionEnvironment.getMavenProject().getPlugin(goal.pluginKey());
        requireNonNull(plugin, "plugin " + goal.pluginKey() + " not found");
        this.config = configuration();
    }

    private Xpp3Dom configuration() {
        Xpp3Dom result;
        final PluginExecution execution = plugin.getExecutionsAsMap().get(DEFAULT_EXECUTION_ID_PREFIX + goal.name());
        if (execution != null && execution.getConfiguration() != null) {
            result = (Xpp3Dom) execution.getConfiguration();
        } else if (plugin.getConfiguration() != null) {
            result = (Xpp3Dom) plugin.getConfiguration();
        } else {
            result = MojoExecutor.configuration();
        }
        return result;
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
     * Executes the goal.
     *
     * @throws Exception if an error occurs.
     */
    public void execute() throws Exception {
        Log.debug("Executing %s goal", goal.name());
        MojoExecutor.executeMojo(plugin, goal.name(), config, executionEnvironment);
    }

    /**
     * Builder.
     */
    public static class Builder {
        private MavenProject mavenProject;
        private MavenSession mavenSession;
        private BuildPluginManager pluginManager;
        private Goal goal;
        private ExecutionEnvironment executionEnvironment;

        /**
         * Sets the maven project.
         *
         * @param mavenProject The project.
         * @return This instance, for chaining.
         */
        public Builder mavenProject(MavenProject mavenProject) {
            this.mavenProject = mavenProject;
            return this;
        }

        /**
         * Sets the maven session.
         *
         * @param mavenSession The session.
         * @return This instance, for chaining.
         */
        public Builder mavenSession(MavenSession mavenSession) {
            this.mavenSession = mavenSession;
            return this;
        }

        /**
         * Sets the plugin manager.
         *
         * @param pluginManager The manager.
         * @return This instance, for chaining.
         */
        public Builder pluginManager(BuildPluginManager pluginManager) {
            this.pluginManager = pluginManager;
            return this;
        }

        /**
         * Sets the goal.
         *
         * @param goal The goal.
         * @return This instance, for chaining.
         */
        public Builder goal(Goal goal) {
            this.goal = goal;
            return this;
        }

        /**
         * Returns the new build step.
         *
         * @return The build step.
         */
        public MavenGoalExecutor build() {
            requireNonNull(mavenProject, "mavenProject required");
            requireNonNull(mavenSession, "mavenSession required");
            requireNonNull(pluginManager, "pluginManager required");
            requireNonNull(goal, "goal required");
            this.executionEnvironment = MojoExecutor.executionEnvironment(mavenProject, mavenSession, pluginManager);
            return new MavenGoalExecutor(this);
        }
    }

    /**
     * A maven goal descriptor.
     */
    public static class Goal {
        private final String name;
        private final String pluginKey;

        /**
         * Constructor.
         *
         * @param goalName The plugin goal to execute.
         * @param pluginGroupId The plugin group id.
         * @param pluginArtifactId The plugin artifact id.
         */
        private Goal(String goalName,
                     String pluginGroupId,
                     String pluginArtifactId) {
            this.name = goalName;
            this.pluginKey = requireNonNull(pluginGroupId) + ":" + requireNonNull(pluginArtifactId);
        }

        /**
         * Returns the plugin goal name.
         *
         * @return The goal name.
         */
        public String name() {
            return name;
        }

        /**
         * Returns the plugin key.
         *
         * @return The key.
         */
        public String pluginKey() {
            return pluginKey;
        }

        @Override
        public String toString() {
            return "Goal{"
                   + "name='"
                   + name
                   + '\''
                   + ", plugin='"
                   + pluginKey
                   + '\''
                   + '}';
        }
    }
}
