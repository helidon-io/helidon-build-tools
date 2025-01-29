/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Tree representation of traversed model nodes.
 */
public final class TemplateModel implements Node.Visitor {

    private final Context context;
    private final ModelNode root;
    private ModelNode head;

    /**
     * Create a new instance.
     *
     * @param context context
     */
    public TemplateModel(Context context) {
        this.context = context;
        this.root = new Map(null, null, 0);
        this.head = root;
    }

    @Override
    public boolean visit(Node node) {
        String key = node.attribute("key").asString().orElse(null);
        int order = node.attribute("order").asInt().orElse(100);
        switch (node.kind()) {
            case MODEL_LIST:
                head = head.add(new List(head, key, order));
                break;
            case MODEL_MAP:
                head = head.add(new Map(head, key, order));
                break;
            case MODEL_VALUE:
                // file attribute or node value
                String content = node.attribute("file").asString()
                        .map(file -> readFile(context, file))
                        .orElseGet(() -> node.value().getString());

                boolean isOverride = node.attribute("override").asBoolean().orElse(false);
                String template = node.attribute("template").asString().orElse(null);

                if (template == null) {
                    // interpolate context variables now since they are expressed as input paths
                    // and the input path changes during traversal
                    content = context.scope().interpolate(content);
                }

                // value is a leaf-node, thus we are not updating the head
                head.add(new Value(head, key, order, content, template, isOverride));
                break;
            default:
        }
        return true;
    }

    @Override
    public void postVisit(Node node) {
        switch (node.kind()) {
            case MODEL_LIST:
                head.sort();
                head = head.parent;
                break;
            case MODEL_MAP:
                head = head.parent;
                break;
            default:
        }
    }

    /**
     * Get the root node of the merged model tree.
     *
     * @return root node
     */
    public ModelNode root() {
        return root;
    }

    private static String readFile(Context context, String file) {
        try {
            Path contentFile = context.cwd().resolve(file);
            if (!Files.exists(contentFile)) {
                throw new IllegalStateException("Value content file does not exist: " + file);
            }
            return Files.readString(contentFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Merged model node.
     */
    public abstract static class ModelNode {

        private final ModelNode parent;
        private final String key;
        private final int order;

        private ModelNode(ModelNode node) {
            this(node.parent, node.key, node.order);
        }

        private ModelNode(ModelNode parent, String key, int order) {
            this.parent = parent;
            this.key = key;
            this.order = order;
        }

        /**
         * Get a node by key.
         *
         * @param key key
         * @return node
         */
        public ModelNode get(String key) {
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
        protected ModelNode add(ModelNode node) {
            throw new UnsupportedOperationException("model node does not support 'add'");
        }
    }

    /**
     * List element.
     *
     * @param <T> node type
     */
    public static class Element<T extends ModelNode> extends ModelNode {

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
        public ModelNode get(String key) {
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
    public static class List extends ModelNode implements Iterable<Element<?>> {

        private final ArrayList<ModelNode> value = new ArrayList<>();

        private List(ModelNode parent, String key, int order) {
            super(parent, key, order);
        }

        /**
         * Get the values.
         *
         * @return values
         */
        public Collection<ModelNode> list() {
            return value;
        }

        @Override
        public Iterator<Element<?>> iterator() {
            final Iterator<ModelNode> iterator = value.iterator();
            return new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Element<?> next() {
                    ModelNode next = iterator.next();
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
        protected ModelNode add(ModelNode node) {
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
    public static class Map extends ModelNode {

        private final HashMap<String, ModelNode> value = new HashMap<>();

        private Map(ModelNode parent, String key, int order) {
            super(parent, key, order);
        }

        @Override
        public ModelNode get(String key) {
            return value.get(key);
        }

        @Override
        protected ModelNode add(ModelNode node) {
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
                if (v instanceof Value && node instanceof Value && ((Value) node).override) {
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
    public static class Value extends ModelNode {

        private final String value;
        private final String template;
        private final boolean override;

        private Value(ModelNode parent, String key, int order, String value, String template, boolean override) {
            super(parent, key, order);
            this.value = value;
            this.template = template;
            this.override = override;
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
