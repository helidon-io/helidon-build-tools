/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.BatchInputResolver;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.Maps;
import io.helidon.build.common.SubstitutionVariables;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.exception.ArchetypeNotConfigured;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstallerException;
import org.apache.maven.shared.transfer.project.NoFileAssignedException;
import org.apache.maven.shared.transfer.project.install.ProjectInstaller;
import org.apache.maven.shared.transfer.project.install.ProjectInstallerRequest;
import org.codehaus.plexus.util.StringUtils;

import static java.nio.file.FileSystems.newFileSystem;

/**
 * {@code archetype:integration-test} mojo.
 */
@Mojo(name = "integration-test")
public class IntegrationTestMojo extends AbstractMojo {

    /**
     * Archetype generate to invoke Maven compatible archetypes.
     */
    @Component
    private ArchetypeGenerator archetypeGenerator;

    /**
     * Maven invoker.
     */
    @Component
    private Invoker invoker;

    /**
     * The archetype project to execute the integration tests on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The {@link MavenSession}.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Skip the integration test.
     */
    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(property = "archetype.test.skip")
    private boolean skip = false;

    /**
     * Directory of test projects.
     */
    @Parameter(property = "archetype.test.projectsDirectory", defaultValue = "${project.build.testOutputDirectory}/projects",
            required = true)
    private File testProjectsDirectory;

    /**
     * Suppress logging to the {@code build.log} file.
     */
    @Parameter(property = "archetype.test.noLog", defaultValue = "false")
    private boolean noLog;

    /**
     * Flag used to determine whether the build logs should be output to the normal mojo log.
     */
    @Parameter(property = "archetype.test.streamLogs", defaultValue = "true")
    private boolean streamLogs;

    /**
     * flag to show the maven version used.
     */
    @Parameter(property = "archetype.test.showVersion", defaultValue = "false")
    private boolean showVersion;

    /**
     * Whether to show debug statements in the build output.
     */
    @Parameter(property = "archetype.test.debug", defaultValue = "false")
    private boolean debug;

    /**
     * Common set of properties to pass in on each project's command line, via -D parameters.
     */
    @Parameter
    private Map<String, String> properties = new HashMap<>();

    /**
     * Indicate if the project should be generated with the maven-archetype-plugin or with the Helidon archetype
     * engine directly.
     */
    @Parameter(defaultValue = "true")
    private boolean mavenArchetypeCompatible;

