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
package io.helidon.build.archetype.engine.v2.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Unit test for class {@link InputPermutations}.
 */
@SuppressWarnings("unchecked")
class InputPermutationsTest {

    //@Disabled
    @Test
    void testList1() {
        List<Map<String, String>> permutations = permutations("permutations/list1.xml");
        assertThat(permutations, contains(
                Map.of("colors", ""),
                Map.of("colors", "red"),
                Map.of("colors", "orange"),
                Map.of("colors", "red orange")));
    }

    //@Disabled
    @Test
    void testList2() {
        List<Map<String, String>> permutations = permutations("permutations/list2.xml");
        assertThat(permutations, contains(
                Map.of("colors", ""),
                Map.of("colors", "red", "red", "burgundy"),
                Map.of("colors", "red", "red", "auburn"),
                Map.of("colors", "orange", "orange", "salmon"),
                Map.of("colors", "orange", "orange", "peach"),
                Map.of("colors", "red orange", "red", "burgundy", "orange", "salmon"),
                Map.of("colors", "red orange", "red", "auburn", "orange", "salmon"),
                Map.of("colors", "red orange", "red", "burgundy", "orange", "peach"),
                Map.of("colors", "red orange", "red", "auburn", "orange", "peach")));
    }

    @Disabled
    @Test
    void testEnum1() {
        assertThat(permutations("permutations/enum1.xml"), contains(
                Map.of("colors", "red"),
                Map.of("colors", "orange")));
    }

    @Disabled
    @Test
    void testEnum2() {
        List<Map<String, String>> permutations = permutations("permutations/enum2.xml");
        assertThat(permutations, contains(
                Map.of("colors", "red", "colors.red", "burgundy"),
                Map.of("colors", "red", "colors.red", "auburn"),
                Map.of("colors", "orange", "colors.orange", "salmon"),
                Map.of("colors", "orange", "colors.orange", "peach")));
    }

    @Disabled
    @Test
    void testBoolean1() {
        List<Map<String, String>> permutations = permutations("permutations/boolean1.xml");
        assertThat(permutations, contains(
                Map.of("colors", "true"),
                Map.of("colors", "false")));
    }

    @Disabled
    @Test
    void testBoolean2() {
        List<Map<String, String>> permutations = permutations("permutations/boolean2.xml");
        assertThat(permutations, contains(
                Map.of("colors", "true", "colors.tones", ""),
                Map.of("colors", "true", "colors.tones", "dark"),
                Map.of("colors", "true", "colors.tones", "light"),
                Map.of("colors", "true", "colors.tones", "dark light"),
                Map.of("colors", "false")));
    }

    @Disabled
    @Test
    void testText1() {
        List<Map<String, String>> permutations = permutations("permutations/text1.xml");
        assertThat(permutations, contains(
                Map.of("name", "Foo")));
    }

    @Disabled
    @Test
    void testText2() {
        List<Map<String, String>> permutations = permutations("permutations/text2.xml");
        assertThat(permutations, contains(
                Map.of("name", "xxx")));
    }

    @Disabled
    @Test
    void testSubstitutions() {
        List<Map<String, String>> permutations = permutations("permutations/substitutions.xml");
        assertThat(permutations, contains(
                Map.of("text", "a-foo-a-bar"),
                Map.of("list-things", ""),
                Map.of("list-things", "a-bar")));
    }

    @Disabled
    @Test
    void testConditionals() {
        List<Map<String, String>> permutations = permutations("permutations/conditionals.xml");
        permutations.forEach(System.out::println);
        List<Map<String, String>> list = new ArrayList<>();
        list.add(Map.of("heat", ""));
        list.add(Map.of("heat", "warm", "warm", ""));
        list.add(Map.of("heat", "warm", "warm", "red", "red", "burgundy"));
        list.add(Map.of("heat", "warm", "warm", "red", "red", "auburn"));
        list.add(Map.of("heat", "cold", "cold", ""));
        list.add(Map.of("heat", "cold", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "cold", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "cold", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "cold", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "cold", "cold", "blue green", "blue", "azure"));
        list.add(Map.of("heat", "cold", "cold", "blue green", "blue", "indigo"));
        list.add(Map.of("heat", "cold", "cold", "blue green", "blue", "azure"));
        list.add(Map.of("heat", "cold", "cold", "blue green", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "salmon", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "orange", "orange", "peach", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "burgundy", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "salmon", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", ""));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "green", "green", "tea"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "green", "green", "lime"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "tea", "blue", "ultramarine"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "azure"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "indigo"));
        list.add(Map.of("heat", "warm cold", "warm", "red orange", "orange", "peach", "red", "auburn", "cold", "blue green", "green", "lime", "blue", "ultramarine"));
        assertThat(permutations, containsInAnyOrder(list.toArray(new Map[0])));
    }

    @Disabled
    @Test
    void testE2e() {
        List<Map<String, String>> permutations = permutations("e2e/main.xml");
        permutations.forEach(System.out::println);
        // TODO reduce number of colors and hard-code expected result
    }

    private static List<Map<String, String>> permutations(String path) {
        return InputPermutations.compute(load(path));
    }
}
