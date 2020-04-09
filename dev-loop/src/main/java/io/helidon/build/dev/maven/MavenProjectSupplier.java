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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.dev.FileType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.ProjectDirectory;
import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProjectConfig;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.dev.BuildComponent.createBuildComponent;
import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.dev.BuildRoot.createBuildRoot;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSPATH;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.loadHelidonCliConfig;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "process-classes", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("process-classes", "-DskipTests");
    private static final String TARGET_DIR_NAME = "target";
    private static final String POM_FILE = "pom.xml";

    private final AtomicReference<MavenProject> project;
    private final MavenSession session;
    private final BuildPluginManager plugins;
    private final AtomicBoolean firstBuild;
    private ProjectConfig config;

    /**
     * Constructor.
     *
     * @param project The maven project.
     * @param session The maven session.
     * @param plugins The maven plugin manager.
     */
    public MavenProjectSupplier(MavenProject project,
                                MavenSession session,
                                BuildPluginManager plugins) {
        ProjectConfigCollector.assertSupportedProject(session);
        this.project = new AtomicReference<>(project);
        this.session = session;
        this.plugins = plugins;
        this.firstBuild = new AtomicBoolean(true);
    }

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
        final Path projectDir = executor.projectDirectory();
        executor.monitor().onBuildStart(cycleNumber, clean ? BuildType.CleanComplete : BuildType.Complete);

        // Get the updated config, performing the full build if needed

        if (clean) {
            build(executor, CLEAN_BUILD_COMMAND);
        } else if (canSkipBuild(projectDir)) {
            Log.info("Project is up to date");
        } else {
            build(executor, BUILD_COMMAND);
        }

        // Create and return the project based on the config
        return createProject(executor.projectDirectory());
    }

    private void build(BuildExecutor executor, List<String> command) throws Exception {
        executor.execute(command);
        config = loadHelidonCliConfig(executor.projectDirectory());
    }

    private boolean canSkipBuild(Path projectDir) {

        // Is this our first request in this session?

        if (firstBuild.getAndSet(false)) {

            // Yes. We can skip the build IFF we have a previously completed build from another session whose build time
            // is more recent than any file in the project (excluding target/* and .helidon)

            config = loadHelidonCliConfig(projectDir);
            final long lastFullBuildTime = config.lastSuccessfulBuildTime();
            if (lastFullBuildTime > 0) {
                final Path targetDir = projectDir.resolve(TARGET_DIR_NAME);
                final Path helidonFile = projectDir.resolve(DOT_HELIDON);
                try (Stream<Path> stream = Files.walk(projectDir)) {
                    return stream.filter(path -> !path.equals(helidonFile))
                                 .filter(path -> !path.startsWith(targetDir))
                                 .allMatch(path -> lastModifiedTime(path) < lastFullBuildTime);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return false;
    }

    private Project createProject(Path projectDir) {
        // Root directory
        final Project.Builder builder = Project.builder();
        final ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);

        // POM file
        final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
        builder.buildSystemFile(createBuildFile(root, FileType.MavenPom, pomFile));

        // Dependencies - Should we consider the classes/lib?
        final List<String> classpath = config.propertyAsList(PROJECT_CLASSPATH);
        classpath.stream().map(s -> Path.of(s)).forEach(builder::dependency);

        // Build components
        final List<String> sourceDirs = config.propertyAsList(PROJECT_SOURCEDIRS);
        final List<String> classesDirs = config.propertyAsList(PROJECT_CLASSDIRS);
        final List<String> resourcesDirs = config.propertyAsList(PROJECT_RESOURCEDIRS);

        for (String sourceDir : sourceDirs) {
            for (String classesDir : classesDirs) {
                Path sourceDirPath = assertDir(projectDir.resolve(sourceDir));
                Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                BuildRoot sources = createBuildRoot(BuildRootType.JavaSources, sourceDirPath);
                BuildRoot classes = createBuildRoot(BuildRootType.JavaClasses, classesDirPath);
                builder.component(createBuildComponent(sources, classes, compileStep()));
            }
        }

        for (String resourcesDir : resourcesDirs) {
            for (String classesDir : classesDirs) {
                Path resourcesDirPath = projectDir.resolve(resourcesDir);
                if (Files.exists(resourcesDirPath)) {
                    Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                    BuildRoot sources = createBuildRoot(BuildRootType.Resources, resourcesDirPath);
                    BuildRoot classes = createBuildRoot(BuildRootType.Resources, classesDirPath);
                    builder.component(createBuildComponent(sources, classes, resourcesStep()));
                }
            }
        }

        // Main class
        builder.mainClassName(config.property(PROJECT_MAINCLASS));

        return builder.build();
    }

    private BuildStep compileStep() {
        return MavenGoalBuildStep.builder()
                                 .mavenProject(project.get())
                                 .mavenSession(session)
                                 .pluginManager(plugins)
                                 .goal(MavenGoalBuildStep.compileGoal())
                                 .build();
    }

    private BuildStep resourcesStep() {
        return MavenGoalBuildStep.builder()
                                 .mavenProject(project.get())
                                 .mavenSession(session)
                                 .pluginManager(plugins)
                                 .goal(MavenGoalBuildStep.resourcesGoal())
                                 .build();
    }
}
