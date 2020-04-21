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
package io.helidon.build.archetype.maven;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.ArchetypeEngine;

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

/**
 * {@code archetype:integration-test} mojo.
 */
@Mojo(name = "integration-test", requiresProject = true)
public class IntegrationTestMojo extends AbstractMojo {

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
     * Skip the integration test.
     */
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
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

        try {
            List<Path> projectGoals = Files.walk(testProjectsDirectory.toPath())
                    .filter((p) -> p.endsWith("goal.txt"))
                    .collect(Collectors.toList());
            if (projectGoals.isEmpty()) {
                getLog().warn("No projects directory with goal.txt found");
                return;
            }

            for (Path goal : projectGoals) {
                processIntegrationTest(goal, archetypeFile);
            }
        } catch (IOException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private void processIntegrationTest(Path projectGoal, File archetypeFile) throws IOException, MojoExecutionException {
        getLog().info("Processing Archetype IT project: " + projectGoal.getParent().toString());
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(projectGoal.getParent().resolve("archetype.properties"))) {
            props.load(in);
        }
        // FIXME
        props.put("maven", "true");
        props.put("name", "test project");
        Path outputDir = projectGoal.getParent().resolve("project");
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(outputDir);
        new ArchetypeEngine(archetypeFile, props).generate(outputDir.toFile());
        List<String> goals = Files.readAllLines(projectGoal).stream()
                .flatMap(g -> Stream.of(g.split(" ")))
                .collect(Collectors.toList());
        if (!goals.isEmpty()) {
            invokePostArchetypeGenerationGoals(goals, outputDir.toFile());
        }
    }

    private void invokePostArchetypeGenerationGoals(List<String> goals, File basedir)
            throws IOException, MojoExecutionException {

        FileLogger logger = setupLogger(basedir);

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
                for (Map.Entry<String, String> entry : properties.entrySet()) {
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

    private FileLogger setupLogger(File basedir) throws IOException {
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
        public void close() throws IOException {
            stream.close();
        }
    }
}
