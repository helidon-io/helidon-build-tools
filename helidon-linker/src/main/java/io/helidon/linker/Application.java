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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;

/**
 * A Helidon application supporting Java dependency collection and installation into a Java Home.
 * This class assumes that the application was built re
 */
public class Application {
    static final Path APP_DIR = Paths.get("app");
    private static final Path ARCHIVE_PATH = Paths.get("lib/start.jsa");
    private static final String MP_FILE_PREFIX = "helidon-microprofile";
    private final Jar mainJar;
    private final List<Jar> classPath;
    private final boolean isMicroprofile;

    /**
     * Constructor.
     *
     * @param mainJar The main jar.
     */
    public Application(Path mainJar) {
        this.mainJar = Jar.open(mainJar);
        this.classPath = collectClassPath();
        this.isMicroprofile = classPath.stream().anyMatch(jar -> jar.name().startsWith(MP_FILE_PREFIX));
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
     * @return The location of the installed application jar.
     */
    public Path install(JavaRuntime jri) {
        final Path appRootDir = mainJar.path().getParent();
        final Path appInstallDir = jri.ensureDirectory(APP_DIR);
        final Path installedAppJar = mainJar.copyToDirectory(appInstallDir, isMicroprofile());
        classPath.forEach(jar -> {
            final Path relativeDir = appRootDir.relativize(jar.path().getParent());
            final Path installDir = jri.ensureDirectory(appInstallDir.resolve(relativeDir));
            jar.copyToDirectory(installDir, isMicroprofile());
        });
        return installedAppJar;
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

    private Stream<Jar> jars() {
        return Stream.concat(Stream.of(mainJar), classPath.stream());
    }

    private List<Jar> collectClassPath() {
        return addClassPath(mainJar, new HashSet<>(), new ArrayList<>());
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
}
