/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.context;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ContextPath}.
 */
class ContextPathTest {

    @Test
    void testParse() {
        String[] segments;

        segments = ContextPath.parse("");
        assertThat(segments.length, is(0));

        segments = ContextPath.parse("..");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining(".."));

        segments = ContextPath.parse("....");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("..", ".."));

        segments = ContextPath.parse("foo");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining("foo"));

        segments = ContextPath.parse("foo-bar");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining("foo-bar"));

        segments = ContextPath.parse("foo.bar");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("foo", "bar"));

        segments = ContextPath.parse("~foo");
        assertThat(segments.length, is(2));
        assertThat(segments, arrayContaining("~", "foo"));

        segments = ContextPath.parse("~foo.bar");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("~", "foo", "bar"));

        segments = ContextPath.parse("~..foo.bar");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("~", "foo", "bar"));

        segments = ContextPath.parse("foo..");
        assertThat(segments.length, is(0));

        segments = ContextPath.parse("..foo..");
        assertThat(segments.length, is(1));
        assertThat(segments, arrayContaining(".."));

        segments = ContextPath.parse("....foo....");
        assertThat(segments.length, is(3));
        assertThat(segments, arrayContaining("..", "..", ".."));
    }

    @Test
    void testInvalidPath() {
        assertThrows(NullPointerException.class, () -> ContextPath.parse(null));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("..."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("foo."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo.."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo..bar"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo.bar"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse(".foo......."));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("-"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("foo-"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("-foo"));
        assertThrows(IllegalArgumentException.class, () -> ContextPath.parse("foo--bar"));
    }
}
