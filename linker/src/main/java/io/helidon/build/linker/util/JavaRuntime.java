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

package io.helidon.build.linker.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.OSType;
import io.helidon.build.linker.Application;
import io.helidon.build.linker.Jar;
import io.helidon.build.linker.ResourceContainer;

import static io.helidon.build.common.FileUtils.WORKING_DIR;
import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.findExecutableInPath;
import static io.helidon.build.common.FileUtils.javaHome;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.OSType.Linux;
import static io.helidon.build.linker.util.Constants.JRI_DIR_SUFFIX;
import static java.util.Objects.requireNonNull;

/**
 * Java Runtime metadata.
 */
public final class JavaRuntime implements ResourceContainer {
    private static final OSType OS = OSType.currentOS();
    private static final AtomicReference<Path> CURRENT_JAVA_HOME_DIR = new AtomicReference<>();
    private static final String JAVA_BASE_JMOD = "java.base.jmod";
    private static final String FILE_SEP = File.separator;
    private static final String JAVA_EXEC = OS.javaExecutable();
    private static final String JAVA_CMD_PATH = "bin" + FILE_SEP + JAVA_EXEC;
    private static final String JAVA_MODULE_NAME_PREFIX = "java.";
    private static final String JDK_MODULE_NAME_PREFIX = "jdk.";
    private static final String HELIDON_JAR_NAME_PREFIX = "helidon-";
    private static final String INCOMPLETE_JDK = "The required *.jmod files (e.g. jmods/%s) are missing in this JDK: %s";
    private static final String HELIDON_JRI = "This is a custom Helidon JRI.";
    private static final String CUSTOM_JRI = "This appears to be a custom JRI.";
    private static final boolean OPEN_JDK = System.getProperty("java.vm.name").toLowerCase(Locale.ENGLISH).contains("openjdk");
    private static final String OPEN_JDK_RPM = "RPM based OpenJDK distributions provide *.jmod files in separate "
                                               + "\"java-*-openjdk-jmods\" packages: try 'yum list | grep jmods' to "
                                               + "find the package corresponding to your version.";
    private static final String OPEN_JDK_DEB = "Debian based OpenJDK distributions provide *.jmod files only in the "
                                               + "\"openjdk-*-jdk-headless\" packages.";
    private static final Map<String, String> OPEN_JDK_LINUX_PACKAGING = Map.of("yum", OPEN_JDK_RPM,
                                                                               "apt", OPEN_JDK_DEB,
                                                                               "apt-get", OPEN_JDK_DEB,
                                                                               "dpkg", OPEN_JDK_DEB,
                                                                               "aptitude", OPEN_JDK_DEB);
    private final Path javaHome;
    private final Runtime.Version version;
    private final Map<String, ModuleReference> jdkModules;

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
     */
    public static Path prepareJriDirectory(Path jriDirectory, Path mainJar, boolean replaceExisting) {
        if (jriDirectory == null) {
            final String jarName = fileName(requireNonNull(mainJar));
            final String dirName = jarName.substring(0, jarName.lastIndexOf('.')) + JRI_DIR_SUFFIX;
            jriDirectory = WORKING_DIR.resolve(dirName);
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
     * Asserts that the given directory points to a valid Java Runtime containing {@code jmod} files.
     *
     * @param jdkDirectory The directory.
     * @return The normalized, absolute directory path.
     * @throws IllegalArgumentException If the directory is not a valid JDK.
     */
    public static Path assertJdk(Path jdkDirectory) {
        final Path result = requireDirectory(jdkDirectory);
        if (!isValidJdk(result)) {
            final StringBuilder sb = new StringBuilder().append(String.format(INCOMPLETE_JDK, JAVA_BASE_JMOD, jdkDirectory));
            incompleteJdkDetailMessage(jdkDirectory).ifPresent(detail -> sb.append(". ").append(detail));
            throw new IllegalArgumentException(sb.toString());
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
        return requireFile(requireDirectory(jriDirectory).resolve(JAVA_CMD_PATH));
    }

    /**
     * Returns a new {@code JavaRuntime} for this JVM.
     *
     * @return The new instance.
     * @throws IllegalArgumentException If this JVM is not a valid JDK.
     */
    public static JavaRuntime current() {
        final Path currentJavaHome = currentJavaHomeDir();
        final Path jdkDir = assertJdk(currentJavaHome);
        return new JavaRuntime(jdkDir);
    }

    private JavaRuntime(Path javaHome) {
        this.javaHome = requireDirectory(javaHome);
        this.version = Runtime.version();
        this.jdkModules = ModuleFinder.ofSystem().findAll().stream().collect(Collectors.toMap(t -> t.descriptor().name(), Function.identity()));
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
        return jdkModules.values().stream().anyMatch(ref -> moduleContainsResource(ref, resourcePath));
    }

    private boolean moduleContainsResource(ModuleReference ref, String resourcePath) {
        try (ModuleReader reader = ref.open()) {
            Optional<URI> resource = reader.find(resourcePath);
            return resource.isPresent();
        } catch (IOException ex) {
            System.out.println("XXXXXX moduleContainResource " + ex);
        }
        return false;
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
        return jdkModules.keySet();
    }

    /**
     * Returns the {@code .jmod} file for the given name as a {@link Jar}.
     *
     * @param moduleName The module name.
     * @return The jar.
     * @throws IllegalArgumentException If the jar cannot be found.
     */
    public ModuleDescriptor jmod(String moduleName) {
        final ModuleDescriptor result = jdkModules.get(moduleName).descriptor();
        if (result == null) {
            throw new IllegalArgumentException("Cannot find .jmod file for module '" + moduleName + "' in " + path());
        }
        return result;
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
        return "JDK " + version;
    }

    private static boolean isValidJdk(Path jdkDirectory) {
        final Path javaCommand = jdkDirectory.resolve(JAVA_CMD_PATH);
        return Files.isRegularFile(javaCommand);
    }

    private static Optional<String> incompleteJdkDetailMessage(Path jdkDirectory) {
        if (isHelidonJri(jdkDirectory)) {
            return Optional.of(HELIDON_JRI);
        } else if (isCustomJri(jdkDirectory)) {
            return Optional.of(CUSTOM_JRI);
        } else if (OPEN_JDK && OS == Linux) {
            return OPEN_JDK_LINUX_PACKAGING.entrySet()
                                           .stream()
                                           .filter(e -> findExecutableInPath(e.getKey()).isPresent())
                                           .map(Map.Entry::getValue)
                                           .findFirst();
        }
        return Optional.empty();
    }

    private static boolean isCustomJri(Path jdkDirectory) {
        if (jdkDirectory.equals(currentJavaHomeDir())) {
            return ModuleFinder.ofSystem()
                               .findAll()
                               .stream()
                               .map(ref -> ref.descriptor().name())
                               .anyMatch(moduleName -> !(moduleName.startsWith(JAVA_MODULE_NAME_PREFIX)
                                                         || moduleName.startsWith(JDK_MODULE_NAME_PREFIX)));
        } else {
            return false;
        }
    }

    private static boolean isHelidonJri(Path jdkDirectory) {
        final Path appDir = jdkDirectory.resolve(Application.APP_DIR);
        if (Files.isDirectory(appDir)) {
            return FileUtils.list(appDir, 2)
                            .stream()
                            .anyMatch(path -> path.getFileName().toString().startsWith(HELIDON_JAR_NAME_PREFIX));
        }
        return false;
    }
}
