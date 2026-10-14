/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import io.helidon.build.cli.impl.ProcessInvocation.Monitor;
import io.helidon.build.cli.impl.ProcessInvocation.MonitorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

/**
 * Class InitDefaultTest.
 */
public class InitCommandTestIT extends InitCommandTestBase {

    private static final String PROJECT_VERSION = "2.5.6";

    @Override
    protected CommandInvoker.Builder commandInvoker() {
        return super.commandInvoker()
                    .helidonVersion(PROJECT_VERSION)
                    .packaged()
                    .buildProject(true);
    }

    @BeforeEach
    public void beforeEach(TestInfo info) {
        System.out.println("\n--- Running " + info.getDisplayName() + "----------------------------------------\n");
    }

    @Test
    void testProjectOptionAndArgumentMatch() {
        String projectDir = uniqueProjectDir("bare-se-match").toString();
        try (Monitor monitor = new ProcessInvocation()
                .cwd(TARGET_DIR)
                .args("init",
                      "--reset",
                      "--url", metadataUrl(),
                      "--batch",
                      "--version", PROJECT_VERSION,
                      "--package", "me.bob.helidon",
                      "--groupId", "me.bob-helidon",
                      "--artifactId", "bare-se",
                      "--project", projectDir,
                      projectDir)
                .start()) {

            monitor.await();
            assertThat(monitor.output(), containsString("Switch directory to " + projectDir + " to use CLI"));
        }
    }

    @Test
    void testProjectOptionAndArgumentMismatch() {
        String projectDir1 = uniqueProjectDir("bare-se-mismatch").toString();
        String projectDir2 = uniqueProjectDir("bare-se-mismatch2").toString();
        try (Monitor monitor = new ProcessInvocation()
                .cwd(TARGET_DIR)
                .args("init",
                      "--reset",
                      "--url", metadataUrl(),
                      "--batch",
                      "--version", PROJECT_VERSION,
                      "--package", "me.bob.helidon",
                      "--groupId", "me.bob-helidon",
                      "--artifactId", "bare-se",
                      "--project", projectDir1,
                      projectDir2)
                .start()) {

            monitor.await();
            throw new AssertionError("Expected exception");
        } catch (MonitorException e) {
            assertThat(e.output(), containsString("Different project directories provided"));
            assertThat(e.output(), containsString("'--project " + projectDir1 + "'"));
            assertThat(e.output(), containsString("'" + projectDir2 + "'"));
        }
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

        try (Monitor monitor = new ProcessInvocation()
                .cwd(TARGET_DIR)
                .stdIn(new File(getClass().getResource("input-full-version-list.txt").getFile()).toPath())
                .args("init",
                      "--reset",
                      "--url", cliDataUrl,
                      "--project", projectDir,
                      projectDir)
                .start()) {

            monitor.await();
            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(28) 2.0.0"));
            assertThat(monitor.output(), containsString("(29) 4.0.0-SNAPSHOT"));
            assertThat(monitor.output(), containsString("Enter selection (default: 29):"));
        }

        if ( helidonProperty != null) {
            System.setProperty(HELIDON_VERSION_PROPERTY, helidonProperty);
        }
    }

    @Test
    public void testInteractiveLatestHelidonVersions() throws Exception {
        String helidonVersionProperty = System.getProperty(HELIDON_VERSION_PROPERTY);
        System.clearProperty(HELIDON_VERSION_PROPERTY);
        String cliDataUrl = testResourcePath(getClass(), "versions")
                .resolve("cli-data").toUri().toURL().toString();
        String projectDir = uniqueProjectDir("quickstart-se").toString();

        try (Monitor monitor = new ProcessInvocation()
                .cwd(TARGET_DIR)
                .stdIn(new File(getClass().getResource("input-latest-version-list.txt").getFile()).toPath())
                .args("init",
                      "--reset",
                      "--url", cliDataUrl,
                      "--project", projectDir,
                      projectDir)
                .start()) {

            monitor.await();
            assertThat(monitor.output(), containsString("Helidon versions"));
            assertThat(monitor.output(), containsString("(1) 3.1.2"));
            assertThat(monitor.output(), containsString("(2) 2.6.0"));
            assertThat(monitor.output(), containsString("(3) 4.0.0-SNAPSHOT"));
            assertThat(monitor.output(), containsString("(4) Show all versions"));
            assertThat(monitor.output(), containsString("Enter selection (default: 3):"));
        }

        if ( helidonVersionProperty != null) {
            System.setProperty(HELIDON_VERSION_PROPERTY, helidonVersionProperty);
        }
    }
}
