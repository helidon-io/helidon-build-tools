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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.util.InputTree.Node;
import io.helidon.build.archetype.engine.v2.util.InputTree.NodeIndex;


/**
 * An iterator over the combinations of inputs present in an archetype.
 */
public class InputCombinations implements Iterable<Map<String, String>> {
    private final InputTree tree;
    private final boolean verbose;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private InputCombinations(InputTree tree, boolean verbose) {
        this.tree = tree;
        this.verbose = verbose;
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        return new CombinationsIterator(tree, verbose);
    }

    private static class CombinationsIterator implements Iterator<Map<String, String>> {
        private final Node root;
        private final boolean verbose;
        private final Map<String, String> combinations;
        private final Map<String, String> immutableCombinations;
        private final List<Node> currentInputs;
        private int iterations;
        private int currentIndex;
        private Node currentLeafNode;

        private CombinationsIterator(InputTree tree, boolean verbose) {
            this.root = tree.root();
            this.verbose = verbose;
            this.combinations = new LinkedHashMap<>();
            this.immutableCombinations = Collections.unmodifiableMap(combinations);
            this.currentInputs = root.collectCurrentInputs(new ArrayList<>());
            this.currentLeafNode = getLeafInput();
            if (verbose) {
                printCurrentInputs("Initial");
            }
        }

        @Override
        public boolean hasNext() {
            return !root.index().completed();
        }

        @Override
        public Map<String, String> next() {
            if (hasNext()) {
                iterations++;

                // Collect

                root.collect(combinations);

                // Advance the current leaf node. Did we complete it?

                if (currentLeafNode.index().next()) {

                    // Yes. Advance up parent chain until we find one that is not complete, if any

                    Node nextParent = advanceIncompleteParent();

                    // Are we done?

                    if (nextParent == null) {

                        // Yep

                        return immutableCombinations;
                    }

                    // No, so update current inputs

                    updateCurrentInputs();
                }

                return immutableCombinations;

            } else {
                throw new NoSuchElementException(root.children().get(0).script().toString() + ": iteration completed");
            }
        }

        /**
         * Returns the number of iterations.
         *
         * @return The count.
         */
        int iterations() {
            return iterations;
        }

        private Node getLeafInput() {
            currentIndex = currentInputs.size() - 1;
            return currentInputs.get(currentIndex);
        }

        private void updateCurrentInputs() {
            root.collectCurrentInputs(currentInputs);
            currentLeafNode = getLeafInput();
            if (verbose) {
                printCurrentInputs("Current");
            }
        }

        private Node advanceIncompleteParent() {
            for (int i = currentIndex - 1; i >= 0; i--) {
                Node parent = currentInputs.get(i);
                NodeIndex parentIndex = parent.index();
                if (!parentIndex.completed()) {
                    if (!parentIndex.next()) {

                        // Reset all children
                        for (int j = currentIndex; j > i; j--) {
                            currentInputs.get(j).index().reset();
                        }
                        return parent;
                    }
                }
            }
            return null;
        }

        private void printCurrentInputs(String stage) {
            System.out.printf("%n%s inputs ---------------- %n%n", stage);
            for (int i = 0; i < currentInputs.size(); i++) {
                Node node = currentInputs.get(i);
                NodeIndex index = node.index();
                int current = index.current();
                int size = index.size();
                boolean completed = index.completed();
                System.out.printf("%d %s (%d of %d) %s%n", i, completed, current, size, node);
            }
            System.out.println();
        }
    }

    /**
     * Builder.
     */
    public static class Builder {
        private final InputTree.Builder builder;
        private boolean verbose;

        Builder() {
            builder = InputTree.builder();
        }

        /**
         * Set the required archetype path.
         *
         * @param archetypePath The path.
         * @return This instance, for chaining.
         */
        public Builder archetypePath(Path archetypePath) {
            builder.archetypePath(archetypePath);
            return this;
        }

        /**
         * Set the entry point file name. Defaults to "main.xml".
         *
         * @param entryPointFileName The file name.
         * @return This instance, for chaining.
         */
        public Builder entryPointFile(String entryPointFileName) {
            builder.entryPointFile(entryPointFileName);
            return this;
        }

        /**
         * Set to print the tree.
         *
         * @param verbose {@code true} if the tree should be printed.
         * @return This instance, for chaining.
         */
        public Builder verbose(boolean verbose) {
            builder.verbose(verbose);
            this.verbose = verbose;
            return this;
        }

        /**
         * Build the instance.
         *
         * @return The instance.
         */
        public InputCombinations build() {
            return new InputCombinations(builder.build(), verbose);
        }
    }
}
