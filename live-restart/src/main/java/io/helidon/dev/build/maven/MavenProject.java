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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.build.util.Constants;
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
import static java.util.Collections.emptyList;

/**
 * A Maven build project.
 *
 * TODO: If multiple threads will access, methods that return collections may need to be converted to visitors invoked
 * under an internal lock, or an acquire/release mechanism must be created and used externally and internally
 */
public class MavenProject implements Project {
    private static final String POM_FILE = "pom.xml";
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final char CLASS_PATH_SEP = File.pathSeparatorChar;
    private static final List<String> CLEAN_BUILD_COMMAND = List.of(MAVEN_EXEC, "clean", "process-classes", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of(MAVEN_EXEC, "process-classes", "-DskipTests");

    private final ProjectDirectory root;
    private final Path pomFile;
    private final BuildFile buildFile;
    private final List<Path> dependencies;
    private final List<BuildComponent> components;
    private final StringBuilder classpath;
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
        this.classpath = new StringBuilder();
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
    public String classpath() {
        return classpath.toString();
    }

    @Override
    public List<BuildComponent> components() {
        return components;
    }

    @Override
    public List<BuildRoot> buildRoots(BuildType type) {
        return byType.computeIfAbsent(type, key -> new ArrayList<>());
    }

    @Override
    public List<BuildRoot.Changes> sourceChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components) {
            final BuildRoot.Changes changes = component.sourceRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    @Override
    public List<BuildRoot.Changes> binaryChanges() {
        final List<BuildRoot.Changes> result = new ArrayList<>();
        for (final BuildComponent component : components) {
            final BuildRoot.Changes changes = component.outputRoot().changes();
            if (!changes.isEmpty()) {
                result.add(changes);
            }
        }
        return result;
    }

    @Override
    public List<String> fullBuild(Consumer<String> stdOut, Consumer<String> stdErr, boolean clean) throws Exception {
        return ProcessMonitor.builder()
                             .processBuilder(new ProcessBuilder().directory(root.path().toFile())
                                                                 .command(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND))
                             .stdOut(stdOut)
                             .stdErr(stdErr)
                             .capture(true)
                             .build()
                             .execute()
                             .output();
    }

    @Override
    public List<String> incrementalBuild(List<BuildRoot.Changes> changes,
                                         Consumer<String> stdOut,
                                         Consumer<String> stdErr) throws Exception {
        if (!changes.isEmpty()) {
            final List<String> output = new ArrayList<>();
            final Consumer<String> out = line -> {
                output.add(line);
                stdOut.accept(line);
            };
            final Consumer<String> err = line -> {
                output.add(line);
                stdErr.accept(line);
            };
            for (final BuildRoot.Changes changed : changes) {
                changed.root().component().incrementalBuild(changed, out, err);
            }
            return output;
        } else {
            return emptyList();
        }
    }

    @Override
    public void update(boolean force) {
        if (force || buildFile.hasChanged()) {

            // Update everything

            final Path rootDir = root.path();
            final Model model = readModel(rootDir, pomFile);
            collectDependencies(rootDir, model);
            collectComponents(rootDir, model);
            byType.clear();
            classpath.setLength(0);
            components.forEach(c -> {
                buildRoots(c.sourceRoot().buildType()).add(c.sourceRoot());
                buildRoots(c.outputRoot().buildType()).add(c.outputRoot());
            });
            final Set<Path> classPath = new LinkedHashSet<>();
            buildRoots(BuildType.JavaClasses).forEach(root -> classPath.add(root.path()));
            classPath.addAll(dependencies);
            classPath.forEach(this::appendClassPath);

            buildFile.update();

        } else {

            // Just update the component file time stamps

            components.forEach(BuildComponent::update);
        }
    }

    @Override
    public String toString() {
        return "MavenProject{" +
               "buildFile=" + buildFile +
               '}';
    }

    private void collectDependencies(Path rootDir, Model model) {
        dependencies.clear();

        // TODO: use model (and parents) to find and create result!

        dependencies.add(rootDir.resolve("target/libs"));
    }

    private void collectComponents(Path rootDir, Model model) {
        components.clear();

        // TODO: use model (and parents) to find and create result!

        final Path sourceDir = assertDir(rootDir.resolve("src/main/java"));
        final Path classesDir = ensureDirectory(rootDir.resolve("target/classes"));
        final BuildRoot sources = createBuildRoot(BuildType.JavaSources, sourceDir);
        final BuildRoot classes = createBuildRoot(BuildType.JavaClasses, classesDir);
        components.add(createBuildComponent(this, sources, classes, new CompileJavaSources()));

        final Path resourcesDir = rootDir.resolve("src/main/resources");
        if (Files.exists(resourcesDir)) {
            final BuildRoot resources = createBuildRoot(BuildType.Resources, resourcesDir);
            final BuildRoot binaries = createBuildRoot(BuildType.Resources, classesDir);
            components.add(createBuildComponent(this, resources, binaries, new CopyResources()));
        }
    }

    private void appendClassPath(Path path) {
        if (classpath.length() > 0) {
            classpath.append(CLASS_PATH_SEP);
        }
        classpath.append(path);
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
