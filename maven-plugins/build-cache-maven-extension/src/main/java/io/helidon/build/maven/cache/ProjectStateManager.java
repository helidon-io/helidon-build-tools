/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.xml.XMLException;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

/**
 * Project state manager.
 */
@Named
@SessionScoped
public class ProjectStateManager {

    @Inject
    private ProjectExecutionManager executionManager;

    @Inject
    private CacheConfigManager configManager;

    @Inject
    private MavenSession session;

    @Inject
    private Logger logger;

    private final LazyValue<Integer> loaded = new LazyValue<>(this::initLoaded);
    private final LazyValue<Map<MavenProject, ProjectStateStatus>> states = new LazyValue<>(this::initStates);

    /**
     * Get the count of loaded states.
     *
     * @return count
     */
    public int loaded() {
        return loaded.get();
    }

    /**
     * Get all the project states.
     *
     * @return map of state statuses by project
     */
    public Map<MavenProject, ProjectStateStatus> states() {
        return states.get();
    }

    private int initLoaded() {
        long count = states().values().stream().filter(s -> s.code() == ProjectStateStatus.STATE_VALID).count();
        return Math.toIntExact(count);
    }

    private Map<MavenProject, ProjectStateStatus> initStates() {
        if (session.getGoals().contains("clean")) {
            return Map.of();
        }
        ProjectDependencyGraph pdg = session.getProjectDependencyGraph();
        Map<MavenProject, ProjectStateStatus> statusMap = new HashMap<>();
        Deque<MavenProject> stack = new ArrayDeque<>(session.getProjects());
        while (!stack.isEmpty()) {
            MavenProject project = stack.pop();
            ProjectStateStatus stateStatus = statusMap.computeIfAbsent(project, this::processState);
            if (stateStatus.code() == ProjectStateStatus.STATE_VALID) {
                for (MavenProject upstream : pdg.getUpstreamProjects(project, true)) {
                    ProjectStateStatus uss = statusMap.computeIfAbsent(upstream, this::processState);
                    if (uss.code() != ProjectStateStatus.STATE_VALID) {
                        statusMap.put(project, stateStatus.invalidate());
                        for (MavenProject downstream : pdg.getDownstreamProjects(project, true)) {
                            if (!stack.contains(downstream)) {
                                stack.addLast(downstream);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return statusMap;
    }

    /**
     * Merge and save the state for a given project.
     *
     * @param project Maven project
     */
    public void save(MavenProject project) {
        if (configManager.cacheConfig().record()) {
            try {
                ProjectState projectState = null;
                ProjectFiles projectFiles = null;
                ProjectStateStatus stateStatus = states().get(project);
                if (stateStatus != null && stateStatus.code() != ProjectStateStatus.STATE_UNAVAILABLE) {
                    projectFiles = stateStatus.projectFiles();
                    projectState = stateStatus.state();
                }
                List<ExecutionEntry> newExecutions = executionManager.recordedExecutions(project);
                ProjectState.merge(projectState, project, session, configManager, newExecutions, projectFiles)
                        .save(project);
            } catch (IOException | UncheckedIOException ex) {
                logger.error("Error while saving project state", ex);
            }
        }
    }

    private ProjectStateStatus processState(MavenProject project) {
        CacheConfig cacheConfig = configManager.cacheConfig();
        if (!cacheConfig.enabled()) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                        "[%s:%s] - cache is disabled, not loading state",
                        project.getGroupId(),
                        project.getArtifactId()));
            }
            return ProjectStateStatus.UNAVAILABLE;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s:%s] - loading state",
                    project.getGroupId(),
                    project.getArtifactId()));
        }
        ProjectState state;
        try {
            state = ProjectState.load(project);
            if (state == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("[%s:%s] - state file not found",
                            project.getGroupId(),
                            project.getArtifactId()));
                }
                return ProjectStateStatus.UNAVAILABLE;
            }
        } catch (IOException | XMLException ex) {
            logger.error("Error while loading project state for " + project, ex);
            return ProjectStateStatus.UNAVAILABLE;
        }

        ProjectFiles projectFiles;
        try {
            projectFiles = ProjectFiles.of(project, configManager);
        } catch (IOException ex) {
            logger.error("Error while checking project files for " + project, ex);
            return ProjectStateStatus.UNAVAILABLE;
        }

        if (!state.projectFiles().equals(projectFiles)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                        "[%s:%s] - files changed - state is invalid",
                        project.getGroupId(),
                        project.getArtifactId()));
            }
            return new ProjectStateStatus(ProjectStateStatus.STATE_FILES_CHANGED, state, projectFiles);
        }
        return new ProjectStateStatus(ProjectStateStatus.STATE_VALID, state, projectFiles);
    }
}
