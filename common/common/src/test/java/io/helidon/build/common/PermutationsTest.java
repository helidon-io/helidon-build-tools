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

package io.helidon.build.common;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link Permutations}.
 */
@SuppressWarnings("unchecked")
class PermutationsTest {

    @Test
    void testPermutations() {
        assertThat(Permutations.of(List.of("foo", "bar")),
                contains(
                        List.of(),
                        List.of("foo"),
                        List.of("bar"),
                        List.of("foo", "bar")));
    }

    @Test
    void testPermutations2() {
        assertThat(Permutations.of(List.of("foo", "bar", "bob", "alice", "joe")),
                contains(
                        List.of(),
                        List.of("foo"),
                        List.of("bar"),
                        List.of("foo", "bar"),
                        List.of("bob"),
                        List.of("foo", "bob"),
                        List.of("bar", "bob"),
                        List.of("foo", "bar", "bob"),
                        List.of("alice"),
                        List.of("foo", "alice"),
                        List.of("bar", "alice"),
                        List.of("foo", "bar", "alice"),
                        List.of("bob", "alice"),
                        List.of("foo", "bob", "alice"),
                        List.of("bar", "bob", "alice"),
                        List.of("foo", "bar", "bob", "alice"),
                        List.of("joe"),
                        List.of("foo", "joe"),
                        List.of("bar", "joe"),
                        List.of("foo", "bar", "joe"),
                        List.of("bob", "joe"),
                        List.of("foo", "bob", "joe"),
                        List.of("bar", "bob", "joe"),
                        List.of("foo", "bar", "bob", "joe"),
                        List.of("alice", "joe"),
                        List.of("foo", "alice", "joe"),
                        List.of("bar", "alice", "joe"),
                        List.of("foo", "bar", "alice", "joe"),
                        List.of("bob", "alice", "joe"),
                        List.of("foo", "bob", "alice", "joe"),
                        List.of("bar", "bob", "alice", "joe"),
                        List.of("foo", "bar", "bob", "alice", "joe")));
    }

    @Test
    void testOfList() {
        List<List<String>> permutations = Permutations.ofList(List.of(
                List.of("", "foo", "bar", "foo bar"),
                List.of("black", "white", "grey"),
                List.of("green", "red")));

        assertThat(permutations,
                contains(List.of("", "black", "green"),
                        List.of("foo", "black", "green"),
                        List.of("bar", "black", "green"),
                        List.of("foo bar", "black", "green"),
                        List.of("", "white", "green"),
                        List.of("foo", "white", "green"),
                        List.of("bar", "white", "green"),
                        List.of("foo bar", "white", "green"),
                        List.of("", "grey", "green"),
                        List.of("foo", "grey", "green"),
                        List.of("bar", "grey", "green"),
                        List.of("foo bar", "grey", "green"),
                        List.of("", "black", "red"),
                        List.of("foo", "black", "red"),
                        List.of("bar", "black", "red"),
                        List.of("foo bar", "black", "red"),
                        List.of("", "white", "red"),
                        List.of("foo", "white", "red"),
                        List.of("bar", "white", "red"),
                        List.of("foo bar", "white", "red"),
                        List.of("", "grey", "red"),
                        List.of("foo", "grey", "red"),
                        List.of("bar", "grey", "red"),
                        List.of("foo bar", "grey", "red")));
    }

    @Test
    void testListPermutationsEmpty() {
        assertThat(Permutations.ofList(List.of(List.of())), is(empty()));
    }

    @Test
    void testListPermutationsEmptyElement() {
        assertThat(Permutations.ofList(List.of(List.of(), List.of("dark", "light"), List.of("red"))),
                contains(List.of("dark", "red"), List.of("light", "red")));
    }
}
