/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARTIFACT_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_GROUP_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARCHETYPE_ID;

import static io.helidon.build.cli.impl.TestUtils.exec;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.helidon.build.cli.impl.InitCommand.Flavor;

/**
 * Class CommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandTest extends BaseCommandTest {

    private final Flavor flavor = Flavor.SE;
    private final Path targetDir = TestFiles.targetDir();

    @BeforeEach
    public void precondition() {
        assumeTrue(TestUtils.apptypeArchetypeFound(flavor, HELIDON_SNAPSHOT_VERSION, DEFAULT_ARCHETYPE_ID));
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        TestUtils.ExecResult res = exec("init",
                "--flavor", flavor.toString(),
                "--project ", targetDir.toString(),
                "--version ", HELIDON_SNAPSHOT_VERSION,
                "--artifactid", DEFAULT_ARTIFACT_ID,
                "--groupid", DEFAULT_GROUP_ID,
                "--name", DEFAULT_NAME,
                "--batch");
        assertThat(res.code, is(equalTo(0)));
        Path projectDir = targetDir.resolve(Path.of(DEFAULT_NAME));
        assertTrue(Files.exists(projectDir));
    }

    @Test
    @Order(2)
    public void testBuild() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(DEFAULT_NAME));
        TestUtils.ExecResult res = exec("build",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
        assertTrue(Files.exists(projectDir.resolve("target/" + DEFAULT_ARTIFACT_ID + ".jar")));
    }

    @Test
    @Order(3)
    public void testInfo() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(DEFAULT_NAME));
        TestUtils.ExecResult res = exec("info",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
    }

    @Test
    @Order(4)
    public void testVersion() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(DEFAULT_NAME));
        TestUtils.ExecResult res = exec("version",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
    }

    @Test
    @Order(5)
    public void testClean() {
        Path projectDir = targetDir.resolve(Path.of(DEFAULT_NAME));
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
