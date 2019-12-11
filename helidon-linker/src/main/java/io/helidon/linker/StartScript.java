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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;
import io.helidon.linker.util.StreamUtils;

import static io.helidon.linker.util.Constants.CDS_REQUIRES_UNLOCK_OPTION;
import static io.helidon.linker.util.Constants.CDS_SUPPORTS_IMAGE_COPY;
import static io.helidon.linker.util.Constants.CDS_UNLOCK_OPTIONS;
import static io.helidon.linker.util.Constants.DIR_SEP;
import static io.helidon.linker.util.Constants.EOL;
import static io.helidon.linker.util.Constants.OSType.MacOS;
import static io.helidon.linker.util.Constants.OSType.Windows;
import static io.helidon.linker.util.Constants.OS_TYPE;
import static io.helidon.linker.util.FileUtils.assertDir;
import static java.util.Collections.emptyList;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {
    private static final String INSTALL_PATH = "start";
    private final Path installDirectory;
    private final Path scriptFile;
    private final String script;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    static Builder builder() {
        return new Builder();
    }

    private StartScript(Builder builder) {
        this.installDirectory = builder.installDirectory;
        this.scriptFile = builder.scriptFile;
        this.script = builder.script;
    }

    /**
     * Returns the install directory.
     *
     * @return The directory.
     */
    Path installDirectory() {
        return installDirectory;
    }

    /**
     * Install the script.
     *
     * @return The path to the installed script.
     */
    Path install() {
        try {
            Files.copy(new ByteArrayInputStream(script.getBytes()), scriptFile);
            Files.setPosixFilePermissions(scriptFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
            ));
            return scriptFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Execute the script with the given arguments.
     *
     * @param transform the output transform.
     * @param args The arguments.
     * @throws RuntimeException If the process fails.
     */
    public void execute(Function<String, String> transform, String... args) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final List<String> command = new ArrayList<>();
        command.add(scriptFile.toString());
        command.addAll(Arrays.asList(args));
        processBuilder.command(command);
        processBuilder.directory(scriptFile.getParent().getParent().toFile());
        try {
            ProcessMonitor.builder()
                          .processBuilder(processBuilder)
                          .stdOut(Log::info)
                          .stdErr(Log::warn)
                          .transform(transform)
                          .capture(false)
                          .build()
                          .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the script.
     *
     * @return The script.
     */
    public String script() {
        return script;
    }

    /**
     * Returns the path to the script file.
     *
     * @return The path.
     */
    public Path scriptFile() {
        return scriptFile;
    }

    /**
     * Returns the script.
     *
     * @return The script.
     */
    @Override
    public String toString() {
        return script;
    }

    /**
     * Platform not supported error.
     */
    public static final class PlatformNotSupportedError extends IllegalStateException {
        private final List<String> command;

        private PlatformNotSupportedError(List<String> command) {
            this.command = command;
        }

        /**
         * Returns the Java command to execute on this platform.
         *
         * @return The command.
         */
        List<String> command() {
            return command;
        }
    }

    /**
     * The builder.
     */
    public static class Builder {
        private Path installHomeDirectory;
        private Path installDirectory;
        private Path mainJar;
        private List<String> defaultJvmOptions;
        private List<String> defaultDebugOptions;
        private List<String> defaultArgs;
        private boolean cdsInstalled;
        private boolean debugInstalled;
        private Path scriptFile;
        private String script;

        private Builder() {
            this.defaultJvmOptions = emptyList();
            this.defaultDebugOptions = List.of(Configuration.Builder.DEFAULT_DEBUG);
            this.cdsInstalled = true;
            this.debugInstalled = true;
            this.defaultArgs = emptyList();
        }

        /**
         * Sets the install home directory.
         *
         * @param installHomeDirectory The target.
         * @return The builder.
         */
        public Builder installHomeDirectory(Path installHomeDirectory) {
            this.installHomeDirectory = assertDir(installHomeDirectory);
            this.installDirectory = assertDir(installHomeDirectory).resolve("bin");
            return this;
        }

        /**
         * Sets the path to the main jar.
         *
         * @param mainJar The path. May not be {@code null}.
         * @return The builder.
         */
        public Builder mainJar(Path mainJar) {
            this.mainJar = FileUtils.assertFile(mainJar);
            return this;
        }

        /**
         * Sets the default JVM options.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder defaultJvmOptions(List<String> jvmOptions) {
            if (isValid(jvmOptions)) {
                this.defaultJvmOptions = jvmOptions;
            }
            return this;
        }

        /**
         * Sets the default arguments.
         *
         * @param args The arguments.
         * @return The builder.
         */
        public Builder defaultArgs(List<String> args) {
            if (isValid(args)) {
                this.defaultArgs = args;
            }
            return this;
        }

        /**
         * Sets the default debug arguments used when starting the application with the {@code --debug} flag.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder defaultDebugOptions(List<String> debugOptions) {
            if (isValid(debugOptions)) {
                this.defaultDebugOptions = debugOptions;
            }
            return this;
        }

        /**
         * Sets whether or not a CDS archive was installed.
         *
         * @param cdsInstalled {@code true} if installed.
         * @return The builder.
         */
        public Builder cdsInstalled(boolean cdsInstalled) {
            this.cdsInstalled = cdsInstalled;
            return this;
        }

        /**
         * Sets whether or not a debug classes and module were installed.
         *
         * @param debugInstalled {@code true} if debug is installed.
         * @return The builder.
         */
        public Builder debugInstalled(boolean debugInstalled) {
            this.debugInstalled = debugInstalled;
            return this;
        }

        /**
         * Builds and returns the instance.
         *
         * @return The instance.
         * @throws IllegalArgumentException If a script cannot be created for the current platform.
         */
        public StartScript build() {
            if (installHomeDirectory == null) {
                throw new IllegalStateException("installTarget is required");
            }
            if (mainJar == null) {
                throw new IllegalStateException("mainJar is required");
            }
            this.scriptFile = installDirectory.resolve(INSTALL_PATH);
            this.script = template().resolve(this);
            return new StartScript(this);
        }

        private Template template() {
            if (OS_TYPE.equals(Windows)) {
                throw new PlatformNotSupportedError(createCommand());
            } else {
                return new BashTemplate();
            }
        }

        private List<String> createCommand() {
            final List<String> command = new ArrayList<>();
            command.add("bin" + DIR_SEP + "java");
            if (cdsInstalled) {
                if (cdsRequiresUnlock()) {
                    command.add(CDS_UNLOCK_OPTIONS);
                }
                command.add("-XX:SharedArchiveFile=lib" + DIR_SEP + "start.jsa");
                command.add("-Xshare:auto");
            }
            command.addAll(defaultJvmOptions);
            command.add("-jar");
            command.add("app" + DIR_SEP + mainJar.getFileName());
            command.addAll(defaultArgs);
            return command;
        }

        private boolean cdsRequiresUnlock() {
            return cdsInstalled && CDS_REQUIRES_UNLOCK_OPTION;
        }

        private boolean cdsSupportsImageCopy() {
            return cdsInstalled && CDS_SUPPORTS_IMAGE_COPY;
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        /**
         * Template resolver.
         */
        interface Template {

            /**
             * Returns the resolved template.
             *
             * @param config The configuration.
             * @return The resolved template.
             */
            String resolve(Builder config);
        }

        /**
         * Simple template that assumes the template file is a valid script. This approach trades off potential
         * brittleness for template file validation support in an IDE.
         */
        abstract static class SimpleTemplate implements Template {

            /**
             * Returns the last modified time of the given file in seconds.
             *
             * @param file The file.
             * @return The last modified time.
             */
            static String lastModifiedTime(Path file) {
                return Long.toString(FileUtils.lastModifiedTime(file));
            }

            /**
             * Removes lines from the given list that match the given predicate.
             *
             * @param lines The lines.
             * @param predicate The predicate.
             * @return The updated list.
             */
            static List<String> removeLines(List<String> lines, BiPredicate<Integer, String> predicate) {
                for (int i = lines.size() - 1; i >= 0; i--) {
                    if (predicate.test(i, lines.get(i))) {
                        lines.remove(i);
                    }
                }
                return lines;
            }

            /**
             * Returns the first index of the line that contains the given substring.
             *
             * @param lines The lines.
             * @param startIndex The start index.
             * @param substring The substring.
             * @return The index.
             * @throws IllegalStateException if no matching line is found.
             */
            static int indexOf(List<String> lines, int startIndex, String substring) {
                return IntStream.range(startIndex, lines.size())
                                .filter(index -> lines.get(index).contains(substring))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new);
            }

            /**
             * Tests if the given line contains the given substring, ignoring case.
             *
             * @param line The line.
             * @param substring The substring.
             * @return {@code true} if the line contains the substring.
             */
            static boolean containsIgnoreCase(String line, String substring) {
                return line.toLowerCase().contains(substring);
            }
        }

        /**
         * Template for bash script.
         */
        private static class BashTemplate extends SimpleTemplate {
            private static final String TEMPLATE_RESOURCE = "start-template.sh";
            private static final String JAR_NAME_VAR = "<JAR_NAME>";
            private static final String DEFAULT_ARGS_VAR = "<DEFAULT_APP_ARGS>";
            private static final String DEFAULT_JVM_VAR = "<DEFAULT_APP_JVM>";
            private static final String DEFAULT_DEBUG_VAR = "<DEFAULT_APP_DEBUG>";
            private static final String HAS_CDS_VAR = "<HAS_CDS>";
            private static final String HAS_DEBUG_VAR = "<HAS_DEBUG>";
            private static final String CDS_UNLOCK_OPTION_VAR = "<CDS_UNLOCK>";
            private static final String DEFAULT_ARGS_DESC_VAR = "<DEFAULT_APP_ARGS_DESC>";
            private static final String DEFAULT_JVM_DESC_VAR = "<DEFAULT_APP_JVM_DESC>";
            private static final String DEFAULT_DEBUG_DESC_VAR = "<DEFAULT_APP_DEBUG_DESC>";
            private static final String STAT_FORMAT_VAR = "<STAT_FORMAT>";
            private static final String STAT_FORMAT_MAC = "-f %m";
            private static final String STAT_FORMAT_LINUX = "-c %Y";
            private static final String MODULES_TIME_STAMP_VAR = "<MODULES_TIME_STAMP>";
            private static final String JAR_TIME_STAMP_VAR = "<JAR_TIME_STAMP>";
            private static final String MODULES_FILE = "lib/modules";
            private static final String OVERRIDES = "Overrides \\\"${default%s}\\\".";
            private static final String CHECK_TIME_STAMPS = "checkTimeStamps()";
            private static final String CDS_WARNING = "WARNING: CDS";
            private static final String SETS = "Sets default %s.";
            private static final String CDS = "cds";
            private static final String DEBUG = "debug";
            private static final String COPY_INSTRUCTIONS_VAR = "<COPY_INSTRUCTIONS>";
            private static final String NO_CDS = ", use the --noCds option or disable CDS in image generation.";
            private static final String COPY_NOT_SUPPORTED = "Copies are not supported in this Java version; avoid them" + NO_CDS;
            private static final String COPY_SUPPORTED = "Use a timestamp preserving copy option (e.g. 'cp -rp')" + NO_CDS;
            private List<String> template;

            @Override
            public String resolve(Builder config) {
                this.template = load();
                return createScript(config);
            }

            private String createScript(Builder config) {
                final String name = config.mainJar.getFileName().toString();

                final String jvm = String.join(" ", config.defaultJvmOptions);
                final String jvmDesc = description(config.defaultJvmOptions, "JVM options", "Jvm");

                final String args = String.join(" ", config.defaultArgs);
                final String argsDesc = description(config.defaultArgs, "arguments", "Args");

                final List<String> debugOptions = config.debugInstalled ? config.defaultDebugOptions : emptyList();
                final String debug = String.join(" ", debugOptions);
                final String debugDesc = description(debugOptions, "debug options", "Debug");

                final String hasCds = config.cdsInstalled ? "yes" : "";
                final String hasDebug = config.debugInstalled ? "yes" : "";
                final String cdsUnlock = config.cdsRequiresUnlock() ? CDS_UNLOCK_OPTIONS + " " : "";

                final String statFormat = OS_TYPE == MacOS ? STAT_FORMAT_MAC : STAT_FORMAT_LINUX;
                final String modulesModTime = lastModifiedTime(config.installHomeDirectory.resolve(MODULES_FILE));
                final String jarModTime = lastModifiedTime(config.mainJar);
                final String copyInstructions = config.cdsSupportsImageCopy() ? COPY_SUPPORTED : COPY_NOT_SUPPORTED;

                if (!config.cdsInstalled) {
                    removeCheckTimeStampFunction();
                    removeTemplateLines(CDS);
                }

                if (!config.debugInstalled) {
                    removeTemplateLines(DEBUG);
                }

                return String.join(EOL, template)
                             .replace(JAR_NAME_VAR, name)
                             .replace(DEFAULT_JVM_VAR, jvm)
                             .replace(DEFAULT_JVM_DESC_VAR, jvmDesc)
                             .replace(DEFAULT_ARGS_VAR, args)
                             .replace(DEFAULT_ARGS_DESC_VAR, argsDesc)
                             .replace(DEFAULT_DEBUG_VAR, debug)
                             .replace(DEFAULT_DEBUG_DESC_VAR, debugDesc)
                             .replace(HAS_CDS_VAR, hasCds)
                             .replace(HAS_DEBUG_VAR, hasDebug)
                             .replace(CDS_UNLOCK_OPTION_VAR, cdsUnlock)
                             .replace(STAT_FORMAT_VAR, statFormat)
                             .replace(MODULES_TIME_STAMP_VAR, modulesModTime)
                             .replace(JAR_TIME_STAMP_VAR, jarModTime)
                             .replace(COPY_INSTRUCTIONS_VAR, copyInstructions);
            }

            private static String description(List<String> defaults, String description, String varName) {
                if (defaults.isEmpty()) {
                    return String.format(SETS, description);
                } else {
                    return String.format(OVERRIDES, varName);
                }
            }

            private void removeCheckTimeStampFunction() {
                final int startIndex = indexOf(template, 0, CHECK_TIME_STAMPS);
                final int warningIndex = indexOf(template, startIndex + 1, CDS_WARNING);
                final int closingBraceIndex = indexOf(template, warningIndex + 1, "}") + 1; // include empty line
                removeLines(template, (index, line) -> index >= startIndex && index <= closingBraceIndex);
            }

            private void removeTemplateLines(String substring) {
                removeLines(template, (index, line) -> containsIgnoreCase(line, substring));
            }

            private static boolean isComment(String line) {
                final int length = line.length();
                return length > 0 && line.charAt(0) == '#' && (length == 1 || line.charAt(1) != '!');
            }

            private List<String> load() {
                final InputStream content = StartScript.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE);
                if (content == null) {
                    throw new IllegalStateException(TEMPLATE_RESOURCE + " not found");
                } else {
                    try {
                        return removeLines(StreamUtils.toLines(content), (index, line) -> isComment(line));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
    }
}
