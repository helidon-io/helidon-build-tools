/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.cli.maven.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.dev.maven.DevLoopBuildConfig;
import io.helidon.build.dev.maven.MavenEnvironment;
import io.helidon.build.dev.maven.MavenGoalReferenceResolver;
import io.helidon.build.dev.maven.MavenProjectConfigCollector;
import io.helidon.build.dev.maven.MavenProjectSupplier;
import io.helidon.build.dev.mode.DevLoop;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenLogWriter;
import io.helidon.build.util.SystemLogWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.DefaultLifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.util.StyleFunction.Yellow;
import static java.util.Collections.emptyList;

/**
 * Maven plugin that runs a {@link DevLoop}.
 */
@Mojo(name = "dev",
        defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevMojo extends AbstractMojo {

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The project directory.
     */
    @Parameter(defaultValue = "${project.basedir}", required = true)
    private File devProjectDir;

    /**
     * Perform an initial clean build.
     */
    @Parameter(defaultValue = "false", property = "dev.clean")
    private boolean clean;

    /**
     * Fork builds.
     */
    @Parameter(defaultValue = "false", property = "dev.fork")
    private boolean fork;

    /**
     * Use terminal mode.
     */
    @Parameter(defaultValue = "false", property = "dev.terminalMode")
    private boolean terminalMode;

    /**
     * Application JVM arguments.
     */
    @Parameter(property = "dev.appJvmArgs")
    private String appJvmArgs;

    /**
     * Application arguments.
     */
    @Parameter(property = "dev.appArgs")
    private String appArgs;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "dev.skip")
    private boolean skip;

    /**
     * DevLoop build lifecycle customization.
     */
    @Parameter
    private DevLoopBuildConfig devLoop;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager plugins;

    /**
     * The Maven MojoExecutor component.
     */
    @Component
    private MojoExecutor mojoExecutor;

    /**
     * The Maven MojoDescriptorCreated component, used to resolve
     * plugin prefixes.
     */
    @Component
    private MojoDescriptorCreator mojoDescriptorCreator;

    /**
     * The Maven DefaultLifecycles component, used to map
     * a phase to a list of goals.
     */
    @Component
    private DefaultLifecycles defaultLifeCycles;

    /**
     * The Maven DefaultLifecycleMappingDelegate component, used to map
     * a phase to a list of goals.
     */
    @Component(hint = DefaultLifecycleMappingDelegate.HINT)
    private LifecycleMappingDelegate standardDelegate;

    /**
     * A map of Maven lifecycle ids to LifecycleMappingDelegate instances, used to map
     * a phase to a list of goals.
     */
    @Component
    private Map<String, LifecycleMappingDelegate> delegates;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }
        try {
            MavenProjectConfigCollector.assertSupportedProject(session);
            if (terminalMode) {
                SystemLogWriter.install(getLog().isDebugEnabled() ? Log.Level.DEBUG : Log.Level.INFO);
            } else {
                MavenLogWriter.install(getLog());
            }
            if (fork) {
                warnForkDeprecated();
            }
            final DevLoopBuildConfig configuration = buildConfig(true);
            final ProjectSupplier projectSupplier = new MavenProjectSupplier(configuration);
            final List<String> jvmArgs = toList(appJvmArgs);
            final List<String> args = toList(appArgs);
            final Path dir = devProjectDir.toPath();
            final DevLoop loop = new DevLoop(dir, projectSupplier, clean, fork, terminalMode, jvmArgs, args, configuration);
            loop.start(Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new MojoExecutionException("Error", e);
        }
    }

    DevLoopBuildConfig buildConfig(boolean resolve) throws Exception {
        final DevLoopBuildConfig config = devLoop == null ? new DevLoopBuildConfig() : devLoop;
        config.validate();
        if (resolve) {
            final MavenEnvironment env = new MavenEnvironment(project, session, mojoDescriptorCreator, defaultLifeCycles,
                                                              standardDelegate, delegates, plugins);
            final MavenGoalReferenceResolver resolver = new MavenGoalReferenceResolver(env);
            config.resolve(resolver);
        }
        return config;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void warnForkDeprecated() {
        getLog().warn(Yellow.apply("fork mode is deprecated and will be removed in the next major release: incremental compiles "
                                   + "may fail to see pom.xml changes."));
        try {
            getLog().warn("press enter to continue");
            System.in.read();
        } catch (IOException ignore) {
        }
    }

    private static List<String> toList(String args) {
        return args == null ? emptyList() : Arrays.asList(args.split(" "));
    }
}
