/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.SystemLogWriter;

import static io.helidon.linker.util.FileUtils.CURRENT_JAVA_HOME_DIR;
import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * JarsLinker configuration.
 */
public class Configuration {
    private final JavaRuntime jdk;
    private final Path mainJar;
    private final List<String> jvmOptions;
    private final List<String> args;
    private final Path jreDirectory;
    private final boolean verbose;
    private final boolean stripDebug;
    private final boolean cds;

    /**
     * Returns a new configuration builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private Configuration(Builder builder) {
        this.jdk = builder.jdk;
        this.mainJar = builder.mainJar;
        this.jvmOptions = builder.jvmOptions;
        this.args = builder.args;
        this.jreDirectory = builder.jreDirectory;
        this.verbose = builder.verbose;
        this.stripDebug = builder.stripDebug;
        this.cds = builder.cds;
    }

    /**
     * Returns the JDK from which to create the JRE.
     *
     * @return The {@link JavaRuntime}.
     */
    public JavaRuntime jdk() {
        return jdk;
    }

    /**
     * Returns the directory at which to create the JRE.
     *
     * @return The path, guaranteed to not exist.
     */
    public Path jreDirectory() {
        return jreDirectory;
    }

    /**
     * Returns the path to the main jar.
     *
     * @return The path.
     */
    public Path mainJar() {
        return mainJar;
    }

    /**
     * Returns the JVM options to use when starting the application.
     *
     * @return The options.
     */
    public List<String> jvmOptions() {
        return jvmOptions;
    }

    /**
     * Returns the arguments to use when starting the application.
     *
     * @return The arguments.
     */
    public List<String> args() {
        return args;
    }

    /**
     * Returns whether or not to create a CDS archive.
     *
     * @return {@code true} if a CDS archive should be created.
     */
    public boolean cds() {
        return cds;
    }

    /**
     * Returns whether or not to log detail messages.
     *
     * @return {@code true} if detail messages should be logged.
     */
    public boolean verbose() {
        return verbose;
    }

    /**
     * Returns whether or not to strip debug information from JDK classes.
     *
     * @return {@code true} if debug information should be stripped.
     */
    public boolean stripDebug() {
        return stripDebug;
    }

    /**
     * A {@link Configuration} builder.
     */
    public static class Builder {
        private Path mainJar;
        private List<String> jvmOptions;
        private List<String> args;
        private List<String> debugOptions;
        private Path jdkDirectory;
        private Path jreDirectory;
        private boolean replace;
        private boolean verbose;
        private boolean stripDebug;
        private JavaRuntime jdk;
        private boolean cds;
        private Log.Writer logWriter;

        private Builder() {
            jvmOptions = emptyList();
            args = emptyList();
            debugOptions = emptyList();
            jdkDirectory = CURRENT_JAVA_HOME_DIR;
            cds = true;
        }

