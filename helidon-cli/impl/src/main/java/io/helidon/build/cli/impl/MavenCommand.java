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

package io.helidon.build.cli.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.build.cli.harness.CommandContext.Verbosity;
import io.helidon.build.util.AnsiConsoleInstaller;
import io.helidon.build.util.Constants;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.build.util.Style;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.Style.Bold;
import static java.util.Objects.requireNonNull;

/**
 * Executes maven in a separate process.
 */
public class MavenCommand {
    private final String name;
    private final ProcessBuilder processBuilder;
    private final int maxWaitSeconds;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;
    private final Predicate<String> filter;
    private final Function<String, String> transform;


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
    }

    /**
     * Executes the command.
     */
    public void execute() {
        try {
            // Fork process and wait for its completion
            if (name != null) {
                Log.info("%s", Bold.apply(name));
            }
            ProcessMonitor processMonitor = ProcessMonitor.builder()
                                                          .processBuilder(processBuilder)
                                                          .stdOut(stdOut)
                                                          .stdErr(stdErr)
                                                          .filter(filter)
                                                          .transform(transform)
                                                          .capture(false)
                                                          .build()
                                                          .start();
            processMonitor.waitForCompletion(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link MavenCommand} builder.
     */
    public static class Builder {
        private static final String MAVEN_EXEC = Constants.OS.mavenExec();
        private static final String JAVA_HOME = Constants.javaHome();
        private static final String JAVA_HOME_BIN = JAVA_HOME + File.separator + "bin";
        private static final String PATH_VAR = "PATH";
        private static final String JAVA_HOME_VAR = "JAVA_HOME";
        private static final String MAVEN_OPTS_VAR = "MAVEN_OPTS";
        private static final int SECONDS_PER_YEAR = 365 * 24 * 60 * 60;
        private static final String DEBUG_OPT_PREFIX = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:";

        private String description;
        private Path directory;
        private List<String> mavenArgs;
        private Verbosity verbosity;
        private int debugPort;
        private int maxWaitSeconds;
        private Consumer<String> stdOut;
        private Consumer<String> stdErr;
        private Predicate<String> filter;
        private Function<String, String> transform;
        private ProcessBuilder processBuilder;

        private Builder() {
            this.mavenArgs = new ArrayList<>();
            this.verbosity = Verbosity.NORMAL;
            this.maxWaitSeconds = SECONDS_PER_YEAR;
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
            this.directory = assertDir(directory);
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
         * Sets verbosity level.
         *
         * @param verbosity The level.
         * @return This instance, for chaining.
         */
        public Builder verbosity(Verbosity verbosity) {
            this.verbosity = verbosity;
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
         * Sets the consumer for process {@code stdout} stream.
         *
         * @param stdOut The description.
         * @return This builder.
         */
        public Builder stdOut(Consumer<String> stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        /**
         * Sets the consumer for process {@code stderr} stream.
         *
         * @param stdErr The description.
         * @return This builder.
         */
        public Builder stdErr(Consumer<String> stdErr) {
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
            command.add(MAVEN_EXEC);
            command.addAll(mavenArgs);
            if (verbosity == Verbosity.DEBUG) {
                command.add("--debug");
            }

            // Create the process builder

            processBuilder = new ProcessBuilder().directory(directory.toFile()).command(command);

            // Ensure we use the current java version

            Map<String, String> env = processBuilder.environment();
            String path = JAVA_HOME_BIN + File.pathSeparatorChar + env.get(PATH_VAR);
            env.put(PATH_VAR, path);
            env.put(JAVA_HOME_VAR, JAVA_HOME);

            // Setup MAVEN_OPTS with debugger, if needed

            String mavenOpts = env.get(MAVEN_OPTS_VAR);
            if (debugPort > 0) {
                mavenOpts = addMavenOption(DEBUG_OPT_PREFIX + debugPort, mavenOpts);
            }

            // Set the jansi.force property depending on whether or not it is enabled in this process
            // Must use MAVEN_OPTS since the property is checked prior to maven's processing of the
            // command-line -D options

            mavenOpts = addMavenOption(AnsiConsoleInstaller.forceAnsiArgument(), mavenOpts);

            env.put(MAVEN_OPTS_VAR, mavenOpts);

            return new MavenCommand(this);
        }

        private static String addMavenOption(String option, String mavenOpts) {
            return mavenOpts == null ? option : mavenOpts + " " + option;
        }

        private void prepare() {
            requireNonNull(directory, "directory required");
            if (stdOut == null) {
                stdOut = Builder::printLineOut;
            }
            if (stdErr == null) {
                stdErr = Builder::printRedLineErr;
            }
        }

        private static void printLineOut(String line) {
            System.out.println(line);
        }

        private static void printRedLineErr(String line) {
            System.out.println(Style.Red.apply(line));
        }
    }
}
