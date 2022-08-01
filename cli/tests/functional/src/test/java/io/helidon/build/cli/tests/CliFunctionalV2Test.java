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

import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CliFunctionalV2Test {

    private static String expectedOutput;

    private static Path workDir;
    private static Path helidonShell;
    private static Path helidonBatch;
    private static Path helidonNativeImage;
    private static File inputFile;

    @BeforeAll
    static void setup() throws IOException {
        FunctionalUtils.setMavenLocalRepoUrl();
        Path input = Files.createTempFile("input","txt");
        Path executableDir = Path.of(FunctionalUtils.getProperty("helidon.executable.directory"));
        workDir = Files.createTempDirectory("generated");
        inputFile = Files.writeString(input, "\n\n\n").toFile();
        helidonBatch = executableDir.resolve("helidon.bat");
        helidonShell = executableDir.resolve("helidon.sh");
        helidonNativeImage = executableDir.resolve("target/helidon");
        expectedOutput = String.format("Switch directory to %s to use CLI", workDir.resolve("bare-se"));
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void batchTest() {
        String output = buildArchetype()
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void batchTestShellScript() {
        String output = buildArchetype()
                .executable(helidonShell)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void batchTestBatScript() {
        String output = buildArchetype()
                .executable(helidonBatch)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void batchTestEmbedded() {
        buildArchetype()
                .addOption("--batch")
                .execute(workDir.resolve("bare-se"));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void batchTestNativeImage() {
        String output = buildArchetype()
                .executable(helidonNativeImage)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    void interactiveTest() {
        String output = buildArchetype()
                .input(inputFile)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void interactiveTestShellScript() {
        String output =  cliProcessBuilder()
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
        String output =  cliProcessBuilder()
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
        String output = buildArchetype()
                .input(inputFile)
                .executable(helidonNativeImage)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    public void testDebug() {
        String output = buildArchetype()
                .addOption("batch")
                .addOption("debug")
                .start(5, TimeUnit.MINUTES);

        assertThat(output, containsString("Found maven executable"));
        FunctionalUtils.validateSeProject(workDir);
    }

    @Test
    public void testVerbose() {
        String output = cliProcessBuilder()
                .workDirectory(workDir)
                .addOption("verbose")
                .info()
                .start(5, TimeUnit.MINUTES);

        assertThat(output, containsString("java.class.path"));
        assertThat(output, containsString("build.date"));
    }

    @Test
    public void IncorrectFlavorTest() {
        try {
            cliProcessBuilder()
                    .addArg("flavor", "wrongFlavor")
                    .addArg("version", FunctionalUtils.CLI_VERSION)
                    .workDirectory(workDir)
                    .init()
                    .start(5, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("ERROR: Invalid choice: wrongFlavor"));
            return;
        }
        assertThat("Exception should have been thrown due to wrong flavor input.", false);
    }

    @Test
    public void IncorrectHelidonVersionTest() {
        try {
            cliProcessBuilder()
                    .addArg("version", "0.0.0")
                    .workDirectory(workDir)
                    .init()
                    .start(5, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Helidon version lookup failed."));
            return;
        }
        assertThat("Exception should have been thrown because of wrong helidon version.", false);
    }

    @Test
    public void testVersionCommand() {
        String output = cliProcessBuilder().version().start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(FunctionalUtils.CLI_VERSION));
    }

    private Builder buildArchetype() {
        return cliProcessBuilder()
                .addArg("artifactId", "bare-se")
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

        public Builder executable(Path executable) {
            this.executable = executable;
            this.mode = ExecutionMode.EXECUTABLE;
            return this;
        }

        public Builder input(File input) {
            this.input = input;
            return this;
        }

        public Builder init() {
            args.add(0, "init");
            return this;
        }

        public Builder info() {
            args.add(0, "info");
            return this;
        }

        public Builder version() {
            args.add(0, "version");
            return this;
        }

        public Builder workDirectory(Path workDir) {
            this.workDir = workDir;
            return this;
        }

        public Builder addArg(String option, String value) {
            addOption(option);
            addValue(value);
            return this;
        }

        public Builder addOption(String option) {
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

        public ProcessMonitor start() {
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

        public String start(long timeout, TimeUnit unit) {
            try {
                return start().waitForCompletion(timeout, unit).output();
            } catch (ProcessMonitor.ProcessTimeoutException toe) {
                throw new RuntimeException("Execution timeout", toe);
            } catch (ProcessMonitor.ProcessFailedException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void execute(Path wd) {
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
