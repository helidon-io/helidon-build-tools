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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import io.helidon.linker.util.Constants;
import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;

import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;
import static io.helidon.linker.util.FileUtils.fileName;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A builder for a CDS archive for a Helidon application either as a jar or a module.
 * Assumes that it can cause the application to exit once startup has completed by setting the "exit.on.startup" system property.
 */
public final class ClassDataSharing {
    private final Path applicationJar;
    private final String applicationModule;
    private final Path jri;
    private final Path classListFile;
    private final Path archiveFile;
    private final List<String> classList;

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
        this.applicationModule = builder.applicationModule;
        this.jri = builder.jri;
        this.classListFile = builder.classListFile;
        this.archiveFile = builder.archiveFile;
        this.classList = builder.classList;
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
     * Returns the name of the main application module.
     *
     * @return The name. Will be {@code null} if a jar was used.
     */
    public String applicationModule() {
        return applicationModule;
    }

    /**
     * Returns the path to the JRI used to build the archive.
     *
     * @return The path.
     */
    public Path jri() {
        return jri;
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
     * Builder.
     */
    public static final class Builder {
        private static final String FILE_PREFIX = "start";
        private static final String ARCHIVE_NAME = FILE_PREFIX + ".jsa";
        private static final String CLASS_LIST_FILE_SUFFIX = ".classlist";
        private static final String JAR_SUFFIX = ".jar";
        private static final String XSHARE_OFF = "-Xshare:off";
        private static final String XSHARE_DUMP = "-Xshare:dump";
        private static final String XX_DUMP_LOADED_CLASS_LIST = "-XX:DumpLoadedClassList=";
        private static final String XX_SHARED_ARCHIVE_FILE = "-XX:SharedArchiveFile=";
        private static final String XX_SHARED_CLASS_LIST_FILE = "-XX:SharedClassListFile=";
        private static final String EXIT_ON_STARTED = "-Dexit.on.started=";
        private static final String EXIT_ON_STARTED_VALUE = "!";
        private static final String UTF_8_ENCODING = "-Dfile.encoding=UTF-8";
        private static final String SKIPPED_CLASS_PREFIX = "skip writing class";
        private static final String CANNOT_FIND_PREFIX = "Preload Warning: Cannot find";
        private static final String LIB_DIR_NAME = "lib";
        private Path jri;
        private String archiveDir;
        private String applicationModule;
        private Path mainJar;
        private Path classListFile;
        private Path archiveFile;
        private List<String> classList;
        private boolean createArchive;
        private String target;
        private String targetOption;
        private String targetDescription;
        private boolean logOutput;
        private List<String> jvmOptions;
        private List<String> args;
        private String exitOnStartedValue;

        private Builder() {
            this.createArchive = true;
            this.archiveDir = LIB_DIR_NAME;
            this.jvmOptions = emptyList();
            this.args = emptyList();
            this.exitOnStartedValue = EXIT_ON_STARTED_VALUE;
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
         * @param mainModuleName The the name of the main application module.
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
         * Sets whether or not to create the CDS archive. Defaults to {@code true}.
         *
         * @param createArchive {@code true} if the archive should be created.
         * @return The builder.
         */
        public Builder createArchive(boolean createArchive) {
            this.createArchive = createArchive;
            return this;
        }

        /**
         * Sets whether or not to output from the build process(es) should be logged.
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
         * Sets the path of the class list file to use. One is generated if not provided.
         *
         * @param classListFile The path.
         * @return The builder.
         */
        public Builder classListFile(Path classListFile) {
            this.classListFile = assertFile(classListFile);
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

            if (classListFile == null) {
                this.classListFile = tempFile(CLASS_LIST_FILE_SUFFIX);
                this.classList = buildClassList();
            } else {
                this.classList = loadClassList();
            }

            if (createArchive) {
                if (archiveFile == null) {
                    archiveFile = assertDir(jri.resolve(archiveDir)).resolve(ARCHIVE_NAME);
                }
                buildCdsArchive();
            }

            return new ClassDataSharing(this);
        }

        private List<String> buildClassList() throws Exception {
            execute("Creating startup class list for " + targetDescription,
                    XSHARE_OFF, XX_DUMP_LOADED_CLASS_LIST + classListFile, UTF_8_ENCODING);
            return loadClassList();
        }

        private void buildCdsArchive() throws Exception {
            final String action = "Creating Class Data Sharing archive for " + targetDescription;
            if (Constants.CDS_REQUIRES_UNLOCK_OPTION) {
                execute(action, Constants.CDS_UNLOCK_OPTIONS, XSHARE_DUMP, XX_SHARED_ARCHIVE_FILE + archiveFile,
                        XX_SHARED_CLASS_LIST_FILE + classListFile, UTF_8_ENCODING);
            } else {
                execute(action, XSHARE_DUMP, XX_SHARED_ARCHIVE_FILE + archiveFile,
                        XX_SHARED_CLASS_LIST_FILE + classListFile, UTF_8_ENCODING);
            }
        }

        private List<String> loadClassList() throws IOException {
            return Files.readAllLines(classListFile);
        }

        private void execute(String action, String... jvmArgs) throws Exception {
            final ProcessBuilder processBuilder = new ProcessBuilder();
            final Consumer<String> stdOut = logOutput ? Log::debug : null;
            final Consumer<String> stdErr = logOutput ? Log::warn : null;
            final List<String> command = new ArrayList<>();

            command.add(javaPath().toString());
            command.addAll(jvmOptions);
            command.add(EXIT_ON_STARTED + exitOnStartedValue);
            command.addAll(Arrays.asList(jvmArgs));
            command.add(targetOption);
            command.add(target);
            command.addAll(args);
            processBuilder.command(command);

            processBuilder.directory(jri.toFile());

            ProcessMonitor.builder()
                          .description(action)
                          .processBuilder(processBuilder)
                          .stdOut(stdOut)
                          .stdErr(stdErr)
                          .filter(Builder::filter)
                          .build()
                          .execute();
        }

        private static boolean filter(String line) {
            return !line.startsWith(SKIPPED_CLASS_PREFIX) && !line.startsWith(CANNOT_FIND_PREFIX);
        }

        private Path javaPath() {
            return JavaRuntime.javaCommand(jri);
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        private static Path tempFile(String suffix) throws IOException {
            final File file = File.createTempFile(FILE_PREFIX, suffix);
            file.deleteOnExit();
            return file.toPath();
        }

        private static Path assertJar(Path path) {
            final String fileName = FileUtils.fileName(assertFile(path));
            if (!fileName.endsWith(JAR_SUFFIX)) {
                throw new IllegalArgumentException(path + " is not a jar");
            }
            return path;
        }
    }
}
