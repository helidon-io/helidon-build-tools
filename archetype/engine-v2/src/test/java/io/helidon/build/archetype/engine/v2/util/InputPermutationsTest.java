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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Unit test for class {@link InputPermutations}.
 */
@SuppressWarnings("unchecked")
class InputPermutationsTest {

    @Test
    void testList1() {
        List<Map<String, String>> permutations = permutations("permutations/list1.xml");
        assertThat(permutations, contains(
                Map.of("colors", ""),
                Map.of("colors", "red"),
                Map.of("colors", "orange"),
                Map.of("colors", "red orange")));
    }

    @Test
    void testList2() {
        assertThat(permutations("permutations/list2.xml"), contains(
                Map.of("colors", ""),
                Map.of("colors", "red", "colors.red-tone", "burgundy"),
                Map.of("colors", "red", "colors.red-tone", "auburn"),
                Map.of("colors", "orange", "colors.orange-tone", "salmon"),
                Map.of("colors", "orange", "colors.orange-tone", "peach"),
                Map.of("colors", "red orange", "colors.red-tone", "burgundy", "colors.orange-tone", "salmon"),
                Map.of("colors", "red orange", "colors.red-tone", "auburn", "colors.orange-tone", "salmon"),
                Map.of("colors", "red orange", "colors.red-tone", "burgundy", "colors.orange-tone", "peach"),
                Map.of("colors", "red orange", "colors.red-tone", "auburn", "colors.orange-tone", "peach")));
    }

    @Test
    void testEnum1() {
        assertThat(permutations("permutations/enum1.xml"), contains(
                Map.of("colors", "red"),
                Map.of("colors", "orange")));
    }

    @Test
    void testEnum2() {
        assertThat(permutations("permutations/enum2.xml"), contains(
                Map.of("colors", "red", "colors.red-tone", "burgundy"),
                Map.of("colors", "red", "colors.red-tone", "auburn"),
                Map.of("colors", "orange", "colors.orange-tone", "salmon"),
                Map.of("colors", "orange", "colors.orange-tone", "peach")));
    }

    @Test
    void testBoolean1() {
        List<Map<String, String>> permutations = permutations("permutations/boolean1.xml");
        assertThat(permutations, contains(
                Map.of("colors", "true"),
                Map.of("colors", "false")));
    }

    @Test
    void testBoolean2() {
        assertThat(permutations("permutations/boolean2.xml"), contains(
                Map.of("colors", "true", "colors.tones", ""),
                Map.of("colors", "true", "colors.tones", "dark"),
                Map.of("colors", "true", "colors.tones", "light"),
                Map.of("colors", "true", "colors.tones", "dark light"),
                Map.of("colors", "false")));
    }

