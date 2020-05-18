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

import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.InitCommand.DEFAULT_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_ARTIFACT_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_GROUP_ID;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_PACKAGE;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_APPTYPE;
import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.exec;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Class InitCommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitBatchTest extends BaseCommandTest {

    private final Flavor flavor = Flavor.MP;
    private final Path targetDir = TestFiles.targetDir();

    @BeforeEach
    public void precondition() {
        assumeTrue(TestUtils.apptypeArchetypeFound(flavor, HELIDON_SNAPSHOT_VERSION, DEFAULT_APPTYPE));
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        TestUtils.ExecResult res = exec("init",
                "--batch",
                "--flavor", flavor.toString(),
                "--project ", targetDir.toString(),
                "--version ", HELIDON_SNAPSHOT_VERSION,
                "--groupid", DEFAULT_GROUP_ID,
                "--artifactid", DEFAULT_ARTIFACT_ID,
                "--package", DEFAULT_PACKAGE);
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
        assertPackageExist(targetDir.resolve(DEFAULT_NAME), DEFAULT_PACKAGE);
    }

    @Test
    @Order(2)
    public void testClean() {
        Path projectDir = targetDir.resolve(DEFAULT_NAME);
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
