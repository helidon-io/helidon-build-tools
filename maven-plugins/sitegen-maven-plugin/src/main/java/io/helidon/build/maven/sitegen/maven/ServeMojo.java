/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.maven;

import java.io.File;

import io.helidon.build.common.maven.plugin.PlexusLoggerHolder;
import io.helidon.build.maven.sitegen.SiteServer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal that generates the site files.
 */
@Mojo(name = "serve")
public class ServeMojo extends AbstractMojo {

    @Component
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing the generated site files.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteOutputDirectory",
               defaultValue = Constants.DEFAULT_SITE_OUTPUT_DIR,
               required = true)
    private File siteOutputDirectory;

    /**
     * TCP port to use.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "sitePort", defaultValue = "8080")
    private int sitePort;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteServeSkip", defaultValue = "false")
    private boolean siteServeSkip;

    @Override
    public void execute() throws MojoExecutionException {
        if (siteServeSkip) {
            getLog().info("processing is skipped.");
            return;
        }
        new SiteServer(sitePort, siteOutputDirectory.toPath()).start();
    }
}
