/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.build.common.InputStreams;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.helidon.build.cli.tests.FunctionalUtils.getProperty;
import static io.helidon.build.cli.tests.FunctionalUtils.setMavenLocalRepoUrl;
import static io.helidon.build.common.FileUtils.list;
import static io.helidon.build.common.FileUtils.unzip;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * CLI distribution tests.
 */
public class CliDistributionTest {

    private static final Logger LOGGER = Logger.getLogger(CliDistributionTest.class.getName());
    private static Path distDir;
    private static final String CLI_VERSION_KEY = "cli.version";
    private static final String DIST_BASE_DIR = "helidon-" + getProperty(CLI_VERSION_KEY);

    @BeforeAll
    static void setup() throws IOException {
        setMavenLocalRepoUrl();
        distDir = Files.createTempDirectory("dist");
        Path targetDir = Path.of(getProperty("helidon.executable.directory"));
        LOGGER.info("targetDir - " + targetDir.toRealPath());
        Path cliZip = targetDir.resolve("target/distribution/helidon.zip");
        LOGGER.info("cliZip - " + cliZip.toRealPath());
        unzip(cliZip, distDir);
    }

    @Test
    void testCliContent() {
        List<String> content = list(distDir, 4).stream()
                .peek(p -> assertThat(Files.exists(p), is(true)))
                .map(p -> distDir.relativize(p))
                .map(Path::toString)
                .map(s -> s.replace("\\", "/"))
                .collect(Collectors.toList());

        //Ensure main directory are present
        assertThat(content, hasItems(DIST_BASE_DIR + "/bin"));
        assertThat(content, hasItems(DIST_BASE_DIR + "/lib"));
        assertThat(content, hasItems(DIST_BASE_DIR + "/lib/libs"));
        //Ensure main files are present
        assertThat(content, hasItems(DIST_BASE_DIR + "/bin/helidon"));
        assertThat(content, hasItems(DIST_BASE_DIR + "/bin/helidon.bat"));
        assertThat(content, hasItems(DIST_BASE_DIR + "/lib/helidon.jar"));
        assertThat(content, hasItems(DIST_BASE_DIR + "/LICENSE.txt"));
    }

    @Test
    @EnabledOnOs(value={OS.LINUX, OS.MAC}, disabledReason = "Run only on Mac or Linux")
    void testShellCreateProject() throws IOException {
        runCreateProjectTest(distDir.resolve(DIST_BASE_DIR + "/bin/helidon").toString());
    }

    @Test
    @EnabledOnOs(value={OS.WINDOWS}, disabledReason = "Run only on Windows")
    void testBatchCreateProject() throws IOException {
        runCreateProjectTest(distDir.resolve(DIST_BASE_DIR + "/bin/helidon.bat").toString());
    }

    @Test
    @EnabledOnOs(value={OS.LINUX, OS.MAC}, disabledReason = "Run only on Mac or Linux")
    void testShellVersion() throws IOException {
        runVersionTest(distDir.resolve(DIST_BASE_DIR + "/bin/helidon").toString());
    }

    @Test
    @EnabledOnOs(value={OS.WINDOWS}, disabledReason = "Run only on Windows")
    void testVersion() throws IOException {
        runVersionTest(distDir.resolve(DIST_BASE_DIR + "/bin/helidon.bat").toString());
    }

    private void runVersionTest(String cliExecutable) throws IOException {
        String expectedVersion = getProperty(CLI_VERSION_KEY);
        Process process = new ProcessBuilder()
                .command(cliExecutable, "version")
                .start();
        String versionLine = InputStreams.toLines(process.getInputStream()).stream()
                .filter(line -> line.contains("build.version"))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("version not found"));
        process.destroy();

        assertThat(versionLine.contains(expectedVersion), is(true));
    }

    private void runCreateProjectTest(String cliExecutable) throws IOException {
        try {
            Path dir = Files.createTempDirectory("project");
            Process process = new ProcessBuilder()
                    .directory(dir.toFile())
                    .command(cliExecutable, "init", "--batch")
                    .start()
                    .onExit()
                    .get(5, TimeUnit.MINUTES);
            String result = String.join("", InputStreams.toLines(process.getInputStream()));
            LOGGER.info("errors - " + InputStreams.toLines(process.getErrorStream()));
            process.destroy();
            LOGGER.info("exitValue - " + process.exitValue());
            LOGGER.info(result);
            assertThat(result, containsString("Switch directory to"));
            assertThat(list(dir).size(), is(not(0)));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
