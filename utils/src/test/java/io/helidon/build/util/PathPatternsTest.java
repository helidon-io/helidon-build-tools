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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link PathPatterns}.
 */
class PathPatternsTest {

    private static final Path ROOT = Path.of("/root");

    void assertMatch(String pattern, String path, boolean expected) {
        assertThat(PathPatterns.matches(pattern).test(Path.of(path), ROOT), is(expected));
    }

    @Test
    void testRelativeFileNameMatch() {
        assertMatch("**/*", "foo.txt", true);
        assertMatch("**/*", "bar/foo.txt", true);
        assertMatch("*.txt", "foo.txt", true);
        assertMatch("*.txt", "bar/foo.txt", false);
        assertMatch("**/*.txt", "foo.txt", true);
        assertMatch("**/*.txt", "bar/foo.txt", true);
        assertMatch("**/f*.txt", "bar/foo.txt", true);
        assertMatch("**/foo.txt", "bar/foo.txt", true);
        assertMatch("**/?o*.txt", "bar/foo.txt", true);
        assertMatch("**/?o*.txt", "bar/zoo.txt", true);
        assertMatch("**/?o*.txt", "bar/zooms.txt", true);
        assertMatch("**/?o*.txt", "bar/sod.txt", true);
    }

    @Test
    void testAbsoluteRelativeFileNameMatch() {
        assertMatch("**/*", "/root/foo.txt", true);
        assertMatch("**/*", "/root/bar/foo.txt", true);
        assertMatch("*.txt", "/root/foo.txt", true);
        assertMatch("*.txt", "/root/bar/foo.txt", false);
        assertMatch("**/*.txt", "/root/foo.txt", true);
        assertMatch("**/*.txt", "/root/bar/foo.txt", true);
        assertMatch("**/f*.txt", "/root/bar/foo.txt", true);
        assertMatch("**/foo.txt", "/root/bar/foo.txt", true);
    }

    //@Test
    void smokeTest() {

        // See https://confluence.atlassian.com/fisheye/pattern-matching-guide-960155410.html

        assertMatch("*.txt", "foo.txt", true);
        assertMatch("*.txt", "bar/foo.txt", true);
        assertMatch("*.txt", "foo.txty", false);
        assertMatch("*.txt", "bar/foo.txty", false);

        assertMatch("/*.txt", "/foo.txt", true);
        assertMatch("/*.txt", "/bar/foo.txt", false);

        assertMatch("dir1/file.txt", "/dir1/file.txt", true);
        assertMatch("dir1/file.txt", "/dir3/dir1/file.txt", true);
        assertMatch("dir1/file.txt", "/dir3/dir2/dir1/file.txt", true);

        assertMatch("**/dir1/file.txt", "/dir1/file.txt", true);
        assertMatch("**/dir1/file.txt", "/dir3/dir1/file.txt", true);
        assertMatch("**/dir1/file.txt", "/dir3/dir2/dir1/file.txt", true);

        assertMatch("/**/dir1/file.txt", "/dir1/file.txt", true);
        assertMatch("/**/dir1/file.txt", "/dir3/dir1/file.txt", true);
        assertMatch("/**/dir1/file.txt", "/dir3/dir2/dir1/file.txt", true);

        assertMatch("/dir3/**/dir1/file.txt", "/dir3/dir1/file.txt", true);
        assertMatch("/dir3/**/dir1/file.txt", "/dir3/dir2/dir1/file.txt", true);
        assertMatch("/dir3/**/dir1/file.txt", "/dir3/file.txt", false);
        assertMatch("/dir3/**/dir1/file.txt", "/dir1/file.txt", false);

        assertMatch("/dir1/**", "/dir1/file.txt", true);
        assertMatch("/dir1/**", "/dir1/dir2/file.txt", true);
        assertMatch("/dir1/**", "/dir2/dir1/file.txt", false);

        assertMatch("/dir1*", "/dir11", true);
        assertMatch("/dir1*", "/dir12", true);
        assertMatch("/dir1*", "/dir1234", true);
        assertMatch("/dir1*", "/dir1contents.txt", true);
        assertMatch("/dir1*", "/dir2", false);

        assertMatch("/dir??", "/dir11", true);
        assertMatch("/dir??", "/dir22", true);
        assertMatch("/dir??", "/dirX9", true);
        assertMatch("/dir??", "/dirX98", false);
    }
}
