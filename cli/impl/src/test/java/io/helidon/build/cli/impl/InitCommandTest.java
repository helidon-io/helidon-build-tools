/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.File;

import io.helidon.build.common.ProcessMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class InitDefaultTest.
 */
public class InitCommandTest extends InitCommandTestBase {

    @Override
    protected CommandInvoker.Builder commandInvoker() {
        return super.commandInvoker()
                    .buildProject(true);
    }

    @BeforeEach
    public void beforeEach(TestInfo info) {
        System.out.println("\n--- Running " + info.getDisplayName() + "----------------------------------------\n");
    }

    @Test
    void testProjectOptionAndArgumentMatch() throws Exception {
        String projectDir = uniqueProjectDir("bare-se-match").toString();
        String output = TestUtils.execWithDirAndInput(TARGET_DIR.toFile(), null,
                                                      "init",
                                                      "--url", metadataUrl(),
                                                      "--batch",
                                                      "--version", HELIDON_TEST_VERSION,
                                                      "--package", "me.bob.helidon",
                                                      "--groupId", "me.bob-helidon",
                                                      "--artifactId", "bare-se",
                                                      "--project", projectDir,
                                                      projectDir);
        assertThat(output, containsString("Switch directory to " + projectDir + " to use CLI"));
    }

    @Test
    void testProjectOptionAndArgumentMismatch() {
        String projectDir1 = uniqueProjectDir("bare-se-mismatch").toString();
        String projectDir2 = uniqueProjectDir("bare-se-mismatch2").toString();
        Exception e = assertThrows(ProcessMonitor.ProcessFailedException.class, () ->
                TestUtils.execWithDirAndInput(TARGET_DIR.toFile(), null,
                                              "init",
                                              "--url", metadataUrl(),
                                              "--batch",
                                              "--version", HELIDON_TEST_VERSION,
                                              "--package", "me.bob.helidon",
                                              "--groupId", "me.bob-helidon",
                                              "--artifactId", "bare-se",
                                              "--project", projectDir1,
                                              projectDir2));
        assertThat(e.getMessage(), containsString("Different project directories provided"));
        assertThat(e.getMessage(), containsString("'--project " + projectDir1 + "'"));
        assertThat(e.getMessage(), containsString("'" + projectDir2 + "'"));
    }

    @Test
    public void testDefaults() throws Exception {
        commandInvoker()
                .useProjectOption(true)
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testFlavor() throws Exception {
        commandInvoker()
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testGroupId() throws Exception {
        commandInvoker()
                .groupId("io.helidon.basicapp")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testArtifactId() throws Exception {
        CommandInvoker invoker = commandInvoker()
                .artifactId("foo-artifact")
                .invokeInit()
                .validateProject();
        assertThat(invoker.projectDir().getFileName().toString(), startsWith("foo-artifact"));
    }

    @Test
    public void testPackage() throws Exception {
        commandInvoker()
                .packageName("io.helidon.mypackage")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testName() throws Exception {
        commandInvoker()
                .projectName("mybasicproject")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testInteractiveSe() throws Exception {
        commandInvoker()
                .input(getClass().getResource("input.txt"))
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testInteractiveMp() throws Exception {
        commandInvoker()
                .input(getClass().getResource("input.txt"))
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testInteractiveAllHelidonVersions() throws Exception {
        String helidonProperty = System.getProperty(HELIDON_VERSION_PROPERTY);
        System.clearProperty(HELIDON_VERSION_PROPERTY);
        String cliDataUrl = testResourcePath(getClass(), "versions")
                .resolve("cli-data").toUri().toURL().toString();
        String projectDir = uniqueProjectDir("quickstart-se").toString();

        String output = TestUtils.execWithDirAndInput(
                TARGET_DIR.toFile(),
                new File(getClass().getResource("input-full-version-list.txt").getFile()),
                "init",
                "--reset",
                "--url", cliDataUrl,
                "--project", projectDir,
                projectDir);

        assertThat(output, containsString("Helidon versions"));
        assertThat(output, containsString("(1) 2.0.0"));
        assertThat(output, containsString("(29) 4.0.0-SNAPSHOT"));
        assertThat(output, containsString("Enter selection (default: 29):"));

        System.setProperty(HELIDON_VERSION_PROPERTY, helidonProperty);
    }

    @Test
    public void testInteractiveLatestHelidonVersions() throws Exception {
        String helidonVersionProperty = System.getProperty(HELIDON_VERSION_PROPERTY);
        System.clearProperty(HELIDON_VERSION_PROPERTY);
        String cliDataUrl = testResourcePath(getClass(), "versions")
                .resolve("cli-data").toUri().toURL().toString();
        String projectDir = uniqueProjectDir("quickstart-se").toString();

        String output = TestUtils.execWithDirAndInput(
                TARGET_DIR.toFile(),
                new File(getClass().getResource("input-latest-version-list.txt").getFile()),
                "init",
                "--reset",
                "--url", cliDataUrl,
                "--project", projectDir,
                projectDir);

        assertThat(output, containsString("Helidon versions"));
        assertThat(output, containsString("(1) 2.6.0"));
        assertThat(output, containsString("(2) 3.1.2"));
        assertThat(output, containsString("(3) 4.0.0-SNAPSHOT"));
        assertThat(output, containsString("Enter selection (default: 3):"));

        System.setProperty(HELIDON_VERSION_PROPERTY, helidonVersionProperty);
    }
}
