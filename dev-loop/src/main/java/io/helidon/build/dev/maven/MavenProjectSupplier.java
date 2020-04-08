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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import io.helidon.build.dev.maven.ProjectConfigCollector.IncrementalBuildStrategy;
import io.helidon.build.dev.steps.CompileJavaSources;
import io.helidon.build.dev.steps.CopyResources;
import io.helidon.build.util.ConfigProperties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import static io.helidon.build.dev.BuildComponent.createBuildComponent;
import static io.helidon.build.dev.BuildFile.createBuildFile;
import static io.helidon.build.dev.BuildRoot.createBuildRoot;
import static io.helidon.build.dev.ProjectDirectory.createProjectDirectory;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_CLASSPATH;
import static io.helidon.build.util.ProjectConfig.PROJECT_INCREMENTAL_BUILD_STRATEGY;
import static io.helidon.build.util.ProjectConfig.PROJECT_MAINCLASS;
import static io.helidon.build.util.ProjectConfig.PROJECT_RESOURCEDIRS;
import static io.helidon.build.util.ProjectConfig.PROJECT_SOURCEDIRS;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final List<String> CLEAN_BUILD_COMMAND = List.of("clean", "prepare-package", "-DskipTests");
    private static final List<String> BUILD_COMMAND = List.of("prepare-package", "-DskipTests");

    static final String POM_FILE = "pom.xml";

    private final AtomicReference<MavenProject> project;
    private final MavenSession session;
    private final BuildPluginManager plugins;
    private final ProjectBuilder projectBuilder;
    private final ConfigProperties config;

    /**
     * Constructor.
     *
     * @param project The maven project.
     * @param session The maven session.
     * @param plugins The maven plugin manager.
     * @param projectBuilder The maven project builder.
     */
    public MavenProjectSupplier(MavenProject project,
                                MavenSession session,
                                BuildPluginManager plugins,
                                ProjectBuilder projectBuilder) {
        this.project = new AtomicReference<>(project);
        this.session = session;
        this.plugins = plugins;
        this.projectBuilder = projectBuilder;
        this.config = new ConfigProperties(DOT_HELIDON);
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
        executor.monitor().onBuildStart(cycleNumber, clean ? BuildType.CleanComplete : BuildType.Complete);

        // TODO: We could work harder to avoid the initial build (e.g. IFF config file contains a lastModifiedTime newer
        //       than *any* file in the project; this requires updating the config after the build completes, every time).

        executor.execute(clean ? CLEAN_BUILD_COMMAND : BUILD_COMMAND);

        /*

        if (clean) {
            MavenGoalExecutor.builder()
                             .goal(MavenGoalExecutor.CLEAN_GOAL)
                             .mavenProject(project.get())
                             .mavenSession(session)
                             .pluginManager(plugins)
                             .build()
                             .execute();
        }

        // Build

        // executor.execute(BUILD_COMMAND);
        updateMavenProject();
        MavenGoalExecutor.builder()
                         .mavenProject(project.get())
                         .mavenSession(session)
                         .pluginManager(plugins)
                         .goal(MavenGoalExecutor.COMPILE_GOAL)
                         .build()
                         .execute();
*/

        // Load config
        config.load();

        // Create and return the project based on the config
        return createProject(executor.projectDirectory());
    }

    // TODO source (and resource?) includes/excludes for change watch!
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
        final IncrementalBuildStrategy buildStrategy = buildStrategy();

        for (String sourceDir : sourceDirs) {
            for (String classesDir : classesDirs) {
                Path sourceDirPath = assertDir(projectDir.resolve(sourceDir));
                Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                BuildRoot sources = createBuildRoot(BuildRootType.JavaSources, sourceDirPath);
                BuildRoot classes = createBuildRoot(BuildRootType.JavaClasses, classesDirPath);
                builder.component(createBuildComponent(sources, classes, compileStep(buildStrategy)));
            }
        }

        for (String resourcesDir : resourcesDirs) {
            for (String classesDir : classesDirs) {
                Path resourcesDirPath = projectDir.resolve(resourcesDir);
                if (Files.exists(resourcesDirPath)) {
                    Path classesDirPath = ensureDirectory(projectDir.resolve(classesDir));
                    BuildRoot sources = createBuildRoot(BuildRootType.Resources, resourcesDirPath);
                    BuildRoot classes = createBuildRoot(BuildRootType.Resources, classesDirPath);
                    builder.component(createBuildComponent(sources, classes, resourcesStep(buildStrategy)));
                }
            }
        }

        // Main class
        builder.mainClassName(config.property(PROJECT_MAINCLASS));

        return builder.build();
    }

    private IncrementalBuildStrategy buildStrategy() {
        return IncrementalBuildStrategy.parse(config.property(PROJECT_INCREMENTAL_BUILD_STRATEGY));
    }

    private BuildStep compileStep(IncrementalBuildStrategy strategy) {
        switch (strategy) {
            case JAVAC:
                return new CompileJavaSources(StandardCharsets.UTF_8, false);
            case MAVEN:
                return MavenGoalBuildStep.builder()
                                         .mavenProject(project.get())
                                         .mavenSession(session)
                                         .pluginManager(plugins)
                                         .goal(MavenGoalExecutor.COMPILE_GOAL)
                                         .build();
            default:
                throw new UnsupportedOperationException("Unknown incremental build strategy");
        }
    }

    private BuildStep resourcesStep(IncrementalBuildStrategy strategy) {
        switch (strategy) {
            case JAVAC:
                return new CopyResources();
            case MAVEN:
                return MavenGoalBuildStep.builder()
                                         .mavenProject(project.get())
                                         .mavenSession(session)
                                         .pluginManager(plugins)
                                         .goal(MavenGoalExecutor.RESOURCES_GOAL)
                                         .build();
            default:
                throw new UnsupportedOperationException("Unknown incremental build strategy");
        }
    }

    private void updateMavenProject() throws Exception {
        final Artifact artifact = project.get().getArtifact();
        final ProjectBuildingRequest request = session.getProjectBuildingRequest()
                                                      .setResolveDependencies(true);
        final MavenProject newProject = projectBuilder.build(artifact, request).getProject();
        project.set(newProject);
        session.setCurrentProject(newProject);
    }
}
