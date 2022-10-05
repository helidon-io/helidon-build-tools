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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.Lists;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link InputPermutations}.
 */
@SuppressWarnings("unchecked")
class InputPermutationsTest {

    @Test
    void testList1() {
        List<Map<String, String>> permutations = permutations("permutations/list1.xml");
        assertPermutations(permutations,
                Map.of("colors", "none"),
                Map.of("colors", "red"),
                Map.of("colors", "orange"),
                Map.of("colors", "red,orange"));
    }

    @Test
    void testList2() {
        List<Map<String, String>> permutations = permutations("permutations/list2.xml");
        assertPermutations(permutations,
                Map.of("colors", "none"),
                Map.of("colors", "red", "red", "burgundy"),
                Map.of("colors", "red", "red", "auburn"),
                Map.of("colors", "orange", "orange", "salmon"),
                Map.of("colors", "orange", "orange", "peach"),
                Map.of("colors", "red,orange", "red", "burgundy", "orange", "salmon"),
                Map.of("colors", "red,orange", "red", "auburn", "orange", "salmon"),
                Map.of("colors", "red,orange", "red", "burgundy", "orange", "peach"),
                Map.of("colors", "red,orange", "red", "auburn", "orange", "peach"));
    }

    @Test
    void testEnum1() {
        List<Map<String, String>> permutations = permutations("permutations/enum1.xml");
        assertPermutations(permutations,
                Map.of("colors", "red"),
                Map.of("colors", "orange"));
    }

    @Test
    void testEnum2() {
        List<Map<String, String>> permutations = permutations("permutations/enum2.xml");
        assertPermutations(permutations,
                Map.of("colors", "green"),
                Map.of("colors", "red", "colors.red", "burgundy"),
                Map.of("colors", "red", "colors.red", "auburn"),
                Map.of("colors", "orange", "colors.orange", "salmon"),
                Map.of("colors", "orange", "colors.orange", "peach"));
    }

    @Test
    void testBoolean1() {
        List<Map<String, String>> permutations = permutations("permutations/boolean1.xml");
        assertPermutations(permutations,
                Map.of("colors", "true"),
                Map.of("colors", "false"));
    }

    @Test
    void testBoolean2() {
        List<Map<String, String>> permutations = permutations("permutations/boolean2.xml");
        assertPermutations(permutations,
                Map.of("colors", "true", "colors.tones", "none"),
                Map.of("colors", "true", "colors.tones", "dark"),
                Map.of("colors", "true", "colors.tones", "light"),
                Map.of("colors", "true", "colors.tones", "dark,light"),
                Map.of("colors", "false"));
    }

    @Test
    void testText1() {
        List<Map<String, String>> permutations = permutations("permutations/text1.xml");
        assertPermutations(permutations,
                Map.of("name", "Foo"));
    }

    @Test
    void testText2() {
        List<Map<String, String>> permutations = permutations("permutations/text2.xml");
        assertThat(permutations.size(), is(1));
        assertThat(permutations.get(0).size(), is(1));
        assertThat(permutations.get(0).get("name"), startsWith("name-"));
    }

    @Test
    void testSubstitutions() {
        List<Map<String, String>> permutations = permutations("permutations/substitutions.xml");
        assertPermutations(permutations,
                Map.of("list-things", "none", "text", "a-foo-a-bar"),
                Map.of("list-things", "a-bar", "text", "a-foo-a-bar"));
    }

