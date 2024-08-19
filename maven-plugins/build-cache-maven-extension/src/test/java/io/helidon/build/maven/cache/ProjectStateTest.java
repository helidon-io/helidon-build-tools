/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.helidon.build.common.Maps;
import io.helidon.build.common.Strings;
import io.helidon.build.common.test.utils.TestFiles;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link ProjectState}.
 */
class ProjectStateTest {

    @Test
    void testSave() throws IOException {
        ProjectState projectState = new ProjectState(Maps.toProperties(Map.of("prop1", "value1")),
                new ArtifactEntry("artifact.jar", "jar", "jar", null, "java", false, true),
                List.of(new ArtifactEntry("artifact-javadoc.jar", "javadoc", "jar", "javadoc", "java", false, false)),
                List.of("src/main/java"),
                List.of("src/test/java"),
                new ProjectFiles(7, 1717820328080L, null, Map.of()),
                List.of(new ExecutionEntry("groupId1", "artifactId1", "version1", "goal1", "id1", XMLElement.builder()
                        .name("configuration")
                        .child(includes -> includes
                                .name("includes")
                                .child(include -> include
                                        .name("include").value("**/*")))
                        .child(excludes -> excludes
                                .name("excludes")
                                .child(exclude -> exclude
                                        .name("exclude").value("foo/**")))
                        .child(elt1 -> elt1
                                .name("elt1")
                                .attributes(Map.of("attr1", "value1")))
                        .build())));

        Path stateFile = Files.createTempFile("state", null);
        projectState.save(stateFile);

        Path expectedFile = TestFiles.testResourcePath(ProjectStateTest.class, "state.xml");
        String actual = Files.readString(stateFile);
        String expected = Files.readString(expectedFile);
        assertThat(actual, is(Strings.normalizeNewLines(stripXmlComments(expected))));
    }

    @Test
    void testLoad() throws IOException {
        Path stateFile = TestFiles.testResourcePath(ProjectStateTest.class, "state.xml");
        ProjectState projectState = ProjectState.load(stateFile);

        assertThat(projectState, is(not(nullValue())));
        assertThat(Maps.fromProperties(projectState.properties()), is(Map.of("prop1", "value1")));

        assertThat(projectState.artifact(),
                is(new ArtifactEntry("artifact.jar", "jar", "jar", null, "java", false, true)));
        assertThat(projectState.attachedArtifacts(),
                is(List.of(new ArtifactEntry("artifact-javadoc.jar", "javadoc", "jar", "javadoc", "java", false, false))));

        assertThat(projectState.compileSourceRoots(), is(List.of("src/main/java")));
        assertThat(projectState.testCompileSourceRoots(), is(List.of("src/test/java")));

        assertThat(projectState.projectFiles(), is(new ProjectFiles(7, 1717820328080L, null, Map.of())));

        assertThat(projectState.executions(),
                is(List.of(new ExecutionEntry("groupId1", "artifactId1", "version1", "goal1", "id1", XMLElement.builder()
                        .name("configuration")
                        .child(includes -> includes
                                .name("includes")
                                .child(include -> include
                                        .name("include").value("**/*")))
                        .child(excludes -> excludes
                                .name("excludes")
                                .child(exclude -> exclude
                                        .name("exclude").value("foo/**")))
                        .child(elt1 -> elt1
                                .name("elt1")
                                .attributes(Map.of("attr1", "value1")))
                        .build()))));
    }

    static final Pattern XML_COMMENT_PATTERN = Pattern.compile("<!--.*-->\\R?", Pattern.DOTALL);

    private static String stripXmlComments(String xml) {
        return XML_COMMENT_PATTERN.matcher(xml).replaceAll("");
    }
}
