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
import io.helidon.build.dev.BuildStep;
import io.helidon.build.util.Log;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import static java.util.Objects.requireNonNull;

/**
 * An executable maven goal. Executions occur in process, in the context of the current project environment.
 */
public class MavenGoal implements BuildStep {
    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    private final String name;
    private final String goal;
    private final String pluginKey;
    private final String executionId;
    private final ExecutionEnvironment environment;
    private final Plugin plugin;
    private final Xpp3Dom config;

    /**
     * Returns a new instance.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @param environment The plugin execution environment.
     * @return The goal.
     */
    public static MavenGoal create(String pluginGroupId,
                                   String pluginArtifactId,
                                   String goalName,
                                   String executionId,
                                   ExecutionEnvironment environment) {
        return new MavenGoal(pluginGroupId, pluginArtifactId, goalName, executionId, environment);
    }

    /**
     * Constructor.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @param environment The plugin execution environment.
     */
    private MavenGoal(String pluginGroupId,
                      String pluginArtifactId,
                      String goalName,
                      String executionId,
                      ExecutionEnvironment environment) {
        this.name = requireNonNull(goalName);
        this.goal = goalName + (executionId == null ? "" : ("#" + executionId));
        this.pluginKey = requireNonNull(pluginGroupId) + ":" + requireNonNull(pluginArtifactId);
        this.executionId = executionId == null ? DEFAULT_EXECUTION_ID_PREFIX + goalName : executionId;
        this.environment = environment;
        this.plugin = environment.getMavenProject().getPlugin(pluginKey);
        requireNonNull(plugin, "plugin " + pluginKey + " not found");

        // Lookup configuration or create default

        final PluginExecution execution = plugin.getExecutionsAsMap().get(executionId);
        if (execution != null && execution.getConfiguration() != null) {
            this.config = (Xpp3Dom) execution.getConfiguration();
        } else if (plugin.getConfiguration() != null) {
            this.config = (Xpp3Dom) plugin.getConfiguration();
        } else {
            this.config = MojoExecutor.configuration();
        }
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            execute();
        }
    }

    /**
     * Executes the goal.
     *
     * @throws Exception if an error occurs.
     */
    public void execute() throws Exception {
        Log.debug("Executing %s", this);
        MojoExecutor.executeMojo(plugin, goal, config, environment);
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

    /**
     * Returns the plugin key.
     *
     * @return The key.
     */
    public String executionId() {
        return executionId;
    }

    @Override
    public String toString() {
        return pluginKey() + ":" + name() + "@" + executionId();
    }
}
