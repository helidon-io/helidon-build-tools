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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.HelidonVersions.HELIDON_PROJECT_ARTIFACT_ID;
import static io.helidon.build.util.HelidonVersions.HELIDON_PROJECT_GROUP_ID;
import static io.helidon.build.util.MavenVersion.toMavenVersion;
import static io.helidon.build.util.MavenVersion.unqualifiedMinimum;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link MavenVersions} .
 */
class MavenVersionsTest {
    private static final String INVALID_HOST_NAME = "not.a.domain.x";
    private static final URI INACCESSIBLE_MAVEN_REPO = toUri("http://" + INVALID_HOST_NAME + "/maven/repository");
    private static final URI MAVEN_REPO_2_0_0_M2 = mavenRepoUri("2.0.0-M2");
    private static final URI MAVEN_REPO_2_0_0 = mavenRepoUri("2.0.0");

    @Test
    void testUriNotAccessible() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> MavenVersions.builder()
                                                              .repository(INACCESSIBLE_MAVEN_REPO)
                                                              .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                              .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                              .build()).getMessage();
        assertThat(errorMessage, containsString("not.a.domain.x"));
    }

    @Test
    void testUriNotAccessibleAndFilteredFallbackIsEmpty() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> MavenVersions.builder()
                                                              .repository(INACCESSIBLE_MAVEN_REPO)
                                                              .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                              .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                              .filter(MavenVersion.notQualified())
                                                              .fallbackVersions(List.of("1.2.3-SNAPSHOT"))
                                                              .build()).getMessage();
        assertThat(errorMessage, containsString("no fallback versions matching the filter"));
    }

    @Test
    void testUriNotAccessibleAndFilteredFallbackIsNotEmpty() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .repository(INACCESSIBLE_MAVEN_REPO)
                                                    .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                    .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                    .filter(MavenVersion.notQualified())
                                                    .fallbackVersions(List.of("0.0.1", "1.2.3", "1.2.0"))
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(3));
        assertThat(versions.latest().toString(), is("1.2.3"));
        assertThat(versions.versions().contains(toMavenVersion("1.2.0")), is(true));
    }

    @Test
    void testQualifiedLessThanUnqualified() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .repository(INACCESSIBLE_MAVEN_REPO)
                                                    .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                    .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                    .fallbackVersions(List.of("2.0.0-SNAPSHOT",
                                                                              "2.0.0-M1",
                                                                              "2.0.0",
                                                                              "1.0.0"))
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("fallback"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(4));
        assertThat(versions.versions().get(0), is(toMavenVersion("2.0.0")));
        assertThat(versions.versions().get(1), is(toMavenVersion("2.0.0-SNAPSHOT")));
        assertThat(versions.versions().get(2), is(toMavenVersion("2.0.0-M1")));
        assertThat(versions.versions().get(3), is(toMavenVersion("1.0.0")));
        assertThat(versions.latest(), is(toMavenVersion("2.0.0")));
    }

    @Test
    void testAllCurrentHelidonReleases() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .repository(MAVEN_REPO_2_0_0_M2)
                                                    .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                    .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("file"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(29));
        assertThat(versions.latest(), is(toMavenVersion("2.0.0-M2")));
        assertThat(versions.versions().get(0), is(toMavenVersion("2.0.0-M2")));
    }

    @Test
    void testAllFutureHelidonReleases() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .repository(MAVEN_REPO_2_0_0)
                                                    .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                    .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("file"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(30));
        assertThat(versions.latest(), is(toMavenVersion("2.0.0")));
        assertThat(versions.versions().get(0), is(toMavenVersion("2.0.0")));
    }

    @Test
    void testUnqualifiedMinimumCurrentHelidonReleases() {
        String errorMessage = assertThrows(IllegalStateException.class,
                                           () -> MavenVersions.builder()
                                                              .filter(unqualifiedMinimum("2.0.0"))
                                                              .repository(MAVEN_REPO_2_0_0_M2)
                                                              .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                              .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                              .build()).getMessage();
        assertThat(errorMessage, containsString("No versions found"));

    }

    @Test
    void testUnqualifiedMinimumFutureHelidonReleases() {
        final MavenVersions versions = MavenVersions.builder()
                                                    .filter(unqualifiedMinimum("2.0.0"))
                                                    .repository(MAVEN_REPO_2_0_0)
                                                    .artifactGroupId(HELIDON_PROJECT_GROUP_ID)
                                                    .artifactId(HELIDON_PROJECT_ARTIFACT_ID)
                                                    .build();
        assertThat(versions, is(not(nullValue())));
        assertThat(versions.source(), containsString("file"));
        assertThat(versions.versions(), is(not(nullValue())));
        assertThat(versions.versions().size(), is(1));
        assertThat(versions.latest(), is(toMavenVersion("2.0.0")));
        assertThat(versions.versions().get(0), is(toMavenVersion("2.0.0")));
    }

    private static URI mavenRepoUri(String version) {
        final String repoName = "maven-repo-" + version;
        final Path testClasses = Paths.get(MavenVersionsTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return assertDir(testClasses.resolve(repoName)).toUri();
    }

    private static URI toUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
