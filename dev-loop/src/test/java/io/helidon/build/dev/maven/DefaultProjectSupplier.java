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

package io.helidon.build.dev.maven;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.dev.FileType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.ProjectDirectory;
import io.helidon.build.dev.ProjectSupplier;

import static io.helidon.build.dev.BuildComponent.createBuildComponent;
import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.dev.BuildRoot.createBuildRoot;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.FIRST;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.LATEST;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;

/**
 * Builds a project assuming a default pom.xml.
 */
public class DefaultProjectSupplier implements ProjectSupplier {
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
    public DefaultProjectSupplier() {
    }

    @Override
    public Project newProject(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        final BuildType buildType = BuildType.completeType(executor.willFork(), clean);
        final Project project = createProject(executor.projectDirectory(), buildType);
        if (clean || !project.isBuildUpToDate()) {
            executor.monitor().onBuildStart(cycleNumber, buildType);
            executor.execute(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND);
            project.update(true);
        }
        return project;
    }

    @Override
    public boolean hasChanges(Path projectDir, FileTime lastCheckTime) {
        return MavenProjectSupplier.changedTime(projectDir, lastCheckTime, FIRST).isPresent();
    }

    @Override
    public Optional<FileTime> changedTime(Path projectDir, FileTime lastCheckTime) {
        return MavenProjectSupplier.changedTime(projectDir, lastCheckTime, LATEST);
    }

    @Override
    public String buildFileName() {
        return POM_FILE;
    }

    private Project createProject(Path projectDir, BuildType buildType) throws IOException {
        final Project.Builder builder = Project.builder().buildType(buildType);
        final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
        final ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);
        builder.buildFile(createBuildFile(root, FileType.MavenPom, pomFile));
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
