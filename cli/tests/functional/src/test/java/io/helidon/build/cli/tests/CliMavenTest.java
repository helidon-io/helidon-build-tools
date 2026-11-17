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
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.helidon.build.cli.tests.ProcessInvocation.Monitor;
import io.helidon.build.cli.tests.ProcessInvocation.MonitorException;
import io.helidon.build.common.LazyValue;
import io.helidon.build.common.maven.MavenVersion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.build.cli.tests.FunctionalUtils.CLI_VERSION;
import static io.helidon.build.cli.tests.FunctionalUtils.MAVEN_LOCAL_REPO;
import static io.helidon.build.cli.tests.WaitStrategy.waitForOutput;
import static io.helidon.build.cli.tests.WaitStrategy.waitForPort;
import static io.helidon.build.cli.tests.WaitStrategy.waitForUrl;
import static io.helidon.build.common.FileUtils.copyDirectory;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.location;
import static io.helidon.build.common.FileUtils.unique;
import static java.util.function.Predicate.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("SpellCheckingInspection")
class CliMavenTest {

    private static final List<String> MAVEN_VERSIONS = List.of("3.2.3", "3.2.5", "3.8.1", "3.8.2", "3.9.16");
    private static final MavenVersion MAVEN_3_2_5 = MavenVersion.toMavenVersion("3.2.5");
    private static final LazyValue<Path> WORK_DIR = new LazyValue<>(CliMavenTest::workDir);
    private static final LazyValue<Path> PROJECT1_DIR = new LazyValue<>(CliMavenTest::project1Dir);
    private static final LazyValue<Path> PROJECT1_MAIN = new LazyValue<>(CliMavenTest::project1Main);

    @BeforeAll
    static void setUp() {
        ensureDirectory(location(CliMavenTest.class).resolve("../.mvn"));
        System.setProperty("io.helidon.build.common.maven.url.localRepo", MAVEN_LOCAL_REPO.get());
    }

    @BeforeEach
    void beforeEach() throws IOException {
        Path dotHelidon = PROJECT1_DIR.get().resolve(".helidon");
        Files.delete(dotHelidon);
        Files.createFile(dotHelidon);
    }

    @Test
    void testWrongMavenVersion() {
        Path projectDir = workDir("wrong-maven-version");
        try (Monitor monitor = new MavenInvocation("3.2.3")
                .cwd(projectDir)
                .args("org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                        "-DinteractiveMode=false",
                        "-DarchetypeGroupId=io.helidon.archetypes",
                        "-DarchetypeArtifactId=helidon",
                        "-DarchetypeVersion=3.0.0-M1",
                        "-DgroupId=groupid",
                        "-DartifactId=artifactid",
                        "-Dpackage=custom.pack.name",
                        "-Dflavor=se",
                        "-Dbase=bare")
                .start()) {

            monitor.await();
            fail("Should have thrown an exception");
        } catch (MonitorException ex) {
            String output = ex.output();
            assertThat(output, containsString("Requires Maven >= 3.2.5"));
        }
    }

    static Stream<String> validMavenVersions() {
        return MAVEN_VERSIONS.stream()
                .filter(v -> MavenVersion.toMavenVersion(v).isGreaterThanOrEqualTo(MAVEN_3_2_5));
    }

    @ParameterizedTest
    @MethodSource("validMavenVersions")
    void testMissingValues(String version) {
        missingArtifactGroupPackageValues(version);
        missingFlavorValue(version);
        missingBaseValue(version);
    }

    @Test
    void testCatchDevLoopRecompilationFails() throws Exception {
        // https://github.com/oracle/helidon-build-tools/issues/499
        String output = devLoopRecompile("2.2.3", "test-catch-dev-loop-recompilation-fails");
        assertThat(output, containsString("COMPILATION ERROR :"));
    }

    @Test
    void testFixDevLoopRecompilationFails() throws Exception {
        // https://github.com/oracle/helidon-build-tools/issues/499
        String output = devLoopRecompile(CLI_VERSION.get(), "test-fix-dev-loop-recompilation-fails");
        assertThat(output, containsString("BUILD SUCCESS"));
    }

    @Test
    void testCatchingJansiIssue() {
        // https://github.com/helidon-io/helidon-build-tools/issues/458
        int port = freePort();
        try (Monitor monitor = new MavenInvocation("3.8.2")
                .cwd(PROJECT1_DIR.get())
                .logDir(workDir("test-cathing-jansi-issue"))
                .args("-B",
                        "-Ddev.appJvmArgs=-Dserver.port=" + port,
                        "io.helidon.build-tools:helidon-cli-maven-plugin:2.1.0:dev")
                .start()) {

            monitor.await();
            fail("Should have thrown an exception");
        } catch (MonitorException ex) {
            String output = ex.output();
            assertThat(output, containsString("org/fusesource/jansi/AnsiOutputStream"));
            assertThat(output, containsString("BUILD FAILURE"));
        }
    }

