/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import io.helidon.build.common.OSType;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.ansi.AnsiConsoleInstaller;
import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.FileUtils.findExecutableInPath;
import static io.helidon.build.common.FileUtils.listFiles;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireJavaExecutable;
import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.ansi.AnsiConsoleInstaller.IS_HELIDON_CHILD_PROCESS;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.Red;
import static java.io.File.pathSeparatorChar;
import static java.util.Objects.requireNonNull;

/**
 * Executes maven in a separate process.
 */
public class MavenCommand {

    private static final String EOL = System.getProperty("line.separator");
    private static final PrintStream RED_STDERR = PrintStreams.apply(STDERR, Red::apply);
    private static final OSType OS = OSType.currentOS();
    private static final String MAVEN_BINARY_NAME = OS.mavenExec();
    private static final String MAVEN_HOME_VAR = "MAVEN_HOME";
    private static final String MVN_HOME_VAR = "MVN_HOME";
    private static final String M2_HOME_VAR = "M2_HOME";
    private static final String MAVEN_CORE_PREFIX = "maven-core";
    private static final String MAVEN_SHIM_TARGET = "libexec/bin/mvn";
    private static final String JAR_SUFFIX = ".jar";
    private static final String MAVEN_DOWNLOAD_URL = "https://maven.apache.org/download.cgi";
    private static final AtomicReference<Path> MAVEN_EXECUTABLE = new AtomicReference<>();
    private static final AtomicReference<Path> MAVEN_HOME = new AtomicReference<>();
    private static final AtomicReference<MavenVersion> MAVEN_VERSION = new AtomicReference<>();
    private static final String VERSION_ERROR = "$(RED Found Maven version %s.)"
                                                + EOL
                                                + "$(bold Version) $(GREEN %s) $(bold or later is required.) "
                                                + "Please update from %s and prepend your PATH or set the MAVEN_HOME or MVN_HOME "
                                                + "environment variable.";

    private final String name;
    private final ProcessBuilder processBuilder;
    private final int maxWaitSeconds;
    private final PrintStream stdOut;
    private final PrintStream stdErr;
    private final Predicate<String> filter;
    private final Function<String, String> transform;
    private final Runnable beforeShutdown;
    private final Runnable afterShutdown;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private MavenCommand(Builder builder) {
        this.name = builder.description;
        this.processBuilder = builder.processBuilder;
        this.maxWaitSeconds = builder.maxWaitSeconds;
        this.stdOut = builder.stdOut;
        this.stdErr = builder.stdErr;
        this.filter = builder.filter;
        this.transform = builder.transform;
        this.beforeShutdown = builder.beforeShutdown;
        this.afterShutdown = builder.afterShutdown;
    }