    @Test
    void testConditionals() {
        List<Map<String, String>> permutations = permutations("permutations/conditionals.xml");
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(Map.of("heat", "none"));
        expected.add(Map.of("heat", "warm", "warm", "none"));
        expected.add(Map.of("heat", "warm", "warm", "red", "red", "burgundy"));
        expected.add(Map.of("heat", "warm", "warm", "red", "red", "auburn"));
        expected.add(Map.of("heat", "cold", "cold", "none"));
        expected.add(Map.of("heat", "cold", "cold", "green"));
        expected.add(Map.of("heat", "cold", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "cold", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "cold", "cold", "blue,green", "blue", "indigo"));
        expected.add(Map.of("heat", "cold", "cold", "blue,green", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "none", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "light", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "light", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "burgundy", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "salmon", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "none"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red,orange", "orange", "peach", "red", "auburn", "cold", "blue,green", "green", "lime", "blue", "ultramarine"));

        assertPermutations(permutations, expected);
    }

    @Test
    void testE2e() {
        List<Map<String, String>> permutations = permutations("e2e/main.xml");
        assertThat(permutations.size(), is(65604));
    }

    @Test
    void testFilters() {
        List<Map<String, String>> permutations =
                InputPermutations.builder()
                                 .script(load("e2e/main.xml"))
                                 .inputFilters(List.of(
                                         "!(${theme.base.colors} contains 'orange')",
                                         "!(${theme.base.colors} contains 'yellow')",
                                         "!(${theme.base.colors} contains 'indigo')",
                                         "!(${theme.base.colors} contains 'violet')",
                                         "!(${theme.base.colors} contains 'pink')",
                                         "!(${theme.base.colors} contains 'light-pink')",
                                         "!(${theme.base.colors} contains 'cyan')",
                                         "!(${theme.base.colors} contains 'light-salmon')",
                                         "!(${theme.base.colors} contains 'coral')",
                                         "!(${theme.base.colors} contains 'tomato')",
                                         "!(${theme.base.colors} contains 'lemon')",
                                         "!(${theme.base.colors} contains 'khaki')"))
                                 .permutationFilters(List.of(
                                         "${theme.base.style} != 'modern'",
                                         "!(${theme.base.shapes} contains 'arrow')"
                                 ))
                                 .build()
                                 .compute();

        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "none", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "red", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "green", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "red,green", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "blue", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "red,blue", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "green,blue", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "custom", "theme.base.colors", "red,green,blue", "theme.base.palette-name", "My Palette", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "colors", "theme.base", "rainbow", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "none", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "triangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,triangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "rectangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,rectangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "triangle,rectangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,triangle,rectangle", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "triangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,triangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "rectangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,rectangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "triangle,rectangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "custom", "theme.base.library-name", "My Shapes", "theme.base.shapes", "circle,triangle,rectangle,donut", "theme.base.style", "classic"));
        expected.add(Map.of("artifactId", "my-project", "theme", "shapes", "theme.base", "2d", "theme.base.style", "classic"));

        assertPermutations(permutations, expected);
    }

    @Test
    void testExternals() {
        List<Map<String, String>> permutations = InputPermutations.builder()
                                                                  .script(load("permutations/text2.xml"))
                                                                  .externalValues(Map.of("name", "foo"))
                                                                  .build()
                                                                  .compute();
        assertThat(permutations.size(), is(1));
        assertThat(permutations.get(0).size(), is(1));
        assertThat(permutations.get(0).get("name"), is("foo"));
    }

    @Test
    void testExternals2() {
        List<Map<String, String>> permutations = InputPermutations.builder()
                                                                  .script(load("permutations/enum2.xml"))
                                                                  .externalValues(Map.of("colors.orange", "peach"))
                                                                  .build()
                                                                  .compute();
        assertPermutations(permutations,
                Map.of("colors", "green"),
                Map.of("colors", "orange", "colors.orange", "peach"),
                Map.of("colors", "red", "colors.red", "burgundy"),
                Map.of("colors", "red", "colors.red", "auburn"));
    }

    private static List<Map<String, String>> permutations(String path) {
        return InputPermutations.builder()
                                .script(load(path))
                                .build()
                                .compute();
    }

    private static void assertPermutations(List<Map<String, String>> actual, Map<String, String>... expected) {
        assertPermutations(actual, Arrays.asList(expected));
    }

    private static void assertPermutations(List<Map<String, String>> actual, List<Map<String, String>> expected) {
        if (actual.size() == expected.size()) {
            assertThat(actual, containsInAnyOrder(expected.toArray(new Map[0])));
        } else {
            fail(diff(actual, expected));
        }
    }

    private static String diff(List<Map<String, String>> actual, List<Map<String, String>> expected) {
        List<Map<String, String>> removed = new ArrayList<>();
        for (Map<String, String> permutation : expected) {
            if (!actual.contains(permutation)) {
                removed.add(permutation);
            }
        }
        List<Map<String, String>> added = new ArrayList<>();
        for (Map<String, String> permutation : actual) {
            if (!expected.contains(permutation)) {
                added.add(permutation);
            }
        }
        List<Map<String, String>> duplicates = new ArrayList<>();
        for (Map<String, String> permutation : actual) {
            List<Map<String, String>> list = Lists.filter(actual, p -> p.equals(permutation));
            if (list.size() > 1) {
                duplicates.add(permutation);
            }
        }
        return String.format("\n\tRemoved: %s\n\tAdded: %s\n\tDuplicates: %s\n",
                removed.isEmpty() ? "-" : "\n" + Lists.join(removed, p -> "\t\t" + p, "\n"),
                added.isEmpty() ? "-" : "\n" + Lists.join(added, p -> "\t\t" + p, "\n"),
                duplicates.isEmpty() ? "-" : "\n" + Lists.join(duplicates, p -> "\t\t" + p, "\n"));
    }
}
