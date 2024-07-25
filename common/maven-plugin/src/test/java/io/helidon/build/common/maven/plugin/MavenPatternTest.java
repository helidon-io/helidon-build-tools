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
package io.helidon.build.common.maven.plugin;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MavenPattern}.
 */
class MavenPatternTest {

    @Test
    void testCreate() {
        MavenPattern p;

        p = MavenPattern.create("com.acme:foo");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.classifier(), is("*"));
        assertThat(p.type(), is("*"));

        p = MavenPattern.create("g:a");
        assertThat(p.groupId(), is("g"));
        assertThat(p.artifactId(), is("a"));
        assertThat(p.classifier(), is("*"));
        assertThat(p.type(), is("*"));

        p = MavenPattern.create("com.acme:foo:sources");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.classifier(), is("sources"));
        assertThat(p.type(), is("*"));

        p = MavenPattern.create("com.acme:foo:sources:jar");
        assertThat(p.groupId(), is("com.acme"));
        assertThat(p.artifactId(), is("foo"));
        assertThat(p.classifier(), is("sources"));
        assertThat(p.type(), is("jar"));

        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create(""));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a:"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a:c:"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a:c::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a::?"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("g:a:c:t:?"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("::::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create(":::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create("::"));
        assertThrows(IllegalArgumentException.class, () -> MavenPattern.create(":"));
    }

    @Test
    void testMatches() {
        assertThat(MavenPattern.create("*:*").matches("com.acme", "foo", null, "jar"), is(true));
        assertThat(MavenPattern.create("com.acme:*").matches("com.acme", "foo", null, "jar"), is(true));
        assertThat(MavenPattern.create("com.acme:bar").matches("com.acme", "foo", null, "jar"), is(false));
        assertThat(MavenPattern.create("com.acme:foo:*").matches("com.acme", "foo", "sources", "jar"), is(true));
        assertThat(MavenPattern.create("com.acme:foo:*:jar").matches("com.acme", "foo", null, "jar"), is(true));
        assertThat(MavenPattern.create("com.acme:foo:javadoc").matches("com.acme", "foo", "sources", "jar"), is(false));
        assertThat(MavenPattern.create("com.acme:foo:javadoc:*").matches("com.acme", "foo", "javadoc", "jar"), is(true));
        assertThat(MavenPattern.create("com.acme:foo:javadoc:jar").matches("com.acme", "foo", "javadoc", "zip"), is(false));
    }
}
