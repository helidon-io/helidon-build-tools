/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link PathFilters}.
 */
class PathFiltersTest {

    private static final String ROOT = "/root";

    void assertMatch(String pattern, String path, boolean expected) {
        assertMatch(pattern, path, ROOT, expected);
    }

    void assertMatch(String pattern, String path, String root, boolean expected) {
        assertThat(PathFilters.matches(pattern).test(Path.of(path), Path.of(root)), is(expected));
    }

    @Test
    void testInvalidPatterns() {
        assertThrows(IllegalArgumentException.class, () -> PathFilters.matches(null));
        assertThrows(IllegalArgumentException.class, () -> PathFilters.matches(""));
    }

    @Test
    void testRelativePathFileNameMatch() {
        assertMatch("*.txt", "foo.txt", true);
        assertMatch("*.txt", "foo.txty", false);
        assertMatch("*.txt", "bar/foo.txt", false);
        assertMatch("**/*", "foo.txt", true);
        assertMatch("**/*", "bar/foo.txt", true);
        assertMatch("**/*.txt", "foo.txt", true);
        assertMatch("**/*.txt", "bar/foo.txt", true);
        assertMatch("**/f*.txt", "bar/foo.txt", true);
        assertMatch("**/foo.txt", "bar/foo.txt", true);
        assertMatch("**/?o*.txt", "bar/foo.txt", true);
        assertMatch("**/?o*.txt", "bar/zoo.txt", true);
        assertMatch("**/?o*.txt", "bar/zooms.txt", true);
        assertMatch("**/?o*.txt", "bar/sod.txt", true);
        assertMatch("**/*o*.txt", "bar/foo.txt", true);
        assertMatch("**/*o*.txt", "bar/zoo.txt", true);
        assertMatch("**/*o*.txt", "bar/zooms.txt", true);
        assertMatch("**/*o*.txt", "bar/sod.txt", true);
        assertMatch("**/*o*.txt", "bar/food.txt", true);
        assertMatch("**/*o*.txt", "bar/far-out.txt", true);
        assertMatch("**/*o*.txt", "bar/brooms.txt", true);
        assertMatch("**/*o*.txt", "bar/slot.txt", true);
        assertMatch("**/bar/**", "foo/bar/baz", true);
        assertMatch("**/bar/**", "foo/bar/baz.txt", true);
        assertMatch("bar/**", "bar/baz", true);
        assertMatch("bar/**", "bar/baz/foo.txt", true);
        assertMatch("bar/**", "foobar/baz/foo.txt", false);
        assertMatch("**/bar/", "foo/bar/baz", true);
        assertMatch("**/bar/", "foo/bar/baz.txt", true);
        assertMatch("bar/", "bar/baz", true);
        assertMatch("bar/", "bar/baz/foo.txt", true);
        assertMatch("bar/", "foobar/baz/foo.txt", false);
    }

