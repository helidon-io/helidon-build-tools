/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.context.Context;

/**
 * Merged model.
 * Tree representation of the model nodes for a given block.
 *
 * @see #resolveModel(Block, Context)
 */
public final class MergedModel {

    private final Block block;
    private final Node root;

    /**
     * Create a new model resolver.
     *
     * @param block block
     * @param node  root node
     */
    MergedModel(Block block, Node node) {
        this.block = block;
        this.root = node;
    }

    /**
     * Get the root node of the merged model tree.
     *
     * @return root node
     */
    public Node node() {
        return root;
    }

    /**
     * Get the original block.
     *
     * @return block
     */
    public Block block() {
        return block;
    }

    /**
     * Perform a full traversal of the given block to resolve the merged model tree.
     *
     * @param block   block, must be non {@code null}
     * @param context context, must be non {@code null}
     * @return root node of the merged model tree
     * @throws NullPointerException if context or block is {@code null}
     */
    public static MergedModel resolveModel(Block block, Context context) {
        ModelResolver modelResolver = new ModelResolver(block);
        Controller.walk(modelResolver, block, context);
        return modelResolver.model();
    }

    /**
     * Merged model node.
     */
    public abstract static class Node {

        private final Node parent;
        private final String key;
        private final int order;

        private Node(Node node) {
            this(node.parent, node.key, node.order);
        }

        private Node(Node parent, String key, int order) {
            this.parent = parent;
            this.key = key;
            this.order = order;
        }

        /**
         * Get the parent.
         *
         * @return parent
         */
        Node parent() {
            return parent;
        }

        /**
         * Get a node by key.
         *
         * @param key key
         * @return node
         */
        public Node get(String key) {
            return this.key != null && this.key.equals(key) ? this : null;
        }

        /**
         * Sort the nested values.
         */
        protected void sort() {
            throw new UnsupportedOperationException("model node does not support 'sort'");
        }

        /**
         * Merge the given node.
         *
         * @param node node
         * @return the merged node
         */
        protected Node add(Node node) {
            throw new UnsupportedOperationException("model node does not support 'add'");
        }
    }

    /**
     * List element.
     *
     * @param <T> node type
     */
    public static class Element<T extends Node> extends Node {

        private final int index;
        private final boolean first;
        private final boolean last;
        private final T wrapped;

        private Element(int index, boolean first, boolean last, T wrapped) {
            super(wrapped);
            this.index = index;
            this.first = first;
            this.last = last;
            this.wrapped = wrapped;
        }

        @Override
        public Node get(String key) {
            return wrapped.get(key);
        }

        /**
         * Get the wrapped node.
         *
         * @return node
         */
        public T wrapped() {
            return wrapped;
        }

        /**
         * Get the index.
         *
         * @return index
         */
        public int index() {
            return index;
        }

        /**
         * Test if this element is first in the enclosing list.
         *
         * @return {@code true} if first, {@code false} otherwise
         */
        public boolean first() {
            return first;
        }

        /**
         * Test if this element is last in the enclosing list.
         *
         * @return {@code true} if last, {@code false} otherwise
         */
        public boolean last() {
            return last;
        }
    }

    /**
     * Iterable list element.
     */
    public static class IterableElement extends Element<List> implements Iterable<Element<?>> {

        private IterableElement(int index, boolean first, boolean last, List wrapped) {
            super(index, first, last, wrapped);
        }

        @Override
        public Iterator<Element<?>> iterator() {
            return wrapped().iterator();
        }
    }

    /**
     * List node.
     */
    public static class List extends Node implements Iterable<Element<?>> {

        private final java.util.List<Node> value = new LinkedList<>();

        /**
         * Create a new instance.
         *
         * @param parent parent node
         * @param key    key
         * @param order  order
         */
        List(Node parent, String key, int order) {
            super(parent, key, order);
        }

        /**
         * Get the values.
         *
         * @return values
         */
        public Collection<Node> list() {
            return value;
        }

        @Override
        public Iterator<Element<?>> iterator() {
            final Iterator<Node> iterator = value.iterator();
            return new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Element<?> next() {
                    Node next = iterator.next();
                    int current = index++;
                    if (next instanceof List) {
                        return new IterableElement(current, current == 0, !iterator.hasNext(), (List) next);
                    }
                    return new Element<>(current, current == 0, !iterator.hasNext(), next);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        protected Node add(Node node) {
            value.add(node);
            return node;
        }

        @Override
        protected void sort() {
            value.sort((c1, c2) -> Integer.compare(c2.order, c1.order));
        }
    }

    /**
     * Map node.
     */
    public static class Map extends Node {

        private final java.util.Map<String, Node> value = new HashMap<>();

        /**
         * Create a new instance.
         *
         * @param parent parent node
         * @param key    key
         * @param order  order
         */
        Map(Node parent, String key, int order) {
            super(parent, key, order);
        }

        @Override
        public Node get(String key) {
            return value.get(key);
        }

        @Override
        protected Node add(Node node) {
            if (node.key == null) {
                throw new IllegalArgumentException("Cannot add a model with no key to a map");
            }
            return value.compute(node.key, (k, v) -> {
                if (v == null) {
                    return node;
                }
                if (v instanceof List && node instanceof List) {
                    ((List) v).value.addAll(((List) node).value);
                    return v;
                }
                if (v instanceof Value && node instanceof Value) {
                    value.put(k, node);
                    return node;
                }
                return v.order < node.order ? node : v;
            });
        }
    }

    /**
     * Value node.
     */
    public static class Value extends Node {

        private final String value;
        private final String template;

        /**
         * Create a new instance.
         *
         * @param parent   parent node
         * @param key      key
         * @param order    order
         * @param value    value
         * @param template template engine
         */
        Value(Node parent, String key, int order, String value, String template) {
            super(parent, key, order);
            this.value = value;
            this.template = template;
        }

        /**
         * Get the template engine.
         *
         * @return template engine, may be {@code null}
         */
        public String template() {
            return template;
        }

        /**
         * Get the value.
         *
         * @return value
         */
        public String value() {
            return value;
        }
    }
}
