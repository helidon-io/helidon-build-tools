/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.helidon.build.util.Constants;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.BuildFile;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildType;
import io.helidon.dev.build.DirectoryType;
import io.helidon.dev.build.FileType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectDirectory;
import io.helidon.dev.build.steps.CompileJavaSources;
import io.helidon.dev.build.steps.CopyResources;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.dev.build.BuildComponent.createBuildComponent;
import static io.helidon.dev.build.BuildFile.createBuildFile;
import static io.helidon.dev.build.BuildRoot.createBuildRoot;
import static io.helidon.dev.build.ProjectDirectory.createProjectDirectory;

/**
 * A Maven build project.
 *
 * TODO: add a locking model if multi-threaded use
 */
public class MavenProject extends Project {
    private static final String POM_FILE = "pom.xml";
    private static final String JAR_FILE_SUFFIX = ".jar";
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final List<String> CLEAN_BUILD_COMMAND = List.of(MAVEN_EXEC, "clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of(MAVEN_EXEC, "prepare-package", "-DskipTests");

    private final ProjectDirectory root;
    private final Path pomFile;
    private final BuildFile buildFile;
    private final List<Path> dependencies;
    private final List<BuildComponent> components;
    private final List<File> classpath;
    private final List<String> compilerFlags;
    private final Map<BuildType, List<BuildRoot>> byType;

    /**
     * Returns whether or not the given directory contains a {@code pom.xml} file.
     *
     * @param rootDir The root directory.
     * @return {@code true} if a {@code pom.xml} file is present.
     */
    public static boolean isMavenProject(Path rootDir) {
        return Files.exists(rootDir.resolve(POM_FILE));
    }

    /**
     * Constructor.
     *
     * @param rootDir The root directory.
     */
    public MavenProject(Path rootDir) {
        this.root = createProjectDirectory(DirectoryType.Project, rootDir);
        this.pomFile = assertFile(rootDir.resolve(POM_FILE));
        this.buildFile = createBuildFile(root, FileType.MavenPom, pomFile);
        this.classpath = new ArrayList<>();
        this.compilerFlags = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.components = new ArrayList<>();
        this.byType = new HashMap<>();
        update(true);
    }

    @Override
    public ProjectDirectory root() {
        return root;
    }

    @Override
    public BuildFile buildSystemFile() {
        return buildFile;
    }

    @Override
    public List<Path> dependencies() {
        return dependencies;
    }

    @Override
    public List<File> classpath() {
        return classpath;
    }

    @Override
    public List<String> compilerFlags() {
        return compilerFlags;
    }

    @Override
    public List<BuildComponent> components() {
        return components;
    }

    @Override
    protected List<BuildRoot> buildRoots(BuildType type) {
        return byType.computeIfAbsent(type, key -> new ArrayList<>());
    }

    @Override
    public void fullBuild(Consumer<String> stdOut, Consumer<String> stdErr, boolean clean) throws Exception {
        ProcessMonitor.builder()
                      .processBuilder(new ProcessBuilder().directory(root.path().toFile())
                                                          .command(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND))
                      .stdOut(stdOut)
                      .stdErr(stdErr)
                      .capture(true)
                      .build()
                      .execute();
    }

    @Override
    public void update(boolean force) {
        if (force || buildFile.hasChanged()) {

            // Update everything

            final Path rootDir = root.path();
            final Model model = readModel(rootDir, pomFile);
            buildFile.update();
            updateDependencies(rootDir, model);
            updateComponents(rootDir, model);
            updateCompilerFlags(model);

            byType.clear();
            components.forEach(c -> {
                buildRoots(c.sourceRoot().buildType()).add(c.sourceRoot());
                buildRoots(c.outputRoot().buildType()).add(c.outputRoot());
            });

            classpath.clear();
            buildRoots(BuildType.JavaClasses).forEach(root -> classpath.add(root.path().toFile()));
            dependencies().forEach(this::addToClasspath);

        } else {

            // Just update the component file time stamps

            components.forEach(BuildComponent::update);
        }
    }

    private void addToClasspath(Path path) {
        if (Files.isRegularFile(path)) {
            classpath.add(path.toFile());
        } else if (Files.isDirectory(path)) {
            FileUtils.listFiles(path, name -> name.endsWith(JAR_FILE_SUFFIX)).forEach(file -> classpath.add(file.toFile()));
        }
    }

    @Override
    public String toString() {
        return "MavenProject{" +
               "buildFile=" + buildFile +
               '}';
    }

    private void updateDependencies(Path rootDir, Model model) {
        dependencies.clear();

        // TODO: use model (and parents) to find and create result!

        dependencies.add(rootDir.resolve("target/libs"));
    }

    private void updateComponents(Path rootDir, Model model) {
        components.clear();

        // TODO: use model (and parents) to find and create result!

        final Path sourceDir = assertDir(rootDir.resolve("src/main/java"));
        final Path classesDir = ensureDirectory(rootDir.resolve("target/classes"));
        final BuildRoot sources = createBuildRoot(BuildType.JavaSources, sourceDir);
        final BuildRoot classes = createBuildRoot(BuildType.JavaClasses, classesDir);
        final Charset sourceEncoding = StandardCharsets.UTF_8;
        components.add(createBuildComponent(this, sources, classes, new CompileJavaSources(sourceEncoding)));

        final Path resourcesDir = rootDir.resolve("src/main/resources");
        if (Files.exists(resourcesDir)) {
            final BuildRoot resources = createBuildRoot(BuildType.Resources, resourcesDir);
            final BuildRoot binaries = createBuildRoot(BuildType.Resources, classesDir);
            components.add(createBuildComponent(this, resources, binaries, new CopyResources()));
        }
    }

    private void updateCompilerFlags(Model model) {
        compilerFlags.clear();
        // TODO: use model (and parents) to find and create result!
    }

    private static Model readModel(Path rootDir, Path pomFile) {
        try {
            try (BufferedReader reader = Files.newBufferedReader(pomFile)) {
                final Model model = new MavenXpp3Reader().read(reader);
                model.getBuild().setDirectory(rootDir.toString());
                return model;
            } catch (XmlPullParserException e) {
                throw new IllegalArgumentException("Error parsing " + pomFile, e);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