    /**
     * Finds the {@code mvn} executable. Searches using the following, in order:
     * <ol>
     *     <li>The {@code MAVEN_HOME} environment variable</li>
     *     <li>The {@code MVN_HOME} environment variable</li>
     *     <li>The {@code PATH} environment variable</li>
     * </ol>
     *
     * @return The path.
     * @throws IllegalStateException if not found.
     */
    public static Path mavenExecutable() {
        if (MAVEN_EXECUTABLE.get() == null) {
            Path maven;
            Optional<Path> path = findExecutableInPath(MAVEN_BINARY_NAME);
            if (path.isPresent()) {
                maven = path.get();
            } else {
                maven = toMavenExecutable(MAVEN_HOME_VAR);
                if (maven == null) {
                    maven = toMavenExecutable(MVN_HOME_VAR);
                    if (maven == null) {
                        throw new IllegalStateException(MAVEN_BINARY_NAME + " not found. Please add it to "
                                                        + "your PATH or set either the MAVEN_HOME or "
                                                        + "MVN_HOME environment variables.");
                    }
                }
            }
            try {
                maven = maven.toRealPath();
                Path shimmed = maven.getParent().getParent().resolve(MAVEN_SHIM_TARGET);
                if (Files.exists(shimmed)) {
                    maven = shimmed;
                }
                MAVEN_EXECUTABLE.set(maven.toRealPath());
            } catch (IOException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
        }
        return MAVEN_EXECUTABLE.get();
    }

    /**
     * Returns the Maven home directory.
     *
     * @return The directory.
     * @throws IllegalStateException if not found.
     */
    public static Path mavenHome() {
        if (MAVEN_HOME.get() == null) {
            MAVEN_HOME.set(mavenExecutable().getParent().getParent());
        }
        return MAVEN_HOME.get();
    }

    /**
     * Returns the version of the {@code mvn} executable found via {@link MavenCommand#mavenExecutable()}.
     *
     * @return The version.
     * @throws IllegalStateException if executable not found.
     */
    public static MavenVersion installedVersion() {
        if (MAVEN_VERSION.get() == null) {
            final Path mavenHome = mavenHome();
            final Path libDir = requireDirectory(mavenHome.resolve("lib"));
            final List<Path> jars = listFiles(libDir, name -> name.startsWith(MAVEN_CORE_PREFIX) && name.endsWith(JAR_SUFFIX));
            if (jars.isEmpty()) {
                throw new IllegalStateException(MAVEN_CORE_PREFIX + "* not found in " + libDir);
            }
            final Path jarFile = jars.get(0);
            final String fileName = jarFile.getFileName().toString();
            final String versionStr = jarVersion(jarFile)
                    .orElse(fileName.substring(MAVEN_CORE_PREFIX.length() + 1, fileName.length() - JAR_SUFFIX.length()));
            final MavenVersion version = MavenVersion.toMavenVersion(versionStr);
            MAVEN_VERSION.set(version);
        }
        return MAVEN_VERSION.get();
    }

    /**
     * Find implementation version from manifest file.
     *
     * @param jarFilePath Jar file to look for version in
     * @return version or empty optional
     */
    public static Optional<String> jarVersion(Path jarFilePath) {
        try {
            return Optional.of(new JarFile(jarFilePath.toFile())
                    .getManifest()
                    .getMainAttributes()
                    .getValue("Implementation-Version"));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Check that then installed Maven version is at least the given minimum.
     *
     * @param requiredMinimumVersion The required minimum version.
     * @throws IllegalStateException If the installed version does not meet the requirement.
     */
    public static void requireMavenVersion(MavenVersion requiredMinimumVersion) {
        // This catches if we can find the mvn executable or not. We want to
        // catch this error independently of getting the maven version (since
        // getting the maven version is fragile).
        Path executable = mavenExecutable();
        Log.debug("Found maven executable " + executable);

        MavenVersion installed;
        try {
            installed = installedVersion();
        } catch (Exception ex) {
            // Could not determine the Maven version. The code to do so is fragile and is known
            // not to work in some environments (especially where shims are involved). So
            // don't fail if we can't determine the maven version.
            Log.debug("Could not determine Maven version: " + ex
                    + " Assuming version is acceptable.");
            return;
        }
        // If we were able to determine the maven version, go ahead and make sure it is acceptable.
        Requirements.require(installed.isGreaterThanOrEqualTo(requiredMinimumVersion),
                VERSION_ERROR, installed, requiredMinimumVersion, MAVEN_DOWNLOAD_URL);

    }

    /**
     * Start the command.
     *
     * @return ProcessMonitor
     * @throws IOException if an IO error occurs
     */
    public ProcessMonitor start() throws Exception {
        // Fork process and wait for its completion
        if (name != null) {
            Log.info("%s", Bold.apply(name));
        }
        return ProcessMonitor.builder()
                             .processBuilder(processBuilder)
                             .stdOut(stdOut)
                             .stdErr(stdErr)
                             .filter(filter)
                             .transform(transform)
                             .beforeShutdown(beforeShutdown)
                             .afterShutdown(afterShutdown)
                             .capture(false)
                             .build()
                             .start();
    }

    /**
     * Executes the command.
     *
     * @throws Exception if an error occurs.
     */
    public void execute() throws Exception {
        start().waitForCompletion(maxWaitSeconds, TimeUnit.SECONDS);
    }

    private static Path toMavenExecutable(String mavenHomeEnvVar) {
        Path mavenHome = envVarPath(mavenHomeEnvVar);
        if (mavenHome != null) {
            if (Files.isDirectory(mavenHome)) {
                Path executable = mavenHome.resolve("bin").resolve(MAVEN_BINARY_NAME);
                if (Files.exists(executable) && (!OSType.currentOS().isPosix() || Files.isExecutable(executable))) {
                    return executable;
                }
            }
        }
        return null;
    }

    private static Path envVarPath(String var) {
        final String path = System.getenv(var);
        return path == null ? null : Paths.get(path);
    }

    /**
     * {@link MavenCommand} builder.
     */
    public static class Builder {
        private static final String DEFAULT_MAVEN_EXEC = OS.mavenExec();
        private static final String PATH_VAR = "PATH";
        private static final String MAVEN_OPTS_VAR = "MAVEN_OPTS";
        private static final String MAVEN_DEBUG_OPTS_VAR = "MAVEN_DEBUG_OPTS";
        private static final int SECONDS_PER_YEAR = 365 * 24 * 60 * 60;
        private static final String DEBUG_OPT_PREFIX = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:";
        private static final String LOG_LEVEL_PROPERTY = "log.level";
        private static final String LOG_LEVEL = System.getProperty(LOG_LEVEL_PROPERTY);
        private static final MavenVersion DEFAULT_MINIMUM = MavenVersion.toMavenVersion("3.6.0");
        private static final String DEBUG_PORT_PROPERTY = "mvn.debug.port";
        private static final String CHILD_DEBUG_PORT_PROPERTY = "mvn.child.debug.port";
        private static final int DEFAULT_DEBUG_PORT = Integer.getInteger(DEBUG_PORT_PROPERTY, 0);
        private static final int DEFAULT_CHILD_DEBUG_PORT = Integer.getInteger(CHILD_DEBUG_PORT_PROPERTY, 0);
        private String description;
        private Path directory;
        private List<String> mavenArgs;
        private boolean verbose;
        private int debugPort;
        private int maxWaitSeconds;
        private PrintStream stdOut;
        private PrintStream stdErr;
        private Predicate<String> filter = line -> true;
        private Function<String, String> transform = Function.identity();
        private Runnable beforeShutdown = () -> {};
        private Runnable afterShutdown = () -> {};
        private MavenVersion requiredMinimumVersion;
        private ProcessBuilder processBuilder;
        private String executable;

        private Builder() {
            this.mavenArgs = new ArrayList<>();
            this.debugPort = IS_HELIDON_CHILD_PROCESS ? DEFAULT_CHILD_DEBUG_PORT : DEFAULT_DEBUG_PORT;
            this.maxWaitSeconds = SECONDS_PER_YEAR;
            this.requiredMinimumVersion = DEFAULT_MINIMUM;
        }

        /**
         * Sets the minimum Maven version required. Defaults to {@code 3.5.4}.
         *
         * @param requiredMinimumVersion The version.
         * @return This instance, for chaining.
         */
        public Builder requiredMinimumVersion(MavenVersion requiredMinimumVersion) {
            this.requiredMinimumVersion = requireNonNull(requiredMinimumVersion);
            return this;
        }

        /**
         * Sets the command description.
         *
         * @param description The description.
         * @return This instance, for chaining.
         */
        public Builder description(String description) {
            this.description = requireNonNull(description);
            return this;
        }

        /**
         * Sets the project directory.
         *
         * @param directory The directory.
         * @return This instance, for chaining.
         */
        public Builder directory(File directory) {
            return directory(directory.toPath());
        }

        /**
         * Sets the project directory.
         *
         * @param directory The directory.
         * @return This instance, for chaining.
         */
        public Builder directory(Path directory) {
            this.directory = requireDirectory(directory);
            return this;
        }

        /**
         * Add a Maven argument.
         *
         * @param argument The argument.
         * @return This instance, for chaining.
         */
        public Builder addArgument(String argument) {
            this.mavenArgs.add(requireNonNull(argument));
            return this;
        }

        /**
         * Add Maven arguments.
         *
         * @param arguments The arguments.
         * @return This instance, for chaining.
         */
        public Builder addArguments(List<String> arguments) {
            this.mavenArgs.addAll(requireNonNull(arguments));
            return this;
        }

        /**
         * Add an optional Maven argument.
         *
         * @param argument The argument, may be {@code null}.
         * @return This instance, for chaining.
         */
        public Builder addOptionalArgument(String argument) {
            if (argument != null) {
                addArgument(argument);
            }
            return this;
        }

        /**
         * Sets the Maven arguments.
         *
         * @param arguments The arguments.
         * @return This instance, for chaining.
         */
        public Builder arguments(List<String> arguments) {
            this.mavenArgs = requireNonNull(arguments);
            return this;
        }

        /**
         * Sets verbose output.
         *
         * @param verbose {@code true} for verbose (i.e. {@code --debug}) maven output.
         * @return This instance, for chaining.
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Enables attaching a debugger on the given port.
         *
         * @param debugPort The port.
         * @return This instance, for chaining.
         */
        public Builder debugPort(int debugPort) {
            this.debugPort = debugPort;
            return this;
        }

        /**
         * Sets the print stream for process {@code stdout}.
         *
         * @param stdOut The handler.
         * @return This builder.
         */
        public Builder stdOut(PrintStream stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        /**
         * Sets the print stream for process {@code stderr}.
         *
         * @param stdErr The handler.
         * @return This builder.
         */
        public Builder stdErr(PrintStream stdErr) {
            this.stdErr = stdErr;
            return this;
        }

        /**
         * Sets a filter for all process output.
         *
         * @param filter The filter.
         * @return This builder.
         */
        public Builder filter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets a transformer for all process output.
         *
         * @param transform The transformer.
         * @return This instance, for chaining.
         */
        public Builder transform(Function<String, String> transform) {
            this.transform = transform;
            return this;
        }

        /**
         * Sets the before shutdown callback.
         *
         * @param beforeShutdown a callback invoked before the process is stopped by the shutdown hook
         * @return This builder.
         */
        public Builder beforeShutdown(Runnable beforeShutdown) {
            this.beforeShutdown = beforeShutdown;
            return this;
        }

        /**
         * Sets the after shutdown callback.
         *
         * @param afterShutdown a callback invoked after the process is stopped by the shutdown hook
         * @return This builder.
         */
        public Builder afterShutdown(Runnable afterShutdown) {
            this.afterShutdown = afterShutdown;
            return this;
        }

        /**
         * Sets the maven executable path to be used for command execution.
         *
         * @param mvn the maven executable path
         * @return This builder.
         */
        public Builder executable(Path mvn) {
            executable = mvn.toString();
            return this;
        }

        /**
         * Sets the maximum number of seconds to wait for command to complete.
         *
         * @param maxWaitSeconds The seconds.
         * @return This instance, for chaining.
         */
        public Builder maxWaitSeconds(int maxWaitSeconds) {
            this.maxWaitSeconds = maxWaitSeconds;
            return this;
        }

        /**
         * Return the command.
         *
         * @return The command.
         */
        public MavenCommand build() {
            prepare();

            // Create the command

            List<String> command = new ArrayList<>();
            command.add(executable == null ? DEFAULT_MAVEN_EXEC : executable);
            command.addAll(mavenArgs);
            if (verbose) {
                command.add("--debug");
            }
            if (LOG_LEVEL != null) {
                command.add("-D" + LOG_LEVEL_PROPERTY + "=" + LOG_LEVEL);
            }
            if (DEFAULT_CHILD_DEBUG_PORT > 0) {
                command.add("-D" + CHILD_DEBUG_PORT_PROPERTY + "=" + DEFAULT_CHILD_DEBUG_PORT);
            }
            if (!AnsiConsoleInstaller.areAnsiEscapesEnabled()) {
                command.add("--batch-mode");
            }

            // Create the process builder

            processBuilder = new ProcessBuilder()
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .directory(directory.toFile())
                    .command(command);

            // Ensure we use the current Maven version

            Map<String, String> env = processBuilder.environment();
            String mavenPath = mavenExecutable().getParent().toString();
            env.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(PATH_VAR))
                    .forEach(e -> env.put(e.getKey(), mavenPath + pathSeparatorChar + e.getValue()));

            // Setup MAVEN_OPTS with debugger, if needed
            env.computeIfPresent(MAVEN_DEBUG_OPTS_VAR, (k, v) -> "");
            String mavenOpts = removeDebugOption(env.get(MAVEN_OPTS_VAR));
            if (debugPort > 0) {
                mavenOpts = addMavenOption(DEBUG_OPT_PREFIX + debugPort, mavenOpts);
            }

            // Ensure that the Ansi configuration in the child process is set correctly.
            // Must use MAVEN_OPTS since properties are checked prior to maven's processing of the command-line -D options.

            mavenOpts = addMavenOption(AnsiConsoleInstaller.childProcessArgument(), mavenOpts);

            env.put(MAVEN_OPTS_VAR, mavenOpts);
            if (executable != null) {
                String mvnHome = Path.of(executable).getParent().getParent().toString();
                processBuilder.environment().put(M2_HOME_VAR, mvnHome);
                processBuilder.environment().put(MVN_HOME_VAR, mvnHome);
                processBuilder.environment().put(MAVEN_HOME_VAR, mvnHome);
            }

            return new MavenCommand(this);
        }

        private static String addMavenOption(String option, String mavenOpts) {
            return mavenOpts == null ? option : mavenOpts.trim() + " " + option;
        }

        private static String removeDebugOption(String mavenOpts) {
            if (mavenOpts != null) {
                if (mavenOpts.contains(DEBUG_OPT_PREFIX)) {
                    mavenOpts = Arrays.stream(mavenOpts.trim().split(" "))
                                      .filter(opt -> !opt.startsWith(DEBUG_OPT_PREFIX))
                                      .collect(Collectors.joining(" "));
                }
            }
            return mavenOpts;
        }

        private void prepare() {
            if (executable == null) {
                requireMavenVersion(requiredMinimumVersion);
            }
            requireNonNull(directory, "directory required");
            requireJavaExecutable();
            if (stdOut == null) {
                stdOut = STDOUT;
            }
            if (stdErr == null) {
                stdErr = RED_STDERR;
            }
        }
    }
}
