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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.common.OSType;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.javaExecutableInDir;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.OSType.CURRENT_OS;
import static io.helidon.build.common.PrintStreams.DEVNULL;
import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.linker.Configuration.DEFAULT_MAX_APP_START_SECONDS;
import static io.helidon.build.linker.JavaRuntime.CURRENT_JDK;
import static java.util.Objects.requireNonNull;

/**
 * A builder for a CDS archive for a Helidon application either as a jar or a module.
 * Assumes that it can cause the application to exit once startup has completed by setting the "exit.on.startup" system property.
 */
public final class ClassDataSharing {
    private final Path applicationJar;
    private final Path classListFile;
    private final Path archiveFile;
    private final List<String> classList;
    private final boolean aot;

    /**
     * Returns a new {@link Builder}.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private ClassDataSharing(Builder builder) {
        this.applicationJar = builder.mainJar;
        this.classListFile = builder.classListFile;
        this.archiveFile = builder.archiveFile;
        this.classList = builder.classList;
        this.aot = builder.aot;
    }

    /**
     * Returns the path to the main application jar.
     *
     * @return The path. Will be {@code null} if a moduleName was used.
     */
    public Path applicationJar() {
        return applicationJar;
    }

    /**
     * Returns the path to the list of classes collected during application startup.
     *
     * @return The path.
     */
    public Path classListFile() {
        return classListFile;
    }

    /**
     * Returns the classes collected during application startup.
     *
     * @return The class names.
     */
    public List<String> classList() {
        return classList;
    }

    /**
     * Returns the path to the archive.
     *
     * @return The path.
     */
    public Path archiveFile() {
        return archiveFile;
    }

    /**
     *  True if aot is enabled.
     *
     * @return true if aot is enabled.
     */
    public boolean aot() {
        return aot;
    }

    /**
     * Builder.
     */
    public static final class Builder {
        private Path jri;
        private String applicationModule;
        private boolean aot = false;
        private Path mainJar;
        private Path classListFile;
        private Path archiveFile;
        private List<String> classList;
        private String target;
        private String targetOption;
        private String targetDescription;
        private boolean logOutput;
        private boolean createArchive = true;
        private List<String> jvmOptions = List.of();
        private List<String> args = List.of();
        private String exitOnStartedValue = "!";
        private int maxWaitSeconds = DEFAULT_MAX_APP_START_SECONDS;

        private Builder() {
        }

        /**
         * Sets the path to the JRI to use when building the archive.
         *
         * @param jri The path.
         * @return The builder.
         */
        public Builder jri(Path jri) {
            this.jri = jri;
            javaPath(); // Validate
            return this;
        }

        /**
         * Sets the path to the main application jar.
         *
         * @param mainJar The path to the main application jar.
         * @return The builder.
         */
        public Builder applicationJar(Path mainJar) {
            if (requireNonNull(mainJar).isAbsolute()) {
                this.mainJar = assertJar(mainJar);
            } else {
                this.mainJar = assertJar(jri.resolve(mainJar));
            }
            return this;
        }

        /**
         * Sets the name of the main application module.
         *
         * @param mainModuleName The name of the main application module.
         * @return The builder.
         */
        public Builder applicationModule(String mainModuleName) {
            this.applicationModule = requireNonNull(mainModuleName);
            return this;
        }

        /**
         * Sets JVM options to use when starting the application.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder jvmOptions(List<String> jvmOptions) {
            if (isValid(jvmOptions)) {
                this.jvmOptions = jvmOptions;
            }
            return this;
        }

        /**
         * Sets arguments to use when starting the application.
         *
         * @param args The arguments.
         * @return The builder.
         */
        public Builder args(List<String> args) {
            if (isValid(args)) {
                this.args = args;
            }
            return this;
        }

        /**
         * Sets the path of the CDS archive file to create.
         *
         * @param archiveFile The path.
         * @return The builder.
         */
        public Builder archiveFile(Path archiveFile) {
            this.archiveFile = requireNonNull(archiveFile);
            return this;
        }

        /**
         * Sets whether to create the CDS archive. Defaults to {@code true}.
         *
         * @param createArchive {@code true} if the archive should be created.
         * @return The builder.
         */
        public Builder createArchive(boolean createArchive) {
            this.createArchive = createArchive;
            return this;
        }

        /**
         * Sets whether or not to enable Aot features. Default is false.
         *
         * @param aot {@code true} if Aot features should be used.
         * @return The builder.
         */
        public Builder aot(boolean aot) {
            this.aot = aot;
            return this;
        }

        /**
         * Sets whether to output from the build process(es) should be logged.
         * Defaults to {@code false} and will include the output in any exception message.
         *
         * @param logOutput {@code true} if output should be logged.
         * @return The builder.
         */
        public Builder logOutput(boolean logOutput) {
            this.logOutput = logOutput;
            return this;
        }

        /**
         * Sets the maximum number of seconds to wait for completion.
         *
         * @param maxWaitSeconds The number of seconds.
         * @return The builder.
         */
        public Builder maxWaitSeconds(int maxWaitSeconds) {
            this.maxWaitSeconds = maxWaitSeconds;
            return this;
        }

