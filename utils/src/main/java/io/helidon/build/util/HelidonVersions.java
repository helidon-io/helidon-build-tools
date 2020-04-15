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

package io.helidon.build.util;

import java.util.List;
import java.util.function.Predicate;

/**
 * Provides {@link MavenVersions versions} for the {@code helidon-bom} artifact published to Maven central, falling back to
 * a built in list if not accessible.
 */
public class HelidonVersions {
    /**
     * The Helidon bom group id.
     */
    public static final String HELIDON_BOM_GROUP_ID = "io.helidon";

    /**
     * The Helidon bom artifact id.
     */
    public static final String HELIDON_BOM_ARTIFACT_ID = "helidon-bom";

    /**
     * Known release versions. Ideally this should be updated on each release!
     */
    private static final List<String> KNOWN_RELEASES = List.of(
        "2.0.0-M2",
        "2.0.0-M1",
        "1.4.4",
        "1.4.3",
        "1.4.2",
        "1.4.1",
        "1.4.0",
        "1.3.1",
        "1.3.0",
        "1.2.1",
        "1.2.0",
        "1.1.2",
        "1.1.1",
        "1.1.0",
        "1.0.3",
        "1.0.2",
        "1.0.1",
        "1.0.0",
        "0.11.1",
        "0.11.0",
        "0.10.6",
        "0.10.5",
        "0.10.4",
        "0.10.3",
        "0.10.2",
        "0.10.1",
        "0.10.0",
        "0.9.1",
        "0.9.0"
    );

    /**
     * Filter that selects all versions.
     */
    public static final Predicate<String> ALL = version -> true;

    /**
     * Returns a filter that selects unqualified versions whose major number is at least the given minimum.
     *
     * @param minimumMajorVersion The minimum major version number.
     * @return The filter.
     */
    public static Predicate<String> unqualifiedMinimumMajorVersion(int minimumMajorVersion) {
        return version -> majorVersionIsAtLeast(version, minimumMajorVersion);
    }

    /**
     * Returns Helidon versions released to Maven central that match the given filter.
     * If Maven central is not accessible, uses a hard coded list of fallback versions.
     *
     * @param filter The filter.
     * @return The versions.
     * @throws IllegalStateException If there are no versions available.
     */
    public static MavenVersions releases(Predicate<String> filter) {
        return MavenVersions.builder()
                            .filter(filter)
                            .artifactGroupId(HELIDON_BOM_GROUP_ID)
                            .artifactId(HELIDON_BOM_ARTIFACT_ID)
                            .fallbackVersions(KNOWN_RELEASES)
                            .build();
    }


    /**
     * Returns Helidon versions released to Maven central that match the given filter.
     * If Maven central is not accessible, uses the given fallback versions.
     *
     * @param filter The filter.
     * @param fallbackVersions The fallback versions.
     * @return The versions.
     * @throws IllegalStateException If there are no versions available.
     */
    public static MavenVersions releases(Predicate<String> filter, List<String> fallbackVersions) {
        return MavenVersions.builder()
                            .filter(filter)
                            .artifactGroupId(HELIDON_BOM_GROUP_ID)
                            .artifactId(HELIDON_BOM_ARTIFACT_ID)
                            .fallbackVersions(fallbackVersions)
                            .build();
    }

    private static boolean majorVersionIsAtLeast(String version, int minimumVersion) {
        if (!version.contains("-")) {
            final int firstDot = version.indexOf('.');
            if (firstDot > 0) {
                try {
                    final int major = Integer.parseInt(version.substring(0, firstDot));
                    return major >= minimumVersion;
                } catch (NumberFormatException ignore) {
                }
            }
        }
        return false;
    }

    private HelidonVersions() {
    }
}
