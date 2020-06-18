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

package io.helidon.linker;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;
import io.helidon.linker.util.JavaRuntime;

import static io.helidon.linker.util.Constants.DIR_SEP;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;

/**
 * A Helidon application supporting Java dependency collection and installation into a Java Home.
 * This class assumes that the application was built such that the main jar contains the class path
 * in its manifest.
 */
public final class Application implements ResourceContainer {
    static final Path APP_DIR = Paths.get("app");
    private static final Path ARCHIVE_PATH = Paths.get("lib" + DIR_SEP + "start.jsa");
    private static final String HELIDON_JAR_NAME_PREFIX = "helidon-";
    private static final String MP_FILE_PREFIX = HELIDON_JAR_NAME_PREFIX + "microprofile";
    private static final String VERSION_1_4_1 = "1.4.1";
    private static final String UNKNOWN_VERSION = "0.0.0";
    private static final String SNAPSHOT = "-SNAPSHOT";
    private final Jar mainJar;
    private final List<Jar> classPath;
    private final boolean isMicroprofile;
    private final String version;

    /**
     * Returns a new instance with the given main jar.
     *
     * @param mainJar The main jar.
     * @return The instance.
     */
    public static Application create(Path mainJar) {
        return new Application(mainJar);
    }

    private Application(Path mainJar) {
        this.mainJar = Jar.open(mainJar);
        this.classPath = collectClassPath();
        this.isMicroprofile = classPath.stream().anyMatch(jar -> jar.name().startsWith(MP_FILE_PREFIX));
        this.version = extractHelidonVersion();
    }

    /**
     * Returns the Java module names on which this application depends.
     *
     * @param javaHome The Java Home in which to find the dependencies.
     * @return The module names.
     */
    public Set<String> javaDependencies(JavaRuntime javaHome) {
        return JavaDependencies.collect(jars(), javaHome);
    }

    /**
     * Copy this application into the given Java Runtime Image.
     *
     * @param jri The JRI in which to install this application.
     * @param stripDebug {@code true} if debug information should be stripped from classes.
     * @return The location of the installed application jar.
     */
    public Path install(JavaRuntime jri, boolean stripDebug) {
        final Path appRootDir = mainJar.path().getParent();
        final Path appInstallDir = jri.ensureDirectory(APP_DIR);
        final Path installedAppJar = mainJar.copyToDirectory(appInstallDir, isMicroprofile(), stripDebug);
        classPath.forEach(jar -> {
            final Path relativeDir = appRootDir.relativize(jar.path().getParent());
            final Path installDir = jri.ensureDirectory(appInstallDir.resolve(relativeDir));
            jar.copyToDirectory(installDir, isMicroprofile(), stripDebug);
        });
        return installedAppJar;
    }

    /**
     * Returns the on disk size of the installed application.
     *
     * @param jri The JRI in which the application is installed.
     * @return The size, in bytes.
     * @throws UncheckedIOException If an error occurs.
     */
    public long installedSize(JavaRuntime jri) {
        return FileUtils.sizeOf(jri.path().resolve(APP_DIR));
    }

    /**
     * Returns the relative path at which to create the CDS archive.
     *
     * @return The path.
     */
    public Path archivePath() {
        return ARCHIVE_PATH;
    }

    /**
     * Whether or not this application requires Microprofile.
     *
     * @return {@code true} if Microprofile.
     */
    public boolean isMicroprofile() {
        return isMicroprofile;
    }

    /**
     * Returns the total number of jars in this application.
     *
     * @return The count.
     */
    public int size() {
        return 1 + classPath.size();
    }

    /**
     * Returns the on disk size.
     *
     * @return The size, in bytes.
     * @throws UncheckedIOException If an error occurs.
     */
    public long diskSize() {
        return jars().mapToLong(jar -> FileUtils.sizeOf(jar.path()))
                     .sum();
    }

    /**
     * Returns the Helidon version in use by this application.
     *
     * @return The version.
     */
    public String helidonVersion() {
        return version;
    }

    /**
     * Returns the value required for the {@code -Dexit.on.started} property to trigger.
     *
     * @return The value.
     */
    public String exitOnStartedValue() {
        return exitOnStartedValue(version);
    }

    /**
     * Returns the value required for the {@code -Dexit.on.started} property to trigger on
     * the given Helidon version.
     *
     * @param helidonVersion The Helidon version.
     * @return The value.
     */
    public static String exitOnStartedValue(String helidonVersion) {
        return helidonVersion.equals(VERSION_1_4_1) ? "✅" : "!";
    }

    @Override
    public boolean containsResource(String resourcePath) {
        return jars().anyMatch(jar -> jar.containsResource(resourcePath));
    }

    private Stream<Jar> jars() {
        return Stream.concat(Stream.of(mainJar), classPath.stream());
    }

    private List<Jar> collectClassPath() {
        final List<Jar> classPath = addClassPath(mainJar, new HashSet<>(), new ArrayList<>());
        if (Log.isDebug()) {
            Log.debug("Application classpath:");
            classPath.forEach(jar -> Log.debug("    %s", jar));
        }
        return classPath;
    }

    private List<Jar> addClassPath(Jar jar, Set<Jar> visited, List<Jar> classPath) {
        if (!visited.contains(jar)) {
            if (!jar.equals(mainJar)) {
                classPath.add(jar);
            }
            for (Path path : jar.classPath()) {
                addClassPath(jar, path, visited, classPath);
            }
        }
        return classPath;
    }

    private void addClassPath(Jar jar, Path classPathEntry, Set<Jar> visited, List<Jar> classPath) {
        if (Files.isRegularFile(classPathEntry)) {
            if (Jar.isJar(classPathEntry)) {
                try {
                    final Jar classPathJar = Jar.open(classPathEntry);
                    addClassPath(classPathJar, visited, classPath);
                } catch (Exception e) {
                    Log.warn("Could not open class path jar: %s", classPathEntry);
                }
            } else {
                Log.debug("Ignoring class path entry: %s", classPathEntry);
            }
        } else if (Files.isDirectory(classPathEntry)) {
            // This won't happen from a normal Helidon app build, but handle it for the custom case.
            FileUtils.list(classPathEntry).forEach(path -> addClassPath(jar, path, visited, classPath));
        }
    }

    private String extractHelidonVersion() {
        return classPath.stream()
                        .filter(jar -> jar.name().startsWith(HELIDON_JAR_NAME_PREFIX))
                        .filter(jar -> jar.manifest() != null)
                        .filter(jar -> jar.manifest().getMainAttributes().containsKey(IMPLEMENTATION_VERSION))
                        .map(jar -> jar.manifest().getMainAttributes().getValue(IMPLEMENTATION_VERSION))
                        .findFirst()
                        .orElse(UNKNOWN_VERSION);
    }
}
