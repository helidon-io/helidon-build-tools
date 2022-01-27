/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static io.helidon.build.common.maven.VersionRange.createFromVersionSpec;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link VersionRange}.
 */
class VersionRangeTest {

    @Test
    void testUnconstrainedVersionRange() {
        // Note: using this form is not advised, as all versions are contained in it!
        String version = "1.2";
        VersionRange range = createFromVersionSpec(version);
        assertThat(range.containsVersion(toMavenVersion(version)), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(true));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("99.99.99")), is(true));
        assertThat(range.toString(), is(version));
    }

    @Test
    void testSingleVersionRange() {
        String version = "1.2";
        String spec = "[" + version + "]";
        VersionRange range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion(version)), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("99.99.99")), is(false));
        assertThat(range.toString(), is("[1.2,1.2]"));
    }

    @Test
    void testVersionRange() {
        String spec = "[2.1,]";
        VersionRange range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("2.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("99.99.99")), is(true));
        assertThat(range.toString(), is(spec));

        spec = "[2.1,3)";
        range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("2.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("3.0.0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("2.99")), is(true));
        assertThat(range.toString(), is(spec));

        spec = "[2.1,3]";
        range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("2.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("3.0.0")), is(true));
        assertThat(range.containsVersion(toMavenVersion("2.99")), is(true));
        assertThat(range.toString(), is(spec));

        spec = "[2-alpha,3-alpha)";
        range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("2.0.0-alpha")), is(true));
        assertThat(range.containsVersion(toMavenVersion("2.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("3.0.0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("2.99")), is(true));
        assertThat(range.toString(), is(spec));

        spec = "[3-alpha,)";
        range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("3.0.0-alpha")), is(true));
        assertThat(range.containsVersion(toMavenVersion("3.1")), is(true));
        assertThat(range.containsVersion(toMavenVersion("0")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("3.0.0")), is(true));
        assertThat(range.containsVersion(toMavenVersion("99.99")), is(true));
        assertThat(range.toString(), is(spec));

        spec = "(,1.0],[1.2,)";
        range = createFromVersionSpec(spec);
        assertThat(range.containsVersion(toMavenVersion("0")), is(true));
        assertThat(range.containsVersion(toMavenVersion("1.0-SNAPSHOT")), is(true));
        assertThat(range.containsVersion(toMavenVersion("1.1")), is(false));
        assertThat(range.containsVersion(toMavenVersion("1.2")), is(true));
        assertThat(range.containsVersion(toMavenVersion("99.99")), is(true));
        assertThat(range.toString(), is(spec));
    }
}
