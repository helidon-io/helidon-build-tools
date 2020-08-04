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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.execWithDirAndInput;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class InitInteractiveTest.
 */
class InitInteractiveTest extends InitBaseTest {

    protected void init(String flavor, String archetypeName) throws Exception {
        super.init(flavor, archetypeName);
        File input = new File(InitCommand.class.getResource("input.txt").getFile());
        execWithDirAndInput(targetDir().toFile(), input,"init",
                            "--url", metadataUrl(),
                            "--version", helidonTestVersion(),
                            "--flavor", flavor);
    }

    @Test
    public void testInitSe() throws Exception {
        init(InitCommand.DEFAULT_FLAVOR, InitCommand.DEFAULT_ARCHETYPE_NAME);
        assertPackageExist(projectDir(), packageName());
    }

    @Test
    public void testInitMp() throws Exception {
        init("MP", InitCommand.DEFAULT_ARCHETYPE_NAME);
        assertPackageExist(projectDir(), packageName());
        Path config = projectDir().resolve("src/main/resources/META-INF/microprofile-config.properties");
        assertTrue(Files.exists(config));
    }
}
