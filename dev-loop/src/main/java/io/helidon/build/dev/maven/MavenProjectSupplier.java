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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.dev.FileType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.Project.Builder;
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
import static io.helidon.build.dev.BuildType.Skipped;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DEPENDENCIES;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.loadHelidonCliConfig;
import static java.util.Objects.requireNonNull;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "process-classes", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("process-classes", "-DskipTests");
    private static final String TARGET_DIR_NAME = "target";
    private static final String POM_FILE = "pom.xml";
    private static final String DOT = ".";

    private static final Predicate<Path> NOT_HIDDEN = file -> {
        final String name = file.getFileName().toString();
        return !name.startsWith(DOT);
    };

    private static final Predicate<Path> NOT_TARGET_DIR = file -> {
        final String name = file.getFileName().toString();
        return !name.equals(TARGET_DIR_NAME);
    };

    private final MavenProject project;
    private final MavenSession session;
    private final BuildPluginManager plugins;
    private final AtomicBoolean firstBuild;
    private BuildType buildType;
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
        MavenProjectConfigCollector.assertSupportedProject(session);
        this.project = project;
        this.session = requireNonNull(session);
        this.plugins = requireNonNull(plugins);
        this.firstBuild = new AtomicBoolean(true);
    }

    @Override
    public boolean hasChanges(Path projectDir, FileTime lastCheckTime) {
        return changedTime(projectDir, lastCheckTime, false).isPresent();
    }

    @Override
    public Optional<FileTime> changedTime(Path projectDir, FileTime lastCheckTime) {
        return changedTime(projectDir, lastCheckTime, true);
    }

    /**
     * Checks whether any matching file has a modified time more recent than the given time.
     *
     * @param projectDir The project directory.
     * @param lastCheckTime The time to check against.
     * @return The time, if changed.
     */
    public static Optional<FileTime> changedTime(Path projectDir, FileTime lastCheckTime, boolean checkAllFiles) {
        return changedTime(projectDir, lastCheckTime, NOT_HIDDEN, NOT_HIDDEN.and(NOT_TARGET_DIR), checkAllFiles);
    }

    /**
     * Checks whether any matching file has a modified time more recent than the given time.
     *
     * @param projectDir The project directory.
     * @param lastCheckTime The time to check against.
     * @param dirFilter A filter for directories to visit.
     * @param fileFilter A filter for which files to check.
     * @param checkAllFiles {@code true} if all files should be checked and the most recent change time returned,
     * {@code false} if the first changed time should be returned.
     * @return The time, if changed.
     */
    public static Optional<FileTime> changedTime(Path projectDir,
                                                 FileTime lastCheckTime,
                                                 Predicate<Path> dirFilter,
                                                 Predicate<Path> fileFilter,
                                                 boolean checkAllFiles) {
        if (lastCheckTime != null) {
            final AtomicReference<FileTime> checkTime = new AtomicReference<>(lastCheckTime);
            final AtomicReference<FileTime> changeTime = new AtomicReference<>();
            Log.debug("Checking if project has files newer than last check time %s", lastCheckTime);
            try {
                Files.walkFileTree(projectDir, new FileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return dirFilter.test(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (fileFilter.test(file)) {
                            final FileTime fileTime = lastModifiedTime(file);
                            if (fileTime.compareTo(checkTime.get()) > 0) {
                                Log.debug("%s @ %s is newer than last check time %s", file, fileTime, lastCheckTime);
                                changeTime.set(fileTime);
                                if (checkAllFiles) {
                                    checkTime.set(fileTime);
                                } else {
                                    return FileVisitResult.TERMINATE;
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        changeTime.set(null);
                        return FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });

                return Optional.ofNullable(changeTime.get());

            } catch (Exception e) {
                Log.warn(e.getMessage());
            }
        }

        return Optional.of(FileTime.fromMillis(System.currentTimeMillis())); // Force it if we get here
    }

    @Override
    public Project newProject(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        final Path projectDir = executor.projectDirectory();

        // Get the updated config, performing the full build if needed

        buildType = BuildType.completeType(executor.willFork(), clean);
        if (clean) {
            build(executor, true, cycleNumber);
        } else if (canSkipBuild(projectDir)) {
            buildType = Skipped;
            executor.monitor().onBuildStart(cycleNumber, BuildType.Skipped);
        } else {
            build(executor, false, cycleNumber);
        }

        // Create and return the project based on the config
        return createProject(executor.projectDirectory(), buildType);
    }

    @Override
    public String buildFileName() {
        return POM_FILE;
    }

    private void build(BuildExecutor executor, boolean clean, int cycleNumber) throws Exception {
        executor.monitor().onBuildStart(cycleNumber, buildType);
        executor.execute(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND);
        config = loadHelidonCliConfig(executor.projectDirectory());
    }

    private boolean canSkipBuild(Path projectDir) {

        // Is this our first request in this session?

        if (firstBuild.getAndSet(false)) {

            // Yes. We can skip the build IFF we have a previously completed build from another session whose build time
            // is more recent than any file in the project (excluding target/* and .*)

            config = loadHelidonCliConfig(projectDir);
            return !hasChanges(projectDir, FileTime.fromMillis(config.lastSuccessfulBuildTime()));
        }
        return false;
    }

    private Project createProject(Path projectDir, BuildType buildType) {
        // Root directory
        final Builder builder = Project.builder().buildType(buildType);
        final ProjectDirectory root = createProjectDirectory(DirectoryType.Project, projectDir);
        builder.rootDirectory(root);

        // POM file
        final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
        builder.buildFile(createBuildFile(root, FileType.MavenPom, pomFile));

        // Dependencies
        final List<String> dependencies = config.propertyAsList(PROJECT_DEPENDENCIES);
        for (String dependency : dependencies) {
            builder.dependency(Path.of(dependency));
        }

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
                                 .mavenProject(project)
                                 .mavenSession(session)
                                 .pluginManager(plugins)
                                 .goal(MavenGoalBuildStep.compileGoal())
                                 .build();
    }

    private BuildStep resourcesStep() {
        return MavenGoalBuildStep.builder()
                                 .mavenProject(project)
                                 .mavenSession(session)
                                 .pluginManager(plugins)
                                 .goal(MavenGoalBuildStep.resourcesGoal())
                                 .build();
    }
}
