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
package io.helidon.build.common;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link Lists}.
 */
class ListsTest {

    @Test
    void testCompare() {
        assertThat(Lists.<String>compare(List.of(), List.of()), is(0));
        assertThat(Lists.compare(List.of("foo"), List.of()), is(1));
        assertThat(Lists.compare(List.of(), List.of("foo")), is(-1));
        assertThat(Lists.compare(List.of("foo"), List.of("foo")), is(0));
        assertThat(Lists.compare(List.of("foo1"), List.of("foo2")), is(-1));
        assertThat(Lists.compare(List.of("foo2"), List.of("foo1")), is(1));
    }

    @Test
    void testAddAllIndex() {
        assertThat(Lists.<String>addAll(List.of("0", "1", "2", "3"), 0), is(List.of("0", "1", "2", "3")));
        assertThat(Lists.<String>addAll(List.of(), 0, "0", "1", "2", "3"), is(List.of("0", "1", "2", "3")));
        assertThat(Lists.<String>addAll(List.of("2", "3"), 0, "0", "1"), is(List.of("0", "1", "2", "3")));
        assertThat(Lists.<String>addAll(List.of("0", "3"), 1, "1", "2"), is(List.of("0", "1", "2", "3")));
    }
}
