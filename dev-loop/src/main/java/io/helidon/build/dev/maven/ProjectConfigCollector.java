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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import static io.helidon.build.dev.maven.ProjectConfigCollector.IncrementalBuildStrategy.JAVAC;
import static io.helidon.build.dev.maven.ProjectConfigCollector.IncrementalBuildStrategy.MAVEN;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSPATH;
import static io.helidon.build.util.ProjectConfig.PROJECT_COMPILER_OPTIONS;
import static io.helidon.build.util.ProjectConfig.PROJECT_COMPILE_SUCCEEDED;
import static io.helidon.build.util.ProjectConfig.PROJECT_INCREMENTAL_BUILD_STRATEGY;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_VERSION;
import static java.util.Collections.emptySet;
import static org.apache.maven.shared.utils.StringUtils.isNotEmpty;

/**
 * Collects settings from a maven project and stores them in the a config file for later use
 * by {@link MavenProjectSupplier}. Must be installed as a maven extension to run.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ProjectConfigCollector extends AbstractMavenLifecycleParticipant {

    private static final String COMPILE_GOAL = "compile";
    private static final String MAINCLASS_PROPERTY = "mainClass";

    @Requirement
    private MavenPluginManager pluginManager;
    private final ConfigProperties config = new ConfigProperties(DOT_HELIDON);
    private final AtomicBoolean compileSucceeded = new AtomicBoolean();
    private final AtomicReference<IncrementalBuildStrategy> incrementalBuildStrategy = new AtomicReference<>(JAVAC);
    private final AtomicReference<List<String>> compileOptions = new AtomicReference<>();

    /**
     * Incremental build strategies.
     */
    public enum IncrementalBuildStrategy {
        /**
         * Use the direct javac compilation strategy.
         */
        JAVAC,

        /**
         * Use the direct maven plugin execution strategy.
         */
        MAVEN;

        /**
         * Returns the strategy matching the given name.
         *
         * @param name The name.
         * @return The strategy.
         */
        public static IncrementalBuildStrategy parse(String name) {
            if (name == null) {
                return MAVEN;
            } else {
                return valueOf(name.toUpperCase());
            }
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        Log.debug("LifecycleParticipant: projects read");
        if (session.getProjects().size() != 1) {
            throw new RuntimeException("Unable to process multi-module projects");
        }
        final MavenExecutionRequest request = session.getRequest();
        request.setExecutionListener(new EventListener(request.getExecutionListener()));
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        Log.debug("LifecycleParticipant: session end");
        if (compileSucceeded.get()) {
            Log.debug("LifecycleParticipant: updating config");
            try {
                updateConfig(session.getProjects().get(0)); // getCurrentProject() *might* be null
            } catch (RuntimeException e) {
                invalidateConfig();
            }
        } else {
            // TODO: hmm, what do we do here? The initial build failed before or during compilation, so
            // we don't have the sourcedirs, etc.; how should the project supplier react?
        }
    }

    private void updateConfig(MavenProject project) {
        try {
            String mainClass = project.getProperties().getProperty(MAINCLASS_PROPERTY);
            if (mainClass == null) {
                throw new RuntimeException("Unable to find property " + MAINCLASS_PROPERTY);
            }
            config.property(PROJECT_MAINCLASS, mainClass);
            config.property(PROJECT_VERSION, project.getVersion());
            config.property(PROJECT_CLASSPATH, project.getRuntimeClasspathElements());
            config.property(PROJECT_SOURCEDIRS, project.getCompileSourceRoots());
            List<String> classesDirs = project.getCompileClasspathElements()
                                              .stream()
                                              .filter(d -> !d.endsWith(".jar"))
                                              .collect(Collectors.toList());
            config.property(PROJECT_CLASSDIRS, classesDirs);
            List<String> resourceDirs = project.getResources()
                                               .stream()
                                               .map(Resource::getDirectory)
                                               .collect(Collectors.toList());
            config.property(PROJECT_RESOURCEDIRS, resourceDirs);
            config.property(PROJECT_INCREMENTAL_BUILD_STRATEGY, incrementalBuildStrategy.get().toString());
            config.property(PROJECT_COMPILE_SUCCEEDED, Boolean.toString(compileSucceeded.get()));
            config.property(PROJECT_COMPILER_OPTIONS, compileOptions.get());
            config.store();
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
    }

    private void invalidateConfig() {
        config.property(PROJECT_INCREMENTAL_BUILD_STRATEGY, IncrementalBuildStrategy.MAVEN.toString());
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
            if (execution.getGoal().equals(COMPILE_GOAL)) {
                compileSucceeded.set(true);
                CompilerMojoAccessor accessor = null;
                try {
                    // execution.getMojoDescriptor().getParameterMap() // use this to collect fields?

                    final Mojo compilerMojo = pluginManager.getConfiguredMojo(Mojo.class, event.getSession(), execution);
                    accessor = new CompilerMojoAccessor(compilerMojo);
                    incrementalBuildStrategy.set(accessor.useJavacBuild() ? JAVAC : MAVEN);
                    compileOptions.set(accessor.compileOptions());
                } catch (Throwable e) {
                    incrementalBuildStrategy.set(MAVEN);
                    compileOptions.set(Collections.emptyList());
                    CompilerMojoAccessor.warnCannotUseJavacBuild("compile configuration access failed");
                    Log.debug("Mojo access failed: %s", e.getMessage());
                } finally {
                    if (accessor != null) {
                        pluginManager.releaseMojo(accessor.mojo(), execution);
                    }
                }
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

    private static class CompilerMojoAccessor {
        private final Mojo mojo;
        private final Class<?> mojoClass;
        private final List<String> compileOptions;
        private final AtomicBoolean useJavacBuild;

        CompilerMojoAccessor(Mojo mojo) {
            this.mojo = mojo;
            this.mojoClass = mojo.getClass();
            this.compileOptions = new ArrayList<>();
            this.useJavacBuild = new AtomicBoolean(true);

            canUseJavacBuild("compilerId", "javac", "non-javac compiler");
            canUseJavacBuild("includes", emptySet(), "includes");
            canUseJavacBuild("excludes", emptySet(), "excludes");

            addCompileOptionIfValidValue("--source", invoke("getSource"));
            addCompileOptionIfValidValue("--target", invoke("getTarget"));
            addCompileOptionIfValidValue("--release", invoke("getRelease"));

            collectCompileOptions();
        }

        Mojo mojo() {
            return mojo;
        }

        List<String> compileOptions() {
            return compileOptions;
        }

        boolean useJavacBuild() {
            return useJavacBuild.get();
        }

        <T> void canUseJavacBuild(String fieldName, T expectedValue, String reason) {
            final T value = extract(fieldName);
            if (expectedValue == null) {
                if (value != null) {
                    cannotUseJavacBuild(fieldName, reason);
                }
            } else if (!value.equals(expectedValue)) {
                useJavacBuild.set(false);
            }
        }

        void cannotUseJavacBuild(String fieldName, String reason) {
            if (useJavacBuild.getAndSet(false)) {
                warnCannotUseJavacBuild(reason + " configured");
            }
        }

        static void warnCannotUseJavacBuild(String reason) {
            Log.warn("Cannot use fast incremental compile: %s", reason);
        }

        void addCompileOptionIfValidValue(String key, String value) {
            if (isNotEmpty(value)) {
                compileOptions.add(key);
                compileOptions.add(value);
            }
        }

        private void collectCompileOptions() {
            // Note: this logic is adapted from the maven CompilerMojo
            final Map<String, String> effectiveCompilerArguments = invoke("getCompilerArguments");
            final String effectiveCompilerArgument = invoke("getCompilerArgument");
            final List<String> compilerArgs = extract("compilerArgs");

            if ((effectiveCompilerArguments != null)
                || (effectiveCompilerArgument != null)
                || (compilerArgs != null)) {
                if (effectiveCompilerArguments != null) {
                    for (Map.Entry<String, String> me : effectiveCompilerArguments.entrySet()) {
                        String key = me.getKey();
                        String value = me.getValue();
                        if (!key.startsWith("-")) {
                            key = "-" + key;
                        }

                        if (key.startsWith("-A") && isNotEmpty(value)) {
                            compileOptions.add(key + "=" + value);
                        } else {
                            compileOptions.add(key);
                            compileOptions.add(value);
                        }
                    }
                }
                if (!StringUtils.isEmpty(effectiveCompilerArgument)) {
                    compileOptions.add(effectiveCompilerArgument);
                }
                if (compilerArgs != null) {
                    compileOptions.addAll(compilerArgs);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T invoke(String methodName) {
            try {
                final Method method = method(methodName);
                method.setAccessible(true);
                return (T) method.invoke(mojo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T extract(String fieldName) {
            try {
                final Field field = field(fieldName);
                field.setAccessible(true);
                return (T) field.get(mojo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Field field(String fieldName) throws Exception {
            try {
                return mojoClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                return mojoClass.getSuperclass().getDeclaredField(fieldName);
            }
        }

        private Method method(String methodName) throws Exception {
            try {
                return mojoClass.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                return mojoClass.getSuperclass().getDeclaredMethod(methodName);
            }
        }
    }
}
