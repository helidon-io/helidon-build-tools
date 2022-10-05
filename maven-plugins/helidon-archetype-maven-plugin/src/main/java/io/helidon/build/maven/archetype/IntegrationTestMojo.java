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
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ArchetypeEngineV2;
import io.helidon.build.archetype.engine.v2.BatchInputResolver;
import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.util.InputPermutations;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.ansi.AnsiConsoleInstaller;

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

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.Strings.padding;
import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;
import static io.helidon.build.common.ansi.AnsiTextStyles.Cyan;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static java.nio.file.FileSystems.newFileSystem;

/**
 * {@code archetype:integration-test} mojo.
 */
@Mojo(name = "integration-test")
public class IntegrationTestMojo extends AbstractMojo {
    private static final String SEP = AnsiConsoleInstaller.areAnsiEscapesEnabled() ? "  " : "  =  ";

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
     * External defaults to use when generating archetypes.
     */
    @Parameter(property = "archetype.test.externalDefaults")
    private Map<String, String> externalDefaults;

    /**
     * Input filters to use when computing permutations.
     */
    @Parameter(property = "archetype.test.inputFilters")
    private List<String> inputFilters;

    /**
     * File that contains input filters to use when computing permutations.
     */
    @Parameter(property = "archetype.test.inputFiltersFile")
    private File inputFiltersFile;

    /**
     * Permutation filters to filter computed permutations.
     */
    @Parameter(property = "archetype.test.permutationFilters")
    private List<String> permutationFilters;

    /**
     * File that contains filters to filter computed permutations.
     */
    @Parameter(property = "archetype.test.permutationFiltersFile")
    private File permutationFiltersFile;

    /**
     * Whether to generate input permutations.
     */
    @Parameter(property = "archetype.test.generatePermutations", defaultValue = "true")
    private boolean generatePermutations;

    /**
     * Whether to only generate input permutations.
     */
    @Parameter(property = "archetype.test.permutationsOnly", defaultValue = "false")
    private boolean permutationsOnly;

    /**
     * Permutations start index.
     */
    @Parameter(property = "archetype.test.permutationStartIndex", defaultValue = "1")
    private int permutationStartIndex;

    /**
     * Permutations to process.
     */
    @Parameter(property = "archetype.test.permutation")
    private String permutation;

    /**
     * Invoker environment variables.
     */
    @Parameter(property = "archetype.test.invokerEnvVars")
    private Map<String, String> invokerEnvVars;

    @Component
    private ProjectInstaller installer;

    /**
     * Generated code inspection.
     */
    @Parameter
    private List<Validation> validations;

