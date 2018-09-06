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
import java.io.IOException;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import static io.helidon.sitegen.maven.Constants.DEFAULT_SITE_OUTPUT_DIR;
import static io.helidon.sitegen.maven.Constants.PROPERTY_PREFIX;

/**
 * Goal that creates the site archive.
 *
 * @author rgrecour
 */
@Mojo(name = "package",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true)
public class PackageMojo extends AbstractMojo {

    private static final String[] DEFAULT_EXCLUDES = new String[] {};
    private static final String[] DEFAULT_INCLUDES = new String[] {"**/**"};

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    /**
     * Directory containing the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File siteArchiveOutputDirectory;

    /**
     * Directory containing the generate site files to archive.
     */
    @Parameter(defaultValue = DEFAULT_SITE_OUTPUT_DIR, required = true)
    private File siteOutputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String siteArchiveFinalName;

    /**
     * List of files to include.
     */
    @Parameter
    private String[] siteArchiveIncludes;

    /**
     * List of files to exclude.
     */
    @Parameter
    private String[] siteArchiveExcludes;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = PROPERTY_PREFIX + "siteArchiveSkip",
            defaultValue = "false",
            required = false)
    private boolean siteArchiveSkip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (siteArchiveSkip) {
            getLog().info("processing is skipped.");
            return;
        }
        getLog().info("Assembling site JAR [" + project.getArtifactId() + "]");

        final File jarFile = new File(siteArchiveOutputDirectory, siteArchiveFinalName + ".jar");
        final MavenArchiver mvnArchiver = new MavenArchiver();
        mvnArchiver.setArchiver(jarArchiver);
        mvnArchiver.setOutputFile(jarFile);
        mvnArchiver.getArchiver().addDirectory(siteOutputDirectory, getIncludes(), getExcludes());

        try {
            mvnArchiver.createArchive(session, project, new MavenArchiveConfiguration());
        } catch (ManifestException
                | IOException
                | DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Error assembling site archive", ex);
        }

        project.getArtifact().setFile(jarFile);
    }

    private String[] getIncludes() {
        if (siteArchiveIncludes != null && siteArchiveIncludes.length > 0) {
            return siteArchiveIncludes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes() {
        if (siteArchiveExcludes != null && siteArchiveExcludes.length > 0) {
            return siteArchiveExcludes;
        }
        return DEFAULT_EXCLUDES;
    }
}
