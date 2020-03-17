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

import java.nio.file.Path;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.HelidonVariant;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.test.TestFiles.quickstartId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class CommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandTest {

    // Helidon version under test
    private static final String HELIDON_VERSION = "2.0.0-M1";

    private HelidonVariant variant = HelidonVariant.SE;
    private Path targetDir = TestFiles.targetDir();

    @Test
    @Order(1)
    public void testInit() throws Exception {
        TestUtils.ExecResult res = exec("init",
                "--flavor", variant.toString(),
                "--project ", targetDir.toString(),
                "--version ", HELIDON_VERSION);
        assertThat(res.code, is(equalTo(0)));
        System.out.println(res.output);
        TestFiles.ensureFile(targetDir.resolve(quickstartId(variant)));
    }

    // @Test - Uncomment after first release of build tools with devloop
    @Order(2)
    public void testBuild() throws Exception {
        Path projectDir = targetDir.resolve(quickstartId(variant));
        TestUtils.ExecResult res = exec("build",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
        System.out.println(res.output);
        TestFiles.ensureFile(TestFiles.helidonSeJar());
    }

    @Test
    @Order(3)
    public void testInfo() throws Exception {
        Path projectDir = targetDir.resolve(quickstartId(variant));
        TestUtils.ExecResult res = exec("info",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
        System.out.println(res.output);
    }

    @Test
    @Order(4)
    public void testVersion() throws Exception {
        Path projectDir = targetDir.resolve(quickstartId(variant));
        TestUtils.ExecResult res = exec("version",
                "--project ", projectDir.toString());
        assertThat(res.code, is(equalTo(0)));
        System.out.println(res.output);
    }

    @Test
    @Order(5)
    public void testFeatures() throws Exception {
        Path projectDir = targetDir.resolve(quickstartId(variant));
        TestUtils.ExecResult res = exec("features",
                "--project ", projectDir.toString(),
                "--all");
        assertThat(res.code, is(equalTo(0)));
        System.out.println(res.output);
    }

    @Test
    @Order(6)
    public void testClean() {
        Path projectDir = targetDir.resolve(quickstartId(variant));
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
