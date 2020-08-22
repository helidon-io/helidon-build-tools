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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.build.dev.BuildExecutor;
import io.helidon.build.dev.BuildRoot;
import io.helidon.build.dev.BuildRootType;
import io.helidon.build.dev.BuildStep;
import io.helidon.build.dev.BuildType;
import io.helidon.build.dev.DirectoryType;
import io.helidon.build.dev.Project;
import io.helidon.build.dev.Project.Builder;
import io.helidon.build.dev.ProjectDirectory;
import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.dev.maven.DevLoopBuildConfig.IncrementalBuildConfig.CustomDirectoryConfig;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;
import io.helidon.build.util.PathPredicates;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.Requirements;

import static io.helidon.build.dev.BuildComponent.createBuildComponent;
import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.dev.BuildRoot.createBuildRoot;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.Constants.ENABLE_HELIDON_CLI;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.FIRST;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.LATEST;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.PathPredicates.matchesJavaClass;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DEPENDENCIES;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_EXCLUDES;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_INCLUDES;
import static io.helidon.build.util.ProjectConfig.projectConfig;
import static java.util.Collections.emptyList;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final String HELIDON_PLUGIN_VERSION_PROP = "version.plugin.helidon";
    private static final String HELIDON_PLUGIN_VERSION = System.getProperty(HELIDON_PLUGIN_VERSION_PROP);
    private static final String CLEAN_ARG = "clean";
    private static final String SKIP_TESTS_ARG = "-DskipTests";
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

    private final DevLoopBuildConfig buildConfig;
    private final AtomicBoolean firstBuild;
    private final List<String> cleanBuildCmd;
    private final List<String> buildCmd;
    private ProjectConfig projectConfig;
    private BuildType buildType;

    /**
     * Constructor.
     *
     * @param buildConfig The build configuration.
     */
    public MavenProjectSupplier(DevLoopBuildConfig buildConfig) {
        this.buildConfig = buildConfig;
        this.firstBuild = new AtomicBoolean(true);
        this.cleanBuildCmd = List.of(CLEAN_ARG, buildConfig.fullBuildPhase(), SKIP_TESTS_ARG, ENABLE_HELIDON_CLI);
        this.buildCmd = List.of(buildConfig.fullBuildPhase(), SKIP_TESTS_ARG, ENABLE_HELIDON_CLI);
    }

    @Override
    public boolean hasChanges(Path projectDir, FileTime lastCheckTime) {
        return changedSince(projectDir, lastCheckTime, FIRST).isPresent();
    }

    @Override
    public Optional<FileTime> changedSince(Path projectDir, FileTime lastCheckTime) {
        return changedSince(projectDir, lastCheckTime, LATEST);
    }

    /**
     * Checks whether any matching file has a modified time more recent than the given time.
     *
     * @param projectDir The project directory.
     * @param lastCheckTime The time to check against.
     * @param type The type of search.
     * @return The time, if changed.
     */
    public static Optional<FileTime> changedSince(Path projectDir, FileTime lastCheckTime, FileUtils.ChangeDetectionType type) {
        return FileUtils.changedSince(projectDir, lastCheckTime, NOT_HIDDEN.and(NOT_TARGET_DIR), NOT_HIDDEN, type);
    }

    @Override
    public Project newProject(BuildExecutor executor, boolean clean, boolean allowSkip, int cycleNumber) throws Exception {
        final Path projectDir = executor.projectDirectory();

        // Get the updated config, performing the full build if needed

        buildType = BuildType.completeType(executor.willFork(), clean);
        if (clean) {
            build(executor, true, cycleNumber);
        } else if (allowSkip && canSkipBuild(projectDir)) {
            try {
                Project result = createProject(executor.projectDirectory(), BuildType.Skipped);
                executor.monitor().onBuildStart(cycleNumber, BuildType.Skipped);
                return result;
            } catch (Exception e) {
                build(executor, false, cycleNumber);
            }
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
        List<String> command = clean ? cleanBuildCmd : buildCmd;
        if (HELIDON_PLUGIN_VERSION != null) {
            command = new ArrayList<>(command);
            command.add("-D" + HELIDON_PLUGIN_VERSION_PROP + "=" + HELIDON_PLUGIN_VERSION);
        }
        executor.monitor().onBuildStart(cycleNumber, buildType);
        executor.execute(command);
        projectConfig = projectConfig(executor.projectDirectory());
        Requirements.require(projectConfig.lastSuccessfulBuildTime() > 0,
                             "$(cyan helidon-maven-plugin) must be configured as an extension");
    }

    private boolean canSkipBuild(Path projectDir) {

        // Is this our first request in this session?

        if (firstBuild.getAndSet(false)) {

            // Yes. We can skip the build IFF we have a previously completed build from another session whose build time
            // is more recent than any file in the project (excluding target/* and .*)

            projectConfig = projectConfig(projectDir);
            return !hasChanges(projectDir, FileTime.fromMillis(projectConfig.lastSuccessfulBuildTime()));
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
        builder.buildFile(createBuildFile(root, PathPredicates.matchesMavenPom(), pomFile));

        // Dependencies
        final List<String> dependencies = projectConfig.propertyAsList(PROJECT_DEPENDENCIES);
        for (String dependency : dependencies) {
            builder.dependency(Path.of(dependency));
        }

        // Build components
        final List<String> sourceDirs = projectConfig.propertyAsList(PROJECT_SOURCEDIRS);
        final List<String> sourceIncludes = projectConfig.propertyAsList(PROJECT_SOURCE_INCLUDES);
        final List<String> sourceExcludes = projectConfig.propertyAsList(PROJECT_SOURCE_EXCLUDES);
        final List<String> classesDirs = projectConfig.propertyAsList(PROJECT_CLASSDIRS);
        final List<String> resourcesDirs = projectConfig.propertyAsList(PROJECT_RESOURCEDIRS);

        // Map classesDirs to a list of BuildRoots so can re-use as the outputRoot for all components.
        //
        // TODO: outputRoot should be customizable for CustomDir iff we keep it. Consider removing it since
        //       it's only use with Maven is so we can watch for changes here in "watchBinariesOnly" mode
        //       which we have not exposed. It is also used for the testing build steps, but that could be
        //       hard wired.

        final BuildRootType classesRootType = BuildRootType.create(DirectoryType.JavaClasses, matchesJavaClass());
        final List<BuildRoot> classesRoots = classesDirs.stream()
                                                        .map(directory -> {
                                                            Path classesDirPath = ensureDirectory(projectDir.resolve(directory));
                                                            return createBuildRoot(classesRootType, classesDirPath);
                                                        })
                                                        .collect(Collectors.toList());

        // Add java source components

        for (String sourceDir : sourceDirs) {
            Path sourceDirPath = assertDir(projectDir.resolve(sourceDir));
            BiPredicate<Path, Path> includes = PathPredicates.matches(sourceIncludes, sourceExcludes);
            BuildRootType sourceRootType = BuildRootType.create(DirectoryType.JavaSources, includes);
            BuildRoot sources = createBuildRoot(sourceRootType, sourceDirPath);
            for (BuildRoot classes : classesRoots) {
                builder.component(createBuildComponent(sources, classes, compileSteps()));
            }
        }

        // Add resource components

        for (String resourcesDirEntry : resourcesDirs) {
            String[] dir = resourcesDirEntry.split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_SEPARATOR);
            String resourcesDir = dir[0];
            List<String> includePatterns = includeExcludeList(dir, 1);
            List<String> excludePatterns = includeExcludeList(dir, 2);
            BiPredicate<Path, Path> includes = PathPredicates.matches(includePatterns, excludePatterns);
            Path resourcesDirPath = projectDir.resolve(resourcesDir);
            if (Files.exists(resourcesDirPath)) {
                BuildRootType buildRootType = BuildRootType.create(DirectoryType.Resources, includes);
                BuildRoot sources = createBuildRoot(buildRootType, resourcesDirPath);
                for (BuildRoot classes : classesRoots) {
                    builder.component(createBuildComponent(sources, classes, compileSteps()));
                }
            }
        }

        // Add custom components

        for (CustomDirectoryConfig customDir : buildConfig.incrementalBuild().customDirectories()) {
            Path directory = customDir.path();
            if (Files.exists(directory)) {
                BiPredicate<Path, Path> includes = customDir.includes();
                BuildRootType buildRootType = BuildRootType.create(DirectoryType.Custom, includes);
                BuildRoot sources = createBuildRoot(buildRootType, directory);
                for (BuildRoot classes : classesRoots) {
                    builder.component(createBuildComponent(sources, classes, customDirectorySteps(customDir)));
                }
            } else {
                Log.warn("%s not found", directory);
            }
        }

        // Main class
        builder.mainClassName(projectConfig.property(PROJECT_MAINCLASS));

        return builder.build();
    }

    private static List<String> includeExcludeList(String[] resourceDir, int index) {
        if (resourceDir.length > index) {
            final String list = resourceDir[index];
            if (!list.isEmpty()) {
                return Arrays.asList(list.split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR));
            }
        }
        return emptyList();
    }

    private List<BuildStep> compileSteps() {
        return new ArrayList<>(buildConfig.incrementalBuild().javaSourceGoals());
    }

    private List<BuildStep> resourcesSteps() {
        return new ArrayList<>(buildConfig.incrementalBuild().resourceGoals());
    }

    private List<BuildStep> customDirectorySteps(CustomDirectoryConfig customDir) {
        return new ArrayList<>(customDir.goals());
    }
}
