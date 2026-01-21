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

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.sizeOf;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;

/**
 * A Helidon application supporting Java dependency collection and installation into a Java Home.
 * This class assumes that the application was built such that the main jar contains the class path
 * in its manifest.
 */
public final class Application implements ResourceContainer {
    /**
     * The relative path of the application directory.
     */
    public static final Path APP_DIR = Paths.get("app");

    private static final Path ARCHIVE_PATH = Paths.get("lib/start.jsa");

    private final Jar mainJar;
    private final List<Jar> classPath;
    private final List<Jar> allJars;
    private final boolean isMicroprofile;
    private final String version;

    /**
     * Returns a new instance with the given Java Home and main jar.
     *
     * @param mainJar The main jar.
     * @return The instance.
     */
    public static Application create(Path mainJar) {
        return new Application(mainJar);
    }

    private Application(Path path) {
        this.mainJar = Jar.open(path);
        this.classPath = classPath(mainJar);
        this.allJars = List.copyOf(concat(mainJar, classPath));
        this.isMicroprofile = isMicroprofile(classPath);
        this.version = extractHelidonVersion(classPath);
    }

    /**
     * Returns the Java module names on which this application depends.
     *
     * @return The module names.
     */
    public Set<String> dependencies() {
        return JavaDependencies.collect(allJars);
    }

    /**
     * Copy this application into the given Java Runtime Image.
     *
     * @param jriDirectory Path to the JRI in which to install this application.
     * @param stripDebug   {@code true} if debug information should be stripped from classes.
     * @return The location of the installed application jar.
     */
    public Path install(Path jriDirectory, boolean stripDebug) {
        Path appRootDir = mainJar.path().getParent();
        Path appInstallDir = ensureDirectory(jriDirectory.resolve(APP_DIR));
        Path installedAppJar = mainJar.copy(appInstallDir, isMicroprofile(), stripDebug);
        for (Jar jar : classPath) {
            Path relativeDir = appRootDir.relativize(jar.path().getParent());
            Path installDir = ensureDirectory(appInstallDir.resolve(relativeDir));
            jar.copy(installDir, isMicroprofile(), stripDebug);
        }
        return installedAppJar;
    }

    /**
     * Returns the on disk size of the installed application.
     *
     * @param jriDirectory Path to The JRI in which the application is installed.
     * @return The size, in bytes.
     * @throws UncheckedIOException If an error occurs.
     */
    public long installedSize(Path jriDirectory) {
        return sizeOf(jriDirectory.resolve(APP_DIR));
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
     * Whether this application requires Microprofile.
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
        return allJars.stream()
                .mapToLong(jar -> sizeOf(jar.path()))
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
        return helidonVersion.equals("1.4.1") ? "✅" : "!";
    }

    @Override
    public boolean containsResource(String resourcePath) {
        return allJars.stream()
                .anyMatch(jar -> jar.containsResource(resourcePath));
    }

    private static boolean isMicroprofile(List<Jar> classPath) {
        return classPath.stream()
                .anyMatch(jar -> jar.name().startsWith("helidon-microprofile"));
    }

    private static String extractHelidonVersion(List<Jar> classPath) {
        return classPath.stream()
                .filter(jar -> jar.name().startsWith("helidon-"))
                .filter(jar -> jar.manifest() != null)
                .filter(jar -> jar.manifest().getMainAttributes().containsKey(IMPLEMENTATION_VERSION))
                .map(jar -> jar.manifest().getMainAttributes().getValue(IMPLEMENTATION_VERSION))
                .findFirst()
                .orElse("0.0.0");
    }

    private static List<Jar> classPath(Jar mainJar) {
        Deque<Path> stack = new ArrayDeque<>(mainJar.classPath());
        Set<Jar> classPath = new LinkedHashSet<>();
        while (!stack.isEmpty()) {
            Path path = stack.pop();
            if (Files.isRegularFile(path)) {
                if (Jar.isJar(path)) {
                    try {
                        Jar jar = Jar.open(path, mainJar.version());
                        if (!jar.equals(mainJar) && classPath.add(jar)) {
                            List<Path> entries = jar.classPath();
                            ListIterator<Path> it = entries.listIterator(entries.size());
                            while (it.hasPrevious()) {
                                stack.push(it.previous());
                            }
                        }
                    } catch (Exception e) {
                        Log.warn("Could not open class path jar: " + path);
                    }
                } else {
                    Log.debug("Ignoring class path entry: " + path);
                }
            } else if (Files.isDirectory(path)) {
                List<Path> entries = list(path);
                ListIterator<Path> it = entries.listIterator(entries.size());
                while (it.hasPrevious()) {
                    stack.push(it.previous());
                }
            }
        }
        Log.debug("Application classpath:");
        for (Jar jar : classPath) {
            Log.debug("\t%s", jar);
        }
        return List.copyOf(classPath);
    }

    private static List<Jar> concat(Jar jar, List<Jar> jars) {
        List<Jar> tmp = new ArrayList<>();
        tmp.add(jar);
        tmp.addAll(jars);
        return tmp;
    }
}
