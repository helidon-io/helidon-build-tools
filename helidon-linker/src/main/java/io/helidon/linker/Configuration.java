/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.List;

import io.helidon.linker.util.Constants;
import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.SystemLogWriter;

import static io.helidon.linker.util.FileUtils.assertFile;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Linker configuration.
 */
public final class Configuration {
    private final JavaRuntime jdk;
    private final Path mainJar;
    private final List<String> defaultJvm;
    private final List<String> defaultArgs;
    private final List<String> defaultDebug;
    private final Path jriDirectory;
    private final boolean verbose;
    private final boolean stripDebug;
    private final boolean cds;
    private final boolean test;

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
        this.defaultJvm = builder.defaultJvm;
        this.defaultArgs = builder.defaultArgs;
        this.defaultDebug = builder.defaultDebug;
        this.jriDirectory = builder.jriDirectory;
        this.verbose = builder.verbose;
        this.stripDebug = builder.stripDebug;
        this.cds = builder.cds;
        this.test = builder.test;
    }

    /**
     * Returns the JDK from which to create the JRI.
     *
     * @return The {@link JavaRuntime}.
     */
    public JavaRuntime jdk() {
        return jdk;
    }

    /**
     * Returns the directory at which to create the JRI.
     *
     * @return The path, guaranteed to not exist.
     */
    public Path jriDirectory() {
        return jriDirectory;
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
     * Returns the default JVM debug options to use when starting the application with {@code --debug}.
     *
     * @return The options.
     */
    public List<String> defaultDebugOptions() {
        return defaultDebug;
    }

    /**
     * Returns the default JVM options to use when starting the application.
     *
     * @return The options.
     */
    public List<String> defaultJvmOptions() {
        return defaultJvm;
    }

    /**
     * Returns the arguments to use when starting the application.
     *
     * @return The arguments.
     */
    public List<String> defaultArgs() {
        return defaultArgs;
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
     * Returns whether or not to test the start script.
     *
     * @return {@code true} if the start script should be tested.
     */
    public boolean test() {
        return test;
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
    public static final class Builder {
        static final String DEFAULT_DEBUG = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005";
        private JavaRuntime jdk;
        private Path mainJar;
        private List<String> defaultJvm;
        private List<String> defaultArgs;
        private List<String> defaultDebug;
        private Path jriDirectory;
        private boolean replace;
        private boolean verbose;
        private boolean stripDebug;
        private boolean cds;
        private boolean test;
        private Log.Writer logWriter;

        private Builder() {
            defaultJvm = emptyList();
            defaultArgs = emptyList();
            defaultDebug = List.of(DEFAULT_DEBUG);
            cds = true;
            test = true;
        }

        /**
         * Set configuration from command line arguments.
         *
         * @param args The arguments: [options] path-to-main-jar. Options:
         * <pre>
         *     --defaultJvmOptions options    Default JVM options to use when starting the application.
         *     --defaultDebugOptions options  Default JVM debug options to use when starting the application with {@code --debug}.
         *     --defaultArgs args             Default arguments to use when starting the application.
         *     --jri directory                The directory at which to create the JRI.
         *     --replace                      Delete the JRI directory if it exists.
         *     --skipCds                      Do not create a CDS archive.
         *     --skipTest                     Do not test the start script.
         *     --verbose                      Log detail messages.
         *     --stripDebug                   Strip debug information from JDK classes. Defaults to false.
         * </pre>
         * @return The builder.
         */
        public Builder commandLine(String... args) {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.startsWith("--")) {
                    if (arg.equalsIgnoreCase("--jri")) {
                        jriDirectory(Paths.get(argAt(++i, args)));
                    } else if (arg.equalsIgnoreCase("--defaultJvmOptions")) {
                        defaultJvmOptions(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--defaultDebugOptions")) {
                        defaultDebugOptions(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--defaultArgs")) {
                        defaultArgs(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--replace")) {
                        replace(true);
                    } else if (arg.equalsIgnoreCase("--skipCds")) {
                        cds(false);
                    } else if (arg.equalsIgnoreCase("--skipTest")) {
                        test(false);
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
         * Sets default JVM options to use when starting the application.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder defaultJvmOptions(String jvmOptions) {
            defaultJvmOptions(toList(jvmOptions));
            return this;
        }

        /**
         * Sets default JVM options to use when starting the application.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder defaultJvmOptions(List<String> jvmOptions) {
            if (isValid(jvmOptions)) {
                this.defaultJvm = split(jvmOptions);
            }
            return this;
        }

        /**
         * Sets default arguments to use when starting the application.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder defaultArgs(String args) {
            defaultArgs(toList(args));
            return this;
        }

        /**
         * Sets default arguments to use when starting the application.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder defaultArgs(List<String> args) {
            if (isValid(args)) {
                this.defaultArgs = split(args);
            }
            return this;
        }

        /**
         * Sets default JVM debug options to use when starting the application with {@code --debug}.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder defaultDebugOptions(String debugOptions) {
            defaultDebugOptions(toList(debugOptions));
            return this;
        }

        /**
         * Sets default JVM debug options to use when starting the application with {@code --debug}.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder defaultDebugOptions(List<String> debugOptions) {
            if (isValid(debugOptions)) {
                this.defaultDebug = split(debugOptions);
            }
            return this;
        }

        /**
         * Sets the directory at which to create the JRI. If not provided, will be created in
         * a subdirectory of the current working directory, with a name based on the {@link #mainJar}.
         *
         * @param jriDirectory The directory. May be {@code null}.
         * @return The builder.
         */
        public Builder jriDirectory(Path jriDirectory) {
            this.jriDirectory = jriDirectory;
            return this;
        }

        /**
         * Sets whether or not to delete the {@code jriDirectory} if it exists. Defaults to {@code false}.
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
         * Sets whether or not to test the start script. Defaults to {@code true}.
         *
         * @param test {@code true} if the start script should be tested.
         * @return The builder.
         */
        public Builder test(boolean test) {
            this.test = test;
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
            jdk = JavaRuntime.current(true);
            if (jdk.version().major() < Constants.MINIMUM_JDK_VERSION) {
                throw new IllegalArgumentException(jdk + " is an unsupported version,"
                                                   + Constants.MINIMUM_JDK_VERSION + " or higher required");
            }
            jriDirectory = JavaRuntime.prepareJriDirectory(jriDirectory, mainJar, replace);
            if (logWriter == null) {
                logWriter = SystemLogWriter.create(verbose ? Log.Level.DEBUG : Log.Level.INFO);
            }
            Log.setWriter(logWriter);
            return new Configuration(this);
        }

        private static List<String> toList(String value) {
            if (value != null && !value.isEmpty()) {
                return Arrays.asList(value.split(" "));
            } else {
                return null;
            }
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        private static List<String> split(List<String> list) {
            if (list.size() == 1) {
                return Arrays.asList(list.get(0).split(" "));
            } else {
                return list;
            }
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
