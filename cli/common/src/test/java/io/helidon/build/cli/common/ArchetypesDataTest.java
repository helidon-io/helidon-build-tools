/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link ArchetypesData}
 */
class ArchetypesDataTest {

    @Test
    void testLoad() {
        Path versionsFile = testResourcePath(ArchetypesData.class, "versions/cli-data/versions.xml");
        ArchetypesData archetypesData = ArchetypesData.load(versionsFile);
        ArchetypesData.Version defaultVersion = archetypesData.rawVersions().stream()
                .filter(ArchetypesData.Version::isDefault)
                .findFirst()
                .orElseThrow();

        assertThat(archetypesData.rawVersions().size(), CoreMatchers.is(29));
        assertThat(archetypesData.rules().size(), CoreMatchers.is(3));
        assertThat(archetypesData.versions(), hasItems("2.0.0", "2.3.4", "3.1.2"));
        assertThat(defaultVersion.id(), CoreMatchers.is("4.0.0-SNAPSHOT"));
        assertThat(archetypesData.rules().get(0).archetypeRange().toString(), CoreMatchers.is("[2.0.0,3.0.0)"));
        assertThat(archetypesData.rules().get(0).cliRange().toString(), CoreMatchers.is("[2.0.0,5.0.0)"));
        assertThat(archetypesData.rules().get(2).archetypeRange().toString(), CoreMatchers.is("[4.0.0,5.0.0)"));
        assertThat(archetypesData.rules().get(2).cliRange().toString(), CoreMatchers.is("[4.0.0,5.0.0)"));
    }

    @Test
    void testLatestVersion() {
        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT").latestVersion(),
                is(toMavenVersion("4.0.1-SNAPSHOT")));

        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9").latestVersion(),
                is(toMavenVersion("4.0.1-SNAPSHOT")));

        assertThat(
                data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9").latestVersion(),
                is(toMavenVersion("3.9.8")));
    }

    @Test
    void testLatestMajorVersions() {
        ArchetypesData data = data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT");
        assertThat(data.latestMajorVersions(), Matchers.contains("4.0.1-SNAPSHOT", "3.9.8", "2.9"));

        data = data("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9");
        assertThat(data.latestMajorVersions(), Matchers.contains("4.0.1-SNAPSHOT", "3.9.8", "2.9"));

        data = data("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9");
        assertThat(data.latestMajorVersions(), Matchers.contains("3.9.8", "2.9"));

        data = ArchetypesData.EMPTY;
        assertThat(data.latestMajorVersions().size(), Matchers.is(0));
    }

    @Test
    void testLatestMajorVersionsOrdering() {
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
    void testVersionOrdering() {
        assertThat(data(version("1.0.0", 100),
                        version("1.0.1", 100),
                        version("2.0.0", 100),
                        version("4.0.0", 200)).latestMajorVersions(),
                Matchers.is(List.of("2.0.0", "1.0.1", "4.0.0")));
    }

    @Test
    void testVersionQualifierOrdering() {
        String version1;
        String version2;
        List<String> versions = List.of(
                "1.0-SNAPSHOT",
                "1.0-ALPHA",
                "1.0-BETA",
                "1.0-MILESTONE",
                "1.0-RC",
                "1.0",
                "1.0-sp");

        for (int i = 0; i < versions.size() - 1; i++) {
            version1 = versions.get(i);
            version2 = versions.get(i + 1);
            assertThat(String.format("%s should be picked over %s", version1, version2),
                    data(version(version1, 100),
                            version(version2, 100))
                            .latestMajorVersions(),
                    Matchers.is(List.of(version2)));
        }
    }

    private static ArchetypesData data(ArchetypesData.Version... versions) {
        return new ArchetypesData(List.of(versions), List.of());
    }

    private static ArchetypesData data(String... versions) {
        return data(Arrays.stream(versions)
                .map(version -> version(version, 0))
                .toArray(ArchetypesData.Version[]::new));
    }

    private static ArchetypesData.Version version(String id, int order) {
        return new ArchetypesData.Version(id, false, order);
    }
}
