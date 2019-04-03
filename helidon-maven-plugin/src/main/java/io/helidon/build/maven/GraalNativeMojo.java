/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Maven goal to invoke GraalVM {@code native-image}.
 */
@Mojo(name = "native-image",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresDependencyResolution = ResolutionScope.RUNTIME,
      requiresProject = true)
public class GraalNativeMojo extends AbstractMojo {

    /**
     * Constant for the {@code native-image} command file name.
     */
    private static final String NATIVE_IMAGE_CMD = "native-image";

    /**
     * Plexus build context used to get the scanner for scanning resources.
     */
    @Component
    private BuildContext buildContext;

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}",
            readonly = true, required = true)
    private File buildDirectory;

    /**
     * GraalVM home.
     */
    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private File graalVMHome;

    /**
     * Name of the output file to be generated.
     */
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true,
            required = true)
    private String finalName;

    /**
     * Show exception stack traces for exceptions during image building.
     */
    @Parameter(defaultValue = "true",
            property = "native.image.reportExceptionStackTraces")
    private boolean reportExceptionStackTraces;

    /**
     * Do not use image-build server.
     */
    @Parameter(defaultValue = "true",
            property = "native.image.noServer")
    private boolean noServer;

    /**
     * Indicates if project resources should be added to the image.
     */
    @Parameter(defaultValue = "true")
    private boolean addProjectResources;

    /**
     * List of regexp matching names of resources to be included in the image.
     */
    @Parameter
    private List<String> includeResources;

    /**
     * Build shared library.
     */
    @Parameter(defaultValue = "false", property = "native.image.buildShared")
    private boolean buildShared;

    /**
     * Build statically linked executable (requires static {@code libc} and
     * {@code zlib}).
     */
    @Parameter(defaultValue = "false", property = "native.image.buildStatic")
    private boolean buildStatic;

    /**
     * Additional command line arguments.
     */
    @Parameter
    private List<String> additionalArgs;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "native.image.skip")
    private boolean skipNativeImage;

    /**
     * The {@code native-image} execution process.
     */
    private Process process;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipNativeImage) {
            getLog().info("Skipping execution.");
            return;
        }

        File artifact = project.getArtifact().getFile();
        if (artifact == null) {
            artifact = new File(buildDirectory,
                    project.getBuild().getFinalName() + ".jar");
        }
        if (!artifact.exists()) {
            throw new MojoFailureException("Artifact does not exist: "
                    + artifact.getAbsolutePath());
        }

        File outputFile = new File(buildDirectory, finalName);
        getLog().info("Building native image :" + outputFile.getAbsolutePath());

        // create the command
        List<String> command = new ArrayList<>();
        command.add(findNativeImageCmd().getAbsolutePath());
        if (buildShared || buildStatic) {
            if (buildShared && buildShared) {
                throw new MojoExecutionException(
                        "static and shared option cannot be used together");
            }
            if (buildShared) {
                getLog().info("Building a shared library");
                command.add("--shared");
            }
            if (buildStatic) {
                getLog().info("Building a statically linked executable");
                command.add("--static");
            }
        }
        command.add("-H:Name=" + outputFile.getAbsolutePath());
        String resources = getResources();
        if (!resources.isEmpty()) {
            command.add("-H:IncludeResources=" + resources);
        }
        if (reportExceptionStackTraces) {
            command.add("-H:+ReportExceptionStackTraces");
        }
        if (noServer) {
            command.add("--no-server");
        }
        command.add("-classpath");
        command.add(getClasspath());
        if (additionalArgs != null) {
            command.addAll(additionalArgs);
        }
        command.add("-jar");
        command.add(artifact.getAbsolutePath());
        getLog().debug("Executing command: " + command);

        // execute the command process
        ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.directory(buildDirectory);
        Thread stdoutReader = new Thread(this::logStdout);
        Thread stderrReader = new Thread(this::logStderr);
        try {
            process = pb.start();
            stdoutReader.start();
            stderrReader.start();
            int exitCode = process.waitFor();
            stdoutReader.join();
            stderrReader.join();
            if (exitCode != 0) {
                throw new MojoFailureException("Image generation failed, "
                        + "exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException ex) {
            throw new MojoExecutionException("Image generation error", ex);
        }
    }

    /**
     * Log the process standard output.
     */
    private void logStdout() {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        try {
            line = reader.readLine();
            while (line != null) {
                getLog().info(line);
                line = reader.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Log the process standard error.
     */
    private void logStderr() {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
        String line;
        try {
            line = reader.readLine();
            while (line != null) {
                getLog().warn(line);
                line = reader.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Scan for project resources and produce a comma separated list of include
     * resources.
     * @return String as comma separated list
     */
    private String getResources(){
        // scan all resources
        getLog().debug("Building resources string");
        List<String> resources = new ArrayList<>();

        if (addProjectResources) {
            getLog().debug("Scanning project resources");
            for (Resource resource : project.getResources()) {
                File resourcesDir = new File(resource.getDirectory());
                Scanner scanner = buildContext.newScanner(resourcesDir);
                String[] includes = null;
                if (resource.getIncludes() != null
                        && !resource.getIncludes().isEmpty()) {
                    includes = (String[]) resource.getIncludes()
                            .toArray(new String[resource.getIncludes().size()]);
                }
                scanner.setIncludes(includes);
                String[] excludes = null;
                if (resource.getExcludes() != null
                        && !resource.getExcludes().isEmpty()) {
                    excludes = (String[]) resource.getExcludes()
                            .toArray(new String[resource.getExcludes().size()]);
                }
                scanner.setExcludes(excludes);
                scanner.scan();
                for (String included : scanner.getIncludedFiles()) {
                    getLog().debug("Found resource: " + included);
                    resources.add(included);
                }
            }
        }

        // add additional resources
        if (includeResources != null) {
            getLog().debug("Adding provided resources: " + includeResources);
            resources.addAll(includeResources);
        }

        // comma separated list
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = resources.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append("|");
            }
        }
        String resourcesStr = sb.toString();
        getLog().debug("Built resources string: " + resourcesStr);
        return resourcesStr;
    }

    /**
     * Get the project run-time class-path.
     *
     * @return String represented the java class-path
     * @throws MojoExecutionException if an
     * {@link DependencyResolutionRequiredException} occurs
     */
    private String getClasspath() throws MojoExecutionException {
        getLog().debug("Building class-path string");
        try {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = project.getRuntimeClasspathElements()
                    .iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(":");
                }
            }
            String classpath = sb.toString();
            getLog().debug("Built class-path: " + classpath);
            return classpath;
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException(
                    "Unable to get compile class-path", ex);
        }
    }

    /**
     * Find the {@code native-image} command file.
     *
     * @param graalVmHome the configured {@code GRAALVM_HOME}
     * @return File
     * @throws MojoExecutionException if unable to find the command file
     */
    private File findNativeImageCmd() throws MojoExecutionException {

        if (graalVMHome == null
                || !graalVMHome.exists()
                || !graalVMHome.isDirectory()) {

            getLog().debug(
                    "graalvm.home not set,looking in the PATH environment");

            String sysPath = System.getenv("PATH");
            if (sysPath == null || sysPath.isEmpty()) {
                throw new MojoExecutionException(
                        "PATH environment variable is unset or empty");
            }
            for (final String p : sysPath.split(File.pathSeparator)) {
                final File e = new File(p, NATIVE_IMAGE_CMD);
                if (e.isFile()) {
                    return e.getAbsoluteFile();
                }
            }
            throw new MojoExecutionException(NATIVE_IMAGE_CMD
                    + " not found in the PATH environment");
        }

        getLog().debug(
                "graalvm.home set, looking for bin/" + NATIVE_IMAGE_CMD);

        File binDir = new File(graalVMHome, "bin");
        if (!binDir.exists() || !binDir.isDirectory()) {
            throw new MojoExecutionException("Unable to find "
                    + NATIVE_IMAGE_CMD + " command path, "
                    + binDir.getAbsolutePath()
                    + " is not a valid directory");
        }

        File cmd = new File(binDir, "native-image");
        if (!cmd.exists() || !cmd.isFile()) {
            throw new MojoExecutionException("Unable to find "
                    + NATIVE_IMAGE_CMD + " command path, "
                    + cmd.getAbsolutePath()
                    + " is not a valid file");
        }
        getLog().debug("Found " + NATIVE_IMAGE_CMD + ": " + cmd);
        return cmd;
    }
}
