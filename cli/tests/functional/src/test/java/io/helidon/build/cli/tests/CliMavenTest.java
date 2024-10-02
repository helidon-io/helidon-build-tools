/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
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

package io.helidon.build.cli.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.webclient.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.helidon.build.cli.tests.FunctionalUtils.ARCHETYPE_URL;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_VERSION;
import static io.helidon.build.cli.tests.FunctionalUtils.downloadMavenDist;
import static io.helidon.build.cli.tests.FunctionalUtils.setMavenLocalRepoUrl;
import static io.helidon.build.cli.tests.FunctionalUtils.validateSeProject;
import static io.helidon.build.cli.tests.FunctionalUtils.waitForApplication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpellCheckingInspection")
class CliMavenTest {

    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");
    private static final MavenVersion MAVEN_3_2_5 = MavenVersion.toMavenVersion("3.2.5");
    private static final String LOCAL_REPO_ARG;

    static {
        String localRepository = System.getProperty("localRepository");
        LOCAL_REPO_ARG = localRepository != null ? "-Dmaven.repo.local=" + localRepository : null;
    }

    private static Path workDir;
    private static Path mavenDirectory;

    private final StringBuilder capturedOutput = new StringBuilder();

    @BeforeAll
    static void setUp() throws IOException {
        setMavenLocalRepoUrl();
        workDir = Files.createTempDirectory("generated");
        mavenDirectory = Files.createTempDirectory("maven");
        for (String version : MAVEN_VERSIONS) {
            downloadMavenDist(mavenDirectory, version);
        }
    }

    @BeforeEach
    void refresh() throws IOException {
        workDir = Files.createTempDirectory("generated");
        capturedOutput.setLength(0);
    }

    @SuppressWarnings("unused")
    static Stream<String> getValidMavenVersions() {
        return MAVEN_VERSIONS.stream()
                .filter(v -> MavenVersion.toMavenVersion(v).isGreaterThanOrEqualTo(MAVEN_3_2_5));
    }

    @Test
    void testWrongMavenVersion() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.1.1/bin");
        List<String> mavenArgs = List.of(
                "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=3.0.0-M1",
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=custom.pack.name",
                "-Dflavor=se",
                "-Dbase=bare");

