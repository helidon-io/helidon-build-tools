/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.helidon.build.util.Constants;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.Strings;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;

import static io.helidon.build.util.PrintStreams.STDOUT;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.HELIDON_VERSION;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DEPENDENCIES;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_EXCLUDES;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_INCLUDES;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;
import static io.helidon.build.util.ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR;
import static io.helidon.build.util.ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_SEPARATOR;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;

/**
 * Collects settings from a maven project and stores them in the a config file for later use
 * by {@link MavenProjectSupplier}. Must be installed as a maven extension to run.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class MavenProjectConfigCollector extends AbstractMavenLifecycleParticipant {

    private static final boolean ENABLED = "true".equals(System.getProperty(Constants.HELIDON_CLI_PROPERTY));
    private static final String DEBUG_PROPERTY = "project.config.collector.debug";
    private static final boolean DEBUG = "true".equals(System.getProperty(DEBUG_PROPERTY));
    private static final String MAIN_CLASS_PROPERTY = "mainClass";
    private static final String HELIDON_GROUP_ID_PREFIX = "io.helidon.";
    private static final String MULTI_MODULE_PROJECT = "Multi-module projects are not supported.";
    private static final String MISSING_MAIN_CLASS = "The required '" + MAIN_CLASS_PROPERTY + "' property is missing.";
    private static final String MISSING_DOT_HELIDON = "The required " + DOT_HELIDON + " file is missing.";
    private static final DependencyFilter DEPENDENCY_FILTER = classpathFilter(COMPILE, RUNTIME);
    private static final String COMPILE_GOAL = "compile";

    @Inject
    private ProjectDependenciesResolver dependenciesResolver;
    private Path supportedProjectDir;
    private ProjectConfig projectConfig;
    private ExecutionListener originalListener;

    /**
     * Assert that the project is one whose configuration we can support.
     *
     * @param session The session.
     * @return The project directory.
     */
    public static Path assertSupportedProject(MavenSession session) {
        assertSupportedProject(session.getProjects().size() == 1, MULTI_MODULE_PROJECT);
        final MavenProject project = session.getProjects().get(0);
        final Path projectDir = project.getBasedir().toPath();
        assertSupportedProject(ProjectConfig.projectConfigExists(projectDir), MISSING_DOT_HELIDON);
        assertSupportedProject(project.getProperties().getProperty(MAIN_CLASS_PROPERTY) != null, MISSING_MAIN_CLASS);
        debug("Helidon project is supported");
        return projectDir;
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        if (ENABLED) {
            // Init state
            supportedProjectDir = null;
            projectConfig = null;
            originalListener = null;
            debug("collector enabled");
            try {
                // Ensure that we support this project
                supportedProjectDir = assertSupportedProject(session);
                // Install our listener so we can know if compilation occurred and succeeded
                final MavenExecutionRequest request = session.getRequest();
                originalListener = request.getExecutionListener();
                request.setExecutionListener(new EventListener(originalListener));
            } catch (IllegalStateException e) {
                supportedProjectDir = null;
                projectConfig = null;
            }
        } else {
            debug("collector disabled");
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        if (ENABLED && supportedProjectDir != null) {
            // Remove our event listener so that incremental compiles don't fire it
            if (originalListener != null) {
                session.getRequest().setExecutionListener(originalListener);
            }
            final MavenExecutionResult result = session.getResult();
            if (result == null) {
                debug("Build failed: no result");
                invalidateConfig();
            } else if (result.hasExceptions()) {
                debug("Build failed: %s", result.getExceptions());
                invalidateConfig();
            } else if (projectConfig != null) {
                debug("Build succeeded, with compilation. Updating config.");
                storeConfig();
            } else {
                debug("Build succeeded, without compilation");
                invalidateConfig();
            }
        }
    }

    private void collectConfig(MavenProject project, MavenSession session, Xpp3Dom pluginConfig) {
        final Path projectDir = project.getBasedir().toPath();
        final ProjectConfig config = ProjectConfig.projectConfig(projectDir);
        final List<Artifact> dependencies = dependencies(project, session);
        final String helidonVersion = helidonVersion(dependencies);
        final Path outputDir = projectDir.resolve(project.getBuild().getOutputDirectory());
        final List<String> classesDirs = List.of(outputDir.toString());
        final List<String> resourceDirs = project.getResources()
                                                 .stream()
                                                 .map(MavenProjectConfigCollector::format)
                                                 .collect(Collectors.toList());
        if (helidonVersion != null) {
            config.property(HELIDON_VERSION, helidonVersion);
        }
        config.property(PROJECT_DEPENDENCIES, dependencyFiles(dependencies));
        config.property(PROJECT_MAINCLASS, project.getProperties().getProperty(MAIN_CLASS_PROPERTY));
        config.property(PROJECT_VERSION, project.getVersion());
        config.property(PROJECT_CLASSDIRS, classesDirs);
        config.property(PROJECT_SOURCEDIRS, project.getCompileSourceRoots());
        config.property(PROJECT_SOURCE_INCLUDES, toList(pluginConfig, "includes"));
        config.property(PROJECT_SOURCE_EXCLUDES, toList(pluginConfig, "excludes"));
        config.property(PROJECT_RESOURCEDIRS, resourceDirs);
        this.projectConfig = config;

        // Make the session available to our dev loop code for incremental builds
        CurrentMavenSession.set(session);
    }

    private static String format(Resource resource) {
        // Format: ${path}:${includesList}:${excludesList}
        // where include/exclude lists are semicolon separated lists and may be empty
        return resource.getDirectory()
               + RESOURCE_INCLUDE_EXCLUDE_SEPARATOR + join(RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR, resource.getIncludes())
               + RESOURCE_INCLUDE_EXCLUDE_SEPARATOR + join(RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR, resource.getExcludes());
    }

    private static List<String> toList(Xpp3Dom pluginConfig, String nodeName) {
        final Xpp3Dom node = pluginConfig.getChild(nodeName);
        if (node == null) {
            return emptyList();
        } else if (node.getChildCount() == 0) {
            final String value = node.getValue();
            return Strings.isValid(value) ? List.of(value) : emptyList();
        } else {
            final List<String> result = new ArrayList<>();
            for (int i = 0; i < node.getChildCount(); i++) {
                final Xpp3Dom child = node.getChild(i);
                final String value = child.getValue();
                if (Strings.isValid(value)) {
                    result.add(value);
                }
            }
            return result;
        }
    }

    private List<Artifact> dependencies(MavenProject project, MavenSession session) {
        try {
            return dependenciesResolver.resolve(new DefaultDependencyResolutionRequest(project, session.getRepositorySession())
                                                        .setResolutionFilter(DEPENDENCY_FILTER))
                                       .getDependencies()
                                       .stream()
                                       .map(Dependency::getArtifact)
                                       .collect(Collectors.toList());
        } catch (DependencyResolutionException e) {
            throw new RuntimeException("Dependency resolution failed: " + e.getMessage());
        }
    }

    private List<String> dependencyFiles(List<Artifact> dependencies) {
        return dependencies.stream().map(d -> d.getFile().toString()).collect(Collectors.toList());
    }

    private String helidonVersion(List<Artifact> dependencies) {
        return dependencies.stream()
                           .filter(a -> a.getGroupId().startsWith(HELIDON_GROUP_ID_PREFIX))
                           .map(Artifact::getVersion)
                           .findFirst().orElse(null);
    }

    private void storeConfig() {
        projectConfig.buildSucceeded();
        projectConfig.store();
    }

    private void invalidateConfig() {
        projectConfig = ProjectConfig.projectConfig(supportedProjectDir);
        projectConfig.buildFailed();
        projectConfig.store();
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
            STDOUT.printf(message + "%n", args);
            STDOUT.flush();
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
            if (execution.getGoal().equals(COMPILE_GOAL)) {
                collectConfig(event.getProject(), event.getSession(), execution.getConfiguration());
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
