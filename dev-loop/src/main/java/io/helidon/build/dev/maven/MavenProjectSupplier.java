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

package io.helidon.build.dev.maven;

import java.io.File;
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
import io.helidon.build.util.PathFilters;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.Requirements;

import static io.helidon.build.dev.BuildComponent.createBuildComponent;
import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.dev.BuildRoot.createBuildRoot;
import static io.helidon.build.dev.BuildRootType.matchesJavaClass;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.Constants.ENABLE_HELIDON_CLI;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.FIRST;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.LATEST;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
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
    private static final List<String> PASS_THROUGH_PROPERTIES = List.of("version.plugin.helidon-cli", "maven.repo.local");
    private static final List<String> DEFAULT_EXCLUDES = List.of("**/.*.swp");
    private static final String CLEAN_ARG = "clean";
    private static final String SKIP_TESTS_ARG = "-DskipTests";
    private static final String TARGET_DIR_NAME = "target";
    private static final String POM_FILE = "pom.xml";
    private static final String DOT = ".";
    private static final List<String> FS_ROOTS = Arrays.stream(File.listRoots())
                                                       .map(File::getPath)
                                                       .collect(Collectors.toList());

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
     * @param config The build configuration.
     */
    public MavenProjectSupplier(DevLoopBuildConfig config) {
        this.buildConfig = config;
        this.firstBuild = new AtomicBoolean(true);
        this.cleanBuildCmd = command(CLEAN_ARG, config.fullBuild().phase(), SKIP_TESTS_ARG, ENABLE_HELIDON_CLI);
        this.buildCmd = command(config.fullBuild().phase(), SKIP_TESTS_ARG, ENABLE_HELIDON_CLI);
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
        final List<String> command = clean ? cleanBuildCmd : buildCmd;
        executor.monitor().onBuildStart(cycleNumber, buildType);
        executor.execute(command);
        projectConfig = projectConfig(executor.projectDirectory());
        Requirements.require(projectConfig.lastSuccessfulBuildTime() > 0,
                             "$(cyan helidon-cli-maven-plugin) must be configured as an extension");
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
        builder.buildFile(createBuildFile(root, pomFile));

        // Dependencies
        final List<String> dependencies = projectConfig.propertyAsList(PROJECT_DEPENDENCIES);
        for (String dependency : dependencies) {
            builder.dependency(Path.of(dependency));
        }

        // Main class
        builder.mainClassName(projectConfig.property(PROJECT_MAINCLASS));

        // Finally, add build components

        final List<String> sourceDirs = projectConfig.propertyAsList(PROJECT_SOURCEDIRS);
        final List<String> sourceIncludes = projectConfig.propertyAsList(PROJECT_SOURCE_INCLUDES);
        final List<String> sourceExcludes = projectConfig.propertyAsList(PROJECT_SOURCE_EXCLUDES);
        final List<String> classesDirs = projectConfig.propertyAsList(PROJECT_CLASSDIRS);
        final List<String> resourcesDirs = projectConfig.propertyAsList(PROJECT_RESOURCEDIRS);

        // Map classesDirs to a list of BuildRoots so can re-use as the outputRoot for all components
        // See issue https://github.com/oracle/helidon-build-tools/issues/280 regarding output roots

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
            BiPredicate<Path, Path> filter = filter(sourceIncludes, sourceExcludes);
            BuildRootType sourceRootType = BuildRootType.create(DirectoryType.JavaSources, filter);
            BuildRoot sources = createBuildRoot(sourceRootType, sourceDirPath);
            for (BuildRoot classes : classesRoots) {
                builder.component(createBuildComponent(sources, classes, compileSteps()));
            }
        }

        // Add resource components

        for (String resourcesDirEntry : resourcesDirs) {

            // capture the file system root part of the entry
            // on Windows this will be the drive (E.g. C:\), on Unix, just a slash
            String prefix = FS_ROOTS.stream()
                    .filter(resourcesDirEntry::startsWith)
                    .findFirst()
                    .orElse("");

            // split the non prefix part of the entry
            String[] dir = resourcesDirEntry
                    .substring(prefix.length())
                    .split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_SEPARATOR);

            String resourcesDir = prefix + dir[0];
            List<String> includes = includeExcludeList(dir, 1);
            List<String> excludes = includeExcludeList(dir, 2);
            BiPredicate<Path, Path> filter = filter(includes, excludes);

            // resourcesDirPath may not be nested inside projectDir if resourcesDir is absolute
            Path resourcesDirPath = projectDir.resolve(resourcesDir);
            if (Files.isDirectory(resourcesDirPath)) {
                BuildRootType buildRootType = BuildRootType.create(DirectoryType.Resources, filter);
                BuildRoot resources = createBuildRoot(buildRootType, resourcesDirPath);
                for (BuildRoot classes : classesRoots) {
                    builder.component(createBuildComponent(resources, classes, resourcesSteps()));
                }
            }
        }

        // Add custom components
        // See issue https://github.com/oracle/helidon-build-tools/issues/280 regarding custom output roots

        for (CustomDirectoryConfig customDir : buildConfig.incrementalBuild().customDirectories()) {
            Path directory = customDir.path();
            if (Files.isDirectory(directory)) {
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

        return builder.build();
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

    private static BiPredicate<Path, Path> filter(List<String> includes, List<String> excludes) {
        return PathFilters.matches(includes, addDefaultExcludes(excludes));
    }

    private static List<String> addDefaultExcludes(List<String> excludes) {
        if (excludes.isEmpty()) {
            return DEFAULT_EXCLUDES;
        } else {
            final List<String> result = new ArrayList<>(excludes);
            result.addAll(DEFAULT_EXCLUDES);
            return result;
        }
    }

    private static List<String> includeExcludeList(String[] resourceDir, int index) {
        if (resourceDir.length > index) {
            final String separatedList = resourceDir[index];
            if (!separatedList.isEmpty()) {
                return Arrays.stream(separatedList.split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR))
                             .filter(item -> !item.isEmpty())
                             .collect(Collectors.toList());
            }
        }
        return emptyList();
    }

    private static List<String> command(String... arguments) {
        final List<String> command = new ArrayList<>(Arrays.asList(arguments));
        PASS_THROUGH_PROPERTIES.forEach(property -> {
            String value = System.getProperty(property);
            if (value != null) {
                command.add("-D" + property + "=" + value);
            }
        });
        return command;
    }
}
