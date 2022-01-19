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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.InvocationException;
import io.helidon.build.archetype.engine.v2.util.InputTree.Node;
import io.helidon.build.archetype.engine.v2.util.InputTree.Node.Kind;
import io.helidon.build.archetype.engine.v2.util.InputTree.NodeIndex;
import io.helidon.build.archetype.engine.v2.util.InputTree.PresetNode;
import io.helidon.build.archetype.engine.v2.util.InputTree.ValueNode;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link InputTree}.
 */
class InputTreeTest {

    @Test
    void testPresetSiblingsNotMoved() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .movePresetSiblings(false)
                                  .build();

        assertThat(tree, is(not(nullValue())));
        Node root = tree.root();
        assertThat(root, is(not(nullValue())));
        List<Node> children = root.children();
        assertThat(children.size(), is(4));
        assertThat(children.get(0).kind(), is(Kind.PRESETS));
        assertThat(children.get(1).kind(), is(Kind.ENUM));
        assertThat(children.get(2).kind(), is(Kind.BOOLEAN));
        assertThat(children.get(3).kind(), is(Kind.BOOLEAN));
    }

    @Test
    void testPresetSiblingsMoved() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .build();

        assertThat(tree, is(not(nullValue())));
        Node root = tree.root();
        assertThat(root, is(not(nullValue())));
        List<Node> children = root.children();
        assertThat(children.size(), is(1));
        Node preset = children.get(0);
        children = preset.children();
        assertThat(children.size(), is(3));
        assertThat(children.get(0).kind(), is(Kind.ENUM));
        assertThat(children.get(1).kind(), is(Kind.BOOLEAN));
        assertThat(children.get(2).kind(), is(Kind.BOOLEAN));
    }

    @Test
    void testUnpruned() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .prunePresets(false)
                                  .build();

        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(21));

        Node root = tree.root();
        assertThat(root, is(not(nullValue())));
        assertThat(root.id(), is(0));
        assertThat(root.kind(), is(Kind.ROOT));

        List<Node> presets = collect(tree, Kind.PRESETS);
        assertThat(presets.size(), is(1));
        assertThat(presets.get(0).id(), is(1));

        assertContains(tree, Kind.VALUE, "choice");
        assertContains(tree, Kind.TEXT, "choice.foo");
        assertContains(tree, Kind.VALUE, "choice.foo");

        assertContains(tree, Kind.VALUE, "include");
        assertContains(tree, Kind.BOOLEAN, "include2");
        assertContains(tree, Kind.VALUE, "include2.y");

        assertContainsValue(tree, "choice", "bar");
        assertContainsValue(tree, "choice.bar", "a-bar");
        assertContainsValue(tree, "include", "yes");
        assertContainsValue(tree, "include.yes", "yes");
        assertContainsValue(tree, "include.yes", "no");
        assertContainsValue(tree, "include2", "no");
    }

    @Test
    void testPruned() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .verbose(false)
                                  .build();
        /*

        UNPRUNED

            0 ROOT
            |   1 PRESETS '{choice=bar, include=true, include2=false}' from main.xml:24
            |   |   2 ENUM 'choice' from main.xml:30
            |   |   |   3 VALUE 'choice' = 'foo' from main.xml:31                REMOVED
            |   |   |   |   4 TEXT 'choice.foo' from foo.xml:25                  REMOVED
            |   |   |   |   |   5 VALUE 'choice.foo' = 'a-foo' from foo.xml:25   REMOVED
            |   |   |   6 VALUE 'choice' = 'bar' from main.xml:34
            |   |   |   |   7 TEXT 'choice.bar' from bar.xml:25
            |   |   |   |   |   8 VALUE 'choice.bar' = 'a-bar' from bar.xml:25
            |   |   9 BOOLEAN 'include' from main.xml:38
            |   |   |   10 VALUE 'include' = 'yes' from main.xml:38
            |   |   |   |   11 BOOLEAN 'include.yes' from main.xml:40
            |   |   |   |   |   12 VALUE 'include.yes' = 'yes' from main.xml:40
            |   |   |   |   |   13 VALUE 'include.yes' = 'no' from main.xml:40
            |   |   |   14 VALUE 'include' = 'no' from main.xml:38               REMOVED
            |   |   15 BOOLEAN 'include2' from main.xml:43
            |   |   |   16 VALUE 'include2' = 'yes' from main.xml:43             REMOVED
            |   |   |   |   17 BOOLEAN 'include2.y' from main.xml:45             REMOVED
            |   |   |   |   |   18 VALUE 'include2.y' = 'yes' from main.xml:45   REMOVED
            |   |   |   |   |   19 VALUE 'include2.y' = 'no' from main.xml:45    REMOVED
            |   |   |   20 VALUE 'include2' = 'no' from main.xml:43

        UPDATED

            0 ROOT
            |   1 PRESETS '{choice=bar, include=true, include2=false}' from main.xml:24
            |   |   2 ENUM 'choice' from main.xml:30
            |   |   |   3 VALUE 'choice' = 'bar' from main.xml:34
            |   |   |   |   4 TEXT 'choice.bar' from bar.xml:25
            |   |   |   |   |   5 VALUE 'choice.bar' = 'a-bar' from bar.xml:25
            |   |   6 BOOLEAN 'include' from main.xml:38
            |   |   |   7 VALUE 'include' = 'yes' from main.xml:38
            |   |   |   |   8 BOOLEAN 'include.yes' from main.xml:40
            |   |   |   |   |   9 VALUE 'include.yes' = 'yes' from main.xml:40
            |   |   |   |   |   10 VALUE 'include.yes' = 'no' from main.xml:40
            |   |   11 BOOLEAN 'include2' from main.xml:43
            |   |   |   12 VALUE 'include2' = 'no' from main.xml:43
         */

        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(13));

        Node root = tree.root();
        assertThat(root, is(not(nullValue())));
        assertThat(root.id(), is(0));
        assertThat(root.kind(), is(Kind.ROOT));

        List<Node> presets = collect(tree, Kind.PRESETS);
        assertThat(presets.size(), is(1));
        assertThat(presets.get(0).id(), is(1));

        assertDoesNotContain(tree, Kind.VALUE, "foo");
        assertDoesNotContain(tree, Kind.TEXT, "choice.foo");
        assertDoesNotContain(tree, Kind.VALUE, "choice.foo");

        assertDoesNotContainValue(tree, "include", "no");

        assertDoesNotContainValue(tree, "include2", "yes");
        assertDoesNotContain(tree, Kind.BOOLEAN, "include2.y");

        assertContainsValue(tree, "choice", "bar");
        assertContainsValue(tree, "choice.bar", "a-bar");
        assertContainsValue(tree, "include", "yes");
        assertContainsValue(tree, "include.yes", "yes");
        assertContainsValue(tree, "include.yes", "no");
        assertContainsValue(tree, "include2", "no");
    }

    @Test
    void testBoolean() {
        InputTree tree = create("input-tree");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(13));

        String path = "include";
        List<Node> nodes = collect(tree, Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("yes"));

        path = "include.yes";
        nodes = collect(tree, Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 2, path, List.of("yes", "no"));

        path = "include2";
        nodes = collect(tree, Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("no"));
    }

    @Test
    void testEnum() {
        InputTree tree = create("e2e");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(52));

        String path = "theme.base";
        List<Node> nodes = collect(tree, Kind.ENUM, path);
        assertThat(nodes.size(), is(2));
        assertChildrenAreValues(nodes.get(0), 2, path, List.of("custom", "rainbow"));
        assertChildrenAreValues(nodes.get(1), 2, path, List.of("custom", "2d"));
    }

    @Test
    void testList() {
        InputTree tree = create("e2e");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(52));

        String path = "theme.base.colors";
        List<Node> nodes = collect(tree, Kind.LIST, path);
        assertThat(nodes.size(), is(1));
        Node input = nodes.get(0);
        assertChildrenAreValues(input, 15, path, List.of(
                "red", "orange", "yellow", "green", "blue",
                "indigo", "violet", "pink", "light-pink", "cyan",
                "light-salmon", "coral", "tomato", "lemon", "khaki"
        ));
    }

    @Test
    void testText() {
        InputTree tree = create("input-tree");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(13));

        String path = "choice.bar";
        List<Node> nodes = collect(tree, Kind.TEXT, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("a-bar"));
    }

    @Test
    void testTextNoDefault() {
        InputTree.Builder builder = InputTree.builder()
                                             .archetypePath(sourceDir("input-tree"))
                                             .entryPointFile("text-no-default.xml");
        InvocationException e = assertThrows(InvocationException.class, builder::build);
        assertThat(e.getCause().getMessage(), is("Unresolved input: text 'foo' requires a default value"));
    }

    @Test
    void testOptionalTextNoDefault() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("optional-text-no-default.xml")
                                  .build();
        assertThat(tree.size(), is(3));
        List<Node> nodes = collect(tree, Kind.VALUE, "foo");
        assertThat(nodes.size(), is(1));
        Node value = nodes.get(0);
        Map<String, String> combinations = new HashMap<>();
        value.collect(combinations);
        assertThat(combinations.size(), is(0));
    }

    @Test
    void testPresets() {
        InputTree tree = create("e2e");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(52));
        String path = "theme.base";
        List<Node> nodes = collect(tree, Kind.PRESETS, path);
        assertThat(nodes.size(), is(2));
        assertPresetsContains(nodes.get(0), Map.of(
                "theme.base.colors", "red,orange,yellow,green,blue,indigo,violet",
                "theme.base.palette-name", "Rainbow"
        ));
        assertPresetsContains(nodes.get(1), Map.of(
                "theme.base.shapes", "circle,triangle,rectangle",
                "theme.base.library-name", "2D Shapes"
        ));
    }

    @Test
    void testNodeIndex() {
        NodeIndex index = new NodeIndex(2);
        assertThat(index.completed(), is(false));
        assertThat(index.current(), is(0));

        assertThat(index.next(), is(false));
        assertThat(index.completed(), is(false));
        assertThat(index.current(), is(1));

        assertThat(index.next(), is(true));
        assertThat(index.completed(), is(true));
        assertThat(index.current(), is(0));

        assertThat(index.next(), is(true));
        assertThat(index.completed(), is(true));
        assertThat(index.current(), is(1));

        assertThat(index.next(), is(true));
        assertThat(index.completed(), is(true));
        assertThat(index.current(), is(0));
    }

    @Test
    void testRootIndexSize() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list1.xml")
                                  .build();
        NodeIndex index = tree.root().index();
        assertThat(index.size(), is(1));
    }

    @Test
    void testCollectListWithDefault() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list1.xml")
                                  .build();

        Node list = tree.root().children().get(0);
        assertThat(list.path(), is("colors"));
        NodeIndex index = list.index();

        Map<String, String> combination = new LinkedHashMap<>();
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,yellow"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.containsKey("colors"), is(false));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("orange"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("yellow"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,orange,yellow"));
        assertThat(index.completed(), is(false));

        // Wrap to default
        assertThat(index.next(), is(true));
        assertThat(index.current(), is(0));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,yellow"));
        assertThat(index.completed(), is(true));
    }

    @Test
    void testCollectListNoDefault() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list2.xml")
                                  .build();
        tree.print();
        Node list = tree.root().children().get(0);
        assertThat(list.path(), is("colors"));
        NodeIndex index = list.index();
        Map<String, String> combination = new LinkedHashMap<>();

        tree.collect(combination);
        assertThat(combination.containsKey("colors"), is(false));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("orange"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,orange"));
        assertThat(index.completed(), is(false));

        // Wrap to first
        assertThat(index.next(), is(true));
        assertThat(index.current(), is(0));
        tree.collect(combination);
        assertThat(combination.containsKey("colors"), is(false));
        assertThat(index.completed(), is(true));
    }

    @Test
    void testCollectListWithCombiner() {
        BiFunction<List<String>, List<String>, List<List<String>>> combiner = (values, defaultValue) -> {
            List<List<String>> result = new ArrayList<>();
            result.add(values);
            List<String> reversed = new ArrayList<>(values);
            Collections.reverse(reversed);
            result.add(reversed);
            return result;
        };

        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list2.xml")
                                  .listCombiner(combiner)
                                  .build();
        tree.print();
        Node list = tree.root().children().get(0);
        assertThat(list.path(), is("colors"));
        NodeIndex index = list.index();
        Map<String, String> combination = new LinkedHashMap<>();

        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,orange"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("orange,red"));
        assertThat(index.completed(), is(false));

        // Wrap to first
        assertThat(index.next(), is(true));
        assertThat(index.current(), is(0));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,orange"));
        assertThat(index.completed(), is(true));
    }

    @Test
    void testCollect() {
        InputTree tree = create("input-tree");
        Map<String, String> combination = new LinkedHashMap<>();
        List<Node> inputNodes = collectInputs(tree);
        assertThat(inputNodes.size(), is(6));
        tree.collect(combination);
        assertThat(combination.isEmpty(), is(false));
        assertThat(combination.size(), is(lessThanOrEqualTo(inputNodes.size())));
        assertThat(combination.get("choice"), is("bar"));
        assertThat(combination.get("include"), is("yes"));
        assertThat(combination.get("include.yes"), is("yes"));
        assertThat(combination.get("include2"), is("no"));
        assertThat(combination.get("choice.bar"), is("a-bar"));

        Node input = inputNodes.get(4);
        assertThat(input.path(), is("include.yes"));
        input.index().next();
        tree.collect(combination);
        assertThat(combination.get("choice"), is("bar"));
        assertThat(combination.get("include"), is("yes"));
        assertThat(combination.get("include.yes"), is("no"));
        assertThat(combination.get("include2"), is("no"));
        assertThat(combination.get("choice.bar"), is("a-bar"));
    }

    @Test
    void testSubstitutions() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("substitutions.xml")
                                  .verbose(true)
                                  .build();
        Map<String, String> combination = new LinkedHashMap<>();
        List<Node> nodes = tree.asList();
        tree.collect(combination);
        assertThat(combination.size(), is(5));
        assertThat(combination.get("foo"), is("a-foo"));
        assertThat(combination.get("bar"), is("a-bar"));
        assertThat(combination.get("preset"), is("a-foo-a-bar"));
        assertThat(combination.get("text"), is("a-foo-a-bar"));
        assertThat(combination.get("list-things"), is("a-foo"));

        Node list = nodes.get(4);
        assertThat(list.kind(), is(Kind.LIST));
        NodeIndex index = list.index();
        index.next(); // Skip empty list-things
        index.next();

        tree.collect(combination);
        assertThat(combination.size(), is(5));
        assertThat(combination.get("foo"), is("a-foo"));
        assertThat(combination.get("bar"), is("a-bar"));
        assertThat(combination.get("preset"), is("a-foo-a-bar"));
        assertThat(combination.get("text"), is("a-foo-a-bar"));
        assertThat(combination.get("list-things"), is("a-bar"));
    }

    @Test
    void testCollectExternalValues() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list2.xml")
                                  .externalValues(Map.of("colors", "orange"))
                                  .verbose(true)
                                  .build();
        Map<String, String> combination = new LinkedHashMap<>();
        List<Node> inputNodes = collectInputs(tree);
        assertThat(inputNodes.size(), is(1));
        tree.collect(combination);
        assertThat(combination.size(), is(1));
        assertThat(combination.get("colors"), is("orange"));
    }

    @Test
    void testExternalDefaults() {
        InputTree tree = InputTree.builder()
                                  .archetypePath(sourceDir("input-tree"))
                                  .entryPointFile("list2.xml")
                                  .externalDefaults(Map.of("colors", "orange"))
                                  .build();

        Node list = tree.root().children().get(0);
        assertThat(list.path(), is("colors"));
        NodeIndex index = list.index();
        Map<String, String> combination = new LinkedHashMap<>();

        tree.collect(combination);
        assertThat(combination.get("colors"), is("orange"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.containsKey("colors"), is(false));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red"));
        assertThat(index.completed(), is(false));

        assertThat(index.next(), is(false));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("red,orange"));
        assertThat(index.completed(), is(false));

        // Wrap to first
        assertThat(index.next(), is(true));
        assertThat(index.current(), is(0));
        tree.collect(combination);
        assertThat(combination.get("colors"), is("orange"));
        assertThat(index.completed(), is(true));
    }

    private static InputTree create(String testDir) {
        return create(sourceDir(testDir));
    }

    private static Path sourceDir(String testDirName) {
        Path targetDir = targetDir(InputTreeTest.class);
        return targetDir.resolve("test-classes/" + testDirName);
    }

    private static InputTree create(Path sourceDir) {
        return InputTree.builder()
                        .archetypePath(sourceDir)
                        .build();
    }

    static void assertContains(InputTree tree, Kind kind, String path) {
        tree.stream().filter(n -> n.kind() == kind && n.path().equals(path)).findFirst().orElseThrow();
    }

    @SuppressWarnings("SameParameterValue")
    static List<Node> collect(InputTree tree, Kind kind) {
        return tree.stream().filter(n -> n.kind() == kind).collect(Collectors.toList());
    }

    static List<Node> collectInputs(InputTree tree) {
        return tree.stream().filter(n -> n.kind() != Kind.ROOT && n.kind() != Kind.VALUE).collect(Collectors.toList());
    }

    static List<Node> collect(InputTree tree, Kind kind, String path) {
        return tree.stream().filter(n -> n.kind() == kind && n.path().equals(path)).collect(Collectors.toList());
    }

    static void assertContainsValue(InputTree tree, String path, String value) {
        tree.stream()
            .filter(n -> n.kind() == Kind.VALUE
                         && n.path().equals(path) &&
                         ((ValueNode) n).value().equals(value))
            .findFirst().orElseThrow();
    }

    static void assertDoesNotContain(InputTree tree, Kind kind, String path) {
        assertThat(tree.stream().anyMatch(n -> n.kind() == kind && n.path().equals(path)), is(false));
    }

    static void assertDoesNotContainValue(InputTree tree, String path, String value) {
        boolean found = tree.stream()
                            .anyMatch(n -> n.kind() == Kind.VALUE
                                           && n.path().equals(path) &&
                                           ((ValueNode) n).value().equals(value));
        assertThat(found, is(false));
    }

    static void assertChildrenAreValues(Node input, int expectedChildren, String expectedPath, List<String> expectedValues) {
        assertThat(input, is(not(nullValue())));
        List<Node> values = input.children();
        assertThat(values.size(), is(expectedChildren));
        assertThat(values.size(), is(expectedValues.size()));
        for (int i = 0; i < expectedValues.size(); i++) {
            Node node = values.get(i);
            String expectedValue = expectedValues.get(i);
            assertThat(node.kind(), is(Kind.VALUE));
            assertThat(node.path(), is(expectedPath));
            ValueNode valueNode = (ValueNode) node;
            assertThat(valueNode.value(), is(not(nullValue())));
            assertThat(valueNode.value(), is(expectedValue));
        }
    }

    static void assertPresetsContains(Node input, Map<String, String> expectedValues) {
        assertThat(input, is(not(nullValue())));
        assertThat(input.kind(), is(Kind.PRESETS));
        PresetNode presetNode = (PresetNode) input;
        Map<String, String> presets = presetNode.presets();
        assertThat(presets, is(not(nullValue())));
        assertThat(presets.size(), is(expectedValues.size()));
        presets.forEach((key, value) -> {
            assertThat(expectedValues.containsKey(key), is(true));
            assertThat(expectedValues.get(key), is(value));
        });
    }
}
