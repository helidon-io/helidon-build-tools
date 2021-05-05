/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop.maven;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import io.helidon.build.devloop.BuildExecutor;
import io.helidon.build.devloop.BuildRoot;
import io.helidon.build.devloop.BuildRootType;
import io.helidon.build.devloop.BuildType;
import io.helidon.build.devloop.DirectoryType;
import io.helidon.build.devloop.Project;
import io.helidon.build.devloop.ProjectDirectory;
import io.helidon.build.devloop.ProjectSupplier;

import static io.helidon.build.cli.common.CliProperties.ENABLE_HELIDON_CLI;
import static io.helidon.build.common.FileChanges.DetectionType.FIRST;
import static io.helidon.build.common.FileChanges.DetectionType.LATEST;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.devloop.BuildComponent.createBuildComponent;
import static io.helidon.build.devloop.BuildFile.createBuildFile;
import static io.helidon.build.devloop.BuildRoot.createBuildRoot;
import static io.helidon.build.devloop.ProjectDirectory.createProjectDirectory;

/**
 * Builds a project assuming a default pom.xml.
 */
public class DefaultProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_CMD = List.of("clean", "prepare-package", "-DskipTests", ENABLE_HELIDON_CLI);
    private static final List<String> BUILD_CMD = List.of("prepare-package", "-DskipTests", ENABLE_HELIDON_CLI);
    private static final String POM_FILE = "pom.xml";
    private static final String JAVA_DIR = "src/main/java";
    private static final String RESOURCES_DIR = "src/main/resources";
    private static final String LIB_DIR = "target/libs";
    private static final String CLASSES_DIR = "target/classes";

    /**
     * Constructor.
     */
    public DefaultProjectSupplier() {
    }

    @Override
    public Project newProject(BuildExecutor executor, boolean clean, boolean allowSkip, int cycleNumber) throws Exception {
        final BuildType buildType = BuildType.completeType(executor.willFork(), clean);
        final Project project = createProject(executor.projectDirectory(), buildType);
        if (clean || (allowSkip && !project.isBuildUpToDate())) {
            executor.monitor().onBuildStart(cycleNumber, buildType);
            executor.execute(clean ? CLEAN_BUILD_CMD : BUILD_CMD);
            project.update(true);
        }
        return project;
    }

    @Override
    public boolean hasChanges(Path projectDir, FileTime lastCheckTime) {
        return MavenProjectSupplier.changedSince(projectDir, lastCheckTime, FIRST).isPresent();
    }

    @Override
    public Optional<FileTime> changedSince(Path projectDir, FileTime lastCheckTime) {
        return MavenProjectSupplier.changedSince(projectDir, lastCheckTime, LATEST);
    }

    @Override
    public String buildFileName() {
        return POM_FILE;
    }

    private Project createProject(Path projectDir, BuildType buildType) throws IOException {
        final Project.Builder builder = Project.builder().buildType(buildType);
        final Path pomFile = requireFile(projectDir.resolve(POM_FILE));
        final ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);
        builder.buildFile(createBuildFile(root, pomFile));
        builder.dependency(projectDir.resolve(LIB_DIR));

        final Path sourceDir = requireDirectory(projectDir.resolve(JAVA_DIR));
        final Path classesDir = ensureDirectory(projectDir.resolve(CLASSES_DIR));
        final BuildRoot sources = createBuildRoot(BuildRootType.javaSources(), sourceDir);
        final BuildRoot classes = createBuildRoot(BuildRootType.javaClasses(), classesDir);
        final Charset sourceEncoding = StandardCharsets.UTF_8;
        builder.component(createBuildComponent(sources, classes, new CompileJavaSources(sourceEncoding, false)));

        final Path resourcesDir = projectDir.resolve(RESOURCES_DIR);
        if (Files.exists(resourcesDir)) {
            final BuildRoot resources = createBuildRoot(BuildRootType.resources(), resourcesDir);
            final BuildRoot binaries = createBuildRoot(BuildRootType.resources(), classesDir);
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