    @Test
    void testAbsolutePathFileNameMatch() {
        assertMatch("*.txt", "/root/foo.txt", "/root", true);
        assertMatch("*.txt", "/root/foo.txty", "/root", false);
        assertMatch("*.txt", "/root/bar/foo.txt", "/root", false);
        assertMatch("**/*", "/root/foo.txt", "/root", true);
        assertMatch("**/*", "/root/bar/foo.txt", "/root", true);
        assertMatch("**/*.txt", "/root/foo.txt", "/root", true);
        assertMatch("**/*.txt", "/root/bar/foo.txt", "/root", true);
        assertMatch("**/f*.txt", "/root/bar/foo.txt", "/root", true);
        assertMatch("**/foo.txt", "/root/bar/foo.txt", "/root", true);
        assertMatch("**/?o*.txt", "/root/bar/foo.txt", "/root", true);
        assertMatch("**/?o*.txt", "/root/bar/zoo.txt", "/root", true);
        assertMatch("**/?o*.txt", "/root/bar/zooms.txt", "/root", true);
        assertMatch("**/?o*.txt", "/root/bar/sod.txt", "/root", true);
        assertMatch("**/*o*.txt", "/root/bar/food.txt", "/root", true);
        assertMatch("**/*o*.txt", "/root/bar/far-out.txt", "/root", true);
        assertMatch("**/*o*.txt", "/root/bar/brooms.txt", "/root", true);
        assertMatch("**/*o*.txt", "/root/bar/slot.txt", "/root", true);
        assertMatch("**/bar/**", "/root/foo/bar/baz", "/root", true);
        assertMatch("**/bar/**", "/root/foo/bar/baz.txt", "/root", true);
        assertMatch("bar/**", "/root/bar/baz", "/root", true);
        assertMatch("bar/**", "/root/bar/baz/foo.txt", "/root", true);
        assertMatch("bar/**", "/root/foobar/baz/foo.txt", "/root", false);
        assertMatch("**/bar/", "/root/foo/bar/baz", "/root", true);
        assertMatch("**/bar/", "/root/foo/bar/baz.txt", "/root", true);
        assertMatch("bar/", "/root/bar/baz", "/root", true);
        assertMatch("bar/", "/root/bar/baz/foo.txt", "/root", true);
        assertMatch("bar/", "/root/foobar/baz/foo.txt", "/root", false);

        assertMatch("*.txt", "/base/foo.txt", "/base", true);
        assertMatch("*.txt", "/base/foo.txty", "/base", false);
        assertMatch("*.txt", "/base/bar/foo.txt", "/base", false);
        assertMatch("**/*", "/base/foo.txt", "/base", true);
        assertMatch("**/*", "/base/bar/foo.txt", "/base", true);
        assertMatch("**/*.txt", "/base/foo.txt", "/base", true);
        assertMatch("**/*.txt", "/base/bar/foo.txt", "/base", true);
        assertMatch("**/f*.txt", "/base/bar/foo.txt", "/base", true);
        assertMatch("**/foo.txt", "/base/bar/foo.txt", "/base", true);
        assertMatch("**/?o*.txt", "/base/bar/foo.txt", "/base", true);
        assertMatch("**/?o*.txt", "/base/bar/zoo.txt", "/base", true);
        assertMatch("**/?o*.txt", "/base/bar/zooms.txt", "/base", true);
        assertMatch("**/?o*.txt", "/base/bar/sod.txt", "/base", true);
        assertMatch("**/*o*.txt", "/base/bar/food.txt", "/base", true);
        assertMatch("**/*o*.txt", "/base/bar/far-out.txt", "/base", true);
        assertMatch("**/*o*.txt", "/base/bar/brooms.txt", "/base", true);
        assertMatch("**/*o*.txt", "/base/bar/slot.txt", "/base", true);
        assertMatch("**/bar/**", "/base/foo/bar/baz", "/base", true);
        assertMatch("**/bar/**", "/base/foo/bar/baz.txt", "/base", true);
        assertMatch("bar/**", "/base/bar/baz", "/base", true);
        assertMatch("bar/**", "/base/bar/baz/foo.txt", "/base", true);
        assertMatch("bar/**", "/base/foobar/baz/foo.txt", "/base", false);
        assertMatch("**/bar/", "/base/foo/bar/baz", "/base", true);
        assertMatch("**/bar/", "/base/foo/bar/baz.txt", "/base", true);
        assertMatch("bar/", "/base/bar/baz", "/base", true);
        assertMatch("bar/", "/base/bar/baz/foo.txt", "/base", true);
        assertMatch("bar/", "/base/foobar/baz/foo.txt", "/base", false);

        assertMatch("*.txt", "/base/foo.txt", "/foo", false);
        assertMatch("*.txt", "/base/foo.txty", "/foo", false);
        assertMatch("*.txt", "/base/bar/foo.txt", "/foo", false);
        assertMatch("**/*", "/base/foo.txt", "/foo", true);
        assertMatch("**/*", "/base/bar/foo.txt", "/foo", true);
        assertMatch("**/*.txt", "/base/foo.txt", "/foo", true);
        assertMatch("**/*.txt", "/base/bar/foo.txt", "/foo", true);
        assertMatch("**/f*.txt", "/base/bar/foo.txt", "/foo", true);
        assertMatch("**/foo.txt", "/base/bar/foo.txt", "/foo", true);
        assertMatch("**/?o*.txt", "/base/bar/foo.txt", "/foo", true);
        assertMatch("**/?o*.txt", "/base/bar/zoo.txt", "/foo", true);
        assertMatch("**/?o*.txt", "/base/bar/zooms.txt", "/foo", true);
        assertMatch("**/?o*.txt", "/base/bar/sod.txt", "/foo", true);
        assertMatch("**/*o*.txt", "/base/bar/food.txt", "/foo", true);
        assertMatch("**/*o*.txt", "/base/bar/far-out.txt", "/foo", true);
        assertMatch("**/*o*.txt", "/base/bar/brooms.txt", "/foo", true);
        assertMatch("**/*o*.txt", "/base/bar/slot.txt", "/foo", true);
        assertMatch("**/bar/**", "/base/foo/bar/baz", "/foo", true);
        assertMatch("**/bar/**", "/base/foo/bar/baz.txt", "/foo", true);
        assertMatch("bar/**", "/base/bar/baz", "/foo", false);
        assertMatch("bar/**", "/base/bar/baz/foo.txt", "/foo", false);
        assertMatch("bar/**", "/base/foobar/baz/foo.txt", "/foo", false);
        assertMatch("**/bar/", "/base/foo/bar/baz", "/foo", true);
        assertMatch("**/bar/", "/base/foo/bar/baz.txt", "/foo", true);
        assertMatch("bar/", "/base/bar/baz", "/foo", false);
        assertMatch("bar/", "/base/bar/baz/foo.txt", "/foo", false);
        assertMatch("bar/", "/base/foobar/baz/foo.txt", "/foo", false);
    }

