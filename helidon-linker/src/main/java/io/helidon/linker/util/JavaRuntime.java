/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.helidon.build.util.FileUtils;
import io.helidon.linker.Jar;
import io.helidon.linker.ResourceContainer;

import static io.helidon.build.util.Constants.OS;
import static io.helidon.build.util.Constants.javaHome;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.fileName;
import static io.helidon.build.util.FileUtils.listFiles;
import static io.helidon.linker.util.Constants.JRI_DIR_SUFFIX;
import static java.util.Objects.requireNonNull;

/**
 * A Java Runtime directory.
 */
public final class JavaRuntime implements ResourceContainer {
    private static final AtomicReference<Path> CURRENT_JAVA_HOME_DIR = new AtomicReference<>();
    private static final String JMODS_DIR = "jmods";
    private static final String JMOD_SUFFIX = ".jmod";
    private static final String JAVA_BASE_JMOD = "java.base.jmod";
    private static final String JMOD_CLASSES_PREFIX = "classes/";
    private static final String JMOD_MODULE_INFO_PATH = JMOD_CLASSES_PREFIX + "module-info.class";
    private static final String FILE_SEP = File.separator;
    private static final String JAVA_EXEC = OS.javaExecutable();
    private static final String JAVA_CMD_PATH = "bin" + FILE_SEP + JAVA_EXEC;
    private final Path javaHome;
    private final Runtime.Version version;
    private final boolean isJdk;
    private final Path jmodsDir;
    private final Map<String, Jar> modules;

    private static Path currentJavaHomeDir() {
        Path result = CURRENT_JAVA_HOME_DIR.get();
        if (result == null) {
            result = Paths.get(javaHome());
            CURRENT_JAVA_HOME_DIR.set(result);
        }
        return result;
    }

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
            final String jarName = fileName(requireNonNull(mainJar));
            final String dirName = jarName.substring(0, jarName.lastIndexOf('.')) + JRI_DIR_SUFFIX;
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
        final Path result = assertDir(jriDirectory);
        if (!isValidJri(jriDirectory)) {
            throw new IllegalArgumentException("Not a valid JRI (" + JAVA_CMD_PATH + " not found): " + jriDirectory);
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
        final Path result = assertDir(jdkDirectory);
        if (!isValidJdk(result)) {
            throw new IllegalArgumentException("Not a valid JDK (" + JAVA_BASE_JMOD + " not found): " + jdkDirectory);
        }
        return result;
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
        final Path currentJavaHome = currentJavaHomeDir();
        final Path jriDir = assertJdk ? assertJdk(currentJavaHome) : assertJri(currentJavaHome);
        return new JavaRuntime(jriDir, null, assertJdk);
    }

    /**
     * Returns a new {@code JavaRuntime} for the given directory, asserting that it is a valid JDK.
     *
     * @param jdkDirectory The directory.
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime jdk(Path jdkDirectory) {
        return new JavaRuntime(assertJdk(jdkDirectory), null, true);
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
        return new JavaRuntime(assertJdk(jdkDirectory), version, true);
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
        final Path jriDir = assertJri(jriDirectory);
        final boolean isJdk = isValidJdk(jriDir);
        return new JavaRuntime(jriDir, version, isJdk);
    }

    private JavaRuntime(Path javaHome, Runtime.Version version, boolean isJdk) {
        this.javaHome = assertDir(javaHome);
        this.jmodsDir = javaHome.resolve(JMODS_DIR);
        if (isJdk) {
            final List<Path> jmodFiles = listFiles(jmodsDir, fileName -> fileName.endsWith(JMOD_SUFFIX));
            this.version = isCurrent() ? Runtime.version() : findVersion();
            this.modules = jmodFiles.stream()
                                    .filter(file -> !Constants.EXCLUDED_MODULES.contains(moduleNameOf(file)))
                                    .collect(Collectors.toMap(JavaRuntime::moduleNameOf, Jar::open));
        } else if (version == null) {
            throw new IllegalArgumentException("Version required in a Java Runtime without 'jmods' dir: " + javaHome);
        } else {
            this.version = version;
            this.modules = Map.of();
        }
        this.isJdk = isJdk;
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
        return Integer.toString(version.feature());
    }

    /**
     * Returns the path from which this instance was built.
     *
     * @return The path.
     */
    public Path path() {
        return javaHome;
    }

    @Override
    public boolean containsResource(String resourcePath) {
        final String path = resourcePath.endsWith(".class") ? JMOD_CLASSES_PREFIX + resourcePath : resourcePath;
        return modules.values().stream().anyMatch(jar -> jar.containsResource(path));
    }

    /**
     * Returns whether or not this instance represents the current JVM.
     *
     * @return {@code true} if this instance is the current JVM.
     */
    public boolean isCurrent() {
        return javaHome.equals(currentJavaHomeDir());
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
     * Returns the {@code .jmod} file for the given name as a {@link Jar}.
     *
     * @param moduleName The module name.
     * @return The jar.
     * @throws IllegalArgumentException If the jar cannot be found.
     */
    public Jar jmod(String moduleName) {
        final Jar result = modules.get(moduleName);
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

    @Override
    public String toString() {
        return (isJdk ? "JDK " : "JRI ") + version;
    }

    private Runtime.Version findVersion() {
        final Path javaBase = assertFile(jmodsDir.resolve(JAVA_BASE_JMOD));
        try (ZipFile zip = new ZipFile(javaBase.toFile())) {
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

    private static boolean isValidJri(Path jriDirectory) {
        final Path javaCommand = jriDirectory.resolve(JAVA_CMD_PATH);
        return Files.isRegularFile(javaCommand);
    }

    private static boolean isValidJdk(Path jdkDirectory) {
        if (isValidJri(jdkDirectory)) {
            final Path jmodsDir = jdkDirectory.resolve(JMODS_DIR);
            final Path javaBase = jmodsDir.resolve(JAVA_BASE_JMOD);
            return Files.isDirectory(jmodsDir) && Files.exists(javaBase);
        }
        return false;
    }

    private static String moduleNameOf(Path jmodFile) {
        final String fileName = fileName(jmodFile);
        return fileName.substring(0, fileName.length() - JMOD_SUFFIX.length());
    }
}
