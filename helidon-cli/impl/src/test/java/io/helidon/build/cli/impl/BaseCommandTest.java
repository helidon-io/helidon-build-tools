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

import org.junit.jupiter.api.BeforeAll;

import io.helidon.build.test.HelidonTestVersions;

import static io.helidon.build.cli.impl.BaseCommand.HELIDON_VERSION_PROPERTY;
import static io.helidon.build.util.PomUtils.HELIDON_PLUGIN_VERSION_PROPERTY;

/**
 * Class BaseCommandTest.
 */
public class BaseCommandTest {

    static final String HELIDON_VERSION_TEST = HelidonTestVersions.currentHelidonReleaseVersion();
    static final String HELIDON_SNAPSHOT_VERSION = HelidonTestVersions.currentHelidonSnapshotVersion();

    /**
     * Overrides version under test. This property must be propagated to all
     * forked processes.
     */
    @BeforeAll
    public static void setHelidonVersion() {
        System.setProperty(HELIDON_VERSION_PROPERTY, HELIDON_SNAPSHOT_VERSION);
        System.setProperty(HELIDON_PLUGIN_VERSION_PROPERTY, HELIDON_VERSION_TEST);
    }
}
