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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Project state manager.
 */
@Component(role = ProjectStateManager.class, hint = "default")
public class ProjectStateManager {

    @Requirement
    private Logger logger;

    @Requirement
    private MavenSession session;

    @Requirement
    private ProjectExecutionManager executionManager;

    private final Map<MavenProject, ProjectStateStatus> statesStatuses = new HashMap<>();

    /**
     * Get the loaded states.
     *
     * @return map of state keyed by project
     */
    public Map<MavenProject, ProjectStateStatus> statesStatuses() {
        return statesStatuses;
    }

    /**
     * Process the state of all the projects in the session.
     *
     * @param session Maven session
     * @return map of state statuses by project
     */
    public Map<MavenProject, ProjectStateStatus> processStates(MavenSession session) {
        ProjectDependencyGraph pdg = session.getProjectDependencyGraph();
        Map<MavenProject, ProjectStateStatus> statusMap = new HashMap<>();
        LinkedList<MavenProject> stack = new LinkedList<>(session.getProjects());
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

    private ProjectStateStatus processState(MavenProject project) {
        ProjectState state;
        if (CacheConfig.of(project, session).skip()) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                        "[%s:%s] - cache.skip is true, not loading state",
                        project.getGroupId(),
                        project.getArtifactId()));
            }
            return ProjectStateStatus.UNAVAILABLE;
        }
        try {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("[%s:%s] - loading state",
                        project.getGroupId(),
                        project.getArtifactId()));
            }
            state = ProjectState.load(project);
            if (state == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("[%s:%s] - state file not found",
                            project.getGroupId(),
                            project.getArtifactId()));
                }
                return ProjectStateStatus.UNAVAILABLE;
            }
        } catch (IOException | XmlPullParserException ex) {
            logger.error("Error while loading project state for " + project, ex);
            return ProjectStateStatus.UNAVAILABLE;
        }

        ProjectFiles projectFiles;
        try {
            projectFiles = ProjectFiles.of(project, session);
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
        ProjectStateStatus stateStatus = new ProjectStateStatus(ProjectStateStatus.STATE_VALID, state, projectFiles);
        statesStatuses.put(project, stateStatus);
        return stateStatus;
    }

    /**
     * Merge and save the state for a given project.
     *
     * @param project Maven project
     * @param session Maven session
     */
    public void save(MavenProject project, MavenSession session) {
        try {
            ProjectState projectState = null;
            ProjectFiles projectFiles = null;
            ProjectStateStatus stateStatus = statesStatuses.get(project);
            if (stateStatus != null && stateStatus.code() != ProjectStateStatus.STATE_UNAVAILABLE) {
                projectFiles = stateStatus.projectFiles();
                projectState = stateStatus.state();
            }
            List<ExecutionEntry> newExecutions = executionManager.recordedExecutions(project);
            ProjectState.merge(projectState, project, session, newExecutions, projectFiles)
                        .save(project);
        } catch (IOException | UncheckedIOException ex) {
            logger.error("Error while saving project state", ex);
        }
    }
}
