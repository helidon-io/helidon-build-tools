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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
     * Node builder.
     *
     * @param <T> node sub-type
     * @param <U> builder sub-type
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public abstract static class Builder<T, U extends Builder<T, U>> {

        private static final Path NULL_SCRIPT_PATH = Path.of("script.xml");
        private static final Position NULL_SOURCE = Position.of(0, 0);

        private final List<Node.Builder<? extends Node, ?>> children = new LinkedList<>();
        private final Map<String, String> attributes = new HashMap<>();
        private final Path scriptPath;
        private final Position position;
        private String value;
        private T instance;

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
         * Get the children.
         *
         * @return children
         */
        protected List<Builder<? extends Node, ?>> children() {
            return children;
        }

        /**
         * Get the script path.
         *
         * @return script path
         */
        protected Path scriptPath() {
            return scriptPath;
        }

        /**
         * Get the position.
         *
         * @return position
         */
        protected Position position() {
            return position;
        }

        /**
         * Get the attributes map.
         *
         * @return attributes map
         */
        protected Map<String, String> attributes() {
            return attributes;
        }

        /**
         * Add a child.
         *
         * @param builder node builder
         * @return this builder
         */
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
            this.value = value;
            return (U) this;
        }

        /**
         * Get the value.
         *
         * @return value.
         */
        protected String value() {
            return value;
        }

        /**
         * Add attributes.
         *
         * @param attributes attributes
         * @return this builder
         */
        public U attributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return (U) this;
        }

        /**
         * Add an attribute.
         *
         * @param name  attribute name
         * @param value attribute value
         * @return this builder
         */
        public U attributes(String name, String value) {
            this.attributes.put(name, value);
            return (U) this;
        }

        /**
         * Get a required attribute.
         *
         * @param key      attribute key
         * @param required {@code true} if required
         * @return value
         * @throws IllegalStateException if {@code required} is {@code true} and the value is {@code null}
         */
        String attribute(String key, boolean required) {
            String value = attributes.get(key);
            if (required && value == null) {
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
