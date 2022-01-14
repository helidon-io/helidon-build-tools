/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import io.helidon.build.common.test.utils.TestFiles;

import static io.helidon.build.cli.impl.TestUtils.helidonTestVersion;

/**
 * Base class for init command tests and other tests that use {@code helidon init}.
 */
class InitCommandTestBase extends MetadataAccessTestBase {

    private static final String HELIDON_TEST_VERSION = helidonTestVersion();
    private final Path targetDir = TestFiles.targetDir(InitCommandTestBase.class);

    /**
     * Create a new init command invoker builder.
     * @return InitCommandInvoker.Builder
     */
    protected CommandInvoker.Builder commandInvoker() {
        return CommandInvoker.builder()
                .helidonVersion(HELIDON_TEST_VERSION)
                .metadataUrl(metadataUrl())
                .userConfig(userConfig())
                .workDir(targetDir);
    }
}
