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

package io.helidon.build.maven.enforcer;

import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.enforcer.GitIgnore.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GitIgnoreTest {

    @Test
    void testSuffix() {
        GitIgnore matcher = create(List.of("*.txt"));

        assertThat(matcher.matches(FileRequest.create(".txt")), is(true));
        assertThat(matcher.matches(FileRequest.create("file.txt")), is(true));
        assertThat(matcher.matches(FileRequest.create(".txt.txt")), is(true));
        assertThat(matcher.matches(FileRequest.create(".txt.foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("dir/.txt")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo.txt.foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("dir/file.txt")), is(false));
        assertThat(matcher.matches(FileRequest.create("/dir/file.txt")), is(false));
    }

    @Test
    void testPrefix() {
        GitIgnore matcher = create(List.of("foo.*"));

        assertThat(matcher.matches(FileRequest.create("foo.")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo.foo.")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar.foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo/foo.")), is(false));
        assertThat(matcher.matches(FileRequest.create("bar.foo.foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo/foo.bar")), is(false));
        assertThat(matcher.matches(FileRequest.create("/foo/foo.bar")), is(false));
    }

    @Test
    void testDirectorySuffix() {
        GitIgnore matcher = create(List.of("**/foo"));

        assertThat(matcher.matches(FileRequest.create("bar/foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar/foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(false));
        assertThat(matcher.matches(FileRequest.create("bar/foo/bar")), is(false));
    }

    @Test
    void testDirectoryPrefix() {
        GitIgnore matcher = create(List.of("foo/**"));

        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar/foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/bar/foo")), is(false));
    }

    @Test
    void testDirectories() {
        GitIgnore matcher = create(List.of("foo/**/bar"));

        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/dir/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("fooo/dir/bar")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo/dir/barr")), is(false));
        assertThat(matcher.matches(FileRequest.create("fooo/dir/barr")), is(false));
    }

    @Test
    void testDirectory() {
        GitIgnore matcher = create(List.of("foo/"));

        assertThat(matcher.matches(FileRequest.create("foo/")), is(true));
        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/foo/")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("fooo/bar")), is(false));
    }

    @Test
    void testComplexPattern() {
        GitIgnore matcher = create(List.of("**/*.foo"));

        assertThat(matcher.matches(FileRequest.create("bar.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("dir/bar.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("dir/dir/foo.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create(".foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("bar.fooo")), is(false));
    }

    @Test
    void testComplexDirectoryPattern() {
        GitIgnore matcher = create(List.of("bar/**/*.foo"));

        assertThat(matcher.matches(FileRequest.create("bar/.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/bar.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/dir/bar.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("bar/dir/dir/foo.foo")), is(true));
        assertThat(matcher.matches(FileRequest.create(".foo")), is(false));
        assertThat(matcher.matches(FileRequest.create("bar/bar.fooo")), is(false));
    }

    @Test
    void testRegex() {
        GitIgnore matcher = create(List.of("[a-z]oo"));

        assertThat(matcher.matches(FileRequest.create("foo")), is(true));
        assertThat(matcher.matches(FileRequest.create("boo")), is(true));
        assertThat(matcher.matches(FileRequest.create("oo")), is(false));
        assertThat(matcher.matches(FileRequest.create("0oo")), is(false));
    }

    @Test
    void testMixRegex() {
        GitIgnore matcher = create(List.of("f?o/b[a-z]r"));

        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(true));
        assertThat(matcher.matches(FileRequest.create("fzo/bzr")), is(true));
        assertThat(matcher.matches(FileRequest.create("f/o/bar")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo/b0r")), is(false));
        assertThat(matcher.matches(FileRequest.create("f/o/b0r")), is(false));
        assertThat(matcher.matches(FileRequest.create("f/oo/b0r")), is(false));
        assertThat(matcher.matches(FileRequest.create("foo/b0rr")), is(false));
    }

    @Test
    void testInclude() {
        GitIgnore matcher = create(List.of("foo", "\\!foo"));
        assertThat(matcher.matches(FileRequest.create("foo")), is(false));

        matcher = create(List.of("/", "\\!foo"));
        assertThat(matcher.matches(FileRequest.create("foo")), is(false));

        matcher = create(List.of("foo/", "\\!foo/bar"));
        assertThat(matcher.matches(FileRequest.create("foo/bar")), is(true));
    }
}
