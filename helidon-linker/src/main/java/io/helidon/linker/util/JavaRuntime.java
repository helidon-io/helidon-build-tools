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

package io.helidon.linker.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.helidon.linker.util.FileUtils.CURRENT_JAVA_HOME_DIR;
import static io.helidon.linker.util.FileUtils.assertDir;
import static io.helidon.linker.util.FileUtils.assertFile;
import static io.helidon.linker.util.FileUtils.listFiles;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * A Java Runtime directory.
 */
public class JavaRuntime {
    private static final String JMODS_DIR = "jmods";
    private static final String JMOD_SUFFIX = ".jmod";
    private static final String JAVA_BASE_JMOD = "java.base.jmod";
    private static final String JMOD_MODULE_INFO_PATH = "classes/module-info.class";
    private static final String JRI_SUFFIX = "-jri";
    private static final String FILE_SEP = File.separator;
    private static final String JAVA_CMD_PATH = "bin" + FILE_SEP + "java";
    private final Path javaHome;
    private final Runtime.Version version;
    private final Path jmodsDir;
    private final Map<String, Path> modules;

    /**
     * Ensures a valid JRI directory path, deleting if required.
     *
     * @param jriDirectory The JRI directory. May be {@code null}.
     * @param mainJar The main jar, used to create a name if {@code jriDirectory} not provided.
     * May not be {@code null}.
     * @param replaceExisting {@code true} if the directory can be deleted if already present.
     * @return The directory.
     * @throws IOException If an error occurs.
     */
    public static Path prepareJriDirectory(Path jriDirectory, Path mainJar, boolean replaceExisting) throws IOException {
        if (jriDirectory == null) {
            final String jarName = requireNonNull(mainJar).getFileName().toString();
            final String dirName = jarName.substring(0, jarName.lastIndexOf('.')) + JRI_SUFFIX;
            jriDirectory = FileUtils.WORKING_DIR.resolve(dirName);
        }
        if (Files.exists(jriDirectory)) {
            if (Files.isDirectory(jriDirectory)) {
                if (replaceExisting) {
                    FileUtils.deleteDirectory(jriDirectory);
                } else {
                    throw new IllegalArgumentException(jriDirectory + " is an existing directory");
                }
            } else {
                throw new IllegalArgumentException(jriDirectory + " is an existing file");
            }
        }
        return jriDirectory;
    }

    /**
     * Asserts that the given directory points to a valid Java Runtime.
     *
     * @param jriDirectory The directory.
     * @return The normalized, absolute directory path.
     * @throws IllegalArgumentException If the directory is not a valid JRI.
     */
    public static Path assertJri(Path jriDirectory) {
        final Path result = FileUtils.assertDir(jriDirectory);
        final Path javaCommand = jriDirectory.resolve(JAVA_CMD_PATH);
        if (!Files.isRegularFile(javaCommand)) {
            throw new IllegalArgumentException("Not a valid JRI (" + javaCommand + " not found): " + jriDirectory);
        }
        return result;
    }

    /**
     * Asserts that the given directory points to a valid Java Runtime containing {@code jmod} files.
     *
     * @param jdkDirectory The directory.
     * @return The normalized, absolute directory path.
     * @throws IllegalArgumentException If the directory is not a valid JDK.
     */
    public static Path assertJdk(Path jdkDirectory) {
        final Path result = assertJri(jdkDirectory);
        final Path jmodsDir = result.resolve(JMODS_DIR);
        final Path javaBase = jmodsDir.resolve(JAVA_BASE_JMOD);
        if (!Files.isDirectory(jmodsDir) || !Files.exists(javaBase)) {
            throw new IllegalArgumentException("Not a valid JDK (" + JAVA_BASE_JMOD + " not found): " + jdkDirectory);
        }
        return jdkDirectory;
    }

    /**
     * Returns the path to the {@code java} executable in the given JRI directory.
     *
     * @param jriDirectory The directory.
     * @return The normalized, absolute directory path.
     * @throws IllegalArgumentException If the directory is not a valid JDK.
     */
    public static Path javaCommand(Path jriDirectory) {
        return assertFile(assertDir(jriDirectory).resolve(JAVA_CMD_PATH));
    }

    /**
     * Returns a new {@code JavaRuntime} for this JVM.
     *
     * @param assertJdk {@code} true if the result must be a valid JDK.
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime current(boolean assertJdk) {
        final Path jriDir = CURRENT_JAVA_HOME_DIR;
        if (assertJdk) {
            assertJdk(jriDir);
        }
        return new JavaRuntime(jriDir, null);
    }

    /**
     * Returns a new {@code JavaRuntime} for the given directory, asserting that it is a valid JDK.
     *
     * @param jdkDirectory The directory.
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime jdk(Path jdkDirectory) {
        return new JavaRuntime(assertJdk(jdkDirectory), null);
    }

    /**
     * Returns a new {@code JavaRuntime} for the given directory, asserting that it is a valid JDK.
     *
     * @param jdkDirectory The directory.
     * @param version The runtime version of the given JDK. Computed if {@code null}.
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime jdk(Path jdkDirectory, Runtime.Version version) {
        return new JavaRuntime(assertJdk(jdkDirectory), version);
    }

    /**
     * Returns a new {@code JavaRuntime} for the given directory.
     *
     * @param jriDirectory The directory.
     * @param version The runtime version of the given JRI. If {@code null}, the version is computed if {@code jmod}
     * files are present otherwise an exception is thrown.
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JRI or the runtime version cannot be computed.
     */
    public static JavaRuntime jri(Path jriDirectory, Runtime.Version version) {
        return new JavaRuntime(jriDirectory, requireNonNull(version));
    }

