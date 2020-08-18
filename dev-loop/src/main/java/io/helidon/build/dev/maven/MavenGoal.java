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
 * TODO: Describe
 */
public class MavenGoal {
    private static final String DEFAULT_EXECUTION_ID_PREFIX = "default-";

    private final String name;
    private final String pluginKey;
    private final String executionId;

        /*
            -- Fully Qualified Execution Reference --

               ${groupId}:${artifactId}:${version}:${goal}@${executionId}


            -- Plugin Prefixes ---

            https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html

                By default, Maven will make a guess at the plugin-prefix to be used, by removing any instances of "maven"
                or "plugin" surrounded by hyphens in the plugin's artifact ID.

                However, if you want to customize the prefix used to reference your plugin, you can specify the prefix
                directly through a configuration parameter on the maven-plugin-plugin in your plugin's POM.

            A plugin prefix can be thought of as an alias for a ${groupId}:${artifactId}:${version}, where version is
            specified in the project pom. Plugin prefixes are handy for use on the command line, e.g. the
            maven-compiler-plugin compile goal can be executed like so

                mvn compiler:compile // ${pluginPrefix}:${goal}


            -- Default Execution Ids ---

            http://maven.apache.org/guides/mini/guide-default-execution-ids.html

                Starting in Maven 2.2.0, each mojo invoked directly from the command line will have an execution Id of
                default-cli assigned to it, which will allow the configuration of that execution from the POM by using
                this default execution Id. Likewise, each mojo bound to the build lifecycle via the default lifecycle
                mapping for the specified POM packaging will have an execution Id of default-<goalName> assigned to it,
                to allow configuration of each default mojo execution independently.

            So "default-cli" is used as default when direct execution from command line without execution id, e.g.

                io.helidon.build-tools:helidon-cli-maven-plugin:2.0.3-SNAPSHOT:dev

            is equivalent to

                io.helidon.build-tools:helidon-cli-maven-plugin:2.0.3-SNAPSHOT:dev@default-cli

            and "default-${goalName}" for mojos bound to default lifecycle.


            -- Supported Goal References ---


            exec:exec@compile-sass    // ${pluginPrefix}:${goal}@${executionId}
            compiler:compile          // ${pluginPrefix}:${goal}@${default-compile} see
            compile                   // lifecycle phase

            Versions are NOT supported.

            -- Reference to Plugin Mapping ---

            pluginKey = ${groupId}:${artifactId}


            Q: Given a reference with a plugin prefix, how do we map it to a pluginKey?
            A: See MojoDescriptorCreator.getMojoDescriptor() --> findPluginForPrefix(prefix, session):

               Where MojoDescriptorCreator is injected like so:

                  @Component
                  private MojoDescriptorCreator mojoDescriptorCreator;

            Q: Given a reference to a lifecycle phase, how do we execute it?
            A: See DefaultLifecycleExecutionPlanCalculator.calculateLifecycleMappings ...?

                Map<String, List<MojoExecution>> phaseToMojoMapping =
                    calculateLifecycleMappings( session, project, lifecyclePhase );

               Where the calculator is injected like so:

                   @Component
                   private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

         */

    /**
     * Returns a new instance with a default execution id.
     *
     * @param pluginGroupId The plugin group id.
     * @param pluginArtifactId The plugin artifact id.
     * @param goalName The plugin goal to execute.
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
