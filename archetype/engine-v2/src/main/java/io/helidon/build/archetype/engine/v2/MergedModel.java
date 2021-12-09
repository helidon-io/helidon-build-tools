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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.ast.Block;

/**
 * Merged model.
 * Tree representation of the model nodes for a given block.
 *
 * @see #resolveModel(Block, Context)
 */
public abstract class MergedModel {

    private final MergedModel parent;
    private final String key;
    private final int order;

    private MergedModel(MergedModel parent, String key, int order) {
        this.parent = parent;
        this.key = key;
        this.order = order;
    }

    /**
     * Get the parent.
     *
     * @return parent
     */
    MergedModel parent() {
        return parent;
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
        ModelResolver modelResolver = new ModelResolver();
        Controller.walk(modelResolver, block, context);
        context.ensureEmptyInputs();
        return modelResolver.head();
    }

    /**
     * Get a model node by key.
     *
     * @param key key
     * @return model
     */
    public MergedModel get(String key) {
        return this.key != null && this.key.equals(key) ? this : null;
    }

    /**
     * Sort the nested values.
     */
    protected void sort() {
        throw new UnsupportedOperationException();
    }

    /**
     * Merge the given node.
     *
     * @param node node
     * @return the merged node
     */
    protected MergedModel add(MergedModel node) {
        throw new UnsupportedOperationException();
    }

    /**
     * List node.
     */
    public static class List extends MergedModel implements Iterable<MergedModel> {

        private final java.util.List<MergedModel> value = new LinkedList<>();

        /**
         * Create a new instance.
         *
         * @param parent parent
         * @param key    key
         * @param order  order
         */
        List(MergedModel parent, String key, int order) {
            super(parent, key, order);
        }

        @Override
        public Iterator<MergedModel> iterator() {
            return value.iterator();
        }

        @Override
        protected MergedModel add(MergedModel node) {
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
    public static class Map extends MergedModel {

        private final java.util.Map<String, MergedModel> value = new HashMap<>();

        /**
         * Create a new instance.
         *
         * @param parent parent
         * @param key    key
         * @param order  order
         */
        Map(MergedModel parent, String key, int order) {
            super(parent, key, order);
        }

        @Override
        public MergedModel get(String key) {
            return value.get(key);
        }

        @Override
        protected MergedModel add(MergedModel node) {
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
                return v.order < node.order ? node : v;
            });
        }
    }

    /**
     * Value node.
     */
    public static class Value extends MergedModel {

        private final String value;
        private final String template;

        /**
         * Create a new instance.
         *
         * @param parent   parent
         * @param key      key
         * @param order    order
         * @param value    value
         * @param template template engine
         */
        Value(MergedModel parent, String key, int order, String value, String template) {
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