    @Test
    void testFixJansiIssue() {
        // https://github.com/helidon-io/helidon-build-tools/issues/458
        int port = freePort();
        try (Monitor monitor = new MavenInvocation("3.8.2")
                .cwd(PROJECT1_DIR.get())
                .logDir(workDir("test-fix-jansi-issue"))
                .args( "-B",
                        "-Ddev.appJvmArgs=-Dserver.port=" + port,
                        "io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION.get() + ":dev")
                .start()) {

            String url = String.format("http://localhost:%d/greet", port);
            monitor.await(waitForUrl(url, body -> true));
            assertThat(monitor.output(), containsString("BUILD SUCCESS"));
        }
    }

    // We test with maven 3.9.x and 3.10.x to verify that upgrade to
    // org.jline.jansi fixes the issue with 3.10.x:
    // https://github.com/helidon-io/helidon-build-tools/issues/1170
    // and does not break with maven 3.9.x
    @ParameterizedTest
    @ValueSource(strings = {"3.8.4", "3.9.16", "3.10.0"})
    void testCliMavenPlugin(String mavenVersion) {
        int port = freePort();
        try (Monitor monitor = new MavenInvocation(mavenVersion)
                .cwd(PROJECT1_DIR.get())
                .logDir(workDir("test-cli-maven-plugin"))
                .args("-Ddev.appJvmArgs=-Dserver.port=" + port,
                        "io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION.get() + ":dev")
                .start()) {

            String url = String.format("http://localhost:%d/greet", port);
            monitor.await(waitForUrl(url, body -> true));
            assertThat(monitor.output(), containsString("BUILD SUCCESS"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.8.4", "3.9.16", "3.10.0"})
    void testCliMavenPluginRebuild(String mavenVersion) throws IOException {
        Path projectDir = workDir("test-cli-maven-plugin-rebuild-" + mavenVersion);
        Files.copy(PROJECT1_DIR.get().resolve("pom.xml"), projectDir.resolve("pom.xml"));
        Files.copy(PROJECT1_DIR.get().resolve(".helidon"), projectDir.resolve(".helidon"));
        copyDirectory(PROJECT1_DIR.get().resolve("src"), projectDir.resolve("src"));
        Path source = projectDir.resolve("src/main/java/com/acme/Main.java");
        String originalSource = Files.readString(source);
        Matcher matcher = HELLO_WORLD_PATTERN.matcher(originalSource);
        assertThat(matcher.find(), is(true));
        String originalGreeting = "Hello " + matcher.group(1) + "!";
        Path resource = projectDir.resolve("src/main/resources/rebuild.txt");
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, "World");

        int port = freePort();
        try (Monitor monitor = new MavenInvocation(mavenVersion)
                .cwd(projectDir)
                .args("-Ddev.clean=true",
                        "-Ddev.appJvmArgs=-Dserver.port=" + port,
                        "io.helidon.build-tools:helidon-cli-maven-plugin:" + CLI_VERSION.get() + ":dev")
                .start()) {

            monitor.await(waitForPort(port));
            String url = "http://localhost:" + port + "/greet";
            HttpClient client = new HttpClient(url);
            String actual;
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                actual = response.body();
                assertThat(actual, is(originalGreeting));
            }

            for (String name : List.of("John", "Jane")) {
                Files.writeString(resource, name);
                Files.writeString(source, originalSource.replace(originalGreeting, "Hello " + name + "!"));

                monitor.await(waitForUrl(url, not(actual::equals)));

                try (HttpClient.Response response = client.request()) {
                    assertThat(response.status(), is(200));
                    actual = response.body();
                    assertThat(actual, is("Hello " + name + "!"));
                    assertThat(Files.readString(projectDir.resolve("target/classes/rebuild.txt")), is(name));
                }
            }
        }
    }

    static final Pattern HELLO_WORLD_PATTERN = Pattern.compile("Hello ([a-zA-Z]+)!");

    String devLoopRecompile(String pluginVersion, String testName) throws Exception {
        assertThat(PROJECT1_MAIN.get(), is(notNullValue()));

        int port = freePort();
        try (Monitor monitor = new MavenInvocation("3.8.2")
                .cwd(PROJECT1_DIR.get())
                .logDir(workDir(testName))
                .args("-Ddev.appJvmArgs=-Dserver.port=" + port,
                        "io.helidon.build-tools:helidon-cli-maven-plugin:" + pluginVersion + ":dev")
                .start()) {

            String url = String.format("http://localhost:%d/greet", port);

            // wait for the application to be running
            monitor.await(waitForPort(port));

            // make a request
            HttpClient client = new HttpClient(url);
            String body;
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                body = response.body();
            }

            // extract greet name
            Matcher matcher = HELLO_WORLD_PATTERN.matcher(body);
            assertThat(matcher.find(), is(true));
            String greetName = matcher.group(1);

            // current output cursor
            int cursor = monitor.output().length();

            // flip the character case
            String updatedGreetName;
            if (greetName.chars().allMatch(Character::isUpperCase)) {
                updatedGreetName = greetName.toLowerCase();
            } else {
                updatedGreetName = greetName.toUpperCase();
            }

            // make a change
            String content = Files.readString(PROJECT1_MAIN.get());
            content = content.replaceAll(greetName, updatedGreetName);
            Files.write(PROJECT1_MAIN.get(), content.getBytes(StandardCharsets.UTF_8));

            // wait for incremental changes
            monitor.await(waitForOutput(cursor, "Changes detected - recompiling the module!"));

            // get incremental build status
            monitor.await(waitForOutput(cursor, "rebuild completed", "build failed"));
            if (monitor.output().indexOf("build failed", cursor) >= 0) {
                return monitor.output();
            }

            // wait for the application to be running
            monitor.await(waitForUrl(url, it -> it.contains(updatedGreetName)));

            // make a request
            try (HttpClient.Response response = client.request()) {
                assertThat(response.status(), is(200));
                assertThat(response.body(), containsString(updatedGreetName));
            }

            return monitor.output();
        }
    }

    void missingArtifactGroupPackageValues(String mavenVersion) {
        try (Monitor monitor = new MavenInvocation(mavenVersion)
                .cwd(workDir("missing-props-mvn-" + mavenVersion))
                .args("-B",
                        "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                        "-DinteractiveMode=false",
                        "-DarchetypeGroupId=io.helidon.archetypes",
                        "-DarchetypeArtifactId=helidon",
                        "-DarchetypeVersion=3.0.0-M1")
                .start()) {

            monitor.await();
            fail("Should have thrown an exception");
        } catch (MonitorException ex) {
            String output = ex.output();
            assertThat(output, containsString("Property groupId is missing."));
            assertThat(output, containsString("Property artifactId is missing."));
            assertThat(output, containsString("Property package is missing."));
            assertThat(output, containsString("BUILD FAILURE"));
        }
    }

    void missingFlavorValue(String mavenVersion) {
        try (Monitor monitor = new MavenInvocation(mavenVersion)
                .cwd(workDir("missing-flavor-mvn-" + mavenVersion))
                .args("-B",
                        "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                        "-DinteractiveMode=false",
                        "-DarchetypeGroupId=io.helidon.archetypes",
                        "-DarchetypeArtifactId=helidon",
                        "-DarchetypeVersion=3.0.0-M1",
                        "-DgroupId=groupid",
                        "-DartifactId=artifactid",
                        "-Dpackage=me.pack.name")
                .start()) {

            monitor.await();
            fail("Should have thrown an exception");
        } catch (MonitorException ex) {
            String output = ex.output();
            assertThat(output, containsString("Unresolved input: flavor"));
            assertThat(output, containsString("BUILD FAILURE"));
        }
    }

    void missingBaseValue(String mavenVersion) {
        try (Monitor monitor = new MavenInvocation(mavenVersion)
                .cwd(workDir("missing-base-mvn-" + mavenVersion))
                .args("-B",
                        "org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate",
                        "-DinteractiveMode=false",
                        "-DarchetypeGroupId=io.helidon.archetypes",
                        "-DarchetypeArtifactId=helidon",
                        "-DarchetypeVersion=3.0.0-M1",
                        "-DgroupId=groupid",
                        "-DartifactId=artifactid",
                        "-Dpackage=me.pack.name",
                        "-Dflavor=se")
                .start()) {

            monitor.await();
            fail("Should have thrown an exception");
        } catch (MonitorException ex) {
            String output = ex.output();
            assertThat(output, containsString("Unresolved input: base"));
            assertThat(output, containsString("BUILD FAILURE"));
        }
    }

    static int freePort() {
        try {
            ServerSocket s = new ServerSocket(0);
            s.close();
            return s.getLocalPort();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    static Path workDir() {
        return location(CliMavenTest.class).resolve("../cli-maven-test");
    }

    static Path workDir(String name) {
        return ensureDirectory(unique(WORK_DIR.get(), name));
    }

    static Path project1Dir() {
        return location(CliMavenTest.class).resolve("../it/projects/project1");
    }

    static Path project1Main() {
        try (Stream<Path> paths = Files.walk(PROJECT1_DIR.get())) {
            return paths.filter(p -> p.toString().endsWith("Main.java"))
                    .findAny()
                    .orElse(null);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
