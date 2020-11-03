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

package io.helidon.build.test;

import static java.lang.System.getProperty;

/**
 * Helidon test versions.
 * <em>IMPORTANT:</em> The constants must be kept up to date as new Helidon releases occur.
 */
public class HelidonTestVersions {

    // IMPORTANT: These constants must be kept up to date as new Helidon releases occur.
    private static final String CURRENT_HELIDON_SNAPSHOT_VERSION = "2.1.1-SNAPSHOT";
    private static final String CURRENT_HELIDON_RELEASE_VERSION = "2.1.0";
    private static final String PREVIOUS_HELIDON_RELEASE_VERSION = "2.0.2";
    private static final String CURRENT_HELIDON_BUILD_TOOLS_RELEASE_VERSION = "2.1.1";

    /**
     * The Helidon test version override property.
     */
    public static final String HELIDON_TEST_VERSION_PROPERTY = "helidon.test.version";
    private static final String HELIDON_TEST_VERSION = getProperty(HELIDON_TEST_VERSION_PROPERTY,
                                                                   CURRENT_HELIDON_RELEASE_VERSION);

    /**
     * The Helidon build-tools test version override property.
     */
    public static final String HELIDON_BUILD_TOOLS_TEST_VERSION_PROPERTY = "helidon.build.tools.test.version";
    private static final String HELIDON_BUILD_TOOLS_TEST_VERSION = getProperty(HELIDON_BUILD_TOOLS_TEST_VERSION_PROPERTY,
                                                                               CURRENT_HELIDON_BUILD_TOOLS_RELEASE_VERSION);

    /**
     * Returns the current Helidon release version by default. Can be overridden by setting the {@code "helidon.test.version"}
     * system property.
     *
     * @return The version.
     */
    public static String helidonTestVersion() {
        return HELIDON_TEST_VERSION;
    }

    /**
     * Returns the current Helidon build tools release version by default. Can be overridden by setting the
     * {@code "helidon.build.tools.test.version"} system property.
     *
     * @return The version.
     */
    public static String helidonBuildToolsTestVersion() {
        return HELIDON_BUILD_TOOLS_TEST_VERSION;
    }

    /**
     * Returns the current Helidon release version.
     *
     * @return the version.
     */
    public static String currentHelidonReleaseVersion() {
        return CURRENT_HELIDON_RELEASE_VERSION;
    }

    /**
     * Returns the current Helidon snapshot version.
     *
     * @return the version.
     */
    public static String currentHelidonSnapshotVersion() {
        return CURRENT_HELIDON_SNAPSHOT_VERSION;
    }

    /**
     * Returns the previous Helidon release version.
     *
     * @return the version.
     */
    public static String previousHelidonReleaseVersion() {
        return PREVIOUS_HELIDON_RELEASE_VERSION;
    }

    /**
     * Returns the current Helidon build tools release version.
     *
     * @return the version.
     */
    public static String currentHelidonBuildToolsReleaseVersion() {
        return CURRENT_HELIDON_BUILD_TOOLS_RELEASE_VERSION;
    }
}
