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
        private Node leafNode;

        private CombinationsIterator(InputTree tree) {
            this.root = tree.root();
            this.combinations = new LinkedHashMap<>();
            this.immutableCombinations = Collections.unmodifiableMap(combinations);
            this.leafNode = root.findLeafNode();
        }

        @Override
        public boolean hasNext() {
            return !root.index().completed();
        }

        @Override
        public Map<String, String> next() {
            if (hasNext()) {

                // Is this our first time through?

                if (iterations++ > 0) {

                    // No, advance the leaf node. Did we complete it?

                    if (leafNode.index().next()) {

                        // Yes. Walk parents until we find one that is not complete and advance it.

                        Node parent = incompleteParent(leafNode);
                        parent.index().next();

                        // Update the leaf node

                        leafNode = parent.findLeafNode();
                    }
                }

                // Collect and return

                root.collect(combinations);
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

        static Node incompleteParent(Node from) {
            Node parent = from.parent();
            while (parent.index().completed()) {
                parent = parent.parent();
            }
            return parent;
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
         * Build the instance.
         *
         * @return The instance.
         */
        public InputCombinations build() {
            return new InputCombinations(builder.build());
        }

    }
}
