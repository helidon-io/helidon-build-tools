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

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.HelidonVersions.unqualifiedMinimumMajorVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link HelidonVersions}.
 */
class HelidonVersionsTest {

    @Test
    void testFilteredFallbackIsEmpty() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> HelidonVersions.releases(unqualifiedMinimumMajorVersion(99999),
                                                                          List.of("2.0.0",
                                                                                  "1.2.3-SNAPSHOT",
                                                                                  "99999.0.0-SNAPSHOT"))
        ).getMessage();
        assertThat(errorMessage, containsString("no fallback versions matching the filter"));
    }

    @Test
    void testFilteredFallbackIsNotEmpty() {
        final MavenVersions versions = HelidonVersions.releases(unqualifiedMinimumMajorVersion(99999),
                                                                List.of("2.0.0", "1.2.3-SNAPSHOT", "99999.0.0"));
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(1));
        assertThat(versions.latest(), is("99999.0.0"));
        assertThat(versions.versions().contains("99999.0.0"), is(true));
    }

    @Test
    void testAllHelidonReleases() {
        final MavenVersions versions = HelidonVersions.releases(HelidonVersions.ALL);
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("http"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions(), is(not(empty())));
        assertThat(versions.latest(), is(not(nullValue())));
        assertThat(versions.versions().contains("2.0.0-M2"), is(true));
    }
}