        assertThrows(ProcessMonitor.ProcessFailedException.class, () -> MavenCommand.builder()
                .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                .directory(workDir)
                .stdOut(new PrintStream(stream))
                .stdErr(new PrintStream(stream))
                .addOptionalArgument(LOCAL_REPO_ARG)
                .addArguments(mavenArgs)
                .build()
                .execute());
        assertThat(stream.toString(), containsString("Requires Maven >= 3.2.5"));
        stream.close();
    }

    @ParameterizedTest
    @MethodSource("getValidMavenVersions")
    void testMissingValues(String version) {
        missingArtifactGroupPackageValues(version);
        missingFlavorValue(version);
        missingBaseValue(version);
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    void catchDevLoopRecompilationFails() {
        String output = runIssue499("2.2.3");
        assertThat(output, containsString("COMPILATION ERROR :"));
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    void testDevLoopRecompilationFails() {
        runIssue499(CLI_VERSION);
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    void catchingJansiIssue() {
        String output = runCliMavenPluginJansiIssue("2.1.0");
        assertThat(output, containsString("org/fusesource/jansi/AnsiOutputStream"));
        assertThat(output, containsString("BUILD FAILURE"));
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    void testFixJansiIssue() {
        String output = runCliMavenPluginJansiIssue(CLI_VERSION);
        assertThat(output, containsString("BUILD SUCCESS"));
        validateSeProject(workDir);
    }

    @Test
    void testCliMavenPlugin() throws Exception {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.4/bin");
        generateBareSe(workDir, "testCliMavenPlugin");

        ProcessMonitor monitor = MavenCommand.builder()
                .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                .directory(workDir.resolve("bare-se"))
                .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                .stdOut(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                .addOptionalArgument(LOCAL_REPO_ARG)
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION + ":dev")
                .build()
                .start();
        waitForApplication(port, capturedOutput::toString);
        monitor.stop();

        assertThat(capturedOutput.toString(), containsString("BUILD SUCCESS"));
    }

    private String runCliMavenPluginJansiIssue(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        generateBareSe(workDir, "runCliMavenPluginJansiIssue" + pluginVersion);
        try {
            ProcessMonitor monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir.resolve("bare-se"))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addArgument("-B")
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            waitForApplication(port, capturedOutput::toString);
            monitor.stop();
            return capturedOutput.toString();
        } catch (Exception e) {
            return capturedOutput.toString();
        }
    }

    String runIssue499(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        generateBareSe(workDir, "runIssue499-" + pluginVersion);
        try {
            ProcessMonitor monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir.resolve("bare-se"))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            waitForApplication(port, capturedOutput::toString);

            try (Stream<Path> paths = Files.walk(workDir)) {
                paths.filter(p -> p.toString().endsWith("Main.java"))
                        .findAny()
                        .ifPresent(path -> {
                            try {
                                String content = Files.readString(path);
                                content = content.replaceAll("\"World\"", "\"John\"");
                                Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                            } catch (IOException ioException) {
                                throw new UncheckedIOException(ioException);
                            }
                        });
            }

            waitForApplication(port, capturedOutput::toString);

            WebClient.builder()
                    .baseUri("http://localhost:" + port + "/greet")
                    .build()
                    .get().request(String.class)
                    .thenAccept(s -> assertThat(s, containsString("John")))
                    .toCompletableFuture().get();

            monitor.stop();
            return capturedOutput.toString();
        } catch (Exception e) {
            return capturedOutput.toString();
        }
    }

    private void runMissingValueTest(List<String> args, String mavenVersion) throws Exception {
        Path mavenBinDir = mavenDirectory.resolve(String.format("apache-maven-%s/bin", mavenVersion));
        try {
            MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir)
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArguments(args)
                    .build()
                    .start()
                    .waitForCompletion(10, TimeUnit.MINUTES);
        } catch (ProcessMonitor.ProcessFailedException e) {
            throw new Exception(capturedOutput.toString());
        }
    }

    private void missingArtifactGroupPackageValues(String mavenVersion) {
        List<String> args = List.of(
                "-B",
                "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=3.0.0-M1");
        Exception e = assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(e.getMessage(), containsString("Property groupId is missing."));
        assertThat(e.getMessage(), containsString("Property artifactId is missing."));
        assertThat(e.getMessage(), containsString("Property package is missing."));
        assertThat(e.getMessage(), containsString("BUILD FAILURE"));
    }

    private void missingFlavorValue(String mavenVersion) {
        List<String> args = List.of(
                "-B",
                "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=3.0.0-M1",
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=me.pack.name"
        );
        Exception e = assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(e.getMessage(), containsString("Unresolved input: flavor"));
        assertThat(e.getMessage(), containsString("BUILD FAILURE"));
    }

    private void missingBaseValue(String mavenVersion) {
        List<String> args = List.of(
                "-B",
                "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=3.0.0-M1",
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=me.pack.name",
                "-Dflavor=se"
        );
        Exception e = assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(e.getMessage(), containsString("Unresolved input: base"));
        assertThat(e.getMessage(), containsString("BUILD FAILURE"));
    }

    void generateBareSe(Path wd, String artifactId) {
        FileUtils.requireDirectory(wd);
        assertThat(FileUtils.list(wd).size(), is(1));
        Helidon.execute(
                "init",
                "--reset",
                "--url", ARCHETYPE_URL,
                "--batch",
                "--project", wd.resolve("bare-se").toString(),
                "--version", CLI_VERSION,
                "--groupId", getClass().getName(),
                "--artifactId", artifactId,
                "--package", "custom.pack.name",
                "--flavor", "se");
        validateSeProject(wd);
    }

    private void record(PrintStream stream, String str) {
        String line = str + System.lineSeparator();
        stream.print(str);
        synchronized (capturedOutput) {
            capturedOutput.append(line);
        }
    }
}
