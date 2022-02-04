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
package io.helidon.build.devloop.maven;

import java.util.List;

import io.helidon.build.common.logging.Log;

import org.apache.maven.lifecycle.NoGoalSpecifiedException;

import static java.util.Objects.requireNonNull;

/**
 * Utility to map a Maven goal reference to a {@link MavenGoal}. References are resolved in the context of the specified project.
 * <br><br>
 * References may be fully qualified:
 * <br><br>
 * <pre>
 *    ${groupId}:${artifactId}:${version}:${goal}@${executionId}
 * </pre>
 * The version will be ignored since it can only be resolved to the plugin configured in the current project. If not provided,
 * a <a href="http://maven.apache.org/guides/mini/guide-default-execution-ids.html">default executionId</a> will be used. For
 * example, with the {@code compile} goal the default execution id is {@code default-compile}.
 * <br><br>
 * A <a href="https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html">plugin prefix</a> may
 * be used as an alias for the {@code groupId} and {@code artifactId}, for example {@code compiler} in the following reference:
 * <br><br>
 * <pre>
 *     compiler:compile
 * </pre>
 * Finally, any lifecycle phase (e.g. {@code process-resources}) may be used as a reference, and will expand to the corresponding
 * list of goals.
 * <br><br>
 * <h2>Example References</h2>
 * <ol>
 *     <li>{@code org.apache.maven.plugins:maven-exec-plugin:3.0.0:exec@compile-sass}</li>
 *     <li>{@code org.apache.maven.plugins:maven-exec-plugin:exec@compile-sass}</li>
 *     <li>{@code exec:exec@compile-sass}</li>
 *     <li>{@code compiler:compile}</li>
 *     <li>{@code compile}</li>
 * </ol>
 * References #1-3 are equivalent, #4 executes only the 'compile' goal and #5 executes all goals in the 'compile' lifecycle.
 */
public class MavenGoalReferenceResolver {
    private final MavenEnvironment environment;

    /**
     * Constructor.
     *
     * @param environment The Maven environment.
     */
    public MavenGoalReferenceResolver(MavenEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Resolve a list of references.
     *
     * @param references The references.
     * @param goals The goals list to append to.
     * @return The goals list.
     * @throws Exception If an error occurs.
     */
    public List<MavenGoal> resolve(List<String> references, List<MavenGoal> goals) throws Exception {
        for (String reference : references) {
            resolve(reference, goals);
        }
        return goals;
    }

    /**
     * Resolve a reference.
     *
     * @param reference The reference.
     * @param goals The goals list to append to.
     * @return The goals list.
     * @throws Exception If an error occurs.
     */
    public List<MavenGoal> resolve(String reference, List<MavenGoal> goals) throws Exception {
        int index = requireNonNull(reference).indexOf('@');
        String executionId = null;
        if (index == 0) {
            throw new NoGoalSpecifiedException(reference);
        } else if (index > 0) {
            executionId = reference.substring(index + 1);
            reference = reference.substring(0, index);
        }
        final String[] components = reference.split(":");
        switch (components.length) {
            case 1:
                if (executionId != null) {
                    Log.warn("Ignoring executionId %s in %s", executionId, reference);
                }
                goals.addAll(environment.phase(components[0]));
                break;
            case 2:
                goals.add(environment.goal(components[0], components[1], executionId));
                break;
            case 3:
                goals.add(MavenGoal.create(components[0], components[1], components[2], executionId, environment));
                break;
            default: // >= 4
                Log.warn("Ignoring version in %s", reference);
                goals.add(MavenGoal.create(components[0], components[1], components[3], executionId, environment));
                break;
        }

        Log.debug("%s resolved to %s", reference, goals);
        return goals;
    }

    /**
     * Asserts that the given phase is valid.
     *
     * @param phase The phase.
     * @throws Exception If not a valid phase.
     */
    public void assertValidPhase(String phase) throws Exception {
        environment.lifecycle(phase);
    }
}
