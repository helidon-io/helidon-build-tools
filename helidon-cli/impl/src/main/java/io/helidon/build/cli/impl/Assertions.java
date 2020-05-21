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

import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.MavenVersion;
import io.helidon.build.util.Requirements;
import io.helidon.build.util.RequirementsFailure;

import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * Common assertions.
 */
public class Assertions {

    static final MavenVersion MINIMUM_HELIDON_VERSION = toMavenVersion("2.0.0-M4");
    static final MavenVersion ALLOWED_HELIDON_SNAPSHOT_VERSION = toMavenVersion("2.0.0-SNAPSHOT");
    static final MavenVersion MINIMUM_REQUIRED_MAVEN_VERSION = toMavenVersion("3.6.0");

    /**
     * Helidon version not supported message.
     */
    private static final String UNSUPPORTED_HELIDON_VERSION = "$(red Helidon version) $(RED %s) $(red is not supported.)";

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementsFailure If the version does not meet the requirement.
     */
    static String assertSupportedHelidonVersion(String helidonVersion) {
        assertSupportedHelidonVersion(toMavenVersion(helidonVersion));
        return helidonVersion;
    }

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementsFailure If the version does not meet the requirement.
     */
    static MavenVersion assertSupportedHelidonVersion(MavenVersion helidonVersion) {
        Requirements.requires(helidonVersion.equals(ALLOWED_HELIDON_SNAPSHOT_VERSION)
                        || helidonVersion.isGreaterThanOrEqualTo(MINIMUM_HELIDON_VERSION),
                UNSUPPORTED_HELIDON_VERSION, helidonVersion);
        return helidonVersion;
    }

    /**
     * Assert that the installed Maven version is at least the required minimum.
     *
     * @throws RequirementsFailure If the installed version does not meet the requirement.
     */
    static void assertRequiredMavenVersion() {
        MavenCommand.assertRequiredMavenVersion(MINIMUM_REQUIRED_MAVEN_VERSION);
    }

    private Assertions() {
    }
}
