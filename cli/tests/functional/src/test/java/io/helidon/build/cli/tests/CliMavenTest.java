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

package io.helidon.build.cli.tests;

import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.webclient.WebClient;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CliMavenTest {

    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");
    private static final MavenVersion MAVEN_3_2_5 = MavenVersion.toMavenVersion("3.2.5");

    private static Path workDir;
    private static Path mavenDirectory;

    private ByteArrayOutputStream stream;

    @BeforeAll
    static void setUp() throws IOException {
        workDir = Files.createTempDirectory("generated");
        mavenDirectory = Files.createTempDirectory("maven");

        for (String version : MAVEN_VERSIONS) {
            FunctionalUtils.downloadMavenDist(mavenDirectory, version);
        }
    }

    @BeforeEach
    void refresh() throws IOException {
        workDir = Files.createTempDirectory("generated");
        stream = new ByteArrayOutputStream();
    }

    @AfterEach
    void close() throws IOException {
        stream.close();
    }

    @SuppressWarnings("unused")
    static Stream<String> getValidMavenVersions() {
        return MAVEN_VERSIONS.stream()
                .filter(v -> MavenVersion.toMavenVersion(v).isGreaterThanOrEqualTo(MAVEN_3_2_5));
    }

    @Test
    public void testWrongMavenVersion() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.1.1/bin");
        List<String> mavenArgs = List.of(
                "archetype:generate",
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
                .addArguments(mavenArgs)
                .build()
                .execute());
        assertThat(stream.toString(), containsString("Requires Maven >= 3.2.5"));
        stream.close();
    }

    @ParameterizedTest
    @MethodSource("getValidMavenVersions")
    public void testMissingValues(String version) {
        missingArtifactGroupPackageValues(version);
        missingFlavorValue(version);
        missingBaseValue(version);
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    public void catchDevLoopRecompilationFails() {
        String output = runIssue499("2.2.3");
        assertThat(output, containsString("COMPILATION ERROR :"));
    }

    @Test //Issue#499 https://github.com/oracle/helidon-build-tools/issues/499
    public void testDevLoopRecompilationFails() throws Exception {
        runIssue499(FunctionalUtils.CLI_VERSION);
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    public void catchingJansiIssue() {
        String output = runCliMavenPluginJansiIssue("2.1.0");
        assertThat(output, containsString("org/fusesource/jansi/AnsiOutputStream"));
        assertThat(output, containsString("BUILD FAILURE"));
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    public void testFixJansiIssue() throws Exception {
        String output = runCliMavenPluginJansiIssue(FunctionalUtils.CLI_VERSION);
        assertThat(output, containsString("BUILD SUCCESS"));
        validateSeProject(workDir);
    }

    @Test
    public void testCliMavenPlugin() throws Exception {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.4/bin");
        FunctionalUtils.generateBareSe(workDir);
        validateSeProject(workDir);

        ProcessMonitor monitor = MavenCommand.builder()
                .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                .directory(workDir.resolve("bare-se"))
                .stdOut(new PrintStream(stream))
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + FunctionalUtils.CLI_VERSION + ":dev")
                .build()
                .start();
        FunctionalUtils.waitForApplication(port, stream);
        monitor.stop();

        assertThat(stream.toString(), containsString("BUILD SUCCESS"));
    }

    private String runCliMavenPluginJansiIssue(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        FunctionalUtils.generateBareSe(workDir);
        validateSeProject(workDir);
        try {
             ProcessMonitor monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir.resolve("bare-se"))
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            FunctionalUtils.waitForApplication(port, stream);
            monitor.stop();
            return stream.toString();
        } catch (Exception e) {
            return stream.toString();
        }
    }

    public String runIssue499(String pluginVersion) {
        int port = FunctionalUtils.getAvailablePort();
        Path mavenBinDir = mavenDirectory.resolve("apache-maven-3.8.2/bin");
        FunctionalUtils.generateBareSe(workDir);
        validateSeProject(workDir);

        try {
            ProcessMonitor monitor = MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir.resolve("bare-se"))
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                    .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                    .build()
                    .start();
            FunctionalUtils.waitForApplication(port, stream);

            Files.walk(workDir)
                    .filter(p -> p.toString().endsWith("GreetService.java"))
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

            FunctionalUtils.waitForApplication(port, stream);

            WebClient.builder()
                    .baseUri("http://localhost:" + port + "/greet")
                    .build()
                    .get().request(String.class)
                    .thenAccept(
                            s -> assertThat(s, containsString("John"))
                    )
                    .toCompletableFuture().get();

            monitor.stop();
            return stream.toString();
        } catch (Exception e) {
            return stream.toString();
        }
    }

    private void runMissingValueTest(List<String> args, String mavenVersion) throws Exception {
        Path mavenBinDir = mavenDirectory.resolve(String.format("apache-maven-%s/bin", mavenVersion));
        try {
            MavenCommand.builder()
                    .executable(mavenBinDir.resolve(FunctionalUtils.getMvnExecutable(mavenBinDir)))
                    .directory(workDir)
                    .stdOut(new PrintStream(stream))
                    .stdErr(new PrintStream(stream))
                    .addArguments(args)
                    .build()
                    .start()
                    .waitForCompletion(10, TimeUnit.MINUTES);
        } catch (ProcessMonitor.ProcessFailedException e) {
            throw new Exception(stream.toString());
        }
    }

    private void missingArtifactGroupPackageValues(String mavenVersion) {
        List<String> args = List.of(
                "archetype:generate",
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
                "archetype:generate",
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
                "archetype:generate",
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

    private void validateSeProject(Path wd) {
        Path projectDir = wd.resolve("bare-se");
        assertThat(Files.exists(projectDir.resolve("pom.xml")), is(true));
        assertThat(Files.exists(projectDir.resolve("src/main/resources/application.yaml")), is(true));
    }

}
