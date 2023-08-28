/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.greaterThan;
import static io.helidon.build.common.maven.MavenVersion.greaterThanOrEqualTo;
import static io.helidon.build.common.maven.MavenVersion.lessThan;
import static io.helidon.build.common.maven.MavenVersion.lessThanOrEqualTo;
import static io.helidon.build.common.maven.MavenVersion.notQualified;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link MavenVersion}.
 */
class MavenVersionTest {
    private static final String ONE_0_0_STRING = "1.0.0";
    private static final String ONE_0_0_SNAPSHOT_STRING = "1.0.0-SNAPSHOT";
    private static final String TWO_0_0_STRING = "2.0.0";
    private static final String TWO_0_0_SNAPSHOT_STRING = "2.0.0-SNAPSHOT";

    private final MavenVersion ONE_0_0 = toMavenVersion(ONE_0_0_STRING);
    private final MavenVersion ONE_0_0_SNAPSHOT = toMavenVersion(ONE_0_0_SNAPSHOT_STRING);
    private final MavenVersion TWO_0_0 = toMavenVersion(TWO_0_0_STRING);
    private final MavenVersion TWO_0_0_M1 = toMavenVersion(TWO_0_0_SNAPSHOT_STRING);

    @Test
    void testNotQualifiedPredicate() {
        assertThat(notQualified().test(ONE_0_0), is(true));
        assertThat(notQualified().test(ONE_0_0_SNAPSHOT), is(false));
    }

    @Test
    void testLessThanPredicate() {
        assertThat(lessThan(TWO_0_0_STRING).test(ONE_0_0), is(true));
        assertThat(lessThan(TWO_0_0_STRING).test(TWO_0_0), is(false));
        assertThat(lessThan(TWO_0_0_STRING).test(TWO_0_0_M1), is(true));
    }

    @Test
    void testLessThanOrEqualToPredicate() {
        assertThat(lessThanOrEqualTo(TWO_0_0_STRING).test(ONE_0_0), is(true));
        assertThat(lessThanOrEqualTo(TWO_0_0_STRING).test(TWO_0_0), is(true));
        assertThat(lessThanOrEqualTo(TWO_0_0_STRING).test(TWO_0_0_M1), is(true));
    }

    @Test
    void testGreaterThanPredicate() {
        assertThat(greaterThan(ONE_0_0_STRING).test(TWO_0_0), is(true));
        assertThat(greaterThan(ONE_0_0_STRING).test(ONE_0_0), is(false));
        assertThat(greaterThan(TWO_0_0_SNAPSHOT_STRING).test(TWO_0_0), is(true));
    }

    @Test
    void testGreaterThanOrEqualToPredicate() {
        assertThat(greaterThanOrEqualTo(ONE_0_0_STRING).test(TWO_0_0), Matchers.is(true));
        assertThat(greaterThanOrEqualTo(ONE_0_0_STRING).test(ONE_0_0), Matchers.is(true));
        assertThat(greaterThanOrEqualTo(TWO_0_0_SNAPSHOT_STRING).test(TWO_0_0), Matchers.is(true));
    }

    @Test
    void testReleaseCandidateVersions() {
        assertThat(toMavenVersion("2.0.0-RC1"), is(org.hamcrest.Matchers.greaterThan(toMavenVersion("2.0.0-M4"))));
        assertThat(toMavenVersion("2.0.0-RC2"), is(org.hamcrest.Matchers.greaterThan(toMavenVersion("2.0.0-RC1"))));
        assertThat(toMavenVersion("2.0.0"), is(org.hamcrest.Matchers.greaterThan(toMavenVersion("2.0.0-RC2"))));
    }

    @Test
    void testQualifierComparison() {
        MavenVersion version1;
        MavenVersion version2;
        List<MavenVersion> versions = List.of(
                toMavenVersion("0-SNAPSHOT"),
                toMavenVersion("0-ALPHA"),
                toMavenVersion("0-BETA"),
                toMavenVersion("0-MILESTONE"),
                toMavenVersion("0-RC"),
                toMavenVersion("0"),
                toMavenVersion("0-sp"));

        for (int i = 0; i < versions.size() - 1; i++) {
            version1 = versions.get(i);
            version2 = versions.get(i + 1);
            assertThat(String.format("%s should be lower than %s", version1, version2),
                    version1, is(org.hamcrest.Matchers.lessThan(version2)));
        }
    }
}
