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

/**
 * Helidon test versions.
 * <em>IMPORTANT:</em> The constants must be kept up to date as new releases occur.
 */
public class HelidonTestVersions {
    private static final String CURRENT_HELIDON_RELEASE_VERSION = "2.0.0-M2";
    private static final String CURRENT_HELIDON_SNAPSHOT_VERSION = "2.0.0-SNAPSHOT";
    private static final String PREVIOUS_HELIDON_RELEASE_VERSION = "2.0.0-M1";

    private static final String CURRENT_HELIDON_BUILD_TOOLS_RELEASE_VERSION = "2.0.0-M2";

    /**
     * Returns the current release version.
     *
     * @return the version.
     */
    public static String currentHelidonReleaseVersion() {
        return CURRENT_HELIDON_RELEASE_VERSION;
    }

    /**
     * Returns the current snapshot version.
     *
     * @return the version.
     */
    public static String currentHelidonSnapshotVersion() {
        return CURRENT_HELIDON_SNAPSHOT_VERSION;
    }

    /**
     * Returns the previous release version.
     *
     * @return the version.
     */
    public static String previousHelidonReleaseVersion() {
        return PREVIOUS_HELIDON_RELEASE_VERSION;
    }

    /**
     * Returns the current release version.
     *
     * @return the version.
     */
    public static String currentHelidonBuildToolsReleaseVersion() {
        return CURRENT_HELIDON_BUILD_TOOLS_RELEASE_VERSION;
    }
}
