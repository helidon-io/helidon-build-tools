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
import io.helidon.build.util.RequirementFailure;
import io.helidon.build.util.Requirements;
import io.helidon.build.util.Style;

import static io.helidon.build.util.MavenVersion.toMavenVersion;

/**
 * Command assertions with message strings formatted via {@link Style#render(String, Object...)}.
 */
public class CommandRequirements {

    private static final MavenVersion MINIMUM_HELIDON_VERSION = toMavenVersion("2.0.0-M4");
    private static final MavenVersion ALLOWED_HELIDON_SNAPSHOT_VERSION = toMavenVersion("2.0.0-SNAPSHOT");
    private static final MavenVersion MINIMUM_REQUIRED_MAVEN_VERSION = toMavenVersion("3.6.0");
    private static final String UNSUPPORTED_HELIDON_VERSION = "$(red Helidon version) $(RED %s) $(red is not supported.)";

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementFailure If the version does not meet the requirement.
     */
    static String requireSupportedHelidonVersion(String helidonVersion) {
        requireSupportedHelidonVersion(toMavenVersion(helidonVersion));
        return helidonVersion;
    }

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementFailure If the version does not meet the requirement.
     */
    static MavenVersion requireSupportedHelidonVersion(MavenVersion helidonVersion) {
        Requirements.require(helidonVersion.equals(ALLOWED_HELIDON_SNAPSHOT_VERSION)
                        || helidonVersion.isGreaterThanOrEqualTo(MINIMUM_HELIDON_VERSION),
                UNSUPPORTED_HELIDON_VERSION, helidonVersion);
        return helidonVersion;
    }

    /**
     * Assert that the installed Maven version is at least the required minimum.
     *
     * @throws RequirementFailure If the installed version does not meet the requirement.
     */
    static void requireMinimumMavenVersion() {
        MavenCommand.assertRequiredMavenVersion(MINIMUM_REQUIRED_MAVEN_VERSION);
    }

    private CommandRequirements() {
    }
}
