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
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
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
    private static final String DEBUG_PROPERTY = "project.config.collector.debug";
    private static final boolean DEBUG = "true".equals(System.getProperty(DEBUG_PROPERTY));
    private static final String MAIN_CLASS_PROPERTY = "mainClass";
    private static final String MULTI_MODULE_PROJECT = "Multi-module projects are not supported.";
    private static final String MISSING_MAIN_CLASS = "The required '" + MAIN_CLASS_PROPERTY + "' property is missing.";
    private static final String MISSING_DOT_HELIDON = "The required " + DOT_HELIDON + " file is missing.";

    private boolean supportedProject;
    private boolean compileSucceeded;

    /**
     * Assert that the project is one whose configuration we can support.
     *
     * @param session The session.
     */
    public static void assertSupportedProject(MavenSession session) {
        assertSupportedProject(session.getProjects().size() == 1, MULTI_MODULE_PROJECT);
        final MavenProject project = session.getProjects().get(0);
        final Path projectDir = project.getBasedir().toPath();
        assertSupportedProject(ProjectConfig.helidonCliConfigExists(projectDir), MISSING_DOT_HELIDON);
        assertSupportedProject(project.getProperties().getProperty(MAIN_CLASS_PROPERTY) != null, MISSING_MAIN_CLASS);
        debug("Helidon project is supported");
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        try {
            // Ensure that we support this project
            assertSupportedProject(session);
            supportedProject = true;

            // Install our listener so we can know if compilation occurred and succeeded
            final MavenExecutionRequest request = session.getRequest();
            request.setExecutionListener(new EventListener(request.getExecutionListener()));
        } catch (IllegalStateException e) {
            supportedProject = false;
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        if (supportedProject) {
            final MavenProject project = session.getProjects().get(0);
            final Path projectDir = project.getBasedir().toPath();
            final ProjectConfig config = ProjectConfig.loadHelidonCliConfig(projectDir);
            final MavenExecutionResult result = session.getResult();
            if (result == null) {
                debug("Build failed: no result");
                invalidateConfig(config);
            } else if (result.hasExceptions()) {
                debug("Build failed: %s", result.getExceptions());
                invalidateConfig(config);
            } else if (compileSucceeded) {
                debug("Build succeeded, with compilation. Updating config.");
                updateConfig(config, project);
            } else {
                debug("Build succeeded, without compilation");
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

    private static void assertSupportedProject(boolean supported, String reasonIfUnsupported) {
        if (!supported) {
            final String message = "Helidon project is not supported: " + reasonIfUnsupported;
            debug(message);
            throw new IllegalStateException(message);
        }
    }

    private static void debug(String message, Object... args) {
        if (DEBUG) {
            System.out.println(String.format(message, args));
        }
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
