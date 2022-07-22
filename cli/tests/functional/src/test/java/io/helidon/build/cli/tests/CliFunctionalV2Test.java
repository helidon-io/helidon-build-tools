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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CliFunctionalV2Test extends BaseFunctionalTest {

    private static final String ARTIFACT_ID = "bare-mp";
    private static final String PACKAGE = "custom.pack.name";

    private static String expectedOutput;

    private static Path workDir;
    private static Path helidonShell;
    private static Path helidonBatch;
    private static Path helidonNativeImage;
    private static File inputFile;

    @BeforeAll
    static void setup() throws IOException {
        Path input = Files.createTempFile("input","txt");
        Path executableDir = getExecutableDir();
        workDir = Files.createTempDirectory("generated");
        inputFile = Files.writeString(input, "\n\n\n").toFile();
        helidonBatch = executableDir.resolve("helidon.bat");
        helidonShell = executableDir.resolve("helidon.sh");
        helidonNativeImage = executableDir.resolve("target/helidon");
        expectedOutput = String.format("Switch directory to %s to use CLI", workDir.resolve("bare-mp"));
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static Path getExecutableDir() {
        String executable = Objects.requireNonNull(
                System.getProperty("helidon.executable.directory"),
                "helidon.executable.directory system property is not set");
        return Path.of(executable);
    }

    @Test
    void batchTest() {
        String output = buildArchetype()
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void batchTestShellScript() {
        String output = buildArchetype()
                .executable(helidonShell)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void batchTestBatScript() {
        String output = buildArchetype()
                .executable(helidonBatch)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
    }

    @Test
    void batchTestEmbedded() {
        buildArchetype()
                .addOption("--batch")
                .execute(workDir.resolve(ARTIFACT_ID));
        validateMpProject(workDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void batchTestNativeImage() {
        String output = buildArchetype()
                .executable(helidonNativeImage)
                .addOption("--batch")
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
    }

    @Test
    void interactiveTest() {
        String output = buildArchetype()
                .input(inputFile)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
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
        validateMpProject(workDir);
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
        validateMpProject(workDir);
    }

    @Test
    @EnabledIfSystemProperty(named = "native.image", matches = "true")
    void interactiveTestNativeImage() {
        String output = buildArchetype()
                .input(inputFile)
                .executable(helidonNativeImage)
                .start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(expectedOutput));
        validateMpProject(workDir);
    }

    @Test
    public void testDebug() {
        String output = buildArchetype()
                .addOption("batch")
                .addOption("debug")
                .start(5, TimeUnit.MINUTES);

        assertThat(output, containsString("Found maven executable"));
        validateMpProject(workDir);
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
                    .addArg("artifactId", ARTIFACT_ID)
                    .addArg("package", PACKAGE)
                    .addArg("version", HELIDON_VERSION)
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
                    .addArg("flavor", "mp")
                    .addArg("artifactId", ARTIFACT_ID)
                    .addArg("package", PACKAGE)
                    .addArg("version", "0.0.0")
                    .workDirectory(workDir)
                    .init()
                    .start(5, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Helidon version 0.0.0 not found."));
            return;
        }
        assertThat("Exception should have been thrown because of wrong helidon version.", false);
    }

    @Test
    public void testVersionCommand() {
        String output = cliProcessBuilder().version().start(5, TimeUnit.MINUTES);
        assertThat(output, containsString(HELIDON_VERSION));
    }

    private Builder buildArchetype() {
        return cliProcessBuilder()
                .addArg("flavor", "mp")
                .addArg("artifactId", ARTIFACT_ID)
                .addArg("package", PACKAGE)
                .addArg("version", HELIDON_VERSION)
                .workDirectory(workDir)
                .init();
    }
}
