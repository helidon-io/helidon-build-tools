/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen.maven;

import java.io.File;
import java.util.Properties;

import io.helidon.sitegen.RenderingException;
import io.helidon.sitegen.Site;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static io.helidon.sitegen.maven.Constants.DEFAULT_SITE_OUTPUT_DIR;
import static io.helidon.sitegen.maven.Constants.DEFAULT_SITE_SOURCE_DIR;
import static io.helidon.sitegen.maven.Constants.PROPERTY_PREFIX;

/**
 * Goal that generates the site files.
 *
 * @author rgrecour
 */
@Mojo(name = "generate",
      defaultPhase = LifecyclePhase.COMPILE,
      requiresProject = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing the generated site files.
     */
    @Parameter(property = PROPERTY_PREFIX + "siteOutputDirectory",
               defaultValue = DEFAULT_SITE_OUTPUT_DIR,
               required = true)
    private File siteOutputDirectory;

    /**
     * Directory containing the site sources.
     */
    @Parameter(property = PROPERTY_PREFIX + "siteSourceDirectory",
               defaultValue = DEFAULT_SITE_SOURCE_DIR,
               required = true)
    private File siteSourceDirectory;

    /**
     * Site configuration file.
     */
    @Parameter(property = PROPERTY_PREFIX + "siteConfigFile", required = true)
    private File siteConfigFile;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = PROPERTY_PREFIX + "siteGenerateSkip",
            defaultValue = "false",
            required = false)
    private boolean siteGenerateSkip;

    @SuppressWarnings("CanBeFinal")
    private Site site = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (siteGenerateSkip) {
            getLog().info("processing is skipped.");
            return;
        }

        project.addCompileSourceRoot(siteSourceDirectory.getAbsolutePath());

        Properties properties = new Properties(project.getProperties());
        properties.setProperty("project.groupId", project.getGroupId());
        properties.setProperty("project.artifactId", project.getArtifactId());
        properties.setProperty("project.version", project.getVersion());

        site = Site.builder()
                .config(siteConfigFile, properties)
                .build();

        try {
            site.generate(siteSourceDirectory, siteOutputDirectory);
        } catch (RenderingException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Get the site instance.
     * @return {@code Site} instance
     */
    public Site getSite() {
        return site;
    }
}
