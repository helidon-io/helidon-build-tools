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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.DirectoryType;
import io.helidon.dev.build.FileType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectDirectory;
import io.helidon.dev.build.ProjectFile;
import io.helidon.dev.build.steps.CompileJavaSources;
import io.helidon.dev.build.steps.CopyResources;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.dev.build.BuildComponent.createBuildComponent;
import static io.helidon.dev.build.DirectoryType.Classes;
import static io.helidon.dev.build.DirectoryType.JavaSources;
import static io.helidon.dev.build.DirectoryType.Resources;
import static io.helidon.dev.build.FileType.JavaClass;
import static io.helidon.dev.build.FileType.JavaSource;
import static io.helidon.dev.build.ProjectDirectory.createProjectDirectory;

/**
 * A Maven build project.
 */
public class MavenProject implements Project {
    private static final String POM_FILE = "pom.xml";
    private static final String PROJECT_BUILD_SOURCE_ENCODING = "project.build.sourceEncoding";
    private static final String UTF_8 = "UTF-8";
    private final ProjectDirectory root;
    private final ProjectFile buildFile;
    private final List<BuildComponent> components;

    /**
     * Returns whether or not the given directory contains a {@code pom.xml} file.
     *
     * @param rootDir The root directory.
     * @return {@code true} if a {@code pom.xml} file is present.
     */
    public static boolean isMavenProject(Path rootDir) {
        return Files.exists(rootDir.resolve(POM_FILE));
    }

    @Override
    public ProjectDirectory root() {
        return root;
    }

    @Override
    public ProjectFile buildFile() {
        return buildFile;
    }

    @Override
    public List<BuildComponent> components() {
        return components;
    }

    @Override
    public List<String> build() {
        return Collections.emptyList(); // TODO
    }

    /**
     * Constructor.
     *
     * @param rootDir The root directory.
     */
    public MavenProject(Path rootDir) {
        this.root = createProjectDirectory(DirectoryType.Project, rootDir);
        this.components = new ArrayList<>();
        try {
            final Path pomFile = assertFile(rootDir.resolve(POM_FILE));
            this.buildFile = ProjectFile.createProjectFile(root, FileType.MavenPom, pomFile);
            final Model model = readModel(rootDir, pomFile); // Validate.

            // TODO: use Maven apis to find and create components!

            final Path sourceDir = assertDir(rootDir.resolve("src/main/java"));
            final Path classesDir = ensureDirectory(rootDir.resolve("target/classes"));
            final ProjectDirectory sources = createProjectDirectory(JavaSources, sourceDir, JavaSource);
            final ProjectDirectory classes = createProjectDirectory(Classes, classesDir, JavaClass);
            components.add(createBuildComponent(sources, classes, new CompileJavaSources()));

            final Path resourcesDir = rootDir.resolve("src/main/resources");
            if (Files.exists(resourcesDir)) {
                final ProjectDirectory resources = createProjectDirectory(Resources, resourcesDir);
                components.add(createBuildComponent(resources, classes, new CopyResources()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return "MavenProject{" +
               "buildFile=" + buildFile +
               '}';
    }

    private static Model readModel(Path rootDir, Path pomFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(pomFile)) {
            final Model model = new MavenXpp3Reader().read(reader);
            model.getBuild().setDirectory(rootDir.toString());
            return model;
        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing " + pomFile, e);
        }
    }
}
