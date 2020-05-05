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
import io.helidon.build.util.HelidonVariant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.BaseCommand.HELIDON_VERSION;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class CommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandTest {

    private static final String HELIDON_VERSION_TEST = "2.0.0-SNAPSHOT";
    private static final String HELIDON_VERSION_PREVIOUS = "2.0.0-M2";
    private static final String MY_PROJECT = "myproject";
    private static final String MY_GROUP_ID = "mygroup";
    private static final String MY_ARTIFACT_ID = "myartifact";

    private HelidonVariant variant = HelidonVariant.SE;
    private Path targetDir = TestFiles.targetDir();

    /**
     * Overrides version under test. This property must be propagated to all
     * forked processes.
     */
    @BeforeAll
    public static void setHelidonVersion() {
        System.setProperty(HELIDON_VERSION, HELIDON_VERSION_TEST);
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        TestUtils.ExecResult res = exec("init",
                "--flavor", variant.toString(),
                "--project ", targetDir.toString(),
                "--version ", HELIDON_VERSION_PREVIOUS,
                "--artifactid", MY_ARTIFACT_ID,
                "--groupid", MY_GROUP_ID,
                "--name", MY_PROJECT,
                "--batch");
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
        Path projectDir = targetDir.resolve(Path.of(MY_PROJECT));
        assertTrue(Files.exists(projectDir));
    }

    @Test
    @Order(2)
    public void testBuild() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(MY_PROJECT));
        TestUtils.ExecResult res = exec("build",
                "--project ", projectDir.toString());
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
        assertTrue(Files.exists(projectDir.resolve("target/" + MY_ARTIFACT_ID + ".jar")));
    }

    @Test
    @Order(3)
    public void testInfo() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(MY_PROJECT));
        TestUtils.ExecResult res = exec("info",
                "--project ", projectDir.toString());
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
    }

    @Test
    @Order(4)
    public void testVersion() throws Exception {
        Path projectDir = targetDir.resolve(Path.of(MY_PROJECT));
        TestUtils.ExecResult res = exec("version",
                "--project ", projectDir.toString());
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
    }

    @Test
    @Order(5)
    public void testClean() {
        Path projectDir = targetDir.resolve(Path.of(MY_PROJECT));
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
