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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
                Map.of("colors", ""),
                Map.of("colors", "red"),
                Map.of("colors", "orange"),
                Map.of("colors", "orange,red"));
    }

    @Test
    void testList2() {
        List<Map<String, String>> permutations = permutations("permutations/list2.xml");
        assertPermutations(permutations,
                Map.of("colors", ""),
                Map.of("colors", "red", "red", "burgundy"),
                Map.of("colors", "red", "red", "auburn"),
                Map.of("colors", "orange", "orange", "salmon"),
                Map.of("colors", "orange", "orange", "peach"),
                Map.of("colors", "orange,red", "red", "burgundy", "orange", "salmon"),
                Map.of("colors", "orange,red", "red", "auburn", "orange", "salmon"),
                Map.of("colors", "orange,red", "red", "burgundy", "orange", "peach"),
                Map.of("colors", "orange,red", "red", "auburn", "orange", "peach"));
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
                Map.of("colors", "true", "colors.tones", ""),
                Map.of("colors", "true", "colors.tones", "dark"),
                Map.of("colors", "true", "colors.tones", "light"),
                Map.of("colors", "true", "colors.tones", "light,dark"),
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
        assertPermutations(permutations,
                Map.of("name", "xxx"));
    }

    @Test
    void testSubstitutions() {
        List<Map<String, String>> permutations = permutations("permutations/substitutions.xml");
        assertPermutations(permutations,
                Map.of("list-things", "", "text", "a-foo-a-bar"),
                Map.of("list-things", "a-bar", "text", "a-foo-a-bar"));
    }

    @Test
    void testConditionals() {
        List<Map<String, String>> permutations = permutations("permutations/conditionals.xml");
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(Map.of("heat", ""));
        expected.add(Map.of("heat", "warm", "warm", ""));
        expected.add(Map.of("heat", "warm", "warm", "red", "red", "burgundy"));
        expected.add(Map.of("heat", "warm", "warm", "red", "red", "auburn"));
        expected.add(Map.of("heat", "cold", "cold", ""));
        expected.add(Map.of("heat", "cold", "cold", "green"));
        expected.add(Map.of("heat", "cold", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "cold", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "cold", "cold", "green,blue", "blue", "indigo"));
        expected.add(Map.of("heat", "cold", "cold", "green,blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "light", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "red", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "salmon", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange", "orange", "peach", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "light", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "light", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "burgundy", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "salmon", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", ""));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green", "green", "tea"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green", "green", "lime"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "blue", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "tea", "blue", "ultramarine"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "azure"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "indigo"));
        expected.add(Map.of("heat", "warm,cold", "warm", "orange,red", "orange", "peach", "red", "auburn", "cold", "green,blue", "green", "lime", "blue", "ultramarine"));

        assertPermutations(permutations, expected);
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