        /**
         * Sets the path of the class list file to use. One is generated if not provided.
         *
         * @param classListFile The path.
         * @return The builder.
         */
        public Builder classListFile(Path classListFile) {
            this.classListFile = requireFile(classListFile);
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
         * Build the instance.
         *
         * @return The instance.
         * @throws Exception If an error occurs.
         */
        public ClassDataSharing build() throws Exception {
            requireNonNull(jri, "java home required");
            if (mainJar == null && applicationModule == null) {
                throw new IllegalStateException("Either application jar or module name required");
            } else if (mainJar != null && applicationModule != null) {
                throw new IllegalStateException("Cannot specify both application jar and module name");
            } else if (mainJar != null) {
                this.targetOption = "-jar";
                // Note that for CDS archives to work correctly, the path used at runtime must be the
                // same as that used here. Make this path relative to the JRI so that it can be moved
                // around and still function.
                this.target = jri.relativize(mainJar).toString();
                this.targetDescription = fileName(mainJar);
            } else {
                this.targetOption = "-m";
                this.target = applicationModule;
                this.targetDescription = "module " + target + " in " + jri;
            }

            if (aot) {
                if (createArchive) {
                    if (archiveFile == null) {
                        archiveFile = requireDirectory(jri.resolve("lib")).resolve("start.aot");
                    }
                }
                buildAotCache();
            } else {
                if (classListFile == null) {
                    this.classListFile = tempFile();
                    this.classList = buildClassList();
                } else {
                    this.classList = loadClassList();
                }

                if (createArchive) {
                    if (archiveFile == null) {
                        archiveFile = requireDirectory(jri.resolve("lib")).resolve("start.jsa");
                    }
                    buildCdsArchive();
                }
            }

            return new ClassDataSharing(this);
        }

        private List<String> buildClassList() throws Exception {
            execute("Creating startup class list for " + targetDescription,
                    "-Xshare:off",
                    "-XX:DumpLoadedClassList=" + classListFile,
                    "-Dfile.encoding=UTF-8");
            return loadClassList();
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void buildCdsArchive() throws Exception {
            String action = "Creating Class Data Sharing archive " + archiveFile + " for " + targetDescription;
            if (CURRENT_JDK.cdsRequiresUnlock()) {
                execute(action,
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-Xshare:dump",
                        "-XX:SharedArchiveFile=" + archiveFile,
                        "-XX:SharedClassListFile=" + classListFile,
                        "-Dfile.encoding=UTF-8");
            } else {
                execute(action,
                        "-Xshare:dump",
                        "-XX:SharedArchiveFile=" + archiveFile,
                        "-XX:SharedClassListFile=" + classListFile,
                        "-Dfile.encoding=UTF-8");
            }
            if (CURRENT_OS == OSType.Windows) {
                // Try to make the archive file writable so that a second run can delete the image
                jri.resolve(archiveFile).toFile().setWritable(true);
            }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void buildAotCache() throws Exception {
           Path cachePath = jri.resolve(archiveFile);
           final String action = "Creating AOTCache " + cachePath + " for " + targetDescription;
           execute(action,
                   "-Xlog:aot",
                   "-XX:AOTCacheOutput=" + cachePath,
                   "-Dfile.encoding=UTF-8");

           if (CURRENT_OS == OSType.Windows) {
                // Try to make the archive file writable so that a second run can delete the image
                jri.resolve(cachePath).toFile().setWritable(true);
           }
       }

        private List<String> loadClassList() throws IOException {
            return Files.readAllLines(classListFile).stream()
                    .filter(s -> !s.startsWith("#"))
                    .map(s -> s.split("\\s+")[0])
                    .collect(Collectors.toList());
        }

        private void execute(String action, String... jvmArgs) throws Exception {
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> command = new ArrayList<>();

            command.add(javaPath().toString());
            command.addAll(jvmOptions);
            command.add("-Dexit.on.started=" + exitOnStartedValue);
            command.addAll(Arrays.asList(jvmArgs));
            command.add(targetOption);
            command.add(target);
            command.addAll(args);
            processBuilder.command(command);

            processBuilder.directory(jri.toFile());

            PrintStream stdOut;
            PrintStream stdErr;
            if (logOutput) {
                stdOut = PrintStreams.apply(STDOUT, LogFormatter.of(LogLevel.DEBUG));
                stdErr = PrintStreams.apply(STDERR, LogFormatter.of(LogLevel.WARN));
            } else {
                stdOut = DEVNULL;
                stdErr = DEVNULL;
            }
            ProcessMonitor.builder()
                          .description(action)
                          .processBuilder(processBuilder)
                          .stdOut(stdOut)
                          .stdErr(stdErr)
                          .filter(Builder::filter)
                          .build()
                          .execute(maxWaitSeconds, TimeUnit.SECONDS);
        }

        private static boolean filter(String line) {
            return !line.startsWith("skip writing class") && !line.startsWith("Preload Warning: Cannot find");
        }

        private Path javaPath() {
            return requireFile(javaExecutableInDir(jri));
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        private static Path tempFile() throws IOException {
            //noinspection SpellCheckingInspection
            Path tempFile = Files.createTempFile("start", ".classlist");
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }

        private static Path assertJar(Path path) {
            final String fileName = fileName(requireFile(path));
            if (!fileName.endsWith(".jar")) {
                throw new IllegalArgumentException(path + " is not a jar");
            }
            return path;
        }
    }
}
