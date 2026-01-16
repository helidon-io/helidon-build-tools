/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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

package io.helidon.build.linker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.linker.util.Constants;
import io.helidon.build.linker.util.JavaRuntime;

import static io.helidon.build.common.FileUtils.requireExistent;
import static io.helidon.build.common.FileUtils.requireFile;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Linker configuration.
 */
public final class Configuration {
    private final JavaRuntime jdk;
    private final Path mainJar;
    private final List<String> defaultJvm;
    private final List<String> defaultArgs;
    private final List<String> defaultDebug;
    private final List<String> additionalJlinkArgs;
    private final Set<String> additionalModules;
    private final Path jriDirectory;
    private final boolean verbose;
    private final boolean stripDebug;
    private final boolean cds;
    private final boolean test;
    private final int maxAppStartSeconds;

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
        this.additionalModules = builder.additionalModules;
        this.additionalJlinkArgs = builder.additionalJlinkArgs;
        this.jriDirectory = builder.jriDirectory;
        this.verbose = builder.verbose;
        this.stripDebug = builder.stripDebug;
        this.cds = builder.cds;
        this.test = builder.test;
        this.maxAppStartSeconds = builder.maxAppStartSeconds;
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
     * Returns the additional arguments to use when invoking {@code jlink}.
     *
     * @return The arguments.
     */
    public List<String> additionalJlinkArgs() {
        return additionalJlinkArgs;
    }

    /**
     * Returns modules to use when starting the application.
     *
     * @return the additional modules.
     */
    public Set<String> additionalModules() {
        return additionalModules;
    }

    /**
     * Returns whether to create a CDS archive.
     *
     * @return {@code true} if a CDS archive should be created.
     */
    public boolean cds() {
        return cds;
    }

    /**
     * Returns whether to test the start script.
     *
     * @return {@code true} if the start script should be tested.
     */
    public boolean test() {
        return test;
    }

    /**
     * Returns whether to log detail messages.
     *
     * @return {@code true} if detail messages should be logged.
     */
    public boolean verbose() {
        return verbose;
    }

    /**
     * Returns whether to strip debug information from JDK classes.
     *
     * @return {@code true} if debug information should be stripped.
     */
    public boolean stripDebug() {
        return stripDebug;
    }

    /**
     * Returns the maximum number of seconds to wait for the application to start.
     *
     * @return The number of seconds.
     */
    public int maxAppStartSeconds() {
        return maxAppStartSeconds;
    }

    /**
     * A {@link Configuration} builder.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        static final String DEFAULT_DEBUG = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005";
        static final int DEFAULT_MAX_APP_START_SECONDS = 1000;
        private JavaRuntime jdk;
        private Path mainJar;
        private List<String> defaultJvm;
        private List<String> defaultArgs;
        private List<String> defaultDebug;
        private List<String> additionalJlinkArgs;
        private Set<String> additionalModules;
        private Path jriDirectory;
        private boolean replace;
        private boolean verbose;
        private boolean stripDebug;
        private boolean cds;
        private boolean test;
        private int maxAppStartSeconds;

        private Builder() {
            defaultJvm = emptyList();
            defaultArgs = emptyList();
            defaultDebug = List.of(DEFAULT_DEBUG);
            additionalJlinkArgs = emptyList();
            additionalModules = emptySet();
            maxAppStartSeconds = DEFAULT_MAX_APP_START_SECONDS;
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
         *     --maxAppStartSeconds seconds   The maximum number of seconds to wait for the application to start.
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
                    } else if (arg.equalsIgnoreCase("--additionalJlinkArgs")) {
                        additionalJlinkArgs(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--additionalModules")) {
                        additionalModules(argAt(++i, args));
                    } else if (arg.equalsIgnoreCase("--maxAppStartSeconds")) {
                        maxAppStartSeconds(Integer.parseInt(argAt(++i, args)));
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
                    mainJar(requireExistent(Paths.get(arg)));
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
            this.mainJar = requireFile(mainJar);
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
         * Sets additional arguments to use when invoking {@code jlink}.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder additionalJlinkArgs(String args) {
            additionalJlinkArgs(toList(args));
            return this;
        }

        /**
         * Sets additional arguments to use when invoking {@code jlink}.
         *
         * @param args The args.
         * @return The builder.
         */
        public Builder additionalJlinkArgs(List<String> args) {
            if (isValid(args)) {
                this.additionalJlinkArgs = split(args);
            }
            return this;
        }

        /**
         * Sets default arguments to use when starting the application.
         *
         * @param modules The modules.
         * @return The builder.
         */
        public Builder additionalModules(String modules) {
            additionalModules(toSet(modules));
            return this;
        }

        /**
         * Sets additional modules to use when starting the application.
         *
         * @param modules The modules.
         * @return The builder.
         */
        public Builder additionalModules(Set<String> modules) {
            if (isValid(modules)) {
                this.additionalModules = modules;
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
         * Sets whether to delete the {@code jriDirectory} if it exists. Defaults to {@code false}.
         *
         * @param replace {@code true} if the directory should be deleted if present.
         * @return The builder.
         */
        public Builder replace(boolean replace) {
            this.replace = replace;
            return this;
        }

        /**
         * Sets whether to build a CDS archive. Defaults to {@code true}.
         *
         * @param cds {@code true} if a CDS archive should be created.
         * @return The builder.
         */
        public Builder cds(boolean cds) {
            this.cds = cds;
            return this;
        }

        /**
         * Sets whether to test the start script. Defaults to {@code true}.
         *
         * @param test {@code true} if the start script should be tested.
         * @return The builder.
         */
        public Builder test(boolean test) {
            this.test = test;
            return this;
        }

        /**
         * Sets whether to log detail messages.
         *
         * @param verbose {@code true} if detail messages should be created.
         * @return The builder.
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Sets whether to strip debug information from JDK classes.
         *
         * @param stripDebug {@code true} if debug information should be stripped.
         * @return The builder.
         */
        public Builder stripDebug(boolean stripDebug) {
            this.stripDebug = stripDebug;
            return this;
        }

        /**
         * Sets the maximum number of seconds to wait for application startup.
         *
         * @param maxAppStartSeconds The number of seconds.
         * @return The builder.
         */
        public Builder maxAppStartSeconds(int maxAppStartSeconds) {
            this.maxAppStartSeconds = maxAppStartSeconds;
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
            jdk = JavaRuntime.current();
            if (jdk.version().feature() < Constants.MINIMUM_JDK_VERSION) {
                throw new IllegalArgumentException(jdk + " is an unsupported version,"
                                                   + Constants.MINIMUM_JDK_VERSION + " or higher required");
            }
            if (cds
                && Constants.DOCKER_BUILD
                && jdk.version().feature() < Constants.MINIMUM_DOCKER_JDK_VERSION) {
                throw new IllegalArgumentException("Class Data Sharing cannot be used in Docker with JDK "
                                                   + jdk.version().feature() + ". Use JDK " + Constants.MINIMUM_DOCKER_JDK_VERSION
                                                   + "+ or disable CDS by setting addClassDataSharingArchive to false "
                                                   + "in the plugin configuration.");
            }
            jriDirectory = JavaRuntime.prepareJriDirectory(jriDirectory, mainJar, replace);
            if (verbose) {
                LogLevel.set(LogLevel.DEBUG);
            }
            return new Configuration(this);
        }

        private static List<String> toList(String value) {
            if (Strings.isValid(value)) {
                return Arrays.asList(value.split(" "));
            } else {
                return null;
            }
        }

        private static Set<String> toSet(String value) {
            if (Strings.isValid(value)) {
                return Set.of(value.split(" "));
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
