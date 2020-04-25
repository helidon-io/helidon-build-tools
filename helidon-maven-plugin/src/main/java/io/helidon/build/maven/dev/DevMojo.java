/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.maven.dev;

import java.io.File;

import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.dev.maven.MavenProjectSupplier;
import io.helidon.build.dev.mode.DevLoop;
import io.helidon.build.maven.link.MavenLogWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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
     * Use maven log.
     */
    @Parameter(defaultValue = "true", property = "dev.useMavenLog")
    private boolean useMavenLog;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "dev.skip")
    private boolean skip;

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

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }
        try {
            if (useMavenLog) {
                MavenLogWriter.bind(getLog());
            }
            final ProjectSupplier projectSupplier = new MavenProjectSupplier(project, session, plugins);
            final DevLoop loop = new DevLoop(devProjectDir.toPath(), projectSupplier, clean, fork);
            loop.start(Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new MojoExecutionException("Error", e);
        }
    }
}
