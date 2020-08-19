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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.helidon.build.util.Log;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.NoGoalSpecifiedException;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import static java.util.Objects.requireNonNull;

/**
 * Utility to map a maven goal reference to a {@link MavenGoalExecutor.Goal}.
 */
public class MavenGoalReferenceResolver {
    private final MavenProject project;
    private final MavenSession session;
    private final MojoDescriptorCreator mojoDescriptorCreator;
    private final DefaultLifecycles defaultLifeCycles;
    private final LifecycleMappingDelegate standardDelegate;
    private final Map<String, LifecycleMappingDelegate> delegates;

    /**
     * Constructor.
     *
     * @param project The project.
     * @param session The session.
     * @param mojoDescriptorCreator Used to resolve plugin prefixes.
     * @param defaultLifeCycles Used to map a phase to a list of goals.
     * @param standardDelegate Used to map a phase to a list of goals.
     * @param delegates Used to map a phase to a list of goals.
     */
    public MavenGoalReferenceResolver(MavenProject project,
                                      MavenSession session,
                                      MojoDescriptorCreator mojoDescriptorCreator,
                                      DefaultLifecycles defaultLifeCycles,
                                      LifecycleMappingDelegate standardDelegate,
                                      Map<String, LifecycleMappingDelegate> delegates) {
        this.project = project;
        this.session = session;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.defaultLifeCycles = defaultLifeCycles;
        this.standardDelegate = standardDelegate;
        this.delegates = delegates;
    }

    public List<MavenGoal> resolve(String reference) throws Exception {
        final List<MavenGoal> goals = new ArrayList<>();
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
                addPhaseGoals(components[0], goals);
                break;
            case 2:
                addPrefixGoal(components[0], components[1], executionId, goals);
                break;
            case 3:
                goals.add(MavenGoal.create(components[0], components[1], components[2], executionId));
                break;
            default: // >= 4
                Log.warn("Ignoring version in %s", reference);
                goals.add(MavenGoal.create(components[0], components[1], components[3], executionId));
                break;
        }

        Log.info("%s resolved to %s", reference, goals); // TODO: change to debug

        return goals;
    }

    private void addPrefixGoal(String prefix, String goal, String executionId, List<MavenGoal> goals) throws Exception {
        final Plugin plugin = mojoDescriptorCreator.findPluginForPrefix(prefix, session);
        goals.add(MavenGoal.create(plugin.getGroupId(), plugin.getArtifactId(), goal, executionId));
    }

    private void addPhaseGoals(String phase, List<MavenGoal> goals) throws Exception {
        // NOTE: This method was mostly copied from DefaultLifecycleExecutionPlanCalculator.calculateLifecycleMappings
        Lifecycle lifecycle = defaultLifeCycles.get(phase);
        if (lifecycle == null) {
            throw new LifecyclePhaseNotFoundException("Unknown lifecycle phase \"" + phase
                                                      + "\". You must specify a valid lifecycle phase" + " or a goal in the "
                                                      + "format <plugin-prefix>:<goal> or "
                                                      + "<plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. "
                                                      + "Available lifecycle phases are: "
                                                      + defaultLifeCycles.getLifecyclePhaseList() + ".", phase);
        }
        LifecycleMappingDelegate delegate = standardDelegate;
        if (Arrays.binarySearch(DefaultLifecycles.STANDARD_LIFECYCLES, lifecycle.getId()) < 0) {
            delegate = delegates.get(lifecycle.getId());
            if (delegate == null) {
                delegate = standardDelegate;
            }
        }

        delegate.calculateLifecycleMappings(session, project, lifecycle, phase)
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> e.getValue().forEach(execution -> goals.add(toGoal(execution))));
    }

    private static MavenGoal toGoal(MojoExecution execution) {
        return MavenGoal.create(execution.getGroupId(), execution.getArtifactId(),
                                execution.getGoal(), execution.getExecutionId());
    }
}
