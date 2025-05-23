/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.BeforeAll;

import static io.helidon.build.cli.impl.TestUtils.helidonTestVersion;
import static io.helidon.build.cli.common.CliProperties.HELIDON_PLUGIN_VERSION_PROPERTY;
import static io.helidon.build.cli.common.CliProperties.HELIDON_VERSION_PROPERTY;

/**
 * Base class for command tests.
 */
class CommandTestBase {

    /**
     * Overrides version under test. This property must be propagated to all
     * forked processes.
     */
    @BeforeAll
    static void setHelidonVersion() {
        System.setProperty(HELIDON_VERSION_PROPERTY, helidonTestVersion());
        System.setProperty(HELIDON_PLUGIN_VERSION_PROPERTY, helidonTestVersion());
    }
}