    private List<Map<String, String>> permutations;
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
            if (generatePermutations) {
                logPermutationsInput(testName);

                log.info("");
                log.info("Computing permutations...");
                permutations = permutations(archetypeFile.toPath());
                Path permutationsFile = writePermutations();
                log.info("");
                log.info("Total permutations: " + permutations.size());
                log.info("Permutations file: " + permutationsFile);

                if (permutationsOnly) {
                    return;
                }

                Set<String> artifactIds = new HashSet<>();
                Map<Integer, Map<String, String>> perms = filterPermutations();
                for (Map.Entry<Integer, Map<String, String>> entry : perms.entrySet()) {
                    index = entry.getKey();
                    Map<String, String> permutation = entry.getValue();
                    String artifactId = permutation.getOrDefault("artifactId", "my-project");
                    if (!artifactIds.add(artifactId)) {
                        permutation.put("artifactId", artifactId + "-" + index);
                    }
                    processIntegrationTest(testName, permutation, archetypeFile);
                }
            } else {
                processIntegrationTest(testName, externalValues, archetypeFile);
            }

        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Integration test failed with error(s)");
        }
    }

    private List<Map<String, String>> permutations(Path archetypeFile) {
        try (FileSystem fileSystem = newFileSystem(archetypeFile, this.getClass().getClassLoader())) {
            Script script = ScriptLoader.load(fileSystem.getPath("main.xml"));
            InputPermutations.Builder builder = InputPermutations.builder()
                                                       .script(script)
                                                       .externalValues(externalValues)
                                                       .externalDefaults(externalDefaults)
                                                       .inputFilters(inputFilters)
                                                       .permutationFilters(permutationFilters);
            if (inputFiltersFile != null) {
                builder.inputFilters(filtersFromFile(inputFiltersFile));
            }
            if (permutationFiltersFile != null) {
                builder.permutationFilters(filtersFromFile(permutationFiltersFile));
            }
            return builder.build().compute();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Path writePermutations() {
        try {
            Path projectDir = project.getFile().toPath().getParent();
            Path targetDir = projectDir.resolve("target");
            Path permutationsFile = targetDir.resolve("permutations.txt");
            Files.createDirectories(targetDir);
            try (PrintWriter csvWriter = new PrintWriter(Files.newBufferedWriter(permutationsFile))) {
                for (Map<String, String> permutation : permutations) {
                    String line = Lists.join(Maps.entries(permutation), e -> e.getKey() + "=" + e.getValue(), " ");
                    csvWriter.println(line);
                }
                csvWriter.flush();
            }
            return permutationsFile;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Map<Integer, Map<String, String>> filterPermutations() {
        Map<Integer, Map<String, String>> perms = new LinkedHashMap<>();
        if (permutation == null || permutation.isEmpty()) {
            Iterator<Map<String, String>> it = permutations.iterator();
            for (int i = 1; it.hasNext(); i++) {
                Map<String, String> next = it.next();
                if (i >= permutationStartIndex) {
                    perms.put(i, next);
                }
            }
        } else {
            List<Integer> indices = Arrays.stream(permutation.split(","))
                                          .map(Integer::valueOf)
                                          .collect(Collectors.toList());
            Iterator<Map<String, String>> it = permutations.iterator();
            for (int i = 1; it.hasNext(); i++) {
                Map<String, String> next = it.next();
                if (indices.contains(i)) {
                    perms.put(i, next);
                }
            }
        }
        return perms;
    }

    private void processIntegrationTest(String testName,
                                        Map<String, String> externalValues,
                                        File archetypeFile) throws IOException, MojoExecutionException {

        logTestDescription(testName, externalValues);

        Properties props = new Properties();
        props.putAll(externalValues);

        Path ourProjectDir = project.getFile().toPath();
        Path projectsDir = ourProjectDir.getParent().resolve("target/projects");
        ensureDirectory(projectsDir);
        String projectName = props.getProperty("artifactId");
        Path outputDir = unique(projectsDir, projectName);
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

    private void logPermutationsInput(String testName) {
        log.info("");
        log.info("--------------------------------------");
        log.info("Generating Archetype Permutations");
        log.info("--------------------------------------");
        log.info("");
        log.info(Bold.apply("Test: ") + BoldBlue.apply(testName));
        int maxKeyWidth = maxKeyWidth(externalValues, externalDefaults);
        logInputs("externalValues", externalValues, maxKeyWidth);
        logInputs("externalDefaults", externalDefaults, maxKeyWidth);
    }

    private void logTestDescription(String testName, Map<String, String> externalValues) {
        String description = Bold.apply("Test: ") + BoldBlue.apply(testName);
        if (permutations != null && index > 0) {
            description += BoldBlue.apply(String.format(", permutation: %s/%s", index, permutations.size()));
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
            inputs.forEach((key, value) -> {
                String padding = padding(" ", maxKeyWidth, key);
                log.info("    " + Cyan.apply(key) + padding + SEP + BoldBlue.apply(value));
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
            validate(basedir);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Execution failure: exit code = " + result.getExitCode(),
                        result.getExecutionException());
            }
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void validate(File basedir) throws MojoExecutionException {
        if (Objects.isNull(validations) || validations.isEmpty()) {
            return;
        }
        List<SourcePath> paths = SourcePath.scan(basedir);
        String error = String.format("Validation failed in directory %s", basedir);
        for (Validation validation : validations) {
            String match = validation.getMatch();
            boolean fail = validation.getFail();
            Set<String> patterns = validation.getPatterns();
            boolean isMatch;
            Predicate<SourcePath> matches = path -> path.matches(patterns);
            switch (match) {
                case "all":
                    isMatch = paths.stream().allMatch(matches);
                    break;
                case "any":
                    isMatch = paths.stream().anyMatch(matches);
                    break;
                case "none":
                    isMatch = paths.stream().noneMatch(matches);
                    break;
                default:
                    throw new MojoExecutionException("Wrong validation match value: " + match);
            }
            if (isMatch == fail) {
                throw new MojoExecutionException(
                        String.format("%s with patterns: %s match: %s, fail: %s", error, patterns, match, fail));
            }
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

    private static Collection<String> filtersFromFile(File file) {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(file.toPath())) {
            props.load(is);
            return Maps.fromProperties(props).values();
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
