/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.tests.functional;

import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.webclient.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CliMavenTest {

    private static final String CLI_VERSION = getProperty("helidon.cli.version");
    private static final String PLUGIN_VERSION = getProperty("helidon.plugin.version");
    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");

    private static Path workDir;
    private static Path mavenHome;

    private static String getProperty(String property) {
        String version = System.getProperty(property);
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException(String.format("%s is not set", property));
        }
    }

    @BeforeAll
    static void setUp() throws IOException {
        workDir = Files.createTempDirectory("generated");
        mavenHome = Files.createTempDirectory("maven");

        for (String version : MAVEN_VERSIONS) {
            TestUtils.downloadMavenDist(mavenHome, version);
        }
    }

    @BeforeEach
    void refresh() throws IOException{
        workDir = Files.createTempDirectory("generated");
    }

    static Stream<String> getValidMavenVersions() {
        return MAVEN_VERSIONS.stream()
                .filter(v -> MavenVersion.toMavenVersion(v).isGreaterThanOrEqualTo(MavenVersion.toMavenVersion("3.2.5")));
    }

    @Test
    public void testWrongMavenVersion() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<String> mavenArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + CLI_VERSION,
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=custom.pack.name",
                "-Dflavor=se",
                "-Dbase=bare");

        try {
            MavenCommand.builder()
                    .executable(Path.of(mavenHome.toString(), "apache-maven-3.1.1", "bin", TestUtils.getMvnExecutable("3.1.1")))
                    .directory(workDir)
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArguments(mavenArgs)
                    .build()
                    .execute();
        } catch (ProcessMonitor.ProcessFailedException e) {
            assertThat(stream.toString(), containsString("Requires Maven >= 3.2.5"));
            stream.close();
            return;
        }
        assertThat("Exception expected when using wrong maven version", false);
    }

    @ParameterizedTest
    @MethodSource("getValidMavenVersions")
    public void testMissingValues(String version) throws Exception {
        missingArtifactGroupPackageValues(version);
        missingFlavorValue(version);
        missingBaseValue(version);
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    public void catchIssue499() {
        try {
            runIssue499("2.2.3");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("COMPILATION ERROR :"));
            return;
        }
        assertThat("Exception expected due to https://github.com/oracle/helidon-build-tools/issues/499", false);
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    public void testIssue499() throws Exception {
        runIssue499(PLUGIN_VERSION);
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    public void catchingJansiIssue() {
        try {
            runCliMavenPluginJansiIssue("2.1.0");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("org/fusesource/jansi/AnsiOutputStream"));
            assertThat(e.getMessage(), containsString("BUILD FAILURE"));
            return;
        }
        assertThat("Exception expected due to Jansi issue", false);
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    public void testFixJansiIssue() throws Exception {
        String output = runCliMavenPluginJansiIssue(PLUGIN_VERSION);
        assertThat(output, containsString("BUILD SUCCESS"));
    }

    @Test
    public void testCliMavenPlugin() throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TestUtils.generateBareSe(workDir, mavenHome.toString());

        ProcessMonitor monitor = MavenCommand.builder()
                .executable(Path.of(mavenHome.toString(), "apache-maven-3.8.4", "bin", TestUtils.getMvnExecutable("3.8.4")))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + PLUGIN_VERSION + ":dev")
                .build()
                .start();
        TestUtils.waitForApplication(port);
        monitor.stop();
        stream.close();

        assertThat(stream.toString(), containsString("BUILD SUCCESS"));
    }

    private String runCliMavenPluginJansiIssue(String pluginVersion) throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TestUtils.generateBareSe(workDir, mavenHome.toString());
        ProcessMonitor monitor = null;
        try {
             monitor = MavenCommand.builder()
                    .executable(Path.of(mavenHome.toString(), "apache-maven-3.8.2", "bin", TestUtils.getMvnExecutable("3.8.2")))
                    .directory(workDir.resolve("artifactid"))
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            TestUtils.waitForApplication(port);
            monitor.stop();
            stream.close();
        } catch (Exception e) {
            monitor.stop();
            stream.close();
            throw new Exception(stream.toString());
        }
        return stream.toString();
    }

    public void runIssue499(String pluginVersion) throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        TestUtils.generateBareSe(workDir, mavenHome.toString());
        ProcessMonitor monitor = MavenCommand.builder()
                .executable(Path.of(mavenHome.toString(), "apache-maven-3.8.2", "bin", TestUtils.getMvnExecutable("3.8.2")))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .stdErr(new PrintStream(stream))
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                .build()
                .start();
        TestUtils.waitForApplication(port);

        Files.walk(workDir)
                .filter(p -> p.toString().endsWith("GreetService.java"))
                .findAny()
                .ifPresent(path -> {
                    try {
                        String content = Files.readString(path);
                        content = content.replaceAll("\"World\"", "\"Jhon\"");
                        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ioException) {
                        throw new UncheckedIOException(ioException);
                    }
                });

        try {
            TestUtils.waitForApplication(port);
        } catch (Exception e) {
            monitor.stop();
            stream.close();
            throw new Exception(stream.toString());
        }

        WebClient.builder()
                .baseUri("http://localhost:" + port + "/greet")
                .build()
                .get().request(String.class)
                .thenAccept(
                        s -> assertThat(s, containsString("Jhon"))
                )
                .toCompletableFuture().get();

        monitor.stop();
        stream.close();
    }

    private String runMissingValueTest(List<String> args, String mavenVersion) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            MavenCommand.builder()
                    .executable(Path.of(mavenHome.toString(), "apache-maven-" + mavenVersion, "bin", TestUtils.getMvnExecutable(mavenVersion)))
                    .directory(workDir)
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArguments(args)
                    .build()
                    .execute();
        } catch (ProcessMonitor.ProcessFailedException e) {
            return stream.toString();
        }
        assertThat("Exception expected due to missing values", false);
        return "failed test";
    }

    private void missingArtifactGroupPackageValues(String mavenVersion) throws Exception {
        List<String> mvnArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + CLI_VERSION);
        String output = runMissingValueTest(mvnArgs, mavenVersion);
        assertThat(output, containsString("Property groupId is missing."));
        assertThat(output, containsString("Property artifactId is missing."));
        assertThat(output, containsString("Property package is missing."));
        assertThat(output, containsString("BUILD FAILURE"));
    }

    private void missingFlavorValue(String mavenVersion) throws Exception {
        List<String> args = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + CLI_VERSION,
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=me.pack.name"
        );
        String output = runMissingValueTest(args, mavenVersion);
        assertThat(output, containsString("Unresolved input: flavor"));
        assertThat(output, containsString("BUILD FAILURE"));
    }

    private void missingBaseValue(String mavenVersion) throws Exception {
        List<String> args = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + CLI_VERSION,
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=me.pack.name",
                "-Dflavor=se"
        );
        String output = runMissingValueTest(args, mavenVersion);
        assertThat(output, containsString("Unresolved input: base"));
        assertThat(output, containsString("BUILD FAILURE"));
    }

}
