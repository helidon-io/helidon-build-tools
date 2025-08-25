/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MavenArtifact}.
 */
class MavenArtifactTest {

    @Test
    void testCreate() {
        MavenArtifact p;

        p = MavenArtifact.create("com.acme:foo:1.0");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.version(), is("1.0"));
        assertThat(p.type(), is(nullValue()));
        assertThat(p.classifier(), is(nullValue()));

        p = MavenArtifact.create("com.acme:foo:jar:1.0");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.version(), is("1.0"));
        assertThat(p.type(), is("jar"));
        assertThat(p.classifier(), is(nullValue()));

        p = MavenArtifact.create("com.acme:foo:jar:javadoc:1.0");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.version(), is("1.0"));
        assertThat(p.type(), is("jar"));
        assertThat(p.classifier(), is("javadoc"));

        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create(""));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:a:"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:a::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:a:c:"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:a:c::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("g:a::?"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("::::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create(":::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create("::"));
        assertThrows(IllegalArgumentException.class, () -> MavenArtifact.create(":"));
    }
}
