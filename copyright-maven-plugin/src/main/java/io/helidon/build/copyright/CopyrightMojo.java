/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.copyright;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.helidon.build.common.Log;
import io.helidon.build.common.maven.plugin.MavenLogWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Copyright check plugin.
 *
 */
@Mojo(name = "check",
      defaultPhase = LifecyclePhase.VALIDATE,
      threadSafe = true)
public class CopyrightMojo extends AbstractMojo {
    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "helidon.copyright.skip")
    private boolean skip;

    /**
     * Fail if copyright is invalid.
     */
    @Parameter(property = "failOnError", defaultValue = "false")
    private Boolean failOnError;

    /**
     * Base directory for project.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File baseDirectory;

    /**
     * File with the template to use for copyright.
     */
    @Parameter(property = "copyright.template")
    private File templateFile;

    /**
     * File with excludes.
     */
    @Parameter(property = "copyright.exclude")
    private File excludeFile;

    /**
     * Git branch to check changes (the current branch must have this branch in its history).
     * Defaults to {@code master}.
     */
    @Parameter(property = "copyright.branch", defaultValue = "master")
    private String gitBranch;

    /**
     * Copyright year separator.
     */
    @Parameter(property = "copyright.year-separator", defaultValue = ", ")
    private String yearSeparator;

    /**
     * Whether to use only files tracked by git.
     */
    @Parameter(property = "copyright.scm-only", defaultValue = "true")
    private boolean scmOnly;

    /**
     * Whether to only check format and ignore year.
     */
    @Parameter(property = "copyright.check-format-only", defaultValue = "false")
    private boolean checkFormatOnly;

    /**
     * Whether to check all files, including unmodified ones.
     */
    @Parameter(property = "copyright.check-all", defaultValue = "false")
    private boolean checkAll;

    /**
     * The {@link MavenSession}.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }

        Path path = baseDirectory.toPath().toAbsolutePath();
        Path rootDir = Paths.get(session.getExecutionRootDirectory());

        if (!path.equals(rootDir) && path.startsWith(rootDir)) {
            // this is not the root dir, and the root dir is my parent
            getLog().info("Parent path " + rootDir + " already checked");
            return;
        }

        getLog().info("Checking copyright for " + path);

        Log.writer(MavenLogWriter.create(getLog()));

        Copyright.Builder builder = Copyright.builder()
                .checkAll(checkAll)
                .checkFormatOnly(checkFormatOnly)
                .scmOnly(scmOnly)
                .yearSeparator(yearSeparator)
                .masterBranch(gitBranch)
                .path(path);

        if (templateFile != null) {
            builder.templateFile(templateFile.toPath());
        }
        if (excludeFile != null) {
            builder.excludesFile(excludeFile.toPath());
        }

        List<String> errors;
        try {
            errors = builder.build().check();
        } catch (CopyrightException e) {
            throw new MojoFailureException("Failed to validate copyright", e);
        }

        if (errors.isEmpty()) {
            return;
        }

        Log.Level logLevel;
        if (failOnError) {
            logLevel = Log.Level.ERROR;
        } else {
            logLevel = Log.Level.WARN;
        }

        Log.log(logLevel, "Copyright failures:");
        for (String error : errors) {
            Log.log(logLevel, error);
        }

        if (failOnError) {
            throw new MojoExecutionException("Failed to validate copyright.");
        }
    }
}
