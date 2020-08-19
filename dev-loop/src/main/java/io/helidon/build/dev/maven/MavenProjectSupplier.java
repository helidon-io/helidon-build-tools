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
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.Requirements;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;

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
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_DEPENDENCIES;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_EXCLUDES;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCE_INCLUDES;
import static io.helidon.build.util.ProjectConfig.projectConfig;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

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

    private final MavenProject project;
    private final MavenSession session;
    private final BuildPluginManager plugins;
    private final DevLoopBuildConfig buildConfig;
    private final List<String> cleanBuildCmd;
    private final List<String> buildCmd;
    private final AtomicBoolean firstBuild;
    private BuildType buildType;
    private ProjectConfig config;

    /**
     * Constructor.
     *  @param project The maven project.
     * @param session The maven session.
     * @param plugins The maven plugin manager.
     * @param buildConfig The build configuration.
     */
    public MavenProjectSupplier(MavenProject project,
                                MavenSession session,
                                BuildPluginManager plugins,
                                DevLoopBuildConfig buildConfig) {
        MavenProjectConfigCollector.assertSupportedProject(session);
        this.project = project;
        this.session = requireNonNull(session);
        this.plugins = requireNonNull(plugins);
        this.buildConfig = requireNonNull(buildConfig);
        this.firstBuild = new AtomicBoolean(true);
        this.cleanBuildCmd =  List.of(CLEAN_ARG, buildConfig.fullBuildPhase(), SKIP_TESTS_ARG, ENABLE_HELIDON_CLI);
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
        config = projectConfig(executor.projectDirectory());
        Requirements.require(config.lastSuccessfulBuildTime() > 0,
                "$(cyan helidon-maven-plugin) must be configured as an extension");
    }

    private boolean canSkipBuild(Path projectDir) {

        // Is this our first request in this session?

        if (firstBuild.getAndSet(false)) {

            // Yes. We can skip the build IFF we have a previously completed build from another session whose build time
            // is more recent than any file in the project (excluding target/* and .*)

            config = projectConfig(projectDir);
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
        final List<String> sourceIncludes = config.propertyAsList(PROJECT_SOURCE_INCLUDES);
        final List<String> sourceExcludes = config.propertyAsList(PROJECT_SOURCE_EXCLUDES);
        final List<String> classesDirs = config.propertyAsList(PROJECT_CLASSDIRS);
        final List<String> resourcesDirs = config.propertyAsList(PROJECT_RESOURCEDIRS);

        // Note that classesDir here is the output

        for (String sourceDir : sourceDirs) {
            for (String classesDir : classesDirs) {
                Path sourceDirPath = assertDir(projectDir.resolve(sourceDir));
                Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                // TODO sourceIncludes / sourceExcludes
                BuildRoot sources = createBuildRoot(BuildRootType.JavaSources, sourceDirPath);
                BuildRoot classes = createBuildRoot(BuildRootType.JavaClasses, classesDirPath);
                builder.component(createBuildComponent(sources, classes, compileStep()));
            }
        }

        for (String resourcesDirEntry : resourcesDirs) {
            for (String classesDir : classesDirs) {
                String[] dir = resourcesDirEntry.split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_SEPARATOR);
                String resourcesDir = dir[0];
                List<String> includes = includeExcludeList(dir, 1);
                List<String> excludes = includeExcludeList(dir, 2);
                // TODO includes / excludes!
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

    private static List<String> includeExcludeList(String[] resourceDir, int index) {
        if (resourceDir.length > index) {
            final String list = resourceDir[index];
            if (!list.isEmpty()) {
                return Arrays.asList(list.split(ProjectConfig.RESOURCE_INCLUDE_EXCLUDE_LIST_SEPARATOR));
            }
        }
        return emptyList();
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
