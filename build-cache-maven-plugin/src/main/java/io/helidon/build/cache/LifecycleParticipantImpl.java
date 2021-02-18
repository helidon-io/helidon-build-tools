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

import java.nio.file.Files;
import java.util.Map.Entry;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Life-cycle participant that provides the hooks for the build cache mechanism.
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "build-cache")
public class LifecycleParticipantImpl extends AbstractMavenLifecycleParticipant {

    @Requirement
    private ProjectStateManager stateManager;

    @Requirement
    private CacheArchiveManager cacheManager;

    @Requirement
    private ProjectExecutionManager executionManager;

    @Requirement
    private Logger logger;

    private CacheConfig config;

    @Override
    public void afterProjectsRead(MavenSession session) {
        config = CacheConfig.of(session.getTopLevelProject(), session);
        if (config.skip()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping build-cache");
            }
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("cache.archive=" + config.archiveFile());
        }

        logger.info("");
        logger.info("----------------------------{ build-cache }-----------------------------");
        if (config.loadArchive() && config.archiveFile() != null && Files.exists(config.archiveFile())) {
            cacheManager.loadCache(session, config.archiveFile());
        }
        if (session.getGoals().contains("clean")) {
            logger.info("Clean requested, state is ignored.");
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Processing state files...");
            }
            for (Entry<MavenProject, ProjectStateStatus> entry : stateManager.processStates(session).entrySet()) {
                MavenProject project = entry.getKey();
                ProjectStateStatus stateStatus = entry.getValue();
                if (stateStatus.code() != ProjectStateStatus.STATE_UNAVAILABLE) {
                    if (stateStatus.code() == ProjectStateStatus.STATE_VALID) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("[%s:%s] - applying state",
                                    project.getGroupId(),
                                    project.getArtifactId()));
                        }
                        stateStatus.state().apply(project, session);
                    }
                    executionManager.processExecutions(session, project, stateStatus);
                }
            }
            logger.info("Loaded " + stateManager.statesStatuses().size() + " state file(s)");
        }
        MavenExecutionRequest request = session.getRequest();
        request.setExecutionListener(new ExecutionListenerImpl(request.getExecutionListener()));
        logger.info("------------------------------------------------------------------------");
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        if (!config.skip() && config.archiveFile() != null && config.createArchive()) {
            logger.info("");
            logger.info("----------------------------{ build-cache }-----------------------------");
            cacheManager.save(session, config.archiveFile());
            logger.info("------------------------------------------------------------------------");
        }
    }

    private final class ExecutionListenerImpl extends DelegatingExecutionListener {

        ExecutionListenerImpl(ExecutionListener delegate) {
            super(delegate);
        }

        @Override
        public void projectStarted(ExecutionEvent event) {
            super.projectStarted(event);
            MavenProject project = event.getProject();
            ProjectExecutionPlan plan = executionManager.plan(project);
            boolean skip = CacheConfig.of(event.getProject(), event.getSession()).skip();
            if (plan != null || skip) {
                logger.info("");
                logger.info("----------------------------{ build-cache }-----------------------------");
                if (skip) {
                    logger.info("Cache is disabled.");
                } else if (plan.hasInvalidDownstream()) {
                    logger.info("Downstream state(s) not available, state is ignored.");
                } else if (!plan.hasFileChanges() && plan.allCached()) {
                    logger.info("All executions are cached! (fast-forward)");
                } else if (plan.hasFileChanges()) {
                    logger.info("File changes detected, state is ignored.");
                    ProjectStateStatus stateStatus = plan.stateStatus();
                    stateStatus.state()
                               .projectFiles()
                               .diff(stateStatus.projectFiles())
                               .forEachRemaining(diff -> logger.info("  +- " + diff.asString()));
                } else {
                    plan.executionStatuses()
                        .stream()
                        .filter(s -> !s.isNew())
                        .forEach(s -> {
                            logger.info(s.toString());
                            if (s.isDiff()) {
                                ConfigDiffs diffs = s.diffs().rewind();
                                while (diffs.hasNext()) {
                                    logger.info("           +- " + diffs.next().asString());
                                }
                            }
                        });
                }
            }
        }

        @Override
        public void mojoSucceeded(ExecutionEvent event) {
            executionManager.recordExecution(event.getMojoExecution(), event.getSession(), event.getProject());
            super.mojoSucceeded(event);
        }

        @Override
        public void projectSucceeded(ExecutionEvent event) {
            stateManager.save(event.getProject(), event.getSession());
            super.projectSucceeded(event);
        }
    }
}
