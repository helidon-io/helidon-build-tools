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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.helidon.build.util.Constants;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.dev.build.BuildMonitor;
import io.helidon.dev.build.BuildRoot;
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
    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final List<String> CLEAN_BUILD_COMMAND = List.of(MAVEN_EXEC, "clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of(MAVEN_EXEC, "prepare-package", "-DskipTests");
    private static final String POM_FILE = "pom.xml";
    private static final String JAVA_DIR = "src/main/java";
    private static final String RESOURCES_DIR = "src/main/resources";
    private static final String LIB_DIR = "target/libs";
    private static final String CLASSES_DIR = "target/classes";

    @Override
    public Project get(Path projectDir, BuildMonitor monitor, boolean clean, int cycleNumber) throws Exception {
        final Project project = createProject(projectDir);
        if (clean || !project.isBuildUpToDate()) {
            monitor.onBuildStart(cycleNumber, !clean);
            build(projectDir, monitor, clean);
            project.update();
        }
        return project;
    }

    private void build(Path projectDir, BuildMonitor monitor, boolean clean) throws Exception {

        // Make sure we use the current JDK by forcing it first in the path and setting JAVA_HOME. This might be required
        // if we're in an IDE whose process was started with a different JDK.

        final String javaHome = System.getProperty("java.home");
        final String javaHomeBin = javaHome + File.separator + "bin";
        final ProcessBuilder processBuilder = new ProcessBuilder().directory(projectDir.toFile())
                                                                  .command(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND);
        final Map<String, String> env = processBuilder.environment();
        final String path = javaHomeBin + File.pathSeparatorChar + env.get("PATH");
        env.put("PATH", path);
        env.put("JAVA_HOME", javaHome);

        ProcessMonitor.builder()
                      .processBuilder(processBuilder)
                      .stdOut(monitor.stdOutConsumer())
                      .stdErr(monitor.stdErrConsumer())
                      .capture(true)
                      .build()
                      .execute();
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
        final BuildRoot sources = createBuildRoot(BuildType.JavaSources, sourceDir);
        final BuildRoot classes = createBuildRoot(BuildType.JavaClasses, classesDir);
        final Charset sourceEncoding = StandardCharsets.UTF_8;
        builder.component(createBuildComponent(sources, classes, new CompileJavaSources(sourceEncoding)));

        final Path resourcesDir = projectDir.resolve(RESOURCES_DIR);
        if (Files.exists(resourcesDir)) {
            final BuildRoot resources = createBuildRoot(BuildType.Resources, resourcesDir);
            final BuildRoot binaries = createBuildRoot(BuildType.Resources, classesDir);
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
