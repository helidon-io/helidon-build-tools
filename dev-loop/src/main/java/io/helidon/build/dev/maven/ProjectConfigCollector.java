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

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import static io.helidon.build.dev.maven.MavenGoalExecutor.COMPILE_GOAL;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSPATH;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;

/**
 * Collects settings from a maven project and stores them in the a config file for later use
 * by {@link MavenProjectSupplier}. Must be installed as a maven extension to run.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ProjectConfigCollector extends AbstractMavenLifecycleParticipant {
    private static final String MAIN_CLASS_PROPERTY = "mainClass";

    private boolean shouldUpdateConfig;
    private boolean compileSucceeded;

    @Override
    public void afterProjectsRead(MavenSession session) {
        if (session.getProjects().size() != 1) {
            Log.debug("Cannot collect project config from multi-module projects");
        } else {
            final MavenProject project = session.getProjects().get(0);
            if (project.getProperties().getProperty(MAIN_CLASS_PROPERTY) == null) {
                throw new RuntimeException("Pom file is missing a required property: " + MAIN_CLASS_PROPERTY);
            }

            // Install our listener so we can know if compilation occurred and succeeded

            final MavenExecutionRequest request = session.getRequest();
            request.setExecutionListener(new EventListener(request.getExecutionListener()));
            shouldUpdateConfig = true;
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        if (shouldUpdateConfig) {
            final MavenProject project = session.getProjects().get(0);
            final Path projectDir = project.getBasedir().toPath();
            final ProjectConfig config = ProjectConfig.loadHelidonCliConfig(projectDir);
            final MavenExecutionResult result = session.getResult();
            if (result == null) {
                Log.debug("Build failed: no result");
                invalidateConfig(config);
            } else if (result.hasExceptions()) {
                Log.debug("Build failed: %s", result.getExceptions());
                invalidateConfig(config);
            } else if (compileSucceeded) {
                Log.debug("Build succeeded, with compilation. Updating config.");
                updateConfig(config, project);
            } else {
                Log.debug("Build succeeded, without compilation");
                invalidateConfig(config);
            }
        }
    }

    private void updateConfig(ProjectConfig config, MavenProject project) {
        try {
            final String mainClass = project.getProperties().getProperty(MAIN_CLASS_PROPERTY);
            config.property(PROJECT_MAINCLASS, mainClass);
            config.property(PROJECT_VERSION, project.getVersion());
            config.property(PROJECT_CLASSPATH, project.getRuntimeClasspathElements());
            config.property(PROJECT_SOURCEDIRS, project.getCompileSourceRoots());
            final List<String> classesDirs = project.getCompileClasspathElements()
                                                    .stream()
                                                    .filter(d -> !d.endsWith(".jar"))
                                                    .collect(Collectors.toList());
            config.property(PROJECT_CLASSDIRS, classesDirs);
            final List<String> resourceDirs = project.getResources()
                                                     .stream()
                                                     .map(Resource::getDirectory)
                                                     .collect(Collectors.toList());
            config.property(PROJECT_RESOURCEDIRS, resourceDirs);
            config.buildSucceeded();
            config.store();
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
    }

    private void invalidateConfig(ProjectConfig config) {
        config.buildFailed();
        config.store();
    }

    private class EventListener implements ExecutionListener {
        private final ExecutionListener next;

        private EventListener(ExecutionListener next) {
            this.next = next == null ? new AbstractExecutionListener() {
            } : next;
        }

        public void projectDiscoveryStarted(ExecutionEvent event) {
            next.projectDiscoveryStarted(event);
        }

        public void sessionStarted(ExecutionEvent event) {
            next.sessionStarted(event);
        }

        public void sessionEnded(ExecutionEvent event) {
            next.sessionEnded(event);
        }

        public void projectSkipped(ExecutionEvent event) {
            next.projectSkipped(event);
        }

        public void projectStarted(ExecutionEvent event) {
            next.projectStarted(event);
        }

        public void projectSucceeded(ExecutionEvent event) {
            next.projectSucceeded(event);
        }

        public void projectFailed(ExecutionEvent event) {
            next.projectFailed(event);
        }

        public void mojoSkipped(ExecutionEvent event) {
            next.mojoSkipped(event);
        }

        public void mojoStarted(ExecutionEvent event) {
            next.mojoStarted(event);
        }

        public void mojoSucceeded(ExecutionEvent event) {
            final MojoExecution execution = event.getMojoExecution();
            next.mojoSucceeded(event);
            if (execution.getGoal().equals(COMPILE_GOAL.name())) {
                compileSucceeded = true;
            }
        }

        public void mojoFailed(ExecutionEvent event) {
            next.mojoFailed(event);
        }

        public void forkStarted(ExecutionEvent event) {
            next.forkStarted(event);
        }

        public void forkSucceeded(ExecutionEvent event) {
            next.forkSucceeded(event);
        }

        public void forkFailed(ExecutionEvent event) {
            next.forkFailed(event);
        }

        public void forkedProjectStarted(ExecutionEvent event) {
            next.forkedProjectStarted(event);
        }

        public void forkedProjectSucceeded(ExecutionEvent event) {
            next.forkedProjectSucceeded(event);
        }

        public void forkedProjectFailed(ExecutionEvent event) {
            next.forkedProjectFailed(event);
        }
    }
}
