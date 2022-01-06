/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.InputTree.Node;
import io.helidon.build.archetype.engine.v2.InputTree.PresetNode;
import io.helidon.build.archetype.engine.v2.InputTree.ValueNode;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link InputTree}.
 */
class InputTreeTest {

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
        assertThat(root.kind(), is(Node.Kind.ROOT));

        List<Node> presets = collect(tree, Node.Kind.PRESETS);
        assertThat(presets.size(), is(1));
        assertThat(presets.get(0).id(), is(1));

        assertContains(tree, Node.Kind.VALUE, "choice");
        assertContains(tree, Node.Kind.TEXT, "choice.foo");
        assertContains(tree, Node.Kind.VALUE, "choice.foo");

        assertContains(tree, Node.Kind.VALUE, "include");
        assertContains(tree, Node.Kind.BOOLEAN, "include2");
        assertContains(tree, Node.Kind.VALUE, "include2.y");

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
                                  .build();
        /*

        UNPRUNED

        0 ROOT
        |   1 PRESETS '{choice=bar, include=true, include2=false}' from main.xml:24
        |   2 ENUM 'choice' from main.xml:30
        |   |   3 VALUE 'choice' = 'foo' from main.xml:31                   REMOVED
        |   |   |   4 TEXT 'choice.foo' from foo.xml:25                     REMOVED
        |   |   |   |   5 VALUE 'choice.foo' = 'a-foo' from foo.xml:25      REMOVED
        |   |   6 VALUE 'choice' = 'bar' from main.xml:34
        |   |   |   7 TEXT 'choice.bar' from bar.xml:25
        |   |   |   |   8 VALUE 'choice.bar' = 'a-bar' from bar.xml:25
        |   9 BOOLEAN 'include' from main.xml:38
        |   |   10 VALUE 'include' = 'yes' from main.xml:38
        |   |   |   11 BOOLEAN 'include.yes' from main.xml:40
        |   |   |   |   12 VALUE 'include.yes' = 'yes' from main.xml:40
        |   |   |   |   13 VALUE 'include.yes' = 'no' from main.xml:40
        |   |   14 VALUE 'include' = 'no' from main.xml:38                  REMOVED
        |   15 BOOLEAN 'include2' from main.xml:43
        |   |   16 VALUE 'include2' = 'yes' from main.xml:43                REMOVED
        |   |   |   17 BOOLEAN 'include2.y' from main.xml:45                REMOVED
        |   |   |   |   18 VALUE 'include2.y' = 'yes' from main.xml:45      REMOVED
        |   |   |   |   19 VALUE 'include2.y' = 'no' from main.xml:45       REMOVED
        |   |   20 VALUE 'include2' = 'no' from main.xml:43

        PRUNED

        0 ROOT
        |   1 PRESETS '{choice=bar, include=true, include2=false}' from main.xml:24
        |   2 ENUM 'choice' from main.xml:30
        |   |   3 VALUE 'choice' = 'bar' from main.xml:34
        |   |   |   4 TEXT 'choice.bar' from bar.xml:25
        |   |   |   |   5 VALUE 'choice.bar' = 'a-bar' from bar.xml:25
        |   6 BOOLEAN 'include' from main.xml:38
        |   |   7 VALUE 'include' = 'yes' from main.xml:38
        |   |   |   8 BOOLEAN 'include.yes' from main.xml:40
        |   |   |   |   9 VALUE 'include.yes' = 'yes' from main.xml:40
        |   |   |   |   10 VALUE 'include.yes' = 'no' from main.xml:40
        |   11 BOOLEAN 'include2' from main.xml:43
        |   |   12 VALUE 'include2' = 'no' from main.xml:43

         */

        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(13));

        Node root = tree.root();
        assertThat(root, is(not(nullValue())));
        assertThat(root.id(), is(0));
        assertThat(root.kind(), is(Node.Kind.ROOT));

        List<Node> presets = collect(tree, Node.Kind.PRESETS);
        assertThat(presets.size(), is(1));
        assertThat(presets.get(0).id(), is(1));

        assertDoesNotContain(tree, Node.Kind.VALUE, "foo");
        assertDoesNotContain(tree, Node.Kind.TEXT, "choice.foo");
        assertDoesNotContain(tree, Node.Kind.VALUE, "choice.foo");

        assertDoesNotContainValue(tree, "include", "no");

        assertDoesNotContainValue(tree, "include2", "yes");
        assertDoesNotContain(tree, Node.Kind.BOOLEAN, "include2.y");

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
        tree.print(); // TODO REMOVE
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(13));

        String path = "include";
        List<Node> nodes = collect(tree, Node.Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("yes"));

        path = "include.yes";
        nodes = collect(tree, Node.Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 2, path, List.of("yes", "no"));

        path = "include2";
        nodes = collect(tree, Node.Kind.BOOLEAN, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("no"));
    }

    @Test
    void testEnum() {
        InputTree tree = create("e2e");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(52));

        String path = "theme.base";
        List<Node> nodes = collect(tree, Node.Kind.ENUM, path);
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
        List<Node> nodes = collect(tree, Node.Kind.LIST, path);
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
        List<Node> nodes = collect(tree, Node.Kind.TEXT, path);
        assertThat(nodes.size(), is(1));
        assertChildrenAreValues(nodes.get(0), 1, path, List.of("a-bar"));
    }

    @Test
    void testPresets() {
        InputTree tree = create("e2e");
        assertThat(tree, is(not(nullValue())));
        assertThat(tree.size(), is(52));
        String path = "theme.base";
        List<Node> nodes = collect(tree, Node.Kind.PRESETS, path);
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
    void printV2() { // TODO Disable since it requires system dependent path
        Path sourceDir = Path.of("/Users/batsatt/dev/helidon/archetypes-v2");
        InputTree tree = create(sourceDir);
        tree.print();
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

    static void assertContains(InputTree tree, Node.Kind kind, String path) {
        tree.stream().filter(n -> n.kind() == kind && n.path().equals(path)).findFirst().orElseThrow();
    }

    static List<Node> collect(InputTree tree, Node.Kind kind) {
        return tree.stream().filter(n -> n.kind() == kind).collect(Collectors.toList());
    }

    static List<Node> collect(InputTree tree, Node.Kind kind, String path) {
        return tree.stream().filter(n -> n.kind() == kind && n.path().equals(path)).collect(Collectors.toList());
    }

    static void assertContainsValue(InputTree tree, String path, String value) {
        tree.stream()
            .filter(n -> n.kind() == Node.Kind.VALUE
                         && n.path().equals(path) &&
                         ((ValueNode) n).value().equals(value))
            .findFirst().orElseThrow();
    }

    static void assertDoesNotContain(InputTree tree, Node.Kind kind, String path) {
        assertThat(tree.stream().anyMatch(n -> n.kind() == kind && n.path().equals(path)), is(false));
    }

    static void assertDoesNotContainValue(InputTree tree, String path, String value) {
        boolean found = tree.stream()
                            .anyMatch(n -> n.kind() == Node.Kind.VALUE
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
            assertThat(node.kind(), is(Node.Kind.VALUE));
            assertThat(node.path(), is(expectedPath));
            ValueNode valueNode = (ValueNode) node;
            assertThat(valueNode.value(), is(not(nullValue())));
            assertThat(valueNode.value(), is(expectedValue));
        }
    }

    static void assertPresetsContains(Node input, Map<String, String> expectedValues) {
        assertThat(input, is(not(nullValue())));
        assertThat(input.kind(), is(Node.Kind.PRESETS));
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
