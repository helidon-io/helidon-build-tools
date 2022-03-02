/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.graal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.util.Strings;

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
      requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GraalNativeMojo extends AbstractMojo {
    private static final String EXEC_MODE_MAIN_CLASS = "main";
    private static final String EXEC_MODE_JAR = "jar";
    private static final String EXEC_MODE_JAR_WITH_CP = "jar-cp";
    private static final String PATH_ENV_VAR = "PATH";
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";

    /**
     * {@code true} if running on WINDOWS.
     */
    private static final boolean IS_WINDOWS = File.pathSeparatorChar != ':';

    /**
     * Constant for the {@code native-image} command file name.
     */
    private static final String NATIVE_IMAGE_CMD = "native-image";

    /**
     * Constant for the file extensions of windows executable scripts.
     */
    private static final List<String> WINDOWS_SCRIPT_EXTENSIONS = List.of("bat", "cmd", "ps1");

    /**
     * Constant for the file extensions that are executable on windows.
     */
    private static final List<String> WINDOWS_EXECUTABLE_EXTENSIONS = List.of("exe", "bin", "bat", "cmd", "ps1");

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

    @Parameter(defaultValue = "${project.basedir}", required = true, property = "native.image.currentDir")
    private File currentDir;

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
    @Parameter(defaultValue = "true", property = "native.image.addProjectResources")
    private boolean addProjectResources;

    @Parameter(defaultValue = EXEC_MODE_JAR,
               property = "native.image.execMode")
    private String execMode;

    @Parameter(defaultValue = "${mainClass}",
               property = "native.image.mainClass")
    private String mainClass;

    /**
     * List of regexp matching names of resources to be included in the image.
     */
    @Parameter(property = "native.image.includeResources")
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
    @Parameter(property = "native.image.additionalArgs")
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
        getLog().debug("Skip: " + skipNativeImage);
        getLog().debug("Type: " + execMode);
        getLog().debug("Main class: " + mainClass);

        if (skipNativeImage) {
            getLog().info("Skipping execution.");
            return;
        }

        NativeContext context = new NativeContext(execMode, mainClass);
        context.artifact(project, buildDirectory);
        context.validate();

        // create the command
        List<String> command = new ArrayList<>();

        File nativeImageCmd = findNativeImageCmd();
        command.add(nativeImageCmd.getAbsolutePath());
        addStaticOrShared(command);

        String quoteToken = IS_WINDOWS && isWindowsScript(nativeImageCmd) ? "\"" : "";
        addNativeImageTarget(command, quoteToken);
        addResources(command, quoteToken);

        if (reportExceptionStackTraces) {
            command.add("-H:+ReportExceptionStackTraces");
        }
        if (noServer) {
            command.add("--no-server");
        }

        if (context.addClasspath()) {
            command.add("-classpath");
            command.add(getClasspath(context));
        }

        if (additionalArgs != null) {
            command.addAll(additionalArgs);
        }

        /*
         * when using a main class, the following two lines must not be used
         * when using a jar, the jar itself and whole `Class-Path` from manifest are added to the
         * classpath automatically.
         */
        if (context.useJar()) {
            command.add("-jar");
            command.add(context.artifact().getAbsolutePath());
        }

        if (context.useMain()) {
            command.add(mainClass);
        }

        getLog().debug("Executing command: " + command);

        // execute the command process
        ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.directory(currentDir);
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

    private void addResources(List<String> command, String quoteToken) {
        String resources = getResources();
        if (!resources.isEmpty()) {
            command.add("-H:IncludeResources=" + quoteToken + resources + quoteToken);
        }
    }

    private void addNativeImageTarget(List<String> command, String quoteToken) {
        Path outputPath = buildDirectory.toPath().resolve(finalName);
        getLog().info("Building native image :" + outputPath.toAbsolutePath());

        // Path is the directory
        command.add("-H:Path=" + quoteToken + buildDirectory.getAbsolutePath() + quoteToken);

        // Name is the filename
        command.add("-H:Name=" + quoteToken + finalName + quoteToken);
    }

    private void addStaticOrShared(List<String> command) throws MojoExecutionException {
        if (buildShared || buildStatic) {
            if (buildShared && buildStatic) {
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
    private String getResources() {
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
                    includes = resource.getIncludes().toArray(new String[0]);
                }
                scanner.setIncludes(includes);
                String[] excludes = null;
                if (resource.getExcludes() != null
                        && !resource.getExcludes().isEmpty()) {
                    excludes = resource.getExcludes().toArray(new String[0]);
                }
                scanner.setExcludes(excludes);
                scanner.scan();
                for (String included : scanner.getIncludedFiles()) {
                    getLog().debug("Found resource: " + included);
                    resources.add(included.replaceAll("\\\\", "/"));
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
     * @param context configuration context
     */
    private String getClasspath(NativeContext context) throws MojoExecutionException {
        getLog().debug("Building class-path string");
        try {
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            File targetClasses = new File(buildDirectory, "classes");

            List<String> classpathElements = new LinkedList<>();

            if (context.useJar()) {
                // Adding the classpath once more causes issues with libraries
                // doing classpath scanning (slf4j, CDI) - ergo we must exclude the target when running
                // from jar
                for (String element : runtimeClasspathElements) {
                    File elementFile = new File(element);
                    if (!targetClasses.equals(elementFile)) {
                        classpathElements.add(element);
                    }
                }
            } else {
                // when running using main class, we need the whole classpath
                classpathElements.addAll(runtimeClasspathElements);
            }

            String classpath = String.join(File.pathSeparator, classpathElements);
            getLog().debug("Built class-path: " + classpath);
            return classpath;
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException(
                    "Unable to get compile class-path", ex);
        }
    }

    /**
     * Find the first command file for the specified name with a known windows executable extension.
     *
     * @param dir directory
     * @param cmd command name
     * @return File or {@code null} if no valid command file is found
     */
    private static File findWindowsCmd(File dir, String cmd) {
        return WINDOWS_EXECUTABLE_EXTENSIONS.stream()
                .map((ext) -> new File(dir, cmd + "." + ext))
                .filter(File::isFile)
                .map(File::getAbsoluteFile)
                .findFirst()
                .orElse(null);
    }

    /**
     * Test if the given command file is a windows script.
     * @param cmd command file
     * @return {@code true} if a windows script, {@code false} otherwise
     */
    private static boolean isWindowsScript(File cmd) {
        return WINDOWS_SCRIPT_EXTENSIONS.stream().anyMatch(ext -> cmd.getAbsolutePath().endsWith("." + ext));
    }

    /**
     * File a command file.
     *
     * @param dir directory
     * @param cmd command name
     * @return File or {@code null} if no valid command file is found
     */
    private static File findCmd(File dir, String cmd) {
        File cmdFile = new File(dir, cmd);
        if (cmdFile.isFile()) {
            return cmdFile;
        }
        return IS_WINDOWS ? findWindowsCmd(dir, cmd) : null;
    }

    /**
     * Find the {@code native-image} command file.
     *
     * @return File
     * @throws MojoExecutionException if unable to find the command file
     */
    private File findNativeImageCmd() throws MojoExecutionException {

        if (graalVMHome == null
                || !graalVMHome.exists()
                || !graalVMHome.isDirectory()) {

            getLog().debug(
                    "graalvm.home not set,looking in the PATH environment");

            String sysPath = System.getenv(PATH_ENV_VAR);
            if (Strings.isNotValid(sysPath)) {
                throw new MojoExecutionException(
                        "PATH environment variable is unset or empty");
            }
            for (final String p : sysPath.split(File.pathSeparator)) {
                File cmd = findCmd(new File(p), NATIVE_IMAGE_CMD);
                if (cmd != null) {
                    getLog().debug("Found " + NATIVE_IMAGE_CMD + ": " + cmd);
                    return cmd;
                }
            }

            String javaHome = System.getenv(JAVA_HOME_ENV_VAR);
            if (Strings.isValid(javaHome)) {
                Path binDir = Path.of(javaHome).resolve("bin");
                if (Files.isDirectory(binDir)) {
                    File cmd = findCmd(binDir.toFile(), NATIVE_IMAGE_CMD);
                    if (cmd != null) {
                        getLog().debug("Found " + NATIVE_IMAGE_CMD + ": " + cmd);
                        return cmd;
                    }
                }
            }

            throw new MojoExecutionException(NATIVE_IMAGE_CMD
                    + " not found in the PATH or JAVA_HOME environment");
        }

        getLog().debug(
                "graalvm.home set, looking for bin/" + NATIVE_IMAGE_CMD);

        File binDir = new File(graalVMHome, "bin");
        if (!binDir.isDirectory()) {
            throw new MojoExecutionException("Unable to find " + NATIVE_IMAGE_CMD + " command, "
                    + binDir.getAbsolutePath() + " is not a valid directory");
        }
        File cmd = findCmd(binDir, NATIVE_IMAGE_CMD);
        if (cmd != null) {
            getLog().debug("Found " + NATIVE_IMAGE_CMD + ": " + cmd);
            return cmd;
        }
        throw new MojoExecutionException(NATIVE_IMAGE_CMD
                + " not found in directory: " + binDir.getAbsolutePath());
    }

    private static final class NativeContext {
        private final boolean useJar;
        private final boolean useMain;
        private final boolean addClasspath;
        private final String mainClass;

        private File artifact;

        private NativeContext(String execMode, String mainClass) throws MojoFailureException {
            this.mainClass = mainClass;
            switch (execMode) {
            case EXEC_MODE_JAR:
                useJar = true;
                useMain = false;
                addClasspath = false;
                break;
            case EXEC_MODE_JAR_WITH_CP:
                useJar = true;
                useMain = false;
                addClasspath = true;
                break;
            case EXEC_MODE_MAIN_CLASS:
                useJar = false;
                useMain = true;
                addClasspath = true;
                break;
            default:
                throw new MojoFailureException("Invalid configuration of \"execMode\". Has to be one of: "
                                                       + EXEC_MODE_JAR + ", "
                                                       + EXEC_MODE_JAR_WITH_CP + ", or "
                                                       + EXEC_MODE_MAIN_CLASS);
            }
        }

        boolean useJar() {
            return useJar;
        }

        boolean useMain() {
            return useMain;
        }

        boolean addClasspath() {
            return addClasspath;
        }

        File artifact() {
            return artifact;
        }

        void artifact(MavenProject project, File buildDirectory) throws MojoFailureException {
            if (useJar) {
                artifact = project.getArtifact().getFile();
                if (artifact == null) {
                    artifact = new File(buildDirectory,
                                        project.getBuild().getFinalName() + ".jar");
                }
                if (!artifact.exists()) {
                    throw new MojoFailureException("Artifact does not exist: "
                                                           + artifact.getAbsolutePath());
                }
            }
        }

        void validate() throws MojoFailureException {
            if ((null == mainClass) && useMain) {
                throw new MojoFailureException("Main class not configured and required. Use option \"mainClass\"");
            }
        }
    }
}
