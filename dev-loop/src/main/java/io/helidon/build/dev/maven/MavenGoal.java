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

import static java.util.Objects.requireNonNull;

/**
 * An executable maven goal.
 */
public class MavenGoal {
    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    private final String name;
    private final String pluginKey;
    private final String executionId;

    /**
     * Returns a new instance with a default execution id.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @return The goal.
     */
    public static MavenGoal create(String pluginGroupId, String pluginArtifactId, String goalName) {
        return create(pluginGroupId, pluginArtifactId, goalName, null);
    }

    /**
     * Returns a new instance.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     * @param executionId The execution id.
     * @return The goal.
     */
    public static MavenGoal create(String pluginGroupId,
                                   String pluginArtifactId,
                                   String goalName,
                                   String executionId) {
        return new MavenGoal(pluginGroupId, pluginArtifactId, goalName, executionId);
    }

    /**
     * Constructor.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
     */
    private MavenGoal(String pluginGroupId,
                      String pluginArtifactId,
                      String goalName,
                      String executionId) {
        this.name = requireNonNull(goalName);
        this.pluginKey = requireNonNull(pluginGroupId) + ":" + requireNonNull(pluginArtifactId);
        this.executionId = executionId == null ? DEFAULT_EXECUTION_ID_PREFIX + goalName : executionId;
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
