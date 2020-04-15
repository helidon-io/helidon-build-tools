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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.HelidonVersions.HELIDON_BOM_ARTIFACT_ID;
import static io.helidon.build.util.HelidonVersions.HELIDON_BOM_GROUP_ID;
import static io.helidon.build.util.MavenVersions.Builder.UNQUALIFIED_FILTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit test for class {@link MavenVersions}.
 */
class MavenVersionsTest {

    @Test
    void testUriNotAccessible() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> MavenVersions.builder()
                                                              .repository(new URI("http://foo.bar/maven/repository"))
                                                              .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                                              .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                                              .build()).getMessage();
        assertThat(errorMessage, containsString("foo.bar"));
    }

    @Test
    void testUriNotAccessibleAndFilteredFallbackIsEmpty() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> MavenVersions.builder()
                                                              .repository(new URI("http://foo.bar/maven/repository"))
                                                              .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                                              .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                                              .filter(UNQUALIFIED_FILTER)
                                                              .fallbackVersions(List.of("1.2.3-SNAPSHOT"))
                                                              .build()).getMessage();
        assertThat(errorMessage, containsString("no fallback versions matching the filter"));
    }

    @Test
    void testUriNotAccessibleAndFilteredFallbackIsNotEmpty() throws Exception {
        final MavenVersions versions = MavenVersions.builder()
                                                    .repository(new URI("http://foo.bar/maven/repository"))
                                                    .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                                    .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                                    .filter(UNQUALIFIED_FILTER)
                                                    .fallbackVersions(List.of("0.0.1", "1.2.3", "1.2.0"))
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(3));
        assertThat(versions.latest(), is("1.2.3"));
        assertThat(versions.versions().contains("1.2.0"), is(true));
    }

    @Test
    void testHelidonReleases() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                                    .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("http"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions(), is(not(empty())));
        assertThat(versions.latest(), is(not(nullValue())));
        assertThat(versions.versions().contains("2.0.0-M1"), is(true));
    }

    @Test
    void testUnqualifiedHelidonReleases() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .filter(UNQUALIFIED_FILTER)
                                                    .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                                    .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("http"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions(), is(not(empty())));
        assertThat(versions.latest(), is(not(nullValue())));
        assertThat(versions.versions().contains("2.0.0-M1"), is(false));
    }

    @Test
    void testLocalHelidonBuilds() {
        final Path localRepo = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");
        assumeTrue(Files.exists(localRepo));

        MavenVersions versions = MavenVersions.builder()
                                              .repository(localRepo.toUri())
                                              .artifactGroupId(HELIDON_BOM_GROUP_ID)
                                              .artifactId(HELIDON_BOM_ARTIFACT_ID)
                                              .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("file"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions(), is(not(empty())));
        assertThat(versions.latest(), is(not(nullValue())));
    }
}
