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
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.BatchInputResolver;
import io.helidon.build.archetype.engine.v2.util.InputCombinations;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.Maps;

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

    /**
     * The goal to use when building archetypes.
     */
    @Parameter(property = "archetype.test.testGoal", defaultValue = "package")
    private String testGoal;

    /**
     * External values to use when generating archetypes.
     */
    @Parameter(property = "archetype.test.externalValues")
    private Map<String, String> externalValues;

    /**
     * Whether to generate input combinations.
     */
    @Parameter(property = "archetype.test.generateCombinations", defaultValue = "true")
    private boolean generateCombinations;

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

        File archetypeFile = project.getArtifact().getFile();
        if (archetypeFile == null) {
            throw new MojoFailureException("Archetype not found");
        }

        if (!generateCombinations && externalValues.isEmpty()) {
            throw new MojoExecutionException("Either generateCombinations must be true or externalValues must be provided");
        }

        String testName = project.getFile().toPath().getParent().getFileName().toString();
        try {
            if (!externalValues.isEmpty()) {
                processIntegrationTest(testName, externalValues, archetypeFile);
            }
            if (generateCombinations) {
                InputCombinations combinations = InputCombinations.builder()
                                                                  .archetypePath(archetypeFile.toPath())
                                                                  .build();
                int combinationNumber = 0;
                for (Map<String, String> combination : combinations) {
                    processIntegrationTest(testName + ", combination " + combinationNumber++, combination, archetypeFile);
                }
            }

        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Integration test failed with error(s)");
        }
    }

    private void processIntegrationTest(String testDescription,
                                        Map<String, String> externalValues,
                                        File archetypeFile) throws IOException, MojoExecutionException {

        getLog().info("Processing Archetype IT project: " + testDescription);
        Properties props = new Properties();
        props.putAll(externalValues);

        // REMOVE this when https://github.com/oracle/helidon-build-tools/issues/590 is fixed.
        if (!externalValues.containsKey("name")) {
            props.put("name", "TODO-remove-me");
        }

        Path ourProjectDir = project.getFile().toPath();
        Path projectsDir =  ourProjectDir.getParent().resolve("target/projects");
        FileUtils.ensureDirectory(projectsDir);
        Path outputDir = projectsDir.resolve(props.getProperty("artifactId"));
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

        invokePostArchetypeGenerationGoals(outputDir.toFile());
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

    private void invokePostArchetypeGenerationGoals(File basedir) throws IOException, MojoExecutionException {

        FileLogger logger = setupBuildLogger(basedir);

        getLog().info("Invoking post-archetype-generation goal: " + testGoal);
        InvocationRequest request = new DefaultInvocationRequest()
                .setBaseDirectory(basedir)
                .setGoals(List.of(testGoal))
                .setBatchMode(true)
                .setShowErrors(true)
                .setDebug(debug)
// TODO REMOVE  .addShellEnvironment("MAVEN_DEBUG_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8002")
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
