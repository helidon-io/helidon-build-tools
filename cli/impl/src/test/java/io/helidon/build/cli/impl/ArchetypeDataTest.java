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

import io.helidon.build.common.Lists;
import io.helidon.build.common.maven.MavenVersion;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.ArchetypesData.Builder;
import static io.helidon.build.cli.impl.ArchetypesData.Version;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link ArchetypesData}
 */
public class ArchetypeDataTest {

    @Test
    public void testLatestVersion() {
        List<String> versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT");
        assertThat(latest(versionIds).toString(), is("4.0.1-SNAPSHOT"));

        versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9");
        assertThat(latest(versionIds).toString(), is("4.0.1-SNAPSHOT"));

        versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9");
        assertThat(latest(versionIds).toString(), is("3.9.8"));

        versionIds = List.of();
        assertThat(latest(versionIds), is(nullValue()));
    }

    @Test
    public void testLatestMajorVersions() {
        List<String> versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9", "4.0.0", "4.0.1-SNAPSHOT");
        assertThat(Lists.map(latestMajorVersions(versionIds), MavenVersion::toString),
                contains("2.9", "3.9.8", "4.0.1-SNAPSHOT"));

        versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "4.0.1-SNAPSHOT", "3.0.0", "3.9.8", "2.9");
        assertThat(Lists.map(latestMajorVersions(versionIds), MavenVersion::toString),
                contains("2.9", "3.9.8", "4.0.1-SNAPSHOT"));

        versionIds = List.of("2.1.3", "2.4.5", "2.0.5", "3.0.0", "3.9.8", "2.9");
        assertThat(Lists.map(latestMajorVersions(versionIds), MavenVersion::toString), contains("2.9", "3.9.8"));

        versionIds = List.of();
        assertThat(latestMajorVersions(versionIds).size(), is(0));
    }

    private MavenVersion latest(List<String> versionIds) {
        Builder builder = ArchetypesData.builder();
        versionIds.forEach(versionId -> builder.addVersion(new Version(versionId)));
        ArchetypesData archetypesData = builder.build();
        return archetypesData.latestVersion();
    }

    private List<MavenVersion> latestMajorVersions(List<String> versionIds) {
        Builder builder = ArchetypesData.builder();
        versionIds.forEach(versionId -> builder.addVersion(new Version(versionId)));
        ArchetypesData archetypesData = builder.build();
        return archetypesData.latestMajorVersions();
    }
}
