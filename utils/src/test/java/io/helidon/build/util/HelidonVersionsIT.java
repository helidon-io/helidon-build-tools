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

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static io.helidon.build.util.MavenVersion.unqualifiedMinimum;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for class {@link HelidonVersions}.
 */
class HelidonVersionsIT {
    private static final String TWO_0_0_VERSION_STRING = "2.0.0";
    private static final String HIGH_VERSION_STRING = "99999.99999.99999";
    private static final String HIGHER_VERSION_STRING = "199999.99999.99999";
    private static final MavenVersion HIGHER_VERSION = toMavenVersion(HIGHER_VERSION_STRING);

    private static final Predicate<MavenVersion> UNQUALIFIED_2_0_0_MINIMUM = unqualifiedMinimum(TWO_0_0_VERSION_STRING);
    private static final Predicate<MavenVersion> UNQUALIFIED_HIGH_MINIMUM = unqualifiedMinimum(HIGH_VERSION_STRING);
    static {
        Proxies.setProxyPropertiesFromEnv();
    }

    @Test
    void testFilteredFallbackIsEmpty() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> HelidonVersions.releases(UNQUALIFIED_HIGH_MINIMUM,
                                                                          List.of("2.0.0",
                                                                                  "1.2.3-SNAPSHOT",
                                                                                  "99999.0.0-SNAPSHOT"))
        ).getMessage();
        assertThat(errorMessage, containsString("No matching fallback versions"));
    }

    @Test
    void testFilteredFallbackIsNotEmpty() throws Exception {
        final MavenVersions versions = HelidonVersions.releases(UNQUALIFIED_HIGH_MINIMUM,
                                                                List.of("2.0.0", "1.2.3-SNAPSHOT", HIGHER_VERSION_STRING));
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(1));
        assertThat(versions.latest(), is(HIGHER_VERSION));
        assertThat(versions.versions().contains(HIGHER_VERSION), is(true));
    }

    @Test
    void testUnqualifiedMinimum() throws Exception {
        final MavenVersions versions = HelidonVersions.releases(UNQUALIFIED_2_0_0_MINIMUM,
                                                                List.of("2.0.0-M1", "2.0.0", "3.0.0", "1.2.3-SNAPSHOT"));
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(2));
        assertThat(versions.latest(), is(toMavenVersion("3.0.0")));
        assertThat(versions.versions().get(0), is(toMavenVersion("3.0.0")));
        assertThat(versions.versions().get(1), is(toMavenVersion("2.0.0")));
    }

    @Test
    void testAllReleases() throws Exception {
        final MavenVersions versions = HelidonVersions.releases(v -> true);
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("http"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions(), is(not(empty())));
        assertThat(versions.latest(), is(not(nullValue())));
        assertThat(versions.versions().contains(toMavenVersion("2.0.0-M2")), is(true));
    }
}
