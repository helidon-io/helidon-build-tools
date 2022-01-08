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

        int iteration = 0;
        for (Map<String, String> combination : InputCombinations.builder()
                                                                .archetypePath(sourceDir)
                                                                .verbose(false)
                                                                .build()) {
            System.out.printf("Iteration %d -----------------------------%n%n", iteration);
            combination.forEach((k, v) -> System.out.println(k + " = " + v));
            System.out.println();

            assertThat(combination, is(expected.get(iteration)));
            iteration++;
        }
    }

    static Map<String, String> nextExpected(Map<String, String> base, Map<String, String> updates) {
        Map<String, String> result = new HashMap<>(base);
        result.putAll(updates);
        return result;
    }

    private static Path sourceDir(String testDirName) {
        Path targetDir = targetDir(InputCombinationsTest.class);
        return targetDir.resolve("test-classes/" + testDirName);
    }
}