    @Component
    private ProjectInstaller installer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }

        // support -DskipTests
        String skipTests = session.getUserProperties().getProperty("skipTests");
        if (skipTests != null && !"false".equalsIgnoreCase(skipTests)) {
            return;
        }

        if (!testProjectsDirectory.exists()) {
            getLog().warn("No Archetype IT projects: root 'projects' directory not found.");
            return;
        }

        File archetypeFile = project.getArtifact().getFile();
        if (archetypeFile == null) {
            throw new MojoFailureException("Archetype not found");
        }

        Path testProjects = testProjectsDirectory.toPath();
        try {
            List<Path> projectGoals = Files.walk(testProjects)
                                           .filter((p) -> p.endsWith("goal.txt"))
                                           .collect(Collectors.toList());
            if (projectGoals.isEmpty()) {
                getLog().warn("No projects directory with goal.txt found");
                return;
            }

            List<String> errors = new LinkedList<>();
            for (Path goal : projectGoals) {
                Path itProject = testProjects.relativize(goal.getParent());
                try {
                    getLog().info("Processing Archetype IT project: " + itProject);
                    processIntegrationTest(goal, archetypeFile);
                } catch (Throwable ex) {
                    getLog().error(ex);
                    errors.add(String.format("Test error: project=%s, error=%s", itProject, ex.getMessage()));
                }
            }
            if (!errors.isEmpty()) {
                errors.forEach(getLog()::error);
                throw new MojoExecutionException("Integration test failed with error(s)");
            }
        } catch (IOException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private void processIntegrationTest(Path projectGoal, File archetypeFile) throws IOException, MojoExecutionException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(projectGoal.getParent().resolve("archetype.properties"))) {
            props.load(in);
        }
        props.put("name", "test project");

        // substitute all variable references
        Map<String, String> propsMap = Maps.fromProperties(props);
        SubstitutionVariables propsVariables = SubstitutionVariables.of(propsMap);
        for (Entry<String, String> entry : propsMap.entrySet()) {
            props.setProperty(entry.getKey(), propsVariables.resolve(entry.getValue()));
        }

        Path outputDir = projectGoal.getParent().resolve(props.getProperty("artifactId"));
        FileUtils.deleteDirectory(outputDir);

        if (mavenArchetypeCompatible) {
            mavenCompatGenerate(
                    project.getGroupId(),
                    project.getArtifactId(),
                    project.getVersion(),
                    archetypeFile,
                    props,
                    outputDir.getParent());
        } else {
            generate(archetypeFile.toPath(), props, outputDir);
        }

        List<String> goals = Files.readAllLines(projectGoal).stream()
                                  .flatMap(g -> Stream.of(g.split(" ")))
                                  .collect(Collectors.toList());
        if (!goals.isEmpty()) {
            invokePostArchetypeGenerationGoals(goals, outputDir.toFile());
        }
    }

    private void generate(Path archetypeFile, Properties props, Path outputDir) {
        try {
            FileSystem fileSystem = newFileSystem(archetypeFile, this.getClass().getClassLoader());
            ArchetypeEngineV2 engine = new ArchetypeEngineV2(fileSystem);
            engine.generate(new BatchInputResolver(), Maps.fromProperties(props), Map.of(), n -> outputDir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void mavenCompatGenerate(String archetypeGroupId,
                                     String archetypeArtifactId,
                                     String archetypeVersion,
                                     File archetypeFile,
                                     Properties properties,
                                     Path basedir) throws MojoExecutionException {

        // pre-install the archetype JAR so that the post-generate script can resolve it
        ProjectInstallerRequest projectInstallerRequest = new ProjectInstallerRequest().setProject(project);
        try {
            installer.install(session.getProjectBuildingRequest(), projectInstallerRequest);
        } catch (IOException | ArtifactInstallerException | NoFileAssignedException ex) {
            throw new MojoExecutionException("Unable to pre-install archetype artifact", ex);
        }

        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
                .setArchetypeGroupId(archetypeGroupId)
                .setArchetypeArtifactId(archetypeArtifactId)
                .setArchetypeVersion(archetypeVersion)
                .setGroupId(properties.getProperty("groupId"))
                .setArtifactId(properties.getProperty("artifactId"))
                .setVersion(properties.getProperty("version"))
                .setPackage(properties.getProperty("package"))
                .setOutputDirectory(basedir.toString())
                .setProperties(properties)
                .setProjectBuildingRequest(session.getProjectBuildingRequest());

        ArchetypeGenerationResult result = new ArchetypeGenerationResult();
        archetypeGenerator.generateArchetype(request, archetypeFile, result);

        if (result.getCause() != null) {
            if (result.getCause() instanceof ArchetypeNotConfigured) {
                ArchetypeNotConfigured anc = (ArchetypeNotConfigured) result.getCause();
                throw new MojoExecutionException(
                        "Missing required properties in archetype.properties: "
                                + StringUtils.join(anc.getMissingProperties().iterator(), ", "), anc);
            }
            throw new MojoExecutionException(result.getCause().getMessage(), result.getCause());
        }
    }

    private void invokePostArchetypeGenerationGoals(List<String> goals, File basedir)
            throws IOException, MojoExecutionException {

        FileLogger logger = setupBuildLogger(basedir);

        if (!goals.isEmpty()) {
            getLog().info("Invoking post-archetype-generation goals: " + goals);
            InvocationRequest request = new DefaultInvocationRequest()
                    .setBaseDirectory(basedir)
                    .setGoals(goals)
                    .setBatchMode(true)
                    .setShowErrors(true)
                    .setDebug(debug)
                    .setShowVersion(showVersion);

            if (logger != null) {
                request.setErrorHandler(logger);
                request.setOutputHandler(logger);
            }

            if (!properties.isEmpty()) {
                Properties props = new Properties();
                for (Entry<String, String> entry : properties.entrySet()) {
                    if (entry.getValue() != null) {
                        props.setProperty(entry.getKey(), entry.getValue());
                    }
                }
                request.setProperties(props);
            }

            try {
                InvocationResult result = invoker.execute(request);
                getLog().info("Post-archetype-generation invoker exit code: " + result.getExitCode());
                if (result.getExitCode() != 0) {
                    throw new MojoExecutionException("Execution failure: exit code = " + result.getExitCode(),
                            result.getExecutionException());
                }
            } catch (MavenInvocationException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        } else {
            getLog().info("No post-archetype-generation goals to invoke.");
        }
    }

    private FileLogger setupBuildLogger(File basedir) throws IOException {
        FileLogger logger = null;
        if (!noLog) {
            File outputLog = new File(basedir, "build.log");
            if (streamLogs) {
                logger = new FileLogger(outputLog, getLog());
            } else {
                logger = new FileLogger(outputLog, null);
            }
            getLog().debug("build log initialized in: " + outputLog);
        }
        return logger;
    }

    private static final class FileLogger implements InvocationOutputHandler, Closeable {

        private final PrintStream stream;
        private final Log log;

        FileLogger(File outputFile, Log log) throws IOException {
            this.log = log;
            //noinspection ResultOfMethodCallIgnored
            outputFile.getParentFile().mkdirs();
            stream = new PrintStream(new FileOutputStream(outputFile));
        }

        @Override
        public void consumeLine(String line) {
            stream.println(line);
            stream.flush();
            if (log != null) {
                log.info(line);
            }
        }

        @Override
        public void close() {
            stream.close();
        }
    }
}
