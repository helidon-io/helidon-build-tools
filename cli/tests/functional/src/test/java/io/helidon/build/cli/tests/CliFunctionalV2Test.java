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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.helidon.build.cli.common.ArchetypesData;
import io.helidon.build.cli.impl.Config;
import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.Strings;
import io.helidon.build.common.ansi.AnsiTextStyle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.helidon.build.cli.tests.FunctionalUtils.getProperty;
import static io.helidon.build.cli.tests.FunctionalUtils.setMavenLocalRepoUrl;
import static io.helidon.build.common.FileUtils.deleteDirectoryContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.fail;

class CliFunctionalV2Test {

    private static String expectedOutput;
    private static Path workDir;
    private static Path helidonShell;
    private static Path helidonBatch;
    private static Path helidonNativeImage;
    private static File inputFile;

    @BeforeAll
    static void setup() throws IOException {
        setMavenLocalRepoUrl();
        Path input = Files.createTempFile("input", "txt");
        Path executableDir = Path.of(getProperty("helidon.executable.directory"));
        workDir = Files.createTempDirectory("generated").toRealPath();
        inputFile = Files.writeString(input, "\n\n\n").toFile();
        helidonBatch = executableDir.resolve("helidon.bat");
        helidonShell = executableDir.resolve("helidon.sh");
        helidonNativeImage = executableDir.resolve("target").resolve(System.getProperty("native.image.name", "helidon"));
        expectedOutput = String.format("Switch directory to %s to use CLI", workDir.resolve("bare-se"));
    }

    @AfterEach
    void cleanUp() throws IOException {
        deleteDirectoryContent(workDir);
    }

