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

import io.helidon.build.cli.impl.CommandInvoker;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.webclient.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CliMavenTest {

    private static final String PLUGIN_VERSION = helidonArchetypeVersion();
    private static final String ARCHETYPE_VERSION = "2.4.2";
    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");

    private static Path workDir;
    private static Path mavenHome;

    private static String helidonArchetypeVersion() {
        String version = System.getProperty("helidon.plugin.version");
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("Helidon archetype version is not set");
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

    @AfterEach //FileUtils not used because of Windows throwing exception
    void cleanUpGeneratedFiles() throws IOException {
        Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @AfterAll //FileUtils not used because of Windows throwing exception
    static void cleanUp() throws IOException {
        Files.walk(mavenHome)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    static Stream<String> getMavenVersions() {
        return MAVEN_VERSIONS.stream();
    }

    @Test
    public void testWrongMavenVersion() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<String> mavenArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon-quickstart-se",
                "-DarchetypeVersion=" + ARCHETYPE_VERSION,
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=custom.pack.name");

        MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.1.1", "bin", TestUtils.mvnExecutable("3.1.1")))
                .ignoreMavenVersion()
                .ignoreExitValue()
                .directory(workDir)
                .stdOut(new PrintStream(stream))
                .addArguments(mavenArgs)
                .build()
                .execute();
        String processOutput = stream.toString();

        Assertions.assertTrue(processOutput.contains("BUILD FAILURE"), "Error with following output:\n" + processOutput);
    }

    @ParameterizedTest
    @MethodSource("getMavenVersions")
    public void testMissingValues(String version) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<String> mvnArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon-quickstart-se",
                "-DarchetypeVersion=" + ARCHETYPE_VERSION);

        MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-" + version, "bin", TestUtils.mvnExecutable(version)))
                .directory(workDir)
                .stdOut(new PrintStream(stream))
                .addArguments(mvnArgs)
                .ignoreExitValue()
                .build()
                .execute();
        String output = stream.toString();

        if (MavenVersion.toMavenVersion(version).isLessThan(MavenVersion.toMavenVersion("3.2.5"))) {
            Assertions.assertTrue(output.contains("BUILD FAILURE"), "Build should be failing:\n" + output);
            return;
        }

        Assertions.assertTrue(output.contains("Property groupId is missing."), "Build should be failing:\n" + output);
        Assertions.assertTrue(output.contains("Property package is missing."), "Build should be failing:\n" + output);
        Assertions.assertTrue(output.contains("Property artifactId is missing."), "Build should be failing:\n" + output);
        Assertions.assertTrue(output.contains("BUILD FAILURE"), "Build should be failing:\n" + output);
    }

    @Test //Test issue https://github.com/oracle/helidon-build-tools/issues/499
    public void testIssue499() throws Exception {
        int port = TestUtils.getAvailablePort();
        TestUtils.generateBareSe(workDir, mavenHome.toString());
        CommandInvoker invoker = CommandInvoker.builder()
                .metadataUrl("https://helidon.io/cli-data")
                .appJvmArgs("-Dserver.port=" + port)
                .environment(Map.of("MAVEN_HOME",
                        Path.of(mavenHome.toString(), "apache-maven-3.8.2").toString()))
                .workDir(workDir.resolve("artifactid"))
                .invokeDev();
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
                        Assertions.fail("Unable to modified GreetService file");
                    }
                });

        TestUtils.waitForApplication(port);

        WebClient client = WebClient.builder()
                .baseUri("http://localhost:" + port + "/greet")
                .build();
        client.get().request(String.class)
                .thenAccept(s -> Assertions.assertTrue(s.contains("Jhon")))
                .toCompletableFuture().get();

        invoker.stopMonitor();
    }

    @Test //Issue#259 https://github.com/oracle/helidon-build-tools/issues/259
    public void testCliMavenPluginJansiIssue() throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TestUtils.generateBareSe(workDir, mavenHome.toString());

        ProcessMonitor monitor = MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.1", "bin", TestUtils.mvnExecutable("3.8.1")))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .stdErr(new PrintStream(stream))
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + PLUGIN_VERSION + ":dev")
                .build()
                .start();
        try {
            TestUtils.waitForApplication(port);
        } catch (Exception e) {
            Assertions.fail("Output : \n" + stream);
        }
        monitor.stop();

        String output = stream.toString();
        Assertions.assertTrue(output.contains("BUILD SUCCESS"));
    }

    @Test
    public void testCliMavenPluginBackwardCompatibility() throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TestUtils.generateBareSe(workDir, mavenHome.toString());

        ProcessMonitor monitor = MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.4", "bin", TestUtils.mvnExecutable("3.8.4")))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .addArgument("-Ddev.appJvmArgs=-Dserver.port=" + port)
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:" + PLUGIN_VERSION + ":dev")
                .build()
                .start();
        TestUtils.waitForApplication(port);
        monitor.stop();

        String output = stream.toString();
        Assertions.assertTrue(output.contains("BUILD SUCCESS"));
    }

}
