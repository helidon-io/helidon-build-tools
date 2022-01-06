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

import io.helidon.build.archetype.engine.v2.InputTree.Node;

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

    static boolean contains(List<Node> nodes, Node.Kind kind, String path) {
        return nodes.stream().anyMatch(n -> n.kind() == kind && n.path().equals(path));
    }

    static Node assertContains(List<Node> nodes, Node.Kind kind, String path) {
        return nodes.stream().filter(n -> n.kind() == kind && n.path().equals(path)).findFirst().orElseThrow();
    }

    static Node assertContainsValue(List<Node> nodes, String path, String value) {
        return nodes.stream()
                    .filter(n -> n.kind() == Node.Kind.VALUE
                                 && n.path().equals(path) &&
                                 ((InputTree.ValueNode)n).value().equals(value))
                    .findFirst().orElseThrow();
    }

    static boolean assertDoesNotContain(List<Node> nodes, Node.Kind kind, String path) {
        return nodes.stream().filter(n -> n.kind() == kind && n.path().equals(path)).findFirst().isEmpty();
    }

    @Test
    void testPresetPruning() {
        InputTree tree = create("input-tree");
        assertThat(tree, is(not(nullValue())));

        tree.print(); // TODO REMOVE

        Node root = tree.root();
        assertThat(root.id(), is(0));
        assertThat(root, is(not(nullValue())));
        List<Node> nodes = tree.asList();
        assertThat(nodes.get(0).id(), is(0));
        assertThat(nodes.get(1).kind(), is(Node.Kind.PRESETS));

        assertDoesNotContain(nodes, Node.Kind.VALUE, "choice");
        assertDoesNotContain(nodes, Node.Kind.TEXT, "choice.foo");
        assertDoesNotContain(nodes, Node.Kind.VALUE, "choice.foo");

        assertDoesNotContain(nodes, Node.Kind.VALUE, "include");
        assertDoesNotContain(nodes, Node.Kind.BOOLEAN, "include2");
        assertDoesNotContain(nodes, Node.Kind.VALUE, "include2.y");

        /*

ORIGINAL

0 ROOT
|   1 PRESETS '{choice=bar, include=true}' from main.xml:24
|   2 ENUM 'choice' from main.xml:29
|   |   3 VALUE 'choice' = 'foo' from main.xml:30                      REMOVED
|   |   |   4 TEXT 'choice.foo' from foo.xml:25                        REMOVED
|   |   |   |   5 VALUE 'choice.foo' = 'a-foo' from foo.xml:25         REMOVED
|   |   6 VALUE 'choice' = 'bar' from main.xml:33
|   |   |   7 TEXT 'choice.bar' from bar.xml:25
|   |   |   |   8 VALUE 'choice.bar' = 'a-bar' from bar.xml:25
|   9 BOOLEAN 'include' from main.xml:37
|   |   10 VALUE 'include' = 'yes' from main.xml:37
|   |   |   11 BOOLEAN 'include.yes' from main.xml:39
|   |   |   |   12 VALUE 'include.yes' = 'yes' from main.xml:39
|   |   |   |   13 VALUE 'include.yes' = 'no' from main.xml:39
|   |   14 VALUE 'include' = 'no' from main.xml:37                     REMOVED
|   15 BOOLEAN 'include2' from main.xml:42
|   |   16 VALUE 'include2' = 'yes' from main.xml:42                   REMOVED
|   |   |   17 BOOLEAN 'include2.y' from main.xml:44                   REMOVED
|   |   |   |   18 VALUE 'include2.y' = 'yes' from main.xml:44         REMOVED
|   |   |   |   19 VALUE 'include2.y' = 'no' from main.xml:44          REMOVED
|   |   20 VALUE 'include2' = 'no' from main.xml:42

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
    }

    @Test
    void testSomething() {
        InputTree tree = create("e2e");
        tree.print();
        //   InputPermutations collector = create(Path.of("/Users/batsatt/dev/helidon/archetypes-v2")); // TODO REMOVE
        assertThat(tree, is(not(nullValue())));
    }

    private InputTree create(String testDir) {
        Path targetDir = targetDir(this.getClass());
        Path sourceDir = targetDir.resolve("test-classes/" + testDir);
        return create(sourceDir);
    }

    private InputTree create(Path sourceDir) {
        return InputTree.builder()
                        .archetypePath(sourceDir)
                        .build();
    }
}
