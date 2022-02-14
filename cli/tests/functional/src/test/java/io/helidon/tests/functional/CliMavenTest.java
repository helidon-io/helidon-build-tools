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
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

    private static final String ARCHETYPE_VERSION = helidonArchetypeVersion();
    private static final List<String> MAVEN_VERSIONS = List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");

    private static Path workDir;
    private static Path mavenHome;

    @BeforeAll
    static void setUp() throws IOException {
        workDir = Files.createTempDirectory("generated");
        mavenHome = Files.createTempDirectory("maven");

        for (String version : MAVEN_VERSIONS) {
            TestUtils.downloadMavenDist(mavenHome, version);
        }
    }

    private static String helidonArchetypeVersion() {
        String version = System.getProperty("helidon.current.test.version");
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("Helidon archetype version is not set");
        }
    }

    @AfterEach
    void cleanUpGeneratedFiles() throws IOException {
        cleanUp(workDir);
    }

    @AfterAll
    static void cleanUp() throws IOException {
        cleanUp(mavenHome);
    }

    static void cleanUp(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(directory))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    static Stream<String> getMavenVersions() {
        return MAVEN_VERSIONS.stream();
    }

    @ParameterizedTest
    @MethodSource("getMavenVersions")
    public void testMavenArchetypeGenerate(String version) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        List<String> mavenArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + ARCHETYPE_VERSION,
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=custom.pack.name",
                "-Dflavor=se",
                "-Dbase=bare");

        MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-" + version, "bin", "mvn"))
                .ignoreMavenVersion()
                .ignoreExitValue()
                .directory(workDir)
                .stdOut(new PrintStream(stream))
                .addArguments(mavenArgs)
                .build()
                .execute();

        if (MavenVersion.toMavenVersion(version).isLessThan(MavenVersion.toMavenVersion("3.2.5"))) {
            Assertions.assertTrue(stream.toString().contains("Requires Maven >= 3.2.5"));
            return;
        }
        Assertions.assertTrue(stream.toString().contains("BUILD SUCCESS"));
    }

    @Test
    public void testMissingValues() throws Exception {
        List<String> mvnArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + ARCHETYPE_VERSION);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.4", "bin", "mvn"))
                .directory(workDir)
                .stdOut(new PrintStream(stream))
                .addArguments(mvnArgs)
                .ignoreExitValue()
                .build()
                .execute();
        String output = stream.toString();
        Assertions.assertTrue(output.contains("Property groupId is missing."));
        Assertions.assertTrue(output.contains("Property package is missing."));
        Assertions.assertTrue(output.contains("Property artifactId is missing."));
        Assertions.assertTrue(output.contains("BUILD FAILURE"));
    }

    @Test //Test issue https://github.com/oracle/helidon-build-tools/issues/499
    public void testIssue499() throws Exception {
        int port = TestUtils.getAvailablePort();
        generateBareSe();
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

    @Test
    public void testCliMavenPluginJansiIssue() throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        generateBareSe();

        setCustomServerPortInPom(workDir.resolve("artifactid").resolve("pom.xml"), port);

        ProcessMonitor monitor = MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.1", "bin", "mvn"))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:3.0.0-M2:dev")
                .build()
                .start();
        TestUtils.waitForApplication(port);
        monitor.stop();

        String output = stream.toString();
        Assertions.assertTrue(output.contains("BUILD SUCCESS"));
    }

    @Test
    public void testCliMavenPluginBackwardCompatibility() throws Exception {
        int port = TestUtils.getAvailablePort();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        generateBareSe();

        setCustomServerPortInPom(workDir.resolve("artifactid").resolve("pom.xml"), port);

        ProcessMonitor monitor = MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.4", "bin", "mvn"))
                .directory(workDir.resolve("artifactid"))
                .stdOut(new PrintStream(stream))
                .addArgument("io.helidon.build-tools:helidon-cli-maven-plugin:3.0.0-M2:dev")
                .build()
                .start();
        TestUtils.waitForApplication(port);
        monitor.stop();

        String output = stream.toString();
        Assertions.assertTrue(output.contains("BUILD SUCCESS"));
    }

    //@Test
    public void testCliPluginCompatibility() throws Exception {
        int port = TestUtils.getAvailablePort();
        generateBareSe();

        Plugin plugin = new Plugin();
        plugin.setGroupId("io.helidon.build-tools");
        plugin.setArtifactId("helidon-maven-plugin");
        plugin.setVersion("2.0.2");
        Path pom = workDir.resolve("artifactid").resolve("pom.xml");
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Model model = mavenReader.read(new FileReader(pom.toFile()));
        model.getBuild().getPlugins().add(plugin);
        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
        mavenWriter.write(new FileWriter(pom.toFile()), model);

        ProcessMonitor monitor = MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome.toString(), "apache-maven-3.8.4", "bin", "mvn"))
                .directory(workDir)
                .stdOut(System.out)
                .addArguments(List.of("helidon:dev", "-Dversion.plugin.helidon=2.0.2"))
                .build()
                .start();
        TestUtils.waitForApplication(port);
        monitor.stop();
    }

    private void setCustomServerPortInPom(Path pom, int port) throws Exception {
        Plugin plugin = new Plugin();
        plugin.setGroupId("io.helidon.build-tools");
        plugin.setArtifactId("helidon-cli-maven-plugin");
        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom appArgs = new Xpp3Dom("appJvmArgs");
        appArgs.setValue("-Dserver.port=" + port);
        config.addChild(appArgs);
        plugin.setConfiguration(config);

        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Model model = mavenReader.read(new FileReader(pom.toFile()));
        model.getBuild().getPlugins().add(plugin);
        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
        mavenWriter.write(new FileWriter(pom.toFile()), model);
    }

    private void generateBareSe() throws Exception {
        CommandInvoker.builder()
                .metadataUrl("https://helidon.io/cli-data")
                .workDir(workDir)
                .artifactId("artifactid")
                .invokeInit();
    }

}
