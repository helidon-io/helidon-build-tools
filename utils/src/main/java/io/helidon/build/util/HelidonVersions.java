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

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provides {@link MavenVersions versions} for the {@code helidon-bom} artifact published to Maven central, falling back to
 * a built in list if not accessible.
 */
public class HelidonVersions {
    /**
     * The Helidon project group id.
     */
    public static final String HELIDON_PROJECT_GROUP_ID = "io.helidon";

    /**
     * The Helidon project artifact id.
     */
    public static final String HELIDON_PROJECT_ARTIFACT_ID = "helidon-project";

    /**
     * Returns Helidon versions released to Maven central that match the given filter.
     *
     * @param filter The filter.
     * @return The versions.
     * @throws IOException If there is a problem accessing the repository, e.g. {@link SocketException} if there is a problem
     * connecting or {@link SocketTimeoutException} if there is a timeout connecting to or reading a result.
     * @throws IllegalStateException If there are no matching versions available.
     */
    public static MavenVersions releases(Predicate<MavenVersion> filter) throws IOException {
        return MavenVersions.builder()
                            .filter(filter)
                            .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                            .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                            .build();
    }


    /**
     * Returns Helidon versions released to Maven central that match the given filter.
     * If Maven central is not accessible, uses the given fallback versions.
     *
     * @param filter The filter.
     * @param fallbackVersions The fallback versions.
     * @return The versions.
     * @throws IOException If there is a problem accessing the repository, e.g. {@link SocketException} if there is a problem
     * connecting or {@link SocketTimeoutException} if there is a timeout connecting to or reading a result.
     * @throws IllegalStateException If there are no matching versions available.
     */
    public static MavenVersions releases(Predicate<MavenVersion> filter, List<String> fallbackVersions) throws IOException {
        return MavenVersions.builder()
                            .filter(filter)
                            .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                            .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                            .fallbackVersions(fallbackVersions)
                            .build();
    }

    private HelidonVersions() {
    }
}
