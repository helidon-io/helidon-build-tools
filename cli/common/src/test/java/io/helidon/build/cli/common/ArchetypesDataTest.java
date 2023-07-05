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

package io.helidon.build.cli.common;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link ArchetypesData}
 */
public class ArchetypesDataTest {

    @Test
    public void testLatestVersion() {
        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT").latestVersion(),
                is(toMavenVersion("4.0.1-SNAPSHOT"))
        );

        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9").latestVersion(),
                is(toMavenVersion("4.0.1-SNAPSHOT"))
        );

        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9").latestVersion(),
                is(toMavenVersion("3.9.8"))
        );
    }

    @Test
    public void testLatestMajorVersions() {
        ArchetypesData data = data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT");
        assertThat(data.latestMajorVersions(), Matchers.contains("4.0.1-SNAPSHOT", "3.9.8", "2.9"));

        data = data("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9");
        assertThat(data.latestMajorVersions(), Matchers.contains("4.0.1-SNAPSHOT", "3.9.8", "2.9"));

        data = data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9");
        assertThat(data.latestMajorVersions(), Matchers.contains("3.9.8", "2.9"));

        data = ArchetypesData.builder().build();
        assertThat(data.latestMajorVersions().size(), Matchers.is(0));
    }

    @Test
    public void testLatestMajorVersionsOrdering() {
        ArchetypesData data = data(version("2.0.0", 100),
                version("3.0.0", 100),
                version("4.0.1-SNAPSHOT", 200));
        assertThat(data.latestMajorVersions(), Matchers.contains("3.0.0", "2.0.0", "4.0.1-SNAPSHOT"));

        data = data(version("3.0.0", 100),
                version("3.0.1", 100),
                version("2.0.0", 100),
                version("4.0.0", 200),
                version("4.0.1", 200));
        assertThat(data.latestMajorVersions(), Matchers.contains("3.0.1", "2.0.0", "4.0.1"));
    }

    @Test
    public void testVersionOrdering() {
        assertThat(data(version("1.0.0", 100),
                version("1.0.1", 100),
                version("2.0.0", 100),
                version("4.0.0", 200)).latestMajorVersions(),
                Matchers.is(List.of("2.0.0", "1.0.1", "4.0.0")));
    }

    private static ArchetypesData data(ArchetypesData.Version... versions) {
        return ArchetypesData.builder().versions(versions).build();
    }

    private static ArchetypesData data(String... versions) {
        return data(Arrays.stream(versions)
                .map(version -> ArchetypesData.Version.builder().id(version).build())
                .toArray(ArchetypesData.Version[]::new));
    }

    private static ArchetypesData.Version version(String id, int order) {
        return ArchetypesData.Version.builder().id(id).order(order).build();
    }
}
