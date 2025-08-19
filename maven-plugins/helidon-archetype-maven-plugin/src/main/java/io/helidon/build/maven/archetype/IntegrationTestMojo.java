/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.Expression;
import io.helidon.build.archetype.engine.v2.ScriptCompiler;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.ansi.AnsiConsoleInstaller;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.maven.archetype.config.Validation;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.Strings.padding;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.Cyan;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static io.helidon.build.maven.archetype.MojoHelper.MAVEN_ARCHETYPE_PLUGIN;
import static io.helidon.build.maven.archetype.ReflectionHelper.invokeMethod;
import static java.nio.file.FileSystems.newFileSystem;

/**
 * {@code archetype:integration-test} mojo.
 */
@Mojo(name = "integration-test")
public class IntegrationTestMojo extends AbstractMojo {

    private static final String SEP = AnsiConsoleInstaller.areAnsiEscapesEnabled() ? "  " : "  =  ";
    private static final String MAVEN_GENERATOR_FCN = MavenArchetypeGenerator.class.getName();

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
     * Tests directory.
     */
    @Parameter(property = "archetype.test.projectsDirectory", defaultValue = "${project.build.directory}/tests",
               required = true)
    private File testsDirectory;

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
     * The profiles to use when building archetypes.
     */
    @Parameter(property = "archetype.test.testProfiles")
    private List<String> testProfiles = List.of();

    /**
     * External values to use when generating archetypes.
     */
    @Parameter(property = "archetype.test.externalValues")
    private Map<String, String> externalValues;

    /**
     * External defaults to use when generating archetypes.
     */
    @Parameter(property = "archetype.test.externalDefaults")
    private Map<String, String> externalDefaults;

    /**
     * File that contains rules to filter the variations.
     */
    @Parameter(property = "archetype.test.rulesFile")
    private File rulesFile;

    /**
     * Whether to generate input variations.
     */
    @Parameter(property = "archetype.test.generateVariations", defaultValue = "true")
    private boolean generateVariations;

    /**
     * Whether to only generate input variations.
     */
    @Parameter(property = "archetype.test.variationsOnly", defaultValue = "false")
    private boolean variationsOnly;

    /**
     * Variation start index.
     */
    @Parameter(property = "archetype.test.variationStartIndex", defaultValue = "1")
    private int variationStartIndex;

    /**
     * Variation end index.
     */
    @Parameter(property = "archetype.test.variationEndIndex", defaultValue = "-1")
    private int variationEndIndex;

    /**
     * Variations to process.
     */
    @Parameter(property = "archetype.test.variation")
    private String variation;

    /**
     * Invoker environment variables.
     */
    @Parameter(property = "archetype.test.invokerEnvVars")
    private Map<String, String> invokerEnvVars;

    @Component
    private RepositorySystem repoSystem;

    @Component
    private PluginContainerManager pluginContainerManager;

    /**
     * Generated code inspection.
     */
    @Parameter
    private List<Validation> validations;

    private Set<Map<String, String>> variations;
    private int index = 1;
    private Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();

        if (skip) {
            return;
        }

        // support skipTests
        String skipTests = session.getUserProperties().getProperty("skipTests");
        if (skipTests != null && !"false".equalsIgnoreCase(skipTests)) {
            return;
        }

        File archetypeFile = project.getArtifact().getFile();
        if (archetypeFile == null) {
            throw new MojoFailureException("Archetype not found");
        }