    @Test
    void testE2e() {
        List<Map<String, String>> permutations = permutations("e2e/main.xml");
        permutations.forEach(System.out::println);
//        Map<String, String> colors = Map.of(
//                "theme", "colors",
//                "theme.base", "custom",
//                "theme.base.style", "modern",
//                "theme.base.palette-name", "My Palette",
//                "theme.base.colors", "red,green,blue",
//                "artifactId", "my-project"
//        );
//
//        Map<String, String> colorsClassic = Map.of(
//                "theme", "colors",
//                "theme.base", "custom",
//                "theme.base.style", "classic",
//                "theme.base.palette-name", "My Palette",
//                "theme.base.colors", "red,green,blue",
//                "artifactId", "my-project"
//        );
//
//        Map<String, String> colorsRainbow = Map.of(
//                "theme", "colors",
//                "theme.base", "rainbow",
//                "theme.base.style", "modern",
//                "theme.base.palette-name", "Rainbow",
//                "theme.base.colors", "red,orange,yellow,green,blue,indigo,violet",
//                "artifactId", "my-project"
//        );
//
//        Map<String, String> shapes = Map.of(
//                "theme", "shapes",
//                "theme.base", "custom",
//                "theme.base.style", "modern",
//                "theme.base.library-name", "My Shapes",
//                "theme.base.shapes", "circle,triangle",
//                "artifactId", "my-project"
//        );
//
//
//        Map<String, String> shapesClassic = Map.of(
//                "theme", "shapes",
//                "theme.base", "custom",
//                "theme.base.style", "classic",
//                "theme.base.library-name", "My Shapes",
//                "theme.base.shapes", "circle,triangle",
//                "artifactId", "my-project"
//        );
//
//        Map<String, String> shapes2d = Map.of(
//                "theme", "shapes",
//                "theme.base", "2d",
//                "theme.base.style", "modern",
//                "theme.base.library-name", "2D Shapes",
//                "theme.base.shapes", "circle,triangle,rectangle",
//                "artifactId", "my-project"
//        );
//        List<Map<String, String>> expected = List.of(
//                colors,
//                nextExpected(colors, Map.of(), "theme.base.colors"),
//                nextExpected(colors, Map.of("theme.base.colors", "red")),
//                nextExpected(colors, Map.of("theme.base.colors", "orange")),
//                nextExpected(colors, Map.of("theme.base.colors", "yellow")),
//                nextExpected(colors, Map.of("theme.base.colors", "green")),
//                nextExpected(colors, Map.of("theme.base.colors", "blue")),
//                nextExpected(colors, Map.of("theme.base.colors", "indigo")),
//                nextExpected(colors, Map.of("theme.base.colors", "violet")),
//                nextExpected(colors, Map.of("theme.base.colors", "pink")),
//                nextExpected(colors, Map.of("theme.base.colors", "light-pink")),
//                nextExpected(colors, Map.of("theme.base.colors", "cyan")),
//                nextExpected(colors, Map.of("theme.base.colors", "light-salmon")),
//                nextExpected(colors, Map.of("theme.base.colors", "coral")),
//                nextExpected(colors, Map.of("theme.base.colors", "tomato")),
//                nextExpected(colors, Map.of("theme.base.colors", "lemon")),
//                nextExpected(colors, Map.of("theme.base.colors", "khaki")),
//                nextExpected(colors, Map.of("theme.base.colors", "red,orange,yellow,green,blue,indigo,violet,"
//                        + "pink,light-pink,cyan,light-salmon,coral,tomato,"
//                        + "lemon,khaki")),
//
//                colorsClassic,
//                nextExpected(colorsClassic, Map.of(), "theme.base.colors"),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "red")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "orange")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "yellow")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "green")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "blue")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "indigo")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "violet")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "pink")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "light-pink")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "cyan")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "light-salmon")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "coral")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "tomato")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "lemon")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "khaki")),
//                nextExpected(colorsClassic, Map.of("theme.base.colors", "red,orange,yellow,green,blue,indigo,violet,"
//                        + "pink,light-pink,cyan,light-salmon,coral,tomato,"
//                        + "lemon,khaki")),
//
//
//                colorsRainbow,
//                nextExpected(colorsRainbow, Map.of("theme.base.style", "classic")),
//
//                shapes,
//                nextExpected(shapes, Map.of(), "theme.base.shapes"),
//                nextExpected(shapes, Map.of("theme.base.shapes", "circle")),
//                nextExpected(shapes, Map.of("theme.base.shapes", "triangle")),
//                nextExpected(shapes, Map.of("theme.base.shapes", "rectangle")),
//                nextExpected(shapes, Map.of("theme.base.shapes", "arrow")),
//                nextExpected(shapes, Map.of("theme.base.shapes", "donut")),
//                nextExpected(shapes, Map.of("theme.base.shapes", "circle,triangle,rectangle,arrow,donut")),
//
//                shapesClassic,
//                nextExpected(shapesClassic, Map.of(), "theme.base.shapes"),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "circle")),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "triangle")),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "rectangle")),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "arrow")),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "donut")),
//                nextExpected(shapesClassic, Map.of("theme.base.shapes", "circle,triangle,rectangle,arrow,donut")),
//
//                shapes2d,
//                nextExpected(shapes2d, Map.of("theme.base.style", "classic"))
//        );
//
//        assertThat(permutations, contains(expected));
    }

    static Map<String, String> nextExpected(Map<String, String> base, Map<String, String> updates, String... removals) {
        Map<String, String> result = new HashMap<>(base);
        result.putAll(updates);
        Arrays.stream(removals).forEach(result::remove);
        return result;
    }

    private static List<Map<String, String>> permutations(String path) {
        return InputPermutations.compute(load(path));
    }
}
