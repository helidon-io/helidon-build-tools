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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.InputTree.Node;

import org.jetbrains.annotations.NotNull;

/**
 * An iterator over the combinations of input present in an archetype.
 */
public class InputCombinations implements Iterable<Map<String, String>> {
    private final InputTree tree;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private InputCombinations(InputTree tree) {
        this.tree = tree;
    }

    @NotNull
    @Override
    public Iterator<Map<String, String>> iterator() {
        return new CombinationsIterator(tree);
    }

    private static class CombinationsIterator implements Iterator<Map<String, String>> {
        private final Node root;
        private final Map<String, String> combinations;
        private final Map<String, String> immutableCombinations;
        private int iterations;
        private List<Node> siblings;
        private int siblingIndex;
        private Node currentNode;
        private Node nextNode;

        private CombinationsIterator(InputTree tree) {
            this.root = tree.root();
            this.combinations = new LinkedHashMap<>();
            this.immutableCombinations = Collections.unmodifiableMap(combinations);
            this.currentNode = findNextNode(root);
            this.nextNode = nextNode(currentNode);
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

                // Advance the leaf node. Did we complete it?

                if (currentNode.index().next()) {

                    // Yes. Advance upwards until we find one that is not complete, if any

                    while (nextNode.index().next()) {

                        // Did we complete the root?

                        if (nextNode == root) {

                            // Yes, so we're done

                            return immutableCombinations;
                        }

                        // No, so move to the next sibling or parent

                        nextNode = nextNode(nextNode);
                    }

                    // Find the next node we want to step through

                    currentNode = findNextNode(nextNode);
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

        Node findNextNode(Node current) {
            Node leaf = current.findLeafNode();
            if (leaf != current) {

                // If the leaf has siblings, we want to walk them

                Node parent = leaf.parent();
                List<Node> siblings = parent.children();
                if (siblings.size() > 1) {
                    this.siblings = siblings;
                    this.siblingIndex = this.siblings.size() - 1;
                    return siblings.get(siblingIndex);
                }
            }
            return leaf;
        }

        Node nextNode(Node node) {

            // TODO: THIS IS A HACK, REMOVE checkSiblings!
/*
            boolean checkSiblings = true;
            if (node.index().completed() && node.id() == 1) {
                checkSiblings = false;
            }
*/

            // Do we have siblings?

            if (siblings != null) {

                // Yes, find the next sibling (in reverse order) that is not completed, if any

                while (--siblingIndex >= 0) {
                    Node sibling = siblings.get(siblingIndex);
                    if (!sibling.index().completed()) {
                        return sibling;
                    }
                }

                // Done with siblings, find a parent

                siblings = null;
            }

            // Otherwise, find the first non VALUE parent that is not completed

            return nextParent(node);
        }

        Node nextParent(Node node) {
            // Find the first non VALUE parent that is not completed
            Node parent = node.parent();
            while (true) {
                if (parent == root) {
                    return parent;
                } else if (parent.kind() != Node.Kind.VALUE && !parent.index().completed()) {
                    return parent;
                } else {
                    parent = parent.parent();
                }
            }
        }
    }

    /**
     * Builder.
     */
    public static class Builder {
        private final InputTree.Builder builder;

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
            return this;
        }

        /**
         * Build the instance.
         *
         * @return The instance.
         */
        public InputCombinations build() {
            return new InputCombinations(builder.build());
        }

    }
}