        String testName = project.getFile().toPath().getParent().getFileName().toString();
        try {
            if (generateVariations) {
                logVariations(testName);

                log.info("");
                log.info("Computing variations...");
                variations = variations(archetypeFile.toPath());
                log.info("");
                log.info("Total projects: " + variations.size());
                log.info("Markdown file: " + writeSummary());
                log.info("CSV file: " + writeCsv());

                if (variationsOnly) {
                    return;
                }

                Set<String> artifactIds = new HashSet<>();
                Map<Integer, Map<String, String>> variations = filterVariations();
                for (Map.Entry<Integer, Map<String, String>> entry : variations.entrySet()) {
                    index = entry.getKey();
                    Map<String, String> variation = entry.getValue();
                    String artifactId = variation.getOrDefault("artifactId", "my-project");
                    if (!artifactIds.add(artifactId)) {
                        variation.put("artifactId", artifactId + "-" + index);
                    }
                    processIntegrationTest(testName, variation, archetypeFile);
                }
            } else {
                processIntegrationTest(testName, externalValues, archetypeFile);
            }

        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Integration test failed with error(s)");
        }
    }

    private Set<Map<String, String>> variations(Path archetypeFile) {
        try (FileSystem fs = newFileSystem(archetypeFile, this.getClass().getClassLoader())) {
            Path cwd = fs.getPath("/");
            ScriptCompiler compiler = new ScriptCompiler(() -> cwd.resolve("main.xml"), cwd);
            return compiler.variations(rules());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Path writeSummary() {
        try {
            Path testsDir = testsDirectory.toPath();
            Path file = testsDir.resolve("projects.md");
            Files.createDirectories(testsDir);
            try (PrintWriter printer = new PrintWriter(Files.newBufferedWriter(file))) {
                printer.println("# Projects Summary");
                printer.println("\nTotal projects: " + variations.size());
                int i = 1;
                for (Map<String, String> variation : variations) {
                    printer.println("\nProject " + i++ + ":");
                    printer.println("```shell");
                    printer.println("helidon init --batch \\");
                    Iterator<Entry<String, String>> it = variation.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<String, String> entry = it.next();
                        printer.print("    -D" + entry.getKey() + "=" + entry.getValue());
                        if (it.hasNext()) {
                            printer.println(" \\");
                        } else {
                            printer.println();
                        }
                    }
                    printer.println("```");
                }
                printer.flush();
            }
            return file;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Path writeCsv() {
        Path testsDir = testsDirectory.toPath();
        Path file = testsDir.resolve("projects.csv");
        try (PrintWriter csvWriter = new PrintWriter(Files.newBufferedWriter(file))) {
            for (Map<String, String> variation : variations) {
                String line = Lists.join(Maps.entries(variation), e -> e.getKey() + "=" + e.getValue(), " ");
                csvWriter.println(line);
            }
            csvWriter.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return file;
    }

    private Map<Integer, Map<String, String>> filterVariations() {
        Map<Integer, Map<String, String>> indexes = new LinkedHashMap<>();
        if (variation == null || variation.isEmpty()) {
            Iterator<Map<String, String>> it = this.variations.iterator();
            for (int i = 1; it.hasNext(); i++) {
                Map<String, String> next = it.next();
                if (i >= variationStartIndex && (variationEndIndex <= 0 || i <= variationEndIndex)) {
                    indexes.put(i, next);
                }
            }
        } else {
            List<Integer> indices = Arrays.stream(variation.split(","))
                    .map(Integer::valueOf)
                    .toList();
            Iterator<Map<String, String>> it = variations.iterator();
            for (int i = 1; it.hasNext(); i++) {
                Map<String, String> next = it.next();
                if (indices.contains(i)) {
                    indexes.put(i, next);
                }
            }
        }
        return indexes;
    }

    private void processIntegrationTest(String testName,
                                        Map<String, String> externalValues,
                                        File archetypeFile) throws IOException, MojoExecutionException {

        logTestDescription(testName, externalValues);

        Properties props = new Properties();
        props.putAll(externalValues);

        Path testsDir = testsDirectory.toPath();
        ensureDirectory(testsDir);
        String projectName = props.getProperty("artifactId", "myproject");
        Path outputDir = unique(testsDir, projectName);
        projectName = outputDir.getFileName().toString();
        props.setProperty("artifactId", projectName);

        if (mavenArchetypeCompatible) {
            log.info("Generating project '" + projectName + "' using Maven archetype");
            System.setProperty("interactiveMode", "false");
            mavenCompatGenerate(
                    project.getGroupId(),
                    project.getArtifactId(),
                    project.getVersion(),
                    archetypeFile,
                    props,
                    outputDir.getParent());
        } else {
            log.info("Generating project '" + projectName + "' using Helidon archetype engine");
            generate(archetypeFile.toPath(), props, outputDir);
        }

        invokePostArchetypeGenerationGoals(outputDir.toFile());
    }

    private void logVariations(String testName) {
        log.info("");
        log.info("--------------------------------------");
        log.info("Generating Archetype Tests Variations");
        log.info("--------------------------------------");
        log.info("");
        log.info(Bold.apply("Test: ") + BoldBlue.apply(testName));
        int maxKeyWidth = maxKeyWidth(externalValues, externalDefaults);
        logInputs("externalValues", externalValues, maxKeyWidth);
        logInputs("externalDefaults", externalDefaults, maxKeyWidth);
    }

    private void logTestDescription(String testName, Map<String, String> externalValues) {
        String description = Bold.apply("Test: ") + BoldBlue.apply(testName);
        if (variations != null && index > 0) {
            description += BoldBlue.apply(String.format(", variation: %s/%s", index, variations.size()));
        }
        log.info("");
        log.info("-------------------------------------");
        log.info("Processing Archetype Integration Test");
        log.info("-------------------------------------");
        log.info("");
        log.info(description);
        int maxKeyWidth = maxKeyWidth(externalValues);
        logInputs("externalValues", externalValues, maxKeyWidth);
        log.info("");
    }

    private void logInputs(String label, Map<String, String> inputs, int maxKeyWidth) {
        boolean empty = inputs.isEmpty();
        log.info("");
        log.info(Bold.apply(label) + ":" + (empty ? Italic.apply(" [none]") : ""));
        if (!empty) {
            log.info("");
            inputs.forEach((k, v) -> {
                String padding = padding(" ", maxKeyWidth, k);
                log.info("    " + Cyan.apply(k) + padding + SEP + BoldBlue.apply(v));
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private static int maxKeyWidth(Map... maps) {
        int maxLen = 0;
        for (Map map : maps) {
            for (Object key : map.keySet()) {
                int len = key.toString().length();
                if (len > maxLen) {
                    maxLen = len;
                }
            }
        }
        return maxLen;
    }

    private void generate(Path archetypeFile, Properties props, Path outputDir) {
        try {
            FileSystem fileSystem = newFileSystem(archetypeFile, this.getClass().getClassLoader());
            ArchetypeEngineV2 engine = ArchetypeEngineV2.builder()
                    .fileSystem(fileSystem)
                    .batch(true)
                    .externalValues(Maps.fromProperties(props))
                    .output(() -> outputDir)
                    .build();
            engine.generate();
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
        try {
            InstallRequest request = new InstallRequest();
            request.addArtifact(RepositoryUtils.toArtifact(new ProjectArtifact(project)));
            request.addArtifact(RepositoryUtils.toArtifact(project.getArtifact()));
            repoSystem.install(session.getRepositorySession(), request);
        } catch (InstallationException ex) {
            throw new MojoExecutionException("Unable to pre-install project", ex);
        }

        PlexusContainer container = pluginContainerManager.create(MAVEN_ARCHETYPE_PLUGIN,
                project.getRemotePluginRepositories(),
                session.getRepositorySession());

        Object[] args = new Object[] {
                container,
                archetypeGroupId,
                archetypeArtifactId,
                archetypeVersion,
                archetypeFile,
                properties,
                basedir,
                session
        };
        invokeMethod(container.getLookupRealm(), MAVEN_GENERATOR_FCN, "generate", args);
    }

    private void invokePostArchetypeGenerationGoals(File basedir) throws IOException, MojoExecutionException {
        FileLogger logger = setupBuildLogger(basedir);

        getLog().info(String.format("Invoking post-archetype-generation goal: %s, profiles: %s", testGoal, testProfiles));

        File localRepo = session.getRepositorySession().getLocalRepository().getBasedir();
        InvocationRequest request = new DefaultInvocationRequest()
                .setUserSettingsFile(session.getRequest().getUserSettingsFile())
                .setLocalRepositoryDirectory(localRepo)
                .setBaseDirectory(basedir)
                .addArgs(List.of(testGoal))
                .setProfiles(testProfiles)
                .setBatchMode(true)
                .setShowErrors(true)
                .setDebug(debug)
                .setShowVersion(showVersion);

        invokerEnvVars.forEach(request::addShellEnvironment);

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

            // validate projects
            if (validations != null) {
                for (Validation validation : validations) {
                    validation.validate(basedir);
                }
            }
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

    private List<Expression> rules() {
        if (rulesFile == null) {
            return List.of();
        }
        try (InputStream is = Files.newInputStream(rulesFile.toPath())) {
            List<Expression> excludes = new ArrayList<>();
            XMLElement root = XMLElement.parse(is);
            for (XMLElement elt : root.traverse(it -> it.name().equals("exclude"))) {
                Expression exclude = Expression.TRUE;
                for (XMLElement n = elt; n.parent() != null; n = n.parent()) {
                    exclude = exclude.and(Expression.create(n.attribute("if")));
                }
                excludes.add(exclude);
            }
            return excludes;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
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
