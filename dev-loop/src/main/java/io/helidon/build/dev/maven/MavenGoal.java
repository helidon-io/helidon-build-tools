/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.util.ConsolePrinter;
import io.helidon.build.util.Log;

import org.apache.maven.plugin.MojoExecution;

import static java.util.Objects.requireNonNull;

/**
 * An executable maven goal. Executions occur in process, in the context of the current project environment.
 */
public class MavenGoal implements BuildStep {

    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    private final String name;
    private final String pluginKey;
    private final String executionId;
    private final MojoExecution execution;
    private final MavenEnvironment environment;

    /**
     * Returns a new instance.
     *
     * @param pluginGroupId    The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName         The plugin goal to execute.
     * @param executionId      The execution id.
     * @param environment      The plugin execution environment.
     * @return The goal.
     */
    public static MavenGoal create(String pluginGroupId,
                                   String pluginArtifactId,
                                   String goalName,
                                   String executionId,
                                   MavenEnvironment environment) {

        return new MavenGoal(requireNonNull(pluginGroupId),
                requireNonNull(pluginArtifactId),
                requireNonNull(goalName),
                executionId == null ? DEFAULT_EXECUTION_ID_PREFIX + goalName : executionId,
                requireNonNull(environment));
    }

    /**
     * Constructor.
     *
     * @param pluginGroupId    The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName         The plugin goal to execute.
     * @param executionId      The execution id.
     * @param environment      The plugin execution environment.
     */
    private MavenGoal(String pluginGroupId,
                      String pluginArtifactId,
                      String goalName,
                      String executionId,
                      MavenEnvironment environment) {

        this.name = goalName;
        this.pluginKey = pluginGroupId + ":" + pluginArtifactId;
        this.executionId = executionId;
        this.execution = environment.execution(pluginKey, goalName, executionId);
        this.environment = environment;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 ConsolePrinter stdOut,
                                 ConsolePrinter stdErr) throws Exception {

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
        environment.execute(execution);
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
