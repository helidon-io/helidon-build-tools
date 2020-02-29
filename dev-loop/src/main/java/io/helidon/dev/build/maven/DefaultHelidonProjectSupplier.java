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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.dev.build.BuildExecutor;
import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildRootType;
import io.helidon.dev.build.BuildType;
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
 * Builds a project assuming a default pom.xml.
 */
public class DefaultHelidonProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("prepare-package", "-DskipTests");
    private static final String POM_FILE = "pom.xml";
    private static final String JAVA_DIR = "src/main/java";
    private static final String RESOURCES_DIR = "src/main/resources";
    private static final String LIB_DIR = "target/libs";
    private static final String CLASSES_DIR = "target/classes";

    /**
     * Constructor.
     */
    public DefaultHelidonProjectSupplier() {
    }

    @Override
    public Project get(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        final Project project = createProject(executor.projectDirectory());
        if (clean || !project.isBuildUpToDate()) {
            executor.monitor().onBuildStart(cycleNumber, clean ? BuildType.CleanComplete : BuildType.Complete);
            executor.execute(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND);
            project.update(true);
        }
        return project;
    }

    private Project createProject(Path projectDir) throws IOException {
        final Project.Builder builder = Project.builder();
        final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
        final ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);
        builder.buildSystemFile(createBuildFile(root, FileType.MavenPom, pomFile));
        builder.dependency(projectDir.resolve(LIB_DIR));

        final Path sourceDir = assertDir(projectDir.resolve(JAVA_DIR));
        final Path classesDir = ensureDirectory(projectDir.resolve(CLASSES_DIR));
        final BuildRoot sources = createBuildRoot(BuildRootType.JavaSources, sourceDir);
        final BuildRoot classes = createBuildRoot(BuildRootType.JavaClasses, classesDir);
        final Charset sourceEncoding = StandardCharsets.UTF_8;
        builder.component(createBuildComponent(sources, classes, new CompileJavaSources(sourceEncoding, false)));

        final Path resourcesDir = projectDir.resolve(RESOURCES_DIR);
        if (Files.exists(resourcesDir)) {
            final BuildRoot resources = createBuildRoot(BuildRootType.Resources, resourcesDir);
            final BuildRoot binaries = createBuildRoot(BuildRootType.Resources, classesDir);
            builder.component(createBuildComponent(resources, binaries, new CopyResources()));
        }

        builder.mainClassName(findMainClassName(pomFile));
        return builder.build();
    }

    private static String findMainClassName(Path pomFile) throws IOException {
        return Files.readAllLines(pomFile).stream()
                    .filter(line -> line.contains("<mainClass>"))
                    .map(line -> {
                        final int close = line.indexOf(">");
                        final int open = line.indexOf("</", close + 1);
                        return line.substring(close + 1, open);
                    })
                    .findFirst()
                    .orElseThrow(() -> new IOException("<mainClass> element not found in " + pomFile));
    }
}
