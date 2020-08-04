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

import io.helidon.build.cli.impl.InitCommand.Flavor;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.assertPackageExist;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;

/**
 * Class InitCommandTest.
 */

public class InitBatchTest extends InitBaseTest {

    private final Flavor flavor = Flavor.MP;

    @Test
    public void testInit() throws Exception {
        exec("init",
             "--url", metadataUrl(),
             "--batch",
             "--flavor", flavor.toString(),
             "--project ", targetDir().toString(),
             "--version ", helidonTestVersion());
        assertPackageExist(projectDir(), packageName());
    }
}
