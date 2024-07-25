/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

/**
 * Manages the "fast-forward" feature.
 */
@Named
@SessionScoped
public class LifecycleManager {

    @Inject
    private ProjectStateManager stateManager;

    @Inject
    private ProjectExecutionManager executionManager;

    @Inject
    private CacheConfigManager configManager;

    @Inject
    private MavenSession session;

    @Inject
    private Logger logger;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Update the lifecycle to fast-forward cached executions.
     */
    public void afterProjectsRead() {
        MavenExecutionRequest request = session.getRequest();
        request.setExecutionListener(new ExecutionListenerImpl(request.getExecutionListener()));
    }

    private void initState(MavenSession session) {
        if (initialized.compareAndSet(false, true)) {
            if (session.getGoals().contains("clean")) {
                logger.debug("Clean requested, state is ignored");
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing state files...");
                }
                stateManager.states().forEach((p, stateStatus) -> {
                    if (stateStatus.code() != ProjectStateStatus.STATE_UNAVAILABLE) {
                        if (stateStatus.code() == ProjectStateStatus.STATE_VALID) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(String.format("[%s:%s] - applying state",
                                        p.getGroupId(),
                                        p.getArtifactId()));
                            }
                            stateStatus.state().apply(p, session);
                        }
                        executionManager.processExecutions(p, stateStatus);
                    }
                });
                logger.info(String.format("Loaded %s state file(s)", stateManager.loaded()));
            }
            latch.countDown();
        } else {
            awaitInit();
        }
    }

    private void awaitInit() {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final class ExecutionListenerImpl extends DelegatingExecutionListener {

        ExecutionListenerImpl(ExecutionListener delegate) {
            super(delegate);
        }

        @Override
        public void projectStarted(ExecutionEvent event) {
            super.projectStarted(event);
            initState(event.getSession());
            MavenProject project = event.getProject();
            CacheConfig.LifecycleConfig lifeCycleConfig = configManager.lifecycleConfig(project);
            ProjectExecutionPlan plan = executionManager.plan(project);
            if (!lifeCycleConfig.enabled()) {
                logger.info("Cache is disabled");
            } else if (plan == null) {
                logger.info("State is not available");
            } else if (plan.hasInvalidDownstream()) {
                logger.info("Downstream state(s) not available, state is ignored");
            } else if (!plan.hasFileChanges() && plan.allCached()) {
                logger.info("All executions are cached! (fast-forward)");
            } else if (plan.hasFileChanges()) {
                logger.info("File changes detected, state is ignored");
                ProjectStateStatus stateStatus = plan.stateStatus();
                stateStatus.state()
                        .projectFiles()
                        .diff(stateStatus.projectFiles())
                        .forEach(diff -> logger.info("  +- " + diff.asString()));
            } else {
                plan.executionStatuses()
                        .stream()
                        .filter(s -> !s.isNew())
                        .forEach(s -> {
                            logger.info(s.toString());
                            if (s.isDiff()) {
                                List<ConfigDiff> diffs = s.diffs();
                                for (ConfigDiff diff : diffs) {
                                    logger.info("           +- " + diff.asString());
                                }
                            }
                        });
            }
        }

        @Override
        public void mojoSucceeded(ExecutionEvent event) {
            super.mojoSucceeded(event);
            executionManager.recordExecution(event.getMojoExecution(), event.getProject());
        }

        @Override
        public void projectSucceeded(ExecutionEvent event) {
            super.projectSucceeded(event);
            MavenProject project = event.getProject();
            if (configManager.lifecycleConfig(project).enabled()) {
                stateManager.save(project);
            }
        }
    }
}
