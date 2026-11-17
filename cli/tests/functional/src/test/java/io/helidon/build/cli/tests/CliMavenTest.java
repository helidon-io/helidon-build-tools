/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.cli.tests.WaitStrategy.AwaitException;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.build.cli.tests.FunctionalUtils.ARCHETYPE_URL;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_VERSION;
import static io.helidon.build.cli.tests.FunctionalUtils.downloadMavenDist;
import static io.helidon.build.cli.tests.FunctionalUtils.setMavenLocalRepoUrl;
import static io.helidon.build.cli.tests.FunctionalUtils.validateSeProject;
import static io.helidon.build.cli.tests.WaitStrategy.waitForPort;
import static io.helidon.build.cli.tests.WaitStrategy.waitForUrl;
import static io.helidon.build.common.FileUtils.unique;
import static java.util.function.Predicate.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpellCheckingInspection")
class CliMavenTest {

    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4", "3.9.16");
    private static final MavenVersion MAVEN_3_2_5 = MavenVersion.toMavenVersion("3.2.5");
    private static final String LOCAL_REPO_ARG;

    static {
        String localRepository = System.getProperty("localRepository");
        LOCAL_REPO_ARG = localRepository != null ? "-Dmaven.repo.local=" + localRepository : null;
    }

    private static Path mavenDirectory;

    private final Path workDir;
    private final StringBuffer capturedOutput = new StringBuffer();