    private JavaRuntime(Path javaHome, Runtime.Version version) {
        javaCommand(javaHome); // Assert valid.
        this.javaHome = assertDir(javaHome);
        this.jmodsDir = javaHome.resolve(JMODS_DIR);
        if (Files.isDirectory(jmodsDir)) {
            final List<Path> jmodFiles = listFiles(jmodsDir, fileName -> fileName.endsWith(JMOD_SUFFIX));
            this.version = isCurrent() ? Runtime.version() : findVersion();
            this.modules = jmodFiles.stream()
                                    .filter(file -> !Constants.EXCLUDED_MODULES.contains(moduleNameOf(file)))
                                    .collect(Collectors.toMap(JavaRuntime::moduleNameOf, identity()));
        } else if (version == null) {
            throw new IllegalArgumentException("Version required in a Java Runtime without 'jmods' dir: " + javaHome);
        } else {
            this.version = version;
            this.modules = Map.of();
        }
    }

    /**
     * Return the version.
     *
     * @return The version.
     */
    public Runtime.Version version() {
        return version;
    }

    /**
     * Returns the feature version.
     *
     * @return The feature version.
     */
    public String featureVersion() {
        return Integer.toString(version.major());
    }

    /**
     * Returns the path from which this instance was built.
     *
     * @return The path.
     */
    public Path path() {
        return javaHome;
    }

    /**
     * Returns whether or not this instance represents the current JVM.
     *
     * @return {@code true} if this instance is the current JVM.
     */
    public boolean isCurrent() {
        return javaHome.equals(CURRENT_JAVA_HOME_DIR);
    }

    /**
     * Returns the module names.
     *
     * @return The module names. Empty if this instance does not contain {@code .jmod} files.
     */
    public Set<String> moduleNames() {
        return modules.keySet();
    }

    /**
     * Returns the path to the {@code .jmod} file for the given name.
     *
     * @param moduleName The module name.
     * @return The path the the {@code .jmod} file.
     * @throws IllegalArgumentException If the file cannot be found.
     */
    public Path jmodFile(String moduleName) {
        final Path result = modules.get(moduleName);
        if (result == null) {
            throw new IllegalArgumentException("Cannot find .jmod file for module '" + moduleName + "' in " + path());
        }
        return result;
    }

    /**
     * Returns the path to the {@code jmods} directory.
     *
     * @return The path.
     */
    public Path jmodsDir() {
        return requireNonNull(jmodsDir);
    }

    /**
     * Ensure that the given directory exists, creating it if necessary.
     *
     * @param directory The directory. May be relative or absolute.
     * @return The directory.
     * @throws IllegalArgumentException If the directory is absolute but is not within this {@link #path()}.
     */
    public Path ensureDirectory(Path directory) {
        Path relativeDir = requireNonNull(directory);
        if (directory.isAbsolute()) {
            // Ensure that the directory is within our directory.
            relativeDir = path().relativize(directory);
        }
        return FileUtils.ensureDirectory(path().resolve(relativeDir));
    }

    /**
     * Returns the on disk size.
     *
     * @return The size, in bytes.
     * @throws UncheckedIOException If an error occurs.
     */
    public long diskSize() {
        return FileUtils.sizeOf(path());
    }

    private Runtime.Version findVersion() {
        assertHasJavaBaseJmod();
        final Path javaBase = assertFile(jmodsDir.resolve(JAVA_BASE_JMOD));
        try {
            final ZipFile zip = new ZipFile(javaBase.toFile());
            final ZipEntry entry = zip.getEntry(JMOD_MODULE_INFO_PATH);
            if (entry == null) {
                throw new IllegalStateException("Cannot find " + JMOD_MODULE_INFO_PATH + " in " + javaBase);
            }
            final ModuleDescriptor descriptor = ModuleDescriptor.read(zip.getInputStream(entry));
            return Runtime.Version.parse(descriptor.version()
                                                   .orElseThrow(() -> new IllegalStateException("No version in " + javaBase))
                                                   .toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertHasJavaBaseJmod() {
        final Path javaBase = jmodsDir.resolve(JAVA_BASE_JMOD);
        if (!Files.isDirectory(jmodsDir) || !Files.exists(javaBase)) {
            throw new IllegalArgumentException("Not a valid JDK (" + JAVA_BASE_JMOD + " not found): " + javaHome);
        }
    }

    private static String moduleNameOf(Path jmodFile) {
        final String fileName = fileNameOf(jmodFile);
        return fileName.substring(0, fileName.length() - JMOD_SUFFIX.length());
    }
    
    private static String fileNameOf(Path file) {
        return file.getFileName().toString();
    }
}
