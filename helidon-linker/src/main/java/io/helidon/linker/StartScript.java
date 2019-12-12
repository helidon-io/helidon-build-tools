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

import static io.helidon.linker.util.Constants.CDS_REQUIRES_UNLOCK_OPTION;
import static io.helidon.linker.util.Constants.CDS_SUPPORTS_IMAGE_COPY;
import static io.helidon.linker.util.Constants.CDS_UNLOCK_OPTIONS;
import static io.helidon.linker.util.Constants.DIR_SEP;
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
        this.installDirectory = builder.scriptInstallDirectory;
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
     * Template configuration.
     */
    public interface TemplateConfig {
        /**
         * Returns the path to the installation home directory.
         *
         * @return The path.
         */
        Path installHomeDirectory();

        /**
         * Returns the path to the script install directory.
         *
         * @return The path.
         */
        Path scriptInstallDirectory();

        /**
         * Returns the path to the main jar.
         *
         * @return The path.
         */
        Path mainJar();

        /**
         * Returns the default JVM options.
         *
         * @return The options.
         */
        List<String> defaultJvmOptions();

        /**
         * Returns the default debug options.
         *
         * @return The options.
         */
        List<String> defaultDebugOptions();

        /**
         * Returns the default arguments.
         *
         * @return The arguments.
         */
        List<String> defaultArgs();

        /**
         * Returns whether or not CDS is installed.
         *
         * @return {@code true} if installed.
         */
        boolean cdsInstalled();

        /**
         * Returns whether or not debug support is installed.
         *
         * @return {@code true} if installed.
         */
        boolean debugInstalled();

        /**
         * Returns whether or not CDS requires the unlock option.
         *
         * @return {@code true} if required.
         */
        default boolean cdsRequiresUnlock() {
            return cdsInstalled() && CDS_REQUIRES_UNLOCK_OPTION;
        }

        /**
         * Returns whether or not CDS supports copying the image.
         *
         * @return {@code true} if supported.
         */
        default boolean cdsSupportsImageCopy() {
            return cdsInstalled() && CDS_SUPPORTS_IMAGE_COPY;
        }

        /**
         * Returns the configuration as a command. Intended as a simple substitute for those
         * cases where a template cannot be created.
         *
         * @return The command.
         */
        default List<String> toCommand() {
            final List<String> command = new ArrayList<>();
            command.add("bin" + DIR_SEP + "java");
            if (cdsInstalled()) {
                if (cdsRequiresUnlock()) {
                    command.add(CDS_UNLOCK_OPTIONS);
                }
                command.add("-XX:SharedArchiveFile=lib" + DIR_SEP + "start.jsa");
                command.add("-Xshare:auto");
            }
            command.addAll(defaultJvmOptions());
            command.add("-jar");
            command.add("app" + DIR_SEP + mainJar().getFileName());
            command.addAll(defaultArgs());
            return command;
        }
    }

    /**
     * Template resolver.
     */
    public interface Template {

        /**
         * Returns the resolved template.
         *
         * @param config The configuration.
         * @return The resolved template.
         */
        String resolve(TemplateConfig config);
    }

    /**
     * Template that uses hand-coded substitutions rather than rely on a full-
     * fledged template engine. Enables the
     * template file to be a valid script that can be error checked in an IDE.
     */
    public abstract static class SimpleTemplate implements Template {

        /**
         * Returns the last modified time of the given file, in seconds.
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
     * The builder.
     */
    public static class Builder {
        private Path installHomeDirectory;
        private Path scriptInstallDirectory;
        private Path mainJar;
        private List<String> defaultJvmOptions;
        private List<String> defaultDebugOptions;
        private List<String> defaultArgs;
        private boolean cdsInstalled;
        private boolean debugInstalled;
        private Template template;
        private TemplateConfig config;
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
            this.scriptInstallDirectory = assertDir(installHomeDirectory).resolve("bin");
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
         * Sets the template.
         *
         * @param template The template.
         * @return The builder.
         */
        public Builder template(Template template) {
            this.template = template;
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
            this.scriptFile = scriptInstallDirectory.resolve(INSTALL_PATH);
            this.config = toConfig();
            this.script = template().resolve(config);
            return new StartScript(this);
        }

        private Template template() {
            if (template == null) {
                if (OS_TYPE.equals(Windows)) {
                    throw new PlatformNotSupportedError(config.toCommand());
                } else {
                    return new BashStartScriptTemplate();
                }
            } else {
                return template;
            }
        }

        private TemplateConfig toConfig() {
            return new TemplateConfig() {
                @Override
                public Path installHomeDirectory() {
                    return installHomeDirectory;
                }

                @Override
                public Path scriptInstallDirectory() {
                    return scriptInstallDirectory;
                }

                @Override
                public Path mainJar() {
                    return mainJar;
                }

                @Override
                public List<String> defaultJvmOptions() {
                    return defaultJvmOptions;
                }

                @Override
                public List<String> defaultDebugOptions() {
                    return defaultDebugOptions;
                }

                @Override
                public List<String> defaultArgs() {
                    return defaultArgs;
                }

                @Override
                public boolean cdsInstalled() {
                    return cdsInstalled;
                }

                @Override
                public boolean debugInstalled() {
                    return debugInstalled;
                }
            };
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }
    }
}