        /**
         * Set configuration from command line arguments.
         *
         * @param args The arguments: [options] path-to-main-jar. Options:
         * <pre>
         *     --jvmOptions options   JVM options to use when starting the application.
         *     --debugOptions options JVM options to use when starting the application with the {@code --debug} flag.
         *     --args args            Arguments to use when starting the application.
         *     --jdk directory        The JDK directory from which to create the JRE. Defaults to current.
         *     --jre directory        The directory at which to create the JRE.
         *     --replace              Delete the JRE directory if it exists.
         *     --cds                  Create a CDS archive.
         *     --verbose              Log detail messages.
         *     --stripDebug           Strip debug information from JDK classes. Defaults to false.
         * </pre>
         * @return The builder.
         */
        public Builder commandLine(String... args) {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.startsWith("--")) {
                    if (arg.equalsIgnoreCase("--jdk")) {
                        jdkDirectory(Paths.get(argAt(++i, args)));
                    } else if (arg.equalsIgnoreCase("--jre")) {
                        jreDirectory(Paths.get(argAt(++i, args)));
                    } else if (arg.equalsIgnoreCase("--jvmOptions")) {
                        jvmOptions(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--args")) {
                        args(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--replace")) {
                        replace(true);
                    } else if (arg.equalsIgnoreCase("--cds")) {
                        cds(true);
                    } else if (arg.equalsIgnoreCase("--verbose")) {
                        verbose(true);
                    } else if (arg.equalsIgnoreCase("--stripDebug")) {
                        stripDebug(true);
                    } else {
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                } else if (mainJar == null) {
                    mainJar(FileUtils.assertExists(Paths.get(arg)));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            return this;
        }

        /**
         * Sets the main jar.
         *
         * @param mainJar The path to the main jar.
         * @return The builder.
         */
        public Builder mainJar(Path mainJar) {
            this.mainJar = assertFile(mainJar);
            return this;
        }

        /**
         * Sets JVM options to use when starting the application.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder jvmOptions(String jvmOptions) {
            if (jvmOptions != null && !jvmOptions.isBlank()) {
                jvmOptions(Arrays.asList(jvmOptions.split(" ")));
            }
            return this;
        }

        /**
         * Sets JVM options to use when starting the application.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder jvmOptions(List<String> jvmOptions) {
            this.jvmOptions = jvmOptions;
            return this;
        }

        /**
         * Sets arguments to use when starting the application.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder args(String args) {
            if (args != null && !args.isBlank()) {
                args(Arrays.asList(args.split(" ")));
            }
            return this;
        }

        /**
         * Sets arguments to use when starting the application.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder args(List<String> args) {
            this.args = args;
            return this;
        }

        /**
         * Sets JVM options to use when starting the application with the {@code --debug} flag.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder debugOptions(String debugOptions) {
            if (debugOptions != null && !debugOptions.isBlank()) {
                debugOptions(Arrays.asList(debugOptions.split(" ")));
            }
            return this;
        }

        /**
         * Sets JVM options to use when starting the application with the {@code --debug} flag.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder debugOptions(List<String> debugOptions) {
            this.debugOptions = debugOptions;
            return this;
        }

        /**
         * Sets the JDK from which to create the JRE. Defaults to current.
         *
         * @param jdkDirectory The directory. If not {@code null}, must be a valid JDK containing jmod files.
         * @return The builder.
         */
        public Builder jdkDirectory(Path jdkDirectory) {
            if (jdkDirectory != null) {
                this.jdkDirectory = assertDir(jdkDirectory);
            }
            return this;
        }

        /**
         * Sets the directory at which to create the JRE. If not provided, will be created in
         * a subdirectory of the current working directory, with a name based on the {@link #mainJar}.
         *
         * @param jreDirectory The directory. May be {@code null}.
         * @return The builder.
         */
        public Builder jreDirectory(Path jreDirectory) {
            this.jreDirectory = jreDirectory;
            return this;
        }

        /**
         * Sets whether or not to delete the {@code jreDirectory} if it exists. Defaults to {@code false}.
         *
         * @param replace {@code true} if the directory should be deleted if present.
         * @return The builder.
         */
        public Builder replace(boolean replace) {
            this.replace = replace;
            return this;
        }

        /**
         * Sets whether or not to build a CDS archive. Defaults to {@code true}.
         *
         * @param cds {@code true} if a CDS archive should be created.
         * @return The builder.
         */
        public Builder cds(boolean cds) {
            this.cds = cds;
            return this;
        }

        /**
         * Sets whether or not to log detail messages.
         *
         * @param verbose {@code true} if detail messages should be created.
         * @return The builder.
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Sets the log writer.
         *
         * @param logWriter The writer.
         * @return The builder.
         */
        public Builder logWriter(Log.Writer logWriter) {
            this.logWriter = requireNonNull(logWriter);
            return this;
        }

        /**
         * Sets whether or not to strip debug information from JDK classes.
         *
         * @param stripDebug {@code true} if debug information should be stripped.
         * @return The builder.
         */
        public Builder stripDebug(boolean stripDebug) {
            this.stripDebug = stripDebug;
            return this;
        }

        /**
         * Returns the {@link Configuration} instance.
         *
         * @return The instance.
         * @throws IOException If an error occurs.
         */
        public Configuration build() throws IOException {
            if (mainJar == null) {
                throw new IllegalArgumentException("applicationJar required");
            }
            jdk = JavaRuntime.jdk(jdkDirectory);
            jreDirectory = JavaRuntime.prepareJreDirectory(jreDirectory, mainJar, replace);
            if (logWriter == null) {
                logWriter = new SystemLogWriter(verbose ? Log.Level.DEBUG : Log.Level.INFO);
            }
            Log.setWriter(logWriter);
            return new Configuration(this);
        }

        private static String argAt(int index, String[] args) {
            if (index < args.length) {
                return args[index];
            } else {
                throw new IllegalArgumentException(args[index - 1] + ": missing required argument");
            }
        }
    }
}
