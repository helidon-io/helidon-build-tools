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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;

import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A builder for a CDS archive for a Helidon application either as a jar or a module.
 * Assumes that it can cause the application to exit once startup has completed by setting the "exit.on.startup" system property.
 */
public class ClassDataSharing {
    private final Path applicationJar;
    private final String applicationModule;
    private final Path jre;
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
        this.jre = builder.jre;
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
     * Returns the path to the JRE used to build the archive.
     *
     * @return The path.
     */
    public Path jre() {
        return jre;
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

    public static class Builder {
        private static final String FILE_PREFIX = "start";
        private static final String ARCHIVE_NAME = FILE_PREFIX + ".jsa";
        private static final String CLASS_LIST_FILE_SUFFIX = ".classlist";
        private static final String JAR_SUFFIX = ".jar";
        private static final String XSHARE_OFF = "-Xshare:off";
        private static final String XSHARE_DUMP = "-Xshare:dump";
        private static final String XX_DUMP_LOADED_CLASS_LIST = "-XX:DumpLoadedClassList=";
        private static final String XX_SHARED_ARCHIVE_FILE = "-XX:SharedArchiveFile=";
        private static final String XX_SHARED_CLASS_LIST_FILE = "-XX:SharedClassListFile=";
        private static final String EXIT_ON_STARTED = "-Dexit.on.started";
        private static final String LIB_DIR_NAME = "lib";
        private static final String CLASS_SUFFIX = ".class";
        private static final String MODULE_INFO_NAME = "module-info";
        private static final String BEAN_ARCHIVE_SCANNER = "org/jboss/weld/environment/deployment/discovery/BeanArchiveScanner";
        private Path jre;
        private String archiveDir;
        private String applicationModule;
        private Path mainJar;
        private Path classListFile;
        private Path archiveFile;
        private List<String> classList;
        private boolean createArchive;
        private Path weldJrtJar;
        private String target;
        private String targetOption;
        private String targetDescription;
        private boolean logOutput;
        private List<String> jvmOptions;
        private List<String> args;

        private Builder() {
            this.createArchive = true;
            this.archiveDir = LIB_DIR_NAME;
            this.jvmOptions = emptyList();
            this.args = emptyList();
        }

        /**
         * Sets the path to the JRE to use when building the archive.
         *
         * @param jre The path.
         * @return The builder.
         */
        public Builder jre(Path jre) {
            this.jre = jre;
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
                this.mainJar = assertJar(jre.resolve(mainJar));
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
         * Sets the path to the {@code helidon-weld-jrt.jar} file. For MP apps, this will
         * insert the class names from this jar into the class list so that they will be
         * included in the CDS archive.
         *
         * @param weldJrtJar The path.
         * @return The builder.
         */
        public Builder weldJrtJar(Path weldJrtJar) {
            this.weldJrtJar = weldJrtJar == null ? null : assertJar(weldJrtJar);
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
         * Build the instance.
         *
         * @return The instance.
         * @throws Exception If an error occurs.
         */
        public ClassDataSharing build() throws Exception {
            requireNonNull(jre, "java home required");
            if (mainJar == null && applicationModule == null) {
                throw new IllegalStateException("Either application jar or module name required");
            } else if (mainJar != null && applicationModule != null) {
                throw new IllegalStateException("Cannot specify both application jar and module name");
            } else if (mainJar != null) {
                this.targetOption = "-jar";
                this.target = mainJar.toString();
                this.targetDescription = mainJar.getFileName().toString();
            } else {
                this.targetOption = "-m";
                this.target = applicationModule;
                this.targetDescription = "module " + target + " in " + jre;
            }

            if (classListFile == null) {
                this.classListFile = tempFile(CLASS_LIST_FILE_SUFFIX);
                this.classList = buildClassList();
            } else {
                this.classList = loadClassList();
            }

            updateClassList();

            if (createArchive) {
                if (archiveFile == null) {
                    archiveFile = assertDir(jre.resolve(archiveDir)).resolve(ARCHIVE_NAME);
                }
                buildCdsArchive();
            }

            return new ClassDataSharing(this);
        }

        private void updateClassList() throws IOException {
            if (weldJrtJar != null) {
                final int beanArchiveScannerIndex = classList.indexOf(BEAN_ARCHIVE_SCANNER);
                if (beanArchiveScannerIndex > 0) {
                    try (final JarFile jar = new JarFile(weldJrtJar.toFile())) {
                        final List<String> classes = new ArrayList<>();
                        final Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            final JarEntry entry = entries.nextElement();
                            final String name = entry.getName();
                            if (name.endsWith(CLASS_SUFFIX) && !name.startsWith(MODULE_INFO_NAME)) {
                                classes.add(name.substring(0, name.length() - CLASS_SUFFIX.length()));
                            }
                        }
                        classList.addAll(beanArchiveScannerIndex, classes);
                    }
                } else {
                    Log.warn("weldJrtJar provided but %s not found", BEAN_ARCHIVE_SCANNER);
                }
            }
        }

        private List<String> buildClassList() throws Exception {
            execute("Building startup class list for " + targetDescription,
                    XSHARE_OFF, XX_DUMP_LOADED_CLASS_LIST + classListFile);
            return loadClassList();
        }

        private void buildCdsArchive() throws Exception {
            execute("Building CDS archive for " + targetDescription,
                    XSHARE_DUMP, XX_SHARED_ARCHIVE_FILE + archiveFile, XX_SHARED_CLASS_LIST_FILE + classListFile);
        }

        private List<String> loadClassList() throws IOException {
            return Files.readAllLines(classListFile);
        }

        private void execute(String action, String... jvmArgs) throws Exception {
            final ProcessBuilder builder = new ProcessBuilder();
            final List<String> command = new ArrayList<>();

            command.add(javaPath().toString());
            command.addAll(jvmOptions);
            command.add(EXIT_ON_STARTED);
            command.addAll(Arrays.asList(jvmArgs));
            command.add(targetOption);
            command.add(target);
            command.addAll(args);
            builder.command(command);

            builder.directory(jre.toFile());

            ProcessMonitor.newMonitor(action, builder, logOutput).run();
        }

        private Path javaPath() {
            return JavaRuntime.javaCommand(jre);
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
            final String fileName = assertFile(path).getFileName().toString();
            if (!fileName.endsWith(JAR_SUFFIX)) {
                throw new IllegalArgumentException(path + " is not a jar");
            }
            return path;
        }
    }
}
