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

package io.helidon.build.maven.report;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.helidon.build.licensing.Report;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal to generate an attribution report.
 */
@Mojo(name = "report", aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ReportMojo
    extends AbstractMojo {

    // Project to run on.
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    // True to skip this goal
    @Parameter(property = "skip", defaultValue = "false", readonly = true, required = true)
    private Boolean skip;

    // True to failOnError
    @Parameter(property = "failOnError", defaultValue = "false", readonly = true, required = true)
    private Boolean failOnError;

    // Comma separated list of (Helidon) modules to include attributions for
    @Parameter(property = Report.MODULES_PROPERTY_NAME, readonly = true)
    private String modules;

    // Attribution input XML file name
    @Parameter(property = Report.INPUT_FILE_NAME_PROPERTY_NAME, defaultValue = Report.DEFAULT_INPUT_FILE_NAME, required = true)
    private String inputFileName;

    // Directory containing attribution input XML file
    @Parameter(property = Report.INPUT_FILE_DIR_PROPERTY_NAME)
    private String inputFileDir;

    // Output report (text) file
    @Parameter(property = Report.OUTPUT_FILE_NAME_PROPERTY_NAME, defaultValue = Report.DEFAULT_OUTPUT_FILE_NAME, required = true)
    private String outputFileName;

    // Directory containing output file
    @Parameter(property = Report.OUTPUT_FILE_DIR_PROPERTY_NAME, defaultValue = "${project.build.directory}")
    private String outputFileDir;

    /**
     * Execute the report goal.
     * @throws MojoExecutionException on error
     */
    public void execute()
        throws MojoExecutionException {

        if (skip) {
            return;
        }

        Report.Builder builder = Report.builder()
                .inputFileName(inputFileName)
                .moduleList(modules)
                .inputFileDir(inputFileDir)
                .outputFileName(outputFileName)
                .outputFileDir(outputFileDir)
                .outputHandler((s) -> getLog().info(s));

        // If no modules were provided, then scan this project and get all the
        // helidon artifacts that are dependencies and use that for the module list.
        if (modules == null || modules.isEmpty()) {
            getLog().debug("No module list configured. Using dependencies of " + project.getArtifactId());
            Set<String> moduleList = getHelidonDependencies(project);
            getLog().debug("Scanned Modules: " + moduleList);
            builder.moduleList(moduleList);
        } else {
            getLog().debug("Configured Modules: " + modules);
            builder.moduleList(modules);
        }

        try {
            builder.build().execute();
        } catch (Exception e) {
            getLog().warn(e.toString());
            if (failOnError) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    private Set<String> getHelidonDependencies(MavenProject project) {
        Set<String> helidonDependencies = new TreeSet<>();

        getLog().info("Scanning " + project.getName());

        String gid;

        // If running on a Helidon module, then include it in the module list.
        gid = project.getGroupId();
        if (isHelidonGroup(gid)) {
            helidonDependencies.add(project.getArtifactId());
        }

        // Get dependencies for current module include transitive dependencies
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null && !artifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                // Save ones that are Helidon artifacts
                gid = artifact.getGroupId();
                if (isHelidonGroup(gid)) {
                    helidonDependencies.add(artifact.getArtifactId());
                }
            }
        } else {
            getLog().debug("No dependencies for " + project.getName());
        }

        // Traverse sub-projects if any
        List<MavenProject> subProjects = project.getCollectedProjects();
        if (subProjects != null && !subProjects.isEmpty()) {
            for (MavenProject p : subProjects) {
                helidonDependencies.addAll(getHelidonDependencies(p));
            }
        }

        return helidonDependencies;
    }

    private boolean isHelidonGroup(String name) {
        return (name != null && name.startsWith("io.helidon"));
    }
}
