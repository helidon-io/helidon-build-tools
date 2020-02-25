/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.dev.mode.DevLoop;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
    requiresDependencyResolution = ResolutionScope.RUNTIME)
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
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "dev.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }
        try {
            DevLoop loop = new DevLoop(devProjectDir.toPath(), clean);
            loop.start(Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new MojoExecutionException("Error", e);
        }
    }
}