    @Test
    void batchTest() {
        String output = buildArchetype("batchTest")
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(workDir.resolve("bare-se").toString()));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void batchTestShellScript() {
        String output = buildArchetype("batchTestShellScript")
                .executable(helidonShell)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void batchTestBatScript() {
        String output = buildArchetype("batchTestShellScript")
                .executable(helidonBatch)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void batchTestEmbedded() {
        buildArchetype("batchTestEmbedded")
                .addOption("--batch")
                .execute(workDir.resolve("bare-se"));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void batchTestNativeImage() {
        String output = buildArchetype("batchTestNativeImage")
                .executable(helidonNativeImage)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void interactiveTest() {
        String output = buildArchetype("interactiveTest")
                .input(inputFile)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(workDir.resolve("bare-se").toString()));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void interactiveTestShellScript() {
        String output = cliProcessBuilder()
                .workDirectory(workDir)
                .input(inputFile)
                .executable(helidonShell)
                .init()
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void interactiveTestBatScript() {
        String output = cliProcessBuilder()
                .workDirectory(workDir)
                .input(inputFile)
                .executable(helidonBatch)
                .init()
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void interactiveTestNativeImage() {
        String output = buildArchetype("interactiveTestNativeImage")
                .input(inputFile)
                .executable(helidonNativeImage)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void testDebug() {
        String output = buildArchetype("testDebug")
                .addOption("batch")
                .addOption("debug")
                .start(5, TimeUnit.MINUTES);

        assertThat(output, containsString("Found maven executable"));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void testVerbose() {
        String output = cliProcessBuilder()
                .workDirectory(workDir)
                .addOption("verbose")
                .info()
                .start(5, TimeUnit.MINUTES);

        assertThat(output, containsString("java.class.path"));
        assertThat(output, containsString("build.date"));
    }

    @Test
    void incorrectFlavorTest() {
        try {
            cliProcessBuilder()
                    .addArg("flavor", "wrongFlavor")
                    .addArg("version", FunctionalUtils.CLI_VERSION)
                    .workDirectory(workDir)
                    .init()
                    .start(5, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Invalid choice: wrongFlavor"));
            return;
        }
        fail("Exception should have been thrown due to wrong flavor input.");
    }

    @Test
    void incorrectHelidonVersionTest() {
        try {
            cliProcessBuilder()
                    .addArg("version", "0.0.0")
                    .workDirectory(workDir)
                    .init()
                    .start(5, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Helidon version 0.0.0 not found."));
            return;
        }
        fail("Exception should have been thrown because of wrong helidon version.");
    }

    @Test
    void testVersionCommand() {
        String output = cliProcessBuilder().version().start(5, TimeUnit.MINUTES);

        assertThat(output, containsString(FunctionalUtils.CLI_VERSION));
        assertThat(output, containsString("default.helidon.version"));
    }

    @Test
    void testCacheContent() {
        cliProcessBuilder().info().addOption("reset").start(5, TimeUnit.MINUTES);
        Path cacheDir = Config.userConfig().cacheDir();
        List<String> content = SourcePath.scan(cacheDir).stream()
                .map(SourcePath::asString)
                .collect(Collectors.toList());
        ArchetypesData data = ArchetypesData.load(cacheDir.resolve("versions.xml"));
        String defaultVersion = data.defaultVersion().toString();

        assertThat(content, hasItem("/versions.xml"));
        assertThat(content, hasItem("/" + defaultVersion + "/.lastUpdate"));
    }

    private Builder buildArchetype(String artifactId) {
        return cliProcessBuilder()
                .addArg("project", workDir.resolve("bare-se").toString())
                .addArg("groupId", getClass().getName())
                .addArg("artifactId", artifactId)
                .addArg("package", "custom.pack.name")
                .addArg("version", FunctionalUtils.CLI_VERSION)
                .workDirectory(workDir)
                .init();
    }

    Builder cliProcessBuilder() {
        return new Builder();
    }

    static class Builder {

        private Path workDir;
        private File input;
        private Path executable;
        private ExecutionMode mode = ExecutionMode.CLASSPATH;
        private final List<String> args = new LinkedList<>();

        Builder executable(Path executable) {
            this.executable = executable;
            this.mode = ExecutionMode.EXECUTABLE;
            return this;
        }

        Builder input(File input) {
            this.input = input;
            return this;
        }

        Builder init() {
            args.add(0, "init");
            return this;
        }

        Builder info() {
            args.add(0, "info");
            return this;
        }

        Builder version() {
            args.add(0, "version");
            return this;
        }

        Builder workDirectory(Path workDir) {
            this.workDir = workDir;
            return this;
        }

        Builder addArg(String option, String value) {
            addOption(option);
            addValue(value);
            return this;
        }

        Builder addOption(String option) {
            Strings.requireValid(option, "Provided option is not valid");
            if (!option.startsWith("--")) {
                option = String.format("--%s", option);
            }
            args.add(option);
            return this;
        }

        private void addValue(String value) {
            args.add(Strings.requireValid(value, "Provided value is not valid"));
        }

        ProcessMonitor start() {
            List<String> cmdArgs = new LinkedList<>();
            if (mode == ExecutionMode.CLASSPATH) {
                cmdArgs.addAll(FunctionalUtils.buildJavaCommand());
            }
            if (mode == ExecutionMode.EXECUTABLE) {
                cmdArgs.add(executable.normalize().toString());
            }
            cmdArgs.addAll(args);
            addResetUrl(cmdArgs);
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmdArgs);

            if (workDir != null) {
                pb.directory(workDir.toFile());
            }

            try {
                return ProcessMonitor.builder()
                        .processBuilder(pb)
                        .stdIn(input)
                        .stdOut(System.out)
                        .stdErr(System.err)
                        .capture(true)
                        .build()
                        .start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not run command", e);
            }
        }

        String start(long timeout, TimeUnit unit) {
            try {
                String output = start().waitForCompletion(timeout, unit).output();
                return AnsiTextStyle.strip(output);
            } catch (ProcessMonitor.ProcessTimeoutException toe) {
                throw new RuntimeException("Execution timeout", toe);
            } catch (ProcessMonitor.ProcessFailedException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void execute(Path wd) {
            args.add("--project");
            args.add(wd.toString());
            addResetUrl(args);
            Helidon.execute(args.toArray(new String[0]));
        }

        private void addResetUrl(List<String> command) {
            if (command.contains("init")) {
                command.add("--reset");
                command.add("--url");
                command.add(FunctionalUtils.ARCHETYPE_URL);
            }
        }

    }

    enum ExecutionMode {
        CLASSPATH, EXECUTABLE
    }
}
