/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.cli.common.ArchetypesData;
import io.helidon.build.cli.impl.Config;
import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.cli.tests.ProcessInvocation.Monitor;
import io.helidon.build.cli.tests.ProcessInvocation.MonitorException;
import io.helidon.build.common.LazyValue;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.test.utils.TestFiles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static io.helidon.build.cli.tests.FunctionalUtils.CLI_DATA_URL;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_DIR;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_DIRNAME;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_VERSION;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_NATIVE;
import static io.helidon.build.cli.tests.FunctionalUtils.CLI_EXE;
import static io.helidon.build.cli.tests.FunctionalUtils.MAVEN_LOCAL_REPO;
import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.unique;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class CliFunctionalTest {

    private static final Path CWD = TestFiles.targetDir(CliFunctionalTest.class).resolve("cli-functional-test");
    private static final LazyValue<Path> INPUT_FILE = new LazyValue<>(CliFunctionalTest::inputFile);

    static {
        LogLevel.set(LogLevel.DEBUG);
    }

    @BeforeAll
    static void setup() {
        System.setProperty("io.helidon.build.common.maven.url.localRepo", MAVEN_LOCAL_REPO.get());
    }

    @Test
    void testCliContent() {
        List<String> content = list(CLI_DIR.get(), 4).stream()
                .peek(p -> assertThat(Files.exists(p), is(true)))
                .map(CLI_DIR.get()::relativize)
                .map(Path::toString)
                .map(s -> s.replace("\\", "/"))
                .collect(Collectors.toList());

        // ensure the main directories are present
        assertThat(content, hasItems(CLI_DIRNAME + "/bin"));
        assertThat(content, hasItems(CLI_DIRNAME + "/libs"));

        // ensure the main files are present
        assertThat(content, hasItems(CLI_DIRNAME + "/bin/helidon"));
        assertThat(content, hasItems(CLI_DIRNAME + "/bin/helidon.bat"));
        assertThat(content, hasItems(CLI_DIRNAME + "/helidon-cli.jar"));
        assertThat(content, hasItems(CLI_DIRNAME + "/LICENSE.txt"));
    }

    @Test
    void batchTest() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(unique(CWD, "batch-test"))
                .args("init",
                        "--batch",
                        "--reset",
                        "--url", CLI_DATA_URL.get(),
                        "--project", "my-project",
                        "--groupId", "com.acme",
                        "--artifactId", "batch-test",
                        "--package", "com.acme",
                        "--version", CLI_VERSION.get())
                .start()) {

            monitor.await();
            Path projectDir = monitor.cwd().resolve("my-project");
            String expectedOutput = String.format("Switch directory to %s to use CLI", projectDir);
            assertThat(monitor.output(), containsString(expectedOutput));
            validateProject(projectDir);
        }
    }

    @Test
    void batchTestEmbedded() {
        Path projectDir = unique(CWD, "batch-test-embedded");
        Helidon.execute(
                "init",
                "--batch",
                "--reset",
                "--url", CLI_DATA_URL.get(),
                "--project", projectDir.toString(),
                "--groupId", "com.acme",
                "--artifactId", "batch-test-embedded",
                "--package", "com.acme",
                "--version", CLI_VERSION.get());

        validateProject(projectDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void batchTestNativeImage() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_NATIVE.get())
                .cwd(unique(CWD, "batch-test-native-image"))
                .args("init",
                        "--batch",
                        "--reset",
                        "--url", CLI_DATA_URL.get(),
                        "--project", "my-project",
                        "--groupId", "com.acme",
                        "--artifactId", "batch-test-native-image",
                        "--package", "com.acme",
                        "--version", CLI_VERSION.get())
                .start()) {

            monitor.await();
            Path projectDir = monitor.cwd().resolve("my-project");
            String expectedOutput = String.format("Switch directory to %s to use CLI", projectDir);
            assertThat(monitor.output(), containsString(expectedOutput));
            validateProject(projectDir);
        }
    }

    @Test
    void interactiveTest() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .inputFile(INPUT_FILE.get())
                .cwd(unique(CWD, "interactive-test"))
                .args("init",
                        "--plain",
                        "--reset",
                        "--url", CLI_DATA_URL.get(),
                        "--project", "my-project")
                .start()) {

            monitor.await();
            Path projectDir = monitor.cwd().resolve("my-project");
            String expectedOutput = String.format("Switch directory to %s to use CLI", projectDir);
            assertThat(monitor.output(), containsString(expectedOutput));
            validateProject(projectDir);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void interactiveTestNativeImage() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_NATIVE.get())
                .inputFile(INPUT_FILE.get())
                .cwd(unique(CWD, "interactive-test-native-image"))
                .args("init",
                        "--reset",
                        "--url", CLI_DATA_URL.get(),
                        "--project", "my-project",
                        "--groupId", "com.acme",
                        "--artifactId", "interactive-test-native-image",
                        "--package", "com.acme",
                        "--version", CLI_VERSION.get())
                .start()) {

            monitor.await();
            Path projectDir = monitor.cwd().resolve("my-project");
            String expectedOutput = String.format("Switch directory to %s to use CLI", projectDir);
            assertThat(monitor.output(), containsString(expectedOutput));
            validateProject(projectDir);
        }
    }

    @Test
    void testDebug() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(unique(CWD, "debug-test"))
                .args("init",
                        "--batch",
                        "--debug",
                        "--reset",
                        "--url", CLI_DATA_URL.get(),
                        "--project", "my-project",
                        "--groupId", "com.acme",
                        "--artifactId", "debug-test",
                        "--package", "com.acme",
                        "--version", CLI_VERSION.get())
                .start()) {

            monitor.await();
            assertThat(monitor.output(), containsString("Found maven executable"));
            validateProject(monitor.cwd().resolve("my-project"));
        }
    }

    @Test
    void testVerbose() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(CWD)
                .args("--verbose", "info")
                .start()) {

            monitor.await();
            String output = monitor.output();
            assertThat(output, containsString("java.class.path"));
            assertThat(output, containsString("build.date"));
        }
    }

    @Test
    void incorrectFlavorTest() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(CWD)
                .args("init",
                        "--flavor", "wrongFlavor",
                        "--version", CLI_VERSION.get())
                .start()) {

            monitor.await();
            fail("Exception should have been thrown due to wrong flavor input.");
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Invalid choice: wrongFlavor"));
        }
    }

    @Test
    void incorrectHelidonVersionTest() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(CWD)
                .args("init",
                        "--version", "0.0.0")
                .start()) {

            monitor.await();
            fail("Exception should have been thrown because of wrong helidon version.");
        } catch (MonitorException ex) {
            assertThat(ex.output(), containsString("Helidon version 0.0.0 not found."));
        }
    }

    @Test
    void testVersionCommand() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(CWD)
                .args("version")
                .start()) {

            monitor.await();
            String output = monitor.output();
            assertThat(output, containsString(CLI_VERSION.get()));
            assertThat(output, containsString("default.helidon.version"));
        }
    }

    @Test
    void testCacheContent() {
        try (Monitor monitor = new CliInvocation()
                .bin(CLI_EXE.get())
                .cwd(CWD)
                .args("info",
                        "--reset",
                        "--url", CLI_DATA_URL.get())
                .start()) {

            monitor.await();
            Path cacheDir = Config.userConfig().cacheDir();
            List<String> content = SourcePath.scan(cacheDir).stream()
                    .map(SourcePath::asString)
                    .collect(Collectors.toList());
            ArchetypesData data = ArchetypesData.load(cacheDir.resolve("versions.xml"));
            String defaultVersion = data.defaultVersion().toString();

            assertThat(content, hasItem("/versions.xml"));
            assertThat(content, hasItem("/" + defaultVersion + "/.lastUpdate"));
        }
    }

    static void validateProject(Path dir) {
        List<SourcePath> files = SourcePath.scan(dir);
        assertThat(files.stream().anyMatch(path -> path.matches("pom.xml")), is(true));
        assertThat(files.stream().anyMatch(path -> path.matches(".helidon")), is(true));
        assertThat(files.stream().anyMatch(path -> path.matches("**/*.java")), is(true));
    }

    static Path inputFile() {
        try {
            return Files.writeString(unique(CWD, "input.txt"), "\n\n\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
