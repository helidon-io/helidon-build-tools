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
package io.helidon.build.cli.common;

import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link LatestVersion}.
 */
class LatestVersionTest {
    private static final String RULE_HELIDON_2 = "cli.[2-alpha,3-alpha).latest=[2-alpha,3-alpha)";
    private static final String RULE_HELIDON_3 = "cli.[3-alpha,).latest=[3-alpha,)";

    @Test
    void testEmpty() {
        Exception e = assertThrows(IllegalStateException.class, () -> LatestVersion.create(List.of()));
        assertThat(e.getMessage(), is("No versions found."));
    }

    @Test
    void testFirstLineOnlyWhitespace() {
        Exception e = assertThrows(IllegalStateException.class, () -> LatestVersion.create(List.of(" ")));
        assertThat(e.getMessage(), is("The first non-empty line must be a 2.x version, but is only whitespace."));
    }

    @Test
    void testFirstLineIsNot2x() {
        Exception e = assertThrows(IllegalStateException.class, () -> LatestVersion.create(List.of("3.0.0-alpha")));
        assertThat(e.getMessage(), is("The first non-empty line must be a 2.x version, is: 3.0.0-alpha"));
    }

    @Test
    void testNoMatchingRule() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.2", "3.0.1", RULE_HELIDON_2, RULE_HELIDON_3));
        Exception e = assertThrows(IllegalStateException.class, () -> latest.latest(toMavenVersion("1.0")));
        assertThat(e.getMessage(), is("No rule matches CLI version 1.0"));
    }

    @Test
    void testNoMatchingVersionForRule() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.2", "2.5.0", RULE_HELIDON_2, RULE_HELIDON_3));
        Exception e = assertThrows(IllegalStateException.class, () -> latest.latest(toMavenVersion("3.0.0")));
        assertThat(e.getMessage(), is("No matching version for CLI version 3.0.0 with rule [3-alpha,). Versions: [2.4.2, 2.5.0]"));
    }

    @Test
    void testDuplicateProperties() {
        Exception e = assertThrows(IllegalStateException.class, () -> LatestVersion.create(List.of("2.4.1","k1=v1", "k1=v2")));
        assertThat(e.getMessage(), is("Duplicate property 'k1'"));
    }

    @Test
    void testSingleVersion() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.1"));
        assertThat(latest, is(not(nullValue())));
        assertThat(latest.versions().size(), is(1));
        assertThat(latest.rules().size(), is(0));
        assertThat(latest.properties().size(), is(0));
        assertThat(latest.latest(toMavenVersion("2.3.0")), is(toMavenVersion("2.4.1")));
    }

    @Test
    void testMultipleVersions() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.1", "3.0.0"));
        assertThat(latest, is(not(nullValue())));
        assertThat(latest.versions().size(), is(2));
        assertThat(latest.rules().size(), is(0));
        assertThat(latest.properties().size(), is(0));
        assertThat(latest.latest(toMavenVersion("2.3.0")), is(toMavenVersion("2.4.1")));
    }

    @Test
    void testProperties() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.1", "k1=v1", "k2=v2","k3=foo=bar"));
        assertThat(latest, is(not(nullValue())));
        assertThat(latest.versions().size(), is(1));
        assertThat(latest.rules().size(), is(0));
        assertThat(latest.properties().size(), is(3));
        assertThat(latest.latest(toMavenVersion("2.3.0")), is(toMavenVersion("2.4.1")));
        assertThat(latest.properties().get("k1"), is("v1"));
        assertThat(latest.properties().get("k2"), is("v2"));
        assertThat(latest.properties().get("k3"), is("foo=bar"));
    }

    @Test
    void testRulesMatch() {
        LatestVersion latest = LatestVersion.create(List.of("2.4.2", "3.0.1", RULE_HELIDON_2, RULE_HELIDON_3));
        assertThat(latest, is(not(nullValue())));
        assertThat(latest.versions().size(), is(2));
        assertThat(latest.rules().size(), is(2));
        assertThat(latest.properties().size(), is(2));
        assertThat(latest.latest(toMavenVersion("2.3.0")), is(toMavenVersion("2.4.2")));
        assertThat(latest.latest(toMavenVersion("3.0.0")), is(toMavenVersion("3.0.1")));
    }
}
