/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.util.List;

import io.helidon.build.common.maven.MavenVersion;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for {@link HelidonVersion}.
 */
public class HelidonVersionTest {

    @Test
    public void testMavenVersionFromList() {
        List<String> versions = List.of("2.1.3", "2.4.5", "2.0.5", "2.9", "2.0.1",
                "3.0.0", "3.9.8",
                "4.0.0", "4.0.0-SNAPSHOT");

        var version = HelidonVersion.of("2.0");
        var mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("2.0.5")));

        version = HelidonVersion.of("2.0-LATEST");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("2.0.5")));

        version = HelidonVersion.of("2.3");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, nullValue());

        version = HelidonVersion.of("2.a.3");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, nullValue());

        version = HelidonVersion.of("3.0.0");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("3.0.0")));

        version = HelidonVersion.of("3-latest");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("3.9.8")));

        version = HelidonVersion.of("4.0");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("4.0.0")));

        version = HelidonVersion.of("4.0.0-SNAPSHOT");
        mavenVersion = version.mavenVersionFromList(versions);
        assertThat(mavenVersion, is(MavenVersion.toMavenVersion("4.0.0-SNAPSHOT")));
    }

    @Test
    public void testHelidonVersionOf() {
        var input = "2.0";
        var version = HelidonVersion.of(input);
        assertVersion(version, 2, 0, null, true);

        input = "123.0";
        version = HelidonVersion.of(input);
        assertVersion(version, 123, 0, null, true);

        input = "123.0-LATEST";
        version = HelidonVersion.of(input);
        assertVersion(version, 123, 0, null, true);

        input = "123.034.567";
        version = HelidonVersion.of(input);
        assertVersion(version, 123, 34, "567", false);

        input = "123.a.567";
        version = HelidonVersion.of(input);
        assertThat(version, is(HelidonVersion.INVALID_VERSION));

        input = "123.a-LATEST.567";
        version = HelidonVersion.of(input);
        assertThat(version, is(HelidonVersion.INVALID_VERSION));

        input = "123..567";
        version = HelidonVersion.of(input);
        assertThat(version, is(HelidonVersion.INVALID_VERSION));

        input = "123.567.0-SNAPSHOT";
        version = HelidonVersion.of(input);
        assertVersion(version, 123, 567, "0-snapshot", false);

        input = "123.567.SNAPSHOT-0";
        version = HelidonVersion.of(input);
        assertThat(version, is(HelidonVersion.INVALID_VERSION));

        input = "123.567.0-SNAPSHOT-latest";
        version = HelidonVersion.of(input);
        assertThat(version, is(HelidonVersion.INVALID_VERSION));
    }

    private void assertVersion(HelidonVersion version, Integer major, Integer minor, String micro, boolean latest) {
        assertThat(version.major(), is(major));
        assertThat(version.minor(), is(minor));
        assertThat(version.micro(), is(micro));
        assertThat(version.latest(), is(latest));
    }
}
