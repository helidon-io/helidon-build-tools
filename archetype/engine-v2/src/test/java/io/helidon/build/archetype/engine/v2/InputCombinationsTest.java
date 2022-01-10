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
package io.helidon.build.archetype.engine.v2;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link InputCombinations}.
 */
class InputCombinationsTest {

    @Test
    void testList() {
        Iterator<Map<String, String>> iter = InputCombinations.builder()
                                                              .archetypePath(sourceDir("input-tree"))
                                                              .entryPointFile("list2.xml")
                                                              .verbose(true)
                                                              .build()
                                                              .iterator();
        assertThat(iter, is(not(nullValue())));
        Map<String, String> values;

        assertThat(iter.hasNext(), is(true));
        values = iter.next();
        assertThat(values.isEmpty(), is(true));

        assertThat(iter.hasNext(), is(true));
        values = iter.next();
        assertThat(values.size(), is(1));
        assertThat(values.get("colors"), is("red"));

        assertThat(iter.hasNext(), is(true));
        values = iter.next();
        assertThat(values.size(), is(1));
        assertThat(values.get("colors"), is("orange"));

        assertThat(iter.hasNext(), is(true));
        values = iter.next();
        assertThat(values.size(), is(1));
        assertThat(values.get("colors"), is("red,orange"));

        assertThat(iter.hasNext(), is(false));
        Exception e = assertThrows(NoSuchElementException.class, iter::next);
        assertThat(e.getMessage().contains("list2.xml"), is(true));
    }

    // used only for local testing
    @Test
    @Disabled
    void testCollectV2() {
        Path sourceDir = Path.of("/Users/batsatt/dev/helidon/archetypes-v2");
        InputCombinations combinations = InputCombinations.builder()
                                                          .archetypePath(sourceDir)
                                                          .verbose(false)
                                                          .build();
        Map<String, String> firstExpected = Map.of(
                "flavor", "se",
                "base", "bare",
                "build-system", "maven",
                "name", "myproject",
                "groupId", "com.examples",
                "artifactId", "myproject",
                "version", "1.0-SNAPSHOT",
                "package", "com.example.myproject"
        );
        List<Map<String, String>> expected = List.of(
                firstExpected,
                nextExpected(firstExpected, Map.of("base", "quickstart")),
                nextExpected(firstExpected, Map.of("base", "database")),
                nextExpected(firstExpected, Map.of("flavor", "mp", "base", "bare")),
                nextExpected(firstExpected, Map.of("flavor", "mp", "base", "quickstart")),
                nextExpected(firstExpected, Map.of("flavor", "mp", "base", "database"))
        );

        assertExpected(combinations, expected);
    }

    @Test
    @Disabled // TODO enable when passing
    void testCollectE2e() {
        InputCombinations combinations = InputCombinations.builder()
                                                          .archetypePath(sourceDir("e2e"))
                                                          .verbose(false)
                                                          .build();
        Map<String, String> colors = Map.of(
                "theme", "colors",
                "theme.base", "custom",
                "theme.base.style", "modern",
                "theme.base.palette-name", "My Palette",
                "theme.base.colors", "red,green,blue",
                "name", "my-project"
        );

        Map<String, String> shapes = Map.of(
                "theme", "shapes",
                "theme.base", "custom",
                "theme.base.style", "modern",
                "theme.base.library-name", "My Shapes",
                "theme.base.shapes", "circle,triangle",
                "name", "my-project"
        );
        Map<String, String> shapes2d = Map.of(
                "theme", "shapes",
                "theme.base", "2d",
                "theme.base.style", "modern",
                "theme.base.library-name", "2D Shapes",
                "theme.base.shapes", "circle,triangle,rectangle",
                "name", "my-project"
        );
        List<Map<String, String>> expected = List.of(
                colors,
                nextExpected(colors, Map.of(), "theme.base.colors"),
                nextExpected(colors, Map.of("theme.base.colors", "red")),
                nextExpected(colors, Map.of("theme.base.colors", "orange")),
                nextExpected(colors, Map.of("theme.base.colors", "yellow")),
                nextExpected(colors, Map.of("theme.base.colors", "green")),
                nextExpected(colors, Map.of("theme.base.colors", "blue")),
                nextExpected(colors, Map.of("theme.base.colors", "indigo")),
                nextExpected(colors, Map.of("theme.base.colors", "violet")),
                nextExpected(colors, Map.of("theme.base.colors", "pink")),
                nextExpected(colors, Map.of("theme.base.colors", "light-pink")),
                nextExpected(colors, Map.of("theme.base.colors", "cyan")),
                nextExpected(colors, Map.of("theme.base.colors", "light-salmon")),
                nextExpected(colors, Map.of("theme.base.colors", "coral")),
                nextExpected(colors, Map.of("theme.base.colors", "tomato")),
                nextExpected(colors, Map.of("theme.base.colors", "lemon")),
                nextExpected(colors, Map.of("theme.base.colors", "khaki")),
                nextExpected(colors, Map.of("theme.base.colors", "red,orange,yellow,green,blue,indigo,violet,"
                                                                    + "pink,light-pink,cyan,light-salmon,coral,tomato,"
                                                                    + "lemon,khaki")),

                nextExpected(colors, Map.of("theme.base.style", "classic")),
                nextExpected(colors, Map.of("theme.base", "rainbow",
                                               "theme.base.colors", "red,orange,yellow,green,blue,indigo,violet",
                                               "theme.base.palette-name", "Rainbow")),
                nextExpected(colors, Map.of("theme.base", "rainbow",
                                               "theme.base.colors", "red,orange,yellow,green,blue,indigo,violet",
                                               "theme.base.palette-name", "Rainbow",
                                               "theme.base.style", "classic")),
                shapes,
                nextExpected(shapes, Map.of(), "theme.base.shapes"),
                nextExpected(shapes, Map.of("theme.base.shapes", "circle")),
                nextExpected(shapes, Map.of("theme.base.shapes", "triangle")),
                nextExpected(shapes, Map.of("theme.base.shapes", "rectangle")),
                nextExpected(shapes, Map.of("theme.base.shapes", "arrow")),
                nextExpected(shapes, Map.of("theme.base.shapes", "donut")),
                nextExpected(shapes, Map.of("theme.base.shapes", "circle,triangle,rectangle,arrow,donut")),
                nextExpected(shapes, Map.of("theme.base.style", "classic")),

                shapes2d,
                nextExpected(shapes2d, Map.of("theme.base.style", "classic"))
        );

        assertExpected(combinations, expected);
    }

    static void assertExpected(InputCombinations combinations, List<Map<String, String>> expected) {
        int iteration = 0;
        for (Map<String, String> combination : combinations) {
            System.out.printf("Iteration %d -----------------------------%n%n", iteration);
            combination.forEach((k, v) -> System.out.println(k + " = " + v));
            System.out.println();
            assertThat("iteration " + iteration, combination, is(expected.get(iteration)));
            iteration++;
        }
    }

    static Map<String, String> nextExpected(Map<String, String> base, Map<String, String> updates, String... removals) {
        Map<String, String> result = new HashMap<>(base);
        result.putAll(updates);
        Arrays.stream(removals).forEach(result::remove);
        return result;
    }

    private static Path sourceDir(String testDirName) {
        Path targetDir = targetDir(InputCombinationsTest.class);
        return targetDir.resolve("test-classes/" + testDirName);
    }
}