    CliMavenTest() {
        try {
            Path codeSource = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            Path generated = codeSource.resolve("../generated");
            Files.createDirectories(generated.resolve(".mvn"));
            workDir = Files.createDirectories(unique(generated, "cli-maven-test"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void setUp() throws IOException {
        setMavenLocalRepoUrl();
        mavenDirectory = Files.createTempDirectory("maven");
        for (String version : MAVEN_VERSIONS) {
            downloadMavenDist(mavenDirectory, version);
        }
        downloadMavenDist(mavenDirectory, "3.10.0");
    }

    static Stream<String> getValidMavenVersions() {
        return MAVEN_VERSIONS.stream()
                .filter(v -> MavenVersion.toMavenVersion(v).isGreaterThanOrEqualTo(MAVEN_3_2_5));
    }

    @Test
    void testWrongMavenVersion() {
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
                .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                .addOptionalArgument(LOCAL_REPO_ARG)
                .addArguments(mavenArgs)
                .build()
                .execute());
        assertThat(capturedOutput.toString(), containsString("Requires Maven >= 3.2.5"));
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
        assertThrows(AwaitException.class, () -> runIssue499("2.2.3"));
        assertThat(capturedOutput.toString(), containsString("COMPILATION ERROR :"));
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    void testDevLoopRecompilationFails() {
        runIssue499(CLI_VERSION);
    }

    @Test
    void catchingJansiIssue() {
        String output = runCliMavenPluginJansiIssue("2.1.0");
        assertThat(output, containsString("org/fusesource/jansi/AnsiOutputStream"));
        assertThat(output, containsString("BUILD FAILURE"));
    }

    @Test
    void testFixJansiIssue() {
        String output = runCliMavenPluginJansiIssue(CLI_VERSION);
        assertThat(output, containsString("BUILD SUCCESS"));
        validateSeProject(workDir);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.8.4", "3.9.16", "3.10.0"})
    void testCliMavenPlugin(String mavenVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-" + mavenVersion + "/bin");
        generateProject(workDir, "testCliMavenPlugin");

        ProcessMonitor monitor = null;
        try {
            monitor = MavenCommand.builder()
                .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                .directory(workDir.resolve("bare-se"))
                .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                .addOptionalArgument(LOCAL_REPO_ARG)
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION + ":dev")
                .build()
                .start();
            waitForPort(port).await(monitor);

            HttpClient client = new HttpClient("http://localhost:" + port + "/greet");
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                assertThat(response.body(), is("Hello World!"));
            }
            assertThat(capturedOutput.toString(), containsString("BUILD SUCCESS"));
        } catch (Exception ex) {
            System.err.println(capturedOutput);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.8.4", "3.9.16", "3.10.0"})
    void testCliMavenPluginRebuild(String mavenVersion) throws IOException {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-" + mavenVersion + "/bin");
        generateProject(workDir, "testCliMavenPluginRebuild");
        Path projectDir = workDir.resolve("bare-se");
        Path source = projectDir.resolve("src/main/java/custom/pack/name/Main.java");
        String originalSource = Files.readString(source);
        Path resource = projectDir.resolve("src/main/resources/rebuild.txt");
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, "World");

        ProcessMonitor monitor = null;
        try {
            monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(projectDir)
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArgument("-Ddev.clean=true")
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION + ":dev")
                    .build()
                    .start();
            waitForPort(port).await(monitor);

            String url = "http://localhost:" + port + "/greet";
            HttpClient client = new HttpClient(url);

            String actual;
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                actual = response.body();
                assertThat(actual, is("Hello World!"));
            }

            for (String name : List.of("John", "Jane")) {
                Files.writeString(resource, name);
                Files.writeString(source, originalSource.replace("World", name));

                // wait for re-build
                waitForUrl(url, not(actual::equals)).await(monitor);

                try (HttpClient.Response response = client.request()) {
                    assertThat(response.status(), is(200));
                    actual = response.body();
                    assertThat(actual, is("Hello " + name + "!"));
                    assertThat(Files.readString(projectDir.resolve("target/classes/rebuild.txt")), is(name));
                }
            }
        } catch (Exception ex) {
            System.err.println(capturedOutput);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private String runCliMavenPluginJansiIssue(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        generateProject(workDir, "runCliMavenPluginJansiIssue" + pluginVersion);
        ProcessMonitor monitor = null;
        try {
            monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir.resolve("bare-se"))
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addArgument("-B")
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            waitForPort(port).await(monitor);

            HttpClient client = new HttpClient("http://localhost:" + port + "/greet");
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                assertThat(response.body(), is("Hello World!"));
            }
            return capturedOutput.toString();
        } catch (Exception e) {
            return capturedOutput.toString();
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    void runIssue499(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        Path projectDir = generateProject(workDir, "runIssue499-" + pluginVersion);
        ProcessMonitor monitor = null;
        try {
            monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(projectDir)
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            waitForPort(port).await(monitor);

            String url = "http://localhost:" + port + "/greet";
            HttpClient client = new HttpClient(url);
            String actual;
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                actual = response.body();
                assertThat(actual, is("Hello World!"));
            }

            // update Main.java
            Path sourceMain = projectDir.resolve("src/main/java/custom/pack/name/Main.java");
            assertThat(Files.exists(sourceMain), is(true));

            String content = Files.readString(sourceMain);
            content = content.replace("Hello World!", "Hello John!");
            Files.write(sourceMain, content.getBytes(StandardCharsets.UTF_8));

            // wait for re-build
            waitForUrl(url, not(actual::equals)).await(monitor);

            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                assertThat(response.body(), is("Hello John!"));
            }
        } catch (AwaitException ex) {
            System.err.println(capturedOutput);
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private void runMissingValueTest(List<String> args, String mavenVersion) {
        capturedOutput.setLength(0);
        Path mavenBinDir = mavenDirectory.resolve(String.format("apache-maven-%s/bin", mavenVersion));
        ProcessMonitor monitor = null;
        try {
            monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir)
                    .stdOut(PrintStreams.delegate(PrintStreams.STDOUT, this::record))
                    .stdErr(PrintStreams.delegate(PrintStreams.STDERR, this::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArguments(args)
                    .build()
                    .start();
            monitor.waitForCompletion(10, TimeUnit.MINUTES);
        } catch (Exception ex) {
            System.err.println(capturedOutput);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
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
        assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(capturedOutput.toString(), containsString("Property groupId is missing."));
        assertThat(capturedOutput.toString(), containsString("Property artifactId is missing."));
        assertThat(capturedOutput.toString(), containsString("Property package is missing."));
        assertThat(capturedOutput.toString(), containsString("BUILD FAILURE"));
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
        assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(capturedOutput.toString(), containsString("Unresolved input: flavor"));
        assertThat(capturedOutput.toString(), containsString("BUILD FAILURE"));
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
        assertThrows(Exception.class, () -> runMissingValueTest(args, mavenVersion));
        assertThat(capturedOutput.toString(), containsString("Unresolved input: base"));
        assertThat(capturedOutput.toString(), containsString("BUILD FAILURE"));
    }

    Path generateProject(Path wd, String artifactId) {
        FileUtils.requireDirectory(wd);
        assertThat(FileUtils.list(wd).size(), is(1));
        Path projectDir = wd.resolve("bare-se");
        Helidon.execute(
                "init",
                "--reset",
                "--url", ARCHETYPE_URL,
                "--batch",
                "--project", projectDir.toString(),
                "--version", CLI_VERSION,
                "--groupId", getClass().getName(),
                "--artifactId", artifactId,
                "--package", "custom.pack.name",
                "--flavor", "se");
        validateSeProject(wd);
        return projectDir;
    }

    private void record(PrintStream stream, String str) {
        String line = str + System.lineSeparator();
        stream.print(str);
        capturedOutput.append(line);
    }
}
