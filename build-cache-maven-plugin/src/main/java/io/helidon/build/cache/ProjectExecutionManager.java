/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.DefaultLifecycleMappingDelegate;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Project execution manager.
 */
@Component(role = ProjectExecutionManager.class, hint = "default")
public class ProjectExecutionManager {

    private final Map<MavenProject, List<ExecutionEntry>> recordedExecutions = new HashMap<>();
    private final Map<MavenProject, ProjectExecutionPlan> executionPlans = new HashMap<>();

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement(hint = DefaultLifecycleMappingDelegate.HINT)
    private LifecycleMappingDelegate standardLifecycleDelegate;

    @Requirement
    private Map<String, LifecycleMappingDelegate> lifecycleDelegates;

    @Requirement
    private ConfigResolver configResolver;

    @Requirement
    private Logger logger;

    /**
     * Process the executions for the given project.
     *
     * @param session     Maven session
     * @param project     Maven project
     * @param stateStatus project state status
     */
    public void processExecutions(MavenSession session, MavenProject project, ProjectStateStatus stateStatus) {
        if (stateStatus.code() == ProjectStateStatus.STATE_FILES_CHANGED
                || stateStatus.code() == ProjectStateStatus.STATE_INVALID_DOWNSTREAM) {
            executionPlans.put(project, new ProjectExecutionPlan(stateStatus, List.of()));
            return;
        }

        // resolve the executions in the current life-cycle
        List<ExecutionEntry> execs = resolveExecutions(session, project);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                    "[%s:%s] - executions in the current life-cycle: \n  %s",
                    project.getGroupId(),
                    project.getArtifactId(),
                    execs.stream()
                            .map(ExecutionEntry::name)
                            .collect(Collectors.joining("\n  "))));
        }

        ProjectExecutionPlan execPlan = new ProjectExecutionPlan(stateStatus, execs);
        executionPlans.put(project, execPlan);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(
                    "[%s:%s] - execution statuses:\n  %s",
                    project.getGroupId(),
                    project.getArtifactId(),
                    execPlan.executionStatuses().stream()
                            .map(s -> {
                                String str = s.toString();
                                if (s.isDiff()) {
                                    str += "\n" + s.diffs().stream()
                                            .map(d -> "             +- " + d.asString())
                                            .collect(Collectors.joining());
                                }
                                return str;
                            })
                            .collect(Collectors.joining("\n  "))));
        }

        // remove the recorded executions
        project.getBuild().getPlugins()
                .forEach(plugin -> removePluginExecutions(plugin, execPlan.cachedExecutions()));
    }

    /**
     * Get the execution plan for a given project.
     *
     * @param project Maven project
     * @return ProjectExecutionPlan or {@code null} if not found
     */
    public ProjectExecutionPlan plan(MavenProject project) {
        return executionPlans.get(project);
    }

    /**
     * Record the given execution.
     *
     * @param execution execution to record
     * @param session   Maven session
     * @param project   Maven project
     */
    public void recordExecution(MojoExecution execution, MavenSession session, MavenProject project) {
        ExecutionEntry executionRecord = ExecutionEntry.create(execution,
                configResolver.resolve(execution, session, project));

        // do not record executions from the clean phase or executions issued from the cli
        if (!isCleanExecution(session, project, executionRecord)
                && !MojoExecution.Source.CLI.equals(execution.getSource())) {
            recordedExecutions.computeIfAbsent(project, p -> new LinkedList<>())
                    .add(executionRecord);
        }
    }

    /**
     * Get the recorded executions for a given project.
     *
     * @param project Maven project
     * @return list of RecordedExecution
     */
    public List<ExecutionEntry> recordedExecutions(MavenProject project) {
        return recordedExecutions.getOrDefault(project, List.of());
    }

    private List<ExecutionEntry> resolveExecutions(MavenSession session, MavenProject project) {
        CacheConfig cacheConfig = CacheConfig.of(project, session);
        List<String> includes = cacheConfig.executionsIncludes();
        List<String> excludes = cacheConfig.executionsExcludes();
        return session.getGoals()
                .stream()
                .filter(phase -> !phase.equals("clean"))
                .flatMap(phase -> resolvePhase(session, project, phase).stream())
                .map(exec -> ExecutionEntry.create(exec, configResolver.resolve(exec, session, project)))
                .filter(exec -> exec.match(includes, excludes))
                .collect(Collectors.toList());
    }

    private List<MojoExecution> resolvePhase(MavenSession session, MavenProject project, String phase) {
        Lifecycle lifecycle = defaultLifeCycles.get(phase);
        LifecycleMappingDelegate lifecycleDelegate = null;
        if (Arrays.binarySearch(DefaultLifecycles.STANDARD_LIFECYCLES, lifecycle.getId()) < 0) {
            lifecycleDelegate = lifecycleDelegates.get(lifecycle.getId());
        }
        if (lifecycleDelegate == null) {
            lifecycleDelegate = standardLifecycleDelegate;
        }
        try {
            return lifecycleDelegate.calculateLifecycleMappings(session, project, lifecycle, phase)
                    .entrySet()
                    .stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
        } catch (PluginNotFoundException
                | PluginResolutionException
                | PluginDescriptorParsingException
                | MojoNotFoundException
                | InvalidPluginDescriptorException e) {
            logger.warn("Unable to resolve mojos for phase: " + phase, e);
            return List.of();
        }
    }

    private boolean isCleanExecution(MavenSession session, MavenProject project, ExecutionEntry execution) {
        return resolvePhase(session, project, "clean")
                .stream()
                .map(e -> ExecutionEntry.create(e, null))
                .anyMatch(e -> e.matches(execution));
    }

    private void removePluginExecutions(Plugin plugin, List<ExecutionEntry> executions) {
        // remove the goals
        plugin.getExecutions().forEach(pluginExec -> {
            Iterator<String> it = pluginExec.getGoals().iterator();
            while (it.hasNext()) {
                String goal = it.next();
                if (executions.stream().anyMatch(e -> e.matches(plugin, goal, pluginExec.getId()))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping: " + plugin.getId() + "@" + pluginExec.getId());
                    }
                    it.remove();
                }
            }
        });

        // remove the executions without goals
        plugin.getExecutions().removeIf(pluginExecution -> pluginExecution.getGoals().isEmpty());
    }
}
