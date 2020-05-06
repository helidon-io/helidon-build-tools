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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.BaseCommand.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.test.HelidonTestVersions.currentHelidonReleaseVersion;
import static io.helidon.build.test.HelidonTestVersions.previousHelidonReleaseVersion;
import static io.helidon.build.util.PomUtils.HELIDON_PLUGIN_VERSION_PROPERTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class InitCommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitCommandTest {

    private static final String MY_GROUP_ID = "mygroup";
    private static final String MY_ARTIFACT_ID = "myartifact";
    private static final String MY_PACKAGE = "com.oracle.mypackage";
    private static final String HELIDON_VERSION_TEST = currentHelidonReleaseVersion();
    private static final String HELIDON_VERSION_PREVIOUS = previousHelidonReleaseVersion();

    private final HelidonVariant variant = HelidonVariant.SE;
    private final Path targetDir = TestFiles.targetDir();

    /**
     * Overrides version under test. This property must be propagated to all
     * forked processes.
     */
    @BeforeAll
    public static void setHelidonVersion() {
        System.setProperty(HELIDON_VERSION_PROPERTY, HELIDON_VERSION_TEST);
        System.setProperty(HELIDON_PLUGIN_VERSION_PROPERTY, HELIDON_VERSION_TEST);
    }

    @Test
    @Order(1)
    public void testInitGroupPackage() throws Exception {
        TestUtils.ExecResult res = exec("init",
                "--flavor", variant.toString(),
                "--project ", targetDir.toString(),
                "--version ", HELIDON_VERSION_PREVIOUS,
                "--groupid", MY_GROUP_ID,
                "--artifactid", MY_ARTIFACT_ID,
                "--package", MY_PACKAGE);
        System.out.println(res.output);
        assertThat(res.code, is(equalTo(0)));
        assertPackageExist(targetDir.resolve(MY_ARTIFACT_ID), MY_PACKAGE);
    }

    @Test
    @Order(2)
    public void testCleanGroupPackage() {
        Path projectDir = targetDir.resolve(MY_ARTIFACT_ID);
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
