/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.stager;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Staging action.
 */
interface StagingAction extends StagingElement {

    /**
     * Execute the action.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed when all tasks have been executed
     */
    CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars);

    /**
     * Describe the task.
     *
     * @param dir  stage directory
     * @param vars variables for the current iteration
     * @return String that describes the task
     */
    String describe(Path dir, Map<String, String> vars);

    /**
     * Indicate if this task should be executed after the previous sibling.
     *
     * @return {@code true} if joined, {@code false} otherwise
     */
    default boolean join() {
        return false;
    }

    /**
     * Convert a {@link PlexusConfiguration} instance to a list of {@link StagingAction}.
     *
     * @param configuration plexus configuration
     * @return list of {@link StagingAction}
     */
    static StagingTasks fromConfiguration(PlexusConfiguration configuration) {
        return fromConfiguration(configuration, new StagingElementFactory());
    }

    /**
     * Convert a {@link PlexusConfiguration} instance to a list of {@link StagingAction}.
     *
     * @param configuration plexus configuration
     * @param factory       staging element factory
     * @return list of {@link StagingAction}
     */
    static StagingTasks fromConfiguration(PlexusConfiguration configuration, StagingElementFactory factory) {
        PlexusConfigNode parent = new PlexusConfigNode(configuration, null);
        return new ConfigReader(factory).read(parent);
    }
}
