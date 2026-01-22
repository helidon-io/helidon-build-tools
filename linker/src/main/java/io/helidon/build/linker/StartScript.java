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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.helidon.build.common.InputStreams;
import io.helidon.build.common.OSType;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.common.FileUtils.lastModifiedSeconds;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.OSType.CURRENT_OS;
import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.linker.JavaRuntime.CURRENT_JDK;
import static java.util.Objects.requireNonNull;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {

    private static final String SCRIPT_FILE_NAME = CURRENT_OS.withScriptExtension("start");
    private static final String EOL = System.lineSeparator();

    private final Path installDirectory;
    private final Path scriptFile;
    private final String script;
    private final int maxAppStartSeconds;

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
        this.maxAppStartSeconds = builder.maxAppStartSeconds;
    }

    /**
     * Returns the installation directory.
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
            Files.copy(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), scriptFile);
            if (CURRENT_OS.isPosix()) {
                Files.setPosixFilePermissions(scriptFile, Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE
                ));
            }
            return scriptFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Execute the script with the given arguments.
     *
     * @param transform the output transform.
     * @param args      The arguments.
     * @throws RuntimeException If the process fails.
     */
    public void execute(Function<String, String> transform, String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        Path root = requireNonNull(requireNonNull(scriptFile.getParent()).getParent());
        if (CURRENT_OS.scriptExecutor() != null) {
            command.add(CURRENT_OS.scriptExecutor());
        }
        command.add(scriptFile.toString());
        command.addAll(Arrays.asList(args));
        Log.debug("Commands: %s", command.toString());
        processBuilder.command(command);
        processBuilder.directory(root.toFile());
        ProcessMonitor monitor = ProcessMonitor.builder()
                .processBuilder(processBuilder)
                .stdOut(PrintStreams.apply(STDOUT, LogFormatter.of(LogLevel.INFO)))
                .stdErr(PrintStreams.apply(STDERR, LogFormatter.of(LogLevel.WARN)))
                .transform(transform)
                .capture(true)
                .build();
        try {
            monitor.execute(maxAppStartSeconds, TimeUnit.SECONDS);
            checkWindowsExecutionPolicyError(monitor, false);
        } catch (ProcessMonitor.ProcessFailedException e) {
            checkWindowsExecutionPolicyError(e.monitor(), true);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkWindowsExecutionPolicyError(ProcessMonitor monitor, boolean failed) {
        if (CURRENT_OS == OSType.Windows) {

            // We might have silently failed (but with warnings), and we have to deal with output that
            // is split across lines, so join stderr output

            String stdErr = String.join(" ", monitor.stdErr());
            if (failed || stdErr.contains("FullyQualifiedErrorId")) {

                // Add help message if this is the execution policy error
                if (stdErr.contains("UnauthorizedAccess")
                    && stdErr.contains("Execution")
                    && stdErr.contains("Policies")) {

                    //noinspection SpellCheckingInspection
                    throw new RuntimeException(RichTextRenderer.render(
                            "Generated %s script failed."
                            + "%n%n"
                            + "$(bold To enable script execution, run the following command: )"
                            + "%n%n"
                            + "    $(bold,bright,yellow powershell Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy "
                            + "RemoteSigned)"
                            + "%n%n"
                            + "$(bold and answer 'Y' if prompted.)",
                            scriptFile.getFileName()));
                } else {
                    throw new RuntimeException(String.format("Generated %s script failed.", scriptFile.getFileName()));
                }
            }
        }
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

        /**
         * The command.
         */
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
         * Returns whether CDS is installed.
         *
         * @return {@code true} if installed.
         */
        boolean cdsInstalled();

        /**
         * Returns whether we should use AOT Cache
         *
         * @return {@code true} if use AOT Cache
         */
        boolean useAot();

        /**
         * Returns whether debug support is installed.
         *
         * @return {@code true} if installed.
         */
        boolean debugInstalled();

        /**
         * Returns the {@code -Dexit.on.started} property value.
         *
         * @return The value.
         */
        String exitOnStartedValue();

        /**
         * Returns whether CDS requires the unlock option.
         *
         * @return {@code true} if required.
         */
        default boolean cdsRequiresUnlock() {
            return cdsInstalled() && CURRENT_JDK.cdsRequiresUnlock();
        }

        /**
         * Returns whether CDS supports copying the image.
         *
         * @return {@code true} if supported.
         */
        default boolean cdsSupportsImageCopy() {
            return cdsInstalled() && CURRENT_JDK.cdsSupportsImageCopy();
        }

        /**
         * Returns the configuration as a command. Intended as a simple substitute for those
         * cases where a template cannot be created.
         *
         * @return The command.
         */
        default List<String> toCommand() {
            final List<String> command = new ArrayList<>();
            command.add("bin" + File.separator + "java");
            if (cdsInstalled()) {
                if (useAot()) {
                    command.add("-Xlog:aot");
                    command.add("-XX:AotCache=lib" + File.separator + "start.aot");
                } else {
                    if (cdsRequiresUnlock()) {
                        command.add("-XX:+UnlockDiagnosticVMOptions");
                    }
                    command.add("-XX:SharedArchiveFile=lib" + File.separator + "start.jsa");
                    command.add("-Xshare:auto");
                }
            }
            command.addAll(defaultJvmOptions());
            command.add("-jar");
            command.add("app" + File.separator + mainJar().getFileName());
            command.addAll(defaultArgs());
            return command;
        }
    }

    /**
     * Template renderer.
     */
    public interface Template {

        /**
         * Returns the final text rendered using the given configuration.
         *
         * @param config The configuration.
         * @return The rendered text.
         */
        String render(TemplateConfig config);
    }

    /**
     * A {@link Template} that relies on hand-coded modifications rather than on a full-fledged template engine.
     * This approach supports having a template file be a valid script that can be error checked in an IDE.
     */
    public abstract static class SimpleTemplate implements Template {
        private final List<String> template;

        /**
         * Constructor that loads the template from the given resource path.
         *
         * @param templateResourcePath The template.
         */
        protected SimpleTemplate(String templateResourcePath) {
            this(load(templateResourcePath));
        }

        /**
         * Constructor.
         *
         * @param template The template lines.
         */
        protected SimpleTemplate(List<String> template) {
            this.template = template;
        }

        /**
         * Removes any lines that contain the given substring.
         *
         * @param substring The substring.
         */
        protected void removeLines(String substring) {
            removeLines((index, line) -> contains(line, substring, true));
        }

        /**
         * Removes any lines that match the given predicate.
         *
         * @param predicate The predicate.
         */
        protected void removeLines(BiPredicate<Integer, String> predicate) {
            for (int i = template.size() - 1; i >= 0; i--) {
                if (predicate.test(i, template.get(i))) {
                    template.remove(i);
                }
            }
        }

        /**
         * Returns the index of the first line that contains the given substring.
         *
         * @param substring The substring.
         * @return The index.
         * @throws IllegalStateException if no matching line is found.
         */
        protected int indexOf(String substring) {
            return indexOf(0, (index, line) -> contains(line, substring, false));
        }

        /**
         * Returns the index of the first line that matches the given predicate.
         *
         * @param startIndex The start index.
         * @param predicate  The predicate.
         * @return The index.
         * @throws IllegalStateException if no matching line is found.
         */
        protected int indexOf(int startIndex, BiPredicate<Integer, String> predicate) {
            return IntStream.range(startIndex, template.size())
                    .filter(index -> predicate.test(index, template.get(index)))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        }

        /**
         * Replaces the given substring in each line.
         *
         * @param substring   The substring.
         * @param replacement The replacement.
         */
        protected void replace(String substring, String replacement) {
            template.replaceAll(s -> s.replace(substring, replacement));
        }

        /**
         * Returns the last modified time of the given file, in seconds.
         *
         * @param file The file.
         * @return The last modified time.
         */
        protected static String lastModifiedTime(Path file) {
            return Long.toString(lastModifiedSeconds(file));
        }

        @Override
        public String toString() {
            return String.join(EOL, template);
        }

        private static boolean contains(String line, String substring, boolean ignoreCase) {
            return ignoreCase ? line.toLowerCase().contains(substring) : line.contains(substring);
        }

        private static List<String> load(String resourcePath) {
            final InputStream content = SimpleTemplate.class.getClassLoader().getResourceAsStream(resourcePath);
            if (content == null) {
                throw new IllegalStateException(resourcePath + " not found");
            } else {
                try {
                    return InputStreams.toLines(content);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    /**
     * The builder.
     */
    public static final class Builder {
        private Path installHomeDirectory;
        private Path scriptInstallDirectory;
        private Path mainJar;
        private List<String> defaultJvmOptions = List.of();
        private List<String> defaultDebugOptions = List.of(Configuration.DEFAULT_DEBUG);
        private List<String> defaultArgs = List.of();
        private boolean cdsInstalled = true;
        private boolean useAot = true;
        private boolean debugInstalled = true;
        private String exitOnStartedValue = "!";
        private Template template;
        private TemplateConfig config;
        private Path scriptFile;
        private String script;
        private int maxAppStartSeconds = Configuration.DEFAULT_MAX_APP_START_SECONDS;

        private Builder() {
        }

        /**
         * Sets the installation home directory.
         *
         * @param installHomeDirectory The target.
         * @return The builder.
         */
        public Builder installHomeDirectory(Path installHomeDirectory) {
            this.installHomeDirectory = requireDirectory(installHomeDirectory);
            this.scriptInstallDirectory = requireDirectory(installHomeDirectory).resolve("bin");
            return this;
        }

        /**
         * Sets the path to the main jar.
         *
         * @param mainJar The path. May not be {@code null}.
         * @return The builder.
         */
        public Builder mainJar(Path mainJar) {
            this.mainJar = requireFile(mainJar);
            return this;
        }

        /**
         * Sets the default JVM options.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder defaultJvmOptions(List<String> jvmOptions) {
            if (hasContent(jvmOptions)) {
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
            if (hasContent(args)) {
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
            if (hasContent(debugOptions)) {
                this.defaultDebugOptions = debugOptions;
            }
            return this;
        }

        /**
         * Sets whether a CDS archive was installed.
         *
         * @param cdsInstalled {@code true} if installed.
         * @return The builder.
         */
        public Builder cdsInstalled(boolean cdsInstalled) {
            this.cdsInstalled = cdsInstalled;
            return this;
        }

        /**
         * Sets whether or not to use AOT Cache
         *
         * @param useAot {@code true} to use AOT Cache
         * @return The builder.
         */
        public Builder useAot(boolean useAot) {
            this.useAot = useAot;
            return this;
        }

        /**
         * Sets whether a debug classes and module were installed.
         *
         * @param debugInstalled {@code true} if debug is installed.
         * @return The builder.
         */
        public Builder debugInstalled(boolean debugInstalled) {
            this.debugInstalled = debugInstalled;
            return this;
        }

        /**
         * Sets the {@code -Dexit.on.started} property value.
         *
         * @param exitOnStartedValue The value
         * @return The builder.
         */
        public Builder exitOnStartedValue(String exitOnStartedValue) {
            this.exitOnStartedValue = requireNonNull(exitOnStartedValue);
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
         * Sets the maximum number of seconds to wait for the application to start.
         *
         * @param maxAppStartSeconds The number of seconds.
         * @return The builder.
         */
        public Builder maxAppStartSeconds(int maxAppStartSeconds) {
            this.maxAppStartSeconds = maxAppStartSeconds;
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
                throw new IllegalStateException("installHomeDirectory is required");
            }
            if (mainJar == null) {
                throw new IllegalStateException("mainJar is required");
            }
            this.scriptFile = scriptInstallDirectory.resolve(SCRIPT_FILE_NAME);
            this.config = toConfig();
            this.script = template().render(config);
            return new StartScript(this);
        }

        private Template template() {
            if (template == null) {
                if (CURRENT_OS == OSType.Unknown) {
                    throw new PlatformNotSupportedError(config.toCommand());
                } else {
                    return new StartScriptTemplate();
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
                public boolean useAot() {
                    return useAot;
                }

                @Override
                public boolean debugInstalled() {
                    return debugInstalled;
                }

                @Override
                public String exitOnStartedValue() {
                    return exitOnStartedValue;
                }
            };
        }

        private static boolean hasContent(Collection<?> value) {
            return value != null && !value.isEmpty();
        }
    }
}
