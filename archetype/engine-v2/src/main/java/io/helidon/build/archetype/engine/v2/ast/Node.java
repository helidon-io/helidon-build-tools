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

package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * AST node.
 */
public abstract class Node {

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id;
    private final Path scriptPath;
    private final Position position;

    /**
     * Create a new node.
     *
     * @param builder builder
     */
    protected Node(Builder<?, ?> builder) {
        this(builder.scriptPath, builder.position);
    }

    /**
     * Create a new node.
     *
     * @param scriptPath scriptPath
     * @param position   position
     */
    protected Node(Path scriptPath, Position position) {
        this.scriptPath = requireNonNull(scriptPath, "source is null");
        this.position = requireNonNull(position, "position is null");
        this.id = NEXT_ID.updateAndGet(i -> i == Integer.MAX_VALUE ? 1 : i + 1);
    }

    /**
     * Get the source position.
     *
     * @return position
     */
    public Position position() {
        return position;
    }

    /**
     * Get the script path.
     *
     * @return script path
     */
    public Path scriptPath() {
        return scriptPath;
    }

    /**
     * Get the node id.
     *
     * @return id
     */
    public int nodeId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Visit this node.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public abstract <A> VisitResult accept(Visitor<A> visitor, A arg);

    /**
     * Post-visit this node.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public <A> VisitResult acceptAfter(Visitor<A> visitor, A arg) {
        return VisitResult.CONTINUE;
    }

    /**
     * Visit result.
     */
    public enum VisitResult {

        /**
         * Continue.
         */
        CONTINUE,

        /**
         * Terminate.
         */
        TERMINATE,

        /**
         * Continue without visiting the children.
         */
        SKIP_SUBTREE,

        /**
         * Continue without visiting the siblings.
         */
        SKIP_SIBLINGS
    }

    /**
     * Visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a condition.
         *
         * @param condition condition
         * @param arg       visitor argument
         * @return visit result
         */
        default VisitResult visitCondition(Condition condition, A arg) {
            return visitAny(condition, arg);
        }

        /**
         * Visit an invocation.
         *
         * @param invocation invocation
         * @param arg        visitor argument
         * @return visit result
         */
        default VisitResult visitInvocation(Invocation invocation, A arg) {
            return visitAny(invocation, arg);
        }

        /**
         * Visit a preset.
         *
         * @param preset preset
         * @param arg    visitor argument
         * @return visit result
         */
        default VisitResult visitPreset(Preset preset, A arg) {
            return visitAny(preset, arg);
        }

        /**
         * Visit a block.
         *
         * @param block block
         * @param arg   visitor argument
         * @return visit result
         */
        default VisitResult visitBlock(Block block, A arg) {
            return visitAny(block, arg);
        }

        /**
         * Visit a block after traversing the nested nodes.
         *
         * @param block block
         * @param arg   visitor argument
         * @return visit result
         */
        default VisitResult postVisitBlock(Block block, A arg) {
            return postVisitAny(block, arg);
        }

        /**
         * Visit a node.
         *
         * @param node node
         * @param arg  visitor argument
         * @return visit result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Node node, A arg) {
            return VisitResult.CONTINUE;
        }

        /**
         * Visit a node after traversing the nested nodes.
         *
         * @param node node
         * @param arg  visitor argument
         * @return visit result
         */
        @SuppressWarnings("unused")
        default VisitResult postVisitAny(Node node, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Remove builders from the given list of node builders.
     *
     * @param children list of node builders
     * @param type     class used to match the builders to remove
     * @param function function invoked to control removal
     */
    static <T> void remove(List<Builder<? extends Node, ?>> children,
                           Class<T> type,
                           Function<T, Boolean> function) {

        Iterator<Builder<? extends Node, ?>> it = children.iterator();
        while (it.hasNext()) {
            Node.Builder<?, ?> b = it.next();
            if (type.isInstance(b)) {
                T tb = type.cast(b);
                if (function.apply(tb)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Node builder.
     *
     * @param <T> node sub-type
     * @param <U> builder sub-type
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T, U extends Builder<T, U>> {

        private static final Path NULL_SCRIPT_PATH = Path.of("script.xml");
        private static final Position NULL_SOURCE = Position.of(0, 0);

        private final List<Node.Builder<? extends Node, ?>> children = new LinkedList<>();
        private final Map<String, String> attributes = new HashMap<>();
        private final Path scriptPath;
        private final Position position;
        private T instance;

        /**
         * Get the children.
         *
         * @return children
         */
        List<Builder<? extends Node, ?>> children() {
            return children;
        }

        /**
         * Get the script path.
         *
         * @return script path
         */
        Path scriptPath() {
            return scriptPath;
        }

        /**
         * Get the position.
         *
         * @return position
         */
        Position position() {
            return position;
        }

        /**
         * Get the attributes map.
         *
         * @return attributes map
         */
        Map<String, String> attributes() {
            return attributes;
        }

        /**
         * Create a new node builder.
         *
         * @param scriptPath scriptPath
         * @param position   position
         */
        protected Builder(Path scriptPath, Position position) {
            this.scriptPath = scriptPath == null ? NULL_SCRIPT_PATH : scriptPath.normalize();
            this.position = position == null ? NULL_SOURCE : position;
        }

        /**
         * Add a child.
         *
         * @param builder node builder
         * @return this builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public U addChild(Node.Builder<? extends Node, ?> builder) {
            children.add(builder);
            return (U) this;
        }

        /**
         * Set the value.
         *
         * @param value value
         * @return this builder
         */
        public U value(String value) {
            throw new UnsupportedOperationException("Unable to add value to " + getClass().getName());
        }

        /**
         * Add attributes.
         *
         * @param attributes attributes
         * @return this builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public U attributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return (U) this;
        }

        /**
         * Get a required attribute.
         *
         * @param key attribute key
         * @return value
         */
        String attribute(String key) {
            String value = attributes.get(key);
            if (value == null) {
                throw new IllegalStateException(String.format(
                        "Unable to get attribute '%s', file=%s, position=%s",
                        key, scriptPath, position));
            }
            return value;
        }

        /**
         * Create the new instance.
         *
         * @return new instance
         */
        protected abstract T doBuild();

        /**
         * Get or create the new instance.
         *
         * @return new instance
         */
        public T build() {
            if (instance == null) {
                instance = doBuild();
            }
            return instance;
        }
    }
}
