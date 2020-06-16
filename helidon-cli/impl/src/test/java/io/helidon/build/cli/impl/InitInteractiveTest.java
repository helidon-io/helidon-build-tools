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

import java.io.File;
import java.nio.file.Path;

import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.InitCommand.DEFAULT_NAME;
import static io.helidon.build.cli.impl.InitCommand.DEFAULT_PACKAGE;
import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.execWithDirAndInput;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class InitInteractiveTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitInteractiveTest extends MetadataCommandTest {

    private final Path targetDir = TestFiles.targetDir();

    @Test
    @Order(1)
    public void testInitSe() throws Exception {
        startMetadataAccess(false);
        try {
            File input = new File(InitCommand.class.getResource("input.txt").getFile());
            execWithDirAndInput(targetDir.toFile(), input,
                    "init", "--version", helidonTestVersion());
            assertPackageExist(targetDir.resolve(DEFAULT_NAME), DEFAULT_PACKAGE);
        } finally {
            stopMetadataAccess();
        }
    }

    @Test
    @Order(2)
    public void testCleanSe() {
        Path projectDir = targetDir.resolve(DEFAULT_NAME);
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }

    @Test
    @Order(3)
    public void testInitMp() throws Exception {
        File input = new File(InitCommand.class.getResource("input.txt").getFile());
        execWithDirAndInput(targetDir.toFile(), input,
                "init", "--version", helidonTestVersion(), "--flavor", "MP");
        assertPackageExist(targetDir.resolve(DEFAULT_NAME), DEFAULT_PACKAGE);
        Path config = targetDir.resolve(DEFAULT_NAME)
                               .resolve("src/main/resources/META-INF/microprofile-config.properties");
        assertTrue(config.toFile().exists());
    }

    @Test
    @Order(4)
    public void testCleanMp() {
        Path projectDir = targetDir.resolve(DEFAULT_NAME);
        assertTrue(TestFiles.deleteDirectory(projectDir.toFile()));
        System.out.println("Directory " + projectDir + " deleted");
    }
}
