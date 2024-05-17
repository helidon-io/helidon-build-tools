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
package io.helidon.build.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link Diff}.
 */
class DiffTest {

    @Test
    void testEmpty() {
        assertThat(Diff.diff(List.of(), List.of()), is(List.of()));
    }

    @Test
    void testClear() {
        var diffs = Diff.diff(List.of("A", "B", "C"), List.of());
        assertThat(diffs, is(List.of(new Diff<>(0, -1, "A"), new Diff<>(0, -1, "B"), new Diff<>(0, -1, "C"))));
    }

    @Test
    void testApplyClear() {
        var list = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(0, -1), new Diff<>(0, -1), new Diff<>(0, -1)), list);
        assertThat(list, is(empty()));
    }

    @Test
    void testAddAll() {
        var diffs = Diff.diff(List.of(), List.of("A", "B", "C"));
        assertThat(diffs, is(List.of(new Diff<>(-1, 0, "A"), new Diff<>(-1, 1, "B"), new Diff<>(-1, 2, "C"))));
    }

    @Test
    void testApplyAddAll() {
        var list = new ArrayList<>();
        Diff.apply(List.of(new Diff<>(-1, 0, "A"), new Diff<>(-1, 1, "B"), new Diff<>(-1, 2, "C")), list);
        assertThat(list, is(List.of("A", "B", "C")));
    }

    @Test
    void testReplaceAll() {
        var diffs = Diff.diff(List.of("A", "B", "C"), List.of("D", "E", "F"));
        assertThat(diffs, is(List.of(
                new Diff<>(0, -1, "A"),
                new Diff<>(-1, 0, "D"),
                new Diff<>(1, -1, "B"),
                new Diff<>(-1, 1, "E"),
                new Diff<>(2, -1, "C"),
                new Diff<>(-1, 2, "F"))));
    }

    @Test
    void testApplyReplaceAll() {
        var list = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(
                new Diff<>(0, -1),
                new Diff<>(-1, 0, "D"),
                new Diff<>(1, -1),
                new Diff<>(-1, 1, "E"),
                new Diff<>(2, -1),
                new Diff<>(-1, 2, "F")), list);

        assertThat(list, is(List.of("D", "E", "F")));
    }

    @Test
    void testReplaceOne() {
        var diff1 = Diff.diff(List.of("A", "B", "C"), List.of("D", "B", "C"));
        assertThat(diff1, is(List.of(new Diff<>(0, -1, "A"), new Diff<>(-1, 0, "D"))));

        var diff2 = Diff.diff(List.of("A", "B", "C"), List.of("A", "D", "C"));
        assertThat(diff2, is(List.of(new Diff<>(1, -1, "B"), new Diff<>(-1, 1, "D"))));

        var diff3 = Diff.diff(List.of("A", "B", "C"), List.of("A", "B", "D"));
        assertThat(diff3, is(List.of(new Diff<>(2, -1, "C"), new Diff<>(-1, 2, "D"))));
    }

    @Test
    void testApplyReplaceOne() {
        var list1 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(0, -1), new Diff<>(-1, 0, "D")), list1);
        assertThat(list1, is(List.of("D", "B", "C")));

        var list2 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(1, -1), new Diff<>(-1, 1, "D")), list2);
        assertThat(list2, is(List.of("A", "D", "C")));

        var list3 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(2, -1), new Diff<>(-1, 2, "D")), list3);
        assertThat(list3, is(List.of("A", "B", "D")));
    }

    @Test
    void testRemoveOne() {
        var diff1 = Diff.diff(List.of("A", "B", "C"), List.of("B", "C"));
        assertThat(diff1, is(List.of(new Diff<>(0, -1, "A"))));

        var diff2 = Diff.diff(List.of("A", "B", "C"), List.of("A", "B"));
        assertThat(diff2, is(List.of(new Diff<>(2, -1, "C"))));

        var diff3 = Diff.diff(List.of("A", "B", "C"), List.of("A", "C"));
        assertThat(diff3, is(List.of(new Diff<>(1, -1, "B"))));

        var diffs4 = Diff.diff(List.of("A", "B", "C", "D"), List.of("B", "C", "D"));
        assertThat(diffs4, is(List.of(new Diff<>(0, -1, "A"))));
    }

    @Test
    void testAddTwo() {
        var diffs = Diff.diff(List.of("A", "B"), List.of("A", "B", "C", "D"));
        assertThat(diffs, is(List.of(new Diff<>(-1, 2, "C"), new Diff<>(-1, 3, "D"))));
    }

    @Test
    void testApplyRemoveOne() {
        var list1 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(0, -1)), list1);
        assertThat(list1, is(List.of("B", "C")));

        var list2 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(2, -1)), list2);
        assertThat(list2, is(List.of("A", "B")));

        var list3 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(new Diff<>(1, -1)), list3);
        assertThat(list3, is(List.of("A", "C")));

        var list4 = new ArrayList<>(List.of("A", "B", "C", "D"));
        Diff.apply(List.of(new Diff<>(0, -1)), list4);
        assertThat(list4, is(List.of("B", "C", "D")));
    }

    @Test
    void testTrim() {
        var diffs = Diff.diff(List.of("A", "B", "C", "D", "B"), List.of("B", "C", "D"));
        assertThat(diffs, is(List.of(new Diff<>(0, -1, "A"), new Diff<>(3, -1, "B"))));
    }

    @Test
    void testApplyTrim() {
        var list = new ArrayList<>(List.of("A", "B", "C", "D", "B"));
        Diff.apply(List.of(new Diff<>(0, -1), new Diff<>(3, -1)), list);
        assertThat(list, is(List.of("B", "C", "D")));
    }

    @Test
    void testDuplicates() {
        var diff1 = Diff.diff(List.of("A", "A", "B", "B", "C", "C"), List.of("A", "B", "C"));
        assertThat(diff1, is(List.of(
                new Diff<>(1, -1, "A"),
                new Diff<>(2, -1, "B"),
                new Diff<>(3, -1, "C"))));

        var diff2 = Diff.diff(List.of("A", "B", "C"), List.of("A", "A", "B", "B", "C", "C"));
        assertThat(diff2, is(List.of(
                new Diff<>(-1, 1, "A"),
                new Diff<>(-1, 3, "B"),
                new Diff<>(-1, 5, "C"))));
    }

    @Test
    void testApplyDupplicates() {
        var list1 = new ArrayList<>(List.of("A", "A", "B", "B", "C", "C"));
        Diff.apply(List.of(
                new Diff<>(1, -1),
                new Diff<>(2, -1),
                new Diff<>(3, -1)), list1);
        assertThat(list1, is(List.of("A", "B", "C")));

        var list2 = new ArrayList<>(List.of("A", "B", "C"));
        Diff.apply(List.of(
                new Diff<>(-1, 1, "A"),
                new Diff<>(-1, 3, "B"),
                new Diff<>(-1, 5, "C")), list2);
        assertThat(list2, is(List.of("A", "A", "B", "B", "C", "C")));
    }
}
