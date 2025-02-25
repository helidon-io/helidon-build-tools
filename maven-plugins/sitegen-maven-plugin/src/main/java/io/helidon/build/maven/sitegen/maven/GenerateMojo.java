/*
 * Copyright (c) 2018, 2025 Oracle and/or its affiliates.
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
import java.util.HashMap;
import java.util.Map;

import io.helidon.build.common.Maps;
import io.helidon.build.common.maven.logging.PlexusLoggerHolder;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.RenderingException;
import io.helidon.build.maven.sitegen.Site;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal that generates the site files.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Component
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Directory containing the generated site files.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteOutputDirectory",
            defaultValue = Constants.DEFAULT_SITE_OUTPUT_DIR,
            required = true)
    private File siteOutputDirectory;

    /**
     * Directory containing the site sources.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteSourceDirectory",
            defaultValue = Constants.DEFAULT_SITE_SOURCE_DIR,
            required = true)
    private File siteSourceDirectory;

    /**
     * Site configuration file.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteConfigFile", required = true)
    private File siteConfigFile;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "siteGenerateSkip", defaultValue = "false")
    private boolean siteGenerateSkip;

    @SuppressWarnings("CanBeFinal")
    private Site site = null;

    @Override
    public void execute() throws MojoExecutionException {
        if (siteGenerateSkip) {
            getLog().info("processing is skipped.");
            return;
        }

        project.addCompileSourceRoot(siteSourceDirectory.getAbsolutePath());

        Map<String, String> properties = new HashMap<>(Maps.fromProperties(project.getProperties()));
        properties.putAll(Maps.fromProperties(session.getUserProperties()));

        properties.put("project.groupId", project.getGroupId());
        properties.put("project.artifactId", project.getArtifactId());
        properties.put("project.version", project.getVersion());
        properties.put("project.basedir", project.getBasedir().getAbsolutePath());

        try {
            Config config = Config.create(siteConfigFile.toPath(), properties);
            site = Site.create(config);

            // enable jruby verbose mode on debugging
            if (getLog().isDebugEnabled()) {
                System.setProperty("jruby.cli.verbose", "true");
            }

            site.generate(siteSourceDirectory.toPath(), siteOutputDirectory.toPath());
        } catch (RenderingException ex) {
            throw new MojoExecutionException("Rendering error", ex);
        }
    }

    /**
     * Get the site instance.
     *
     * @return {@code Site} instance
     */
    public Site getSite() {
        return site;
    }
}
