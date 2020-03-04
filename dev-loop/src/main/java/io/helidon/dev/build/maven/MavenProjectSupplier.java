/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.util.ConfigProperties;
import io.helidon.dev.build.BuildExecutor;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildRootType;
import io.helidon.dev.build.DirectoryType;
import io.helidon.dev.build.FileType;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectDirectory;
import io.helidon.dev.build.ProjectSupplier;
import io.helidon.dev.build.steps.CompileJavaSources;
import io.helidon.dev.build.steps.CopyResources;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.dev.build.BuildComponent.createBuildComponent;
import static io.helidon.dev.build.BuildFile.createBuildFile;
import static io.helidon.dev.build.BuildRoot.createBuildRoot;
import static io.helidon.dev.build.ProjectDirectory.createProjectDirectory;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("prepare-package", "-DskipTests");

    static final String POM_FILE = "pom.xml";
    static final String DOT_HELIDON = ".helidon";
    static final String PROJECT_CLASSPATH = "project.classpath";
    static final String PROJECT_SOURCEDIRS = "project.sourcedirs";
    static final String PROJECT_CLASSESDIRS = "project.classesdirs";
    static final String PROJECT_RESOURCESDIRS = "project.resourcesdirs";
    static final String PROJECT_MAINCLASS = "project.mainclass";

    private final ConfigProperties properties = new ConfigProperties(DOT_HELIDON);

    /**
     * Gets a project instance.
     *
     * @param executor The build executor.
     * @param clean {@code true} if the project should be cleaned and built.
     * @param cycleNumber The cycle number.
     * @return A project.
     * @throws Exception If a problem is found.
     */
    @Override
    public Project get(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        if (clean) {
            executor.execute(CLEAN_BUILD_COMMAND);
        }

        if (!configurationExists()) {
            executor.execute(BUILD_COMMAND);
            properties.load();
        }

        Path projectDir = executor.projectDirectory();
        Project.Builder builder = Project.builder();

        // Root directory
        ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);

        // POM file
        final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
        builder.buildSystemFile(createBuildFile(root, FileType.MavenPom, pomFile));

        // Dependencies - Should we consider the classes/lib?
        List<String> classpath = properties.propertyAsList(PROJECT_CLASSPATH);
        classpath.stream().map(s -> Path.of(s)).forEach(builder::dependency);

        // Build components
        List<String> sourceDirs = properties.propertyAsList(PROJECT_SOURCEDIRS);
        List<String> classesDirs = properties.propertyAsList(PROJECT_CLASSESDIRS);
        List<String> resourcesDirs = properties.propertyAsList(PROJECT_RESOURCESDIRS);

        for (String sourceDir : sourceDirs) {
            for (String classesDir : classesDirs) {
                Path sourceDirPath = assertDir(projectDir.resolve(sourceDir));
                Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                BuildRoot sources = createBuildRoot(BuildRootType.JavaSources, sourceDirPath);
                BuildRoot classes = createBuildRoot(BuildRootType.JavaClasses, classesDirPath);
                builder.component(createBuildComponent(sources, classes,
                        new CompileJavaSources(StandardCharsets.UTF_8, false)));
            }
        }

        for (String resourcesDir : resourcesDirs) {
            for (String classesDir : classesDirs) {
                Path resourcesDirPath = projectDir.resolve(resourcesDir);
                if (Files.exists(resourcesDirPath)) {
                    Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                    BuildRoot sources = createBuildRoot(BuildRootType.Resources, resourcesDirPath);
                    BuildRoot classes = createBuildRoot(BuildRootType.Resources, classesDirPath);
                    builder.component(createBuildComponent(sources, classes, new CopyResources()));
                }
            }
        }

        // Main class
        builder.mainClassName(properties.property(PROJECT_MAINCLASS));

        return builder.build();
    }

    /**
     * Check if we have the necessary properties. For now we just check
     * that classpath is there.
     *
     * @return Outcome of test.
     */
    private boolean configurationExists() {
        return properties.contains(PROJECT_CLASSPATH);
    }
}