    @Test
    void testMatchesAny() {
        Path root = Path.of("/r");
        BiPredicate<Path, Path> any = PathFilters.matchesAny(List.of("**/a.txt", "**/b.txt", "**/c.txt"));
        assertThat(any.test(Path.of("a.txt"), root), is(true));
        assertThat(any.test(Path.of("b.txt"), root), is(true));
        assertThat(any.test(Path.of("c.txt"), root), is(true));
        assertThat(any.test(Path.of("d.txt"), root), is(false));

        assertThat(any.test(Path.of("/r/a.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/b.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/c.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/d.txt"), root), is(false));

        any = PathFilters.matchesAny(emptyList());

        assertThat(any.test(Path.of("a"), root), is(true));
        assertThat(any.test(Path.of("b.txt"), root), is(true));
        assertThat(any.test(Path.of("c.txt"), root), is(true));
        assertThat(any.test(Path.of("d.txt"), root), is(true));

        assertThat(any.test(Path.of("/r/a.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/b.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/c.txt"), root), is(true));
        assertThat(any.test(Path.of("/r/d.txt"), root), is(true));

    }

    @Test
    void testMatchesNone() {
        Path root = Path.of("/r");
        BiPredicate<Path, Path> none = PathFilters.matchesNone(List.of("**/x.txt", "**/y.txt", "**/z.txt"));
        assertThat(none.test(Path.of("a.txt"), root), is(true));
        assertThat(none.test(Path.of("b.txt"), root), is(true));
        assertThat(none.test(Path.of("c.txt"), root), is(true));
        assertThat(none.test(Path.of("y.txt"), root), is(false));

        assertThat(none.test(Path.of("/r/a.txt"), root), is(true));
        assertThat(none.test(Path.of("/r/b.txt"), root), is(true));
        assertThat(none.test(Path.of("/r/c.txt"), root), is(true));
        assertThat(none.test(Path.of("/r/y.txt"), root), is(false));
    }

    @Test
    void testEmptyMatchesNone() {
        Path root = Path.of("/r");
        BiPredicate<Path, Path> filter = PathFilters.matchesNone(List.of());
        assertThat(filter.test(Path.of("a.txt"), root), is(true));
        assertThat(filter.test(Path.of("b.txt"), root), is(true));
        assertThat(filter.test(Path.of("c.txt"), root), is(true));
        assertThat(filter.test(Path.of("y.txt"), root), is(true));
    }

    @Test
    void testIncludesExcludes() {
        Path root = Path.of("/r");
        BiPredicate<Path, Path> filter = PathFilters.matches(List.of("**/a.txt", "**/b.txt", "**/c.txt"), List.of("**/c.txt"));
        assertThat(filter.test(Path.of("a.txt"), root), is(true));
        assertThat(filter.test(Path.of("b.txt"), root), is(true));
        assertThat(filter.test(Path.of("c.txt"), root), is(false));
        assertThat(filter.test(Path.of("y.txt"), root), is(false));
    }

    @Test
    void testEmptyIncludesNonEmptyExcludes() {
        Path root = Path.of("/r");
        BiPredicate<Path, Path> filter = PathFilters.matches(List.of(), List.of("**/c.txt"));
        assertThat(filter.test(Path.of("a.txt"), root), is(true));
        assertThat(filter.test(Path.of("b.txt"), root), is(true));
        assertThat(filter.test(Path.of("c.txt"), root), is(false));
        assertThat(filter.test(Path.of("y.txt"), root), is(true));
    }
}
