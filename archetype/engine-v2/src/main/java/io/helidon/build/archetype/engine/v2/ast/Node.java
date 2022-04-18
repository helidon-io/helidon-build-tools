/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.common.GenericType;

import static java.util.Objects.requireNonNull;

/**
 * AST node.
 */
public abstract class Node {

    // TODO get the id from the script loader
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id;
    private final ScriptLoader loader;
    private final Path scriptPath;
    private final Location location;
    private final Map<String, Value> attributes;

    /**
     * Create a new node.
     *
     * @param builder builder
     */
    protected Node(Builder<?, ?> builder) {
        this(builder.info, builder.attributes);
    }

    /**
     * Create a new node.
     *
     * @param info       builder info
     * @param attributes attributes map
     */
    protected Node(BuilderInfo info, Map<String, Value> attributes) {
        this.loader = info.loader;
        this.scriptPath = info.scriptPath;
        this.location = info.location;
        this.attributes = requireNonNull(attributes, "attributes is null");
        this.id = NEXT_ID.updateAndGet(i -> i == Integer.MAX_VALUE ? 1 : i + 1);
    }

    /**
     * Get the source location.
     *
     * @return location
     */
    public Location location() {
        return location;
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
     * Get the script loader.
     *
     * @return script loader
     */
    public ScriptLoader loader() {
        return loader;
    }

    /**
     * Get the attributes.
     *
     * @return attributes map
     */
    public Map<String, Value> attributes() {
        return attributes;
    }

    /**
     * Get the enclosing script.
     *
     * @return script
     */
    public Script script() {
        return loader.get(scriptPath);
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
        //  TODO use both id and loader
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
     * Builder info.
     */
    public static final class BuilderInfo {

        private final ScriptLoader loader;
        private final Path scriptPath;
        private final Location location;

        private BuilderInfo(ScriptLoader loader, Path scriptPath, Location location) {
            this.loader = requireNonNull(loader, "loader is null");
            this.scriptPath = requireNonNull(scriptPath, "scriptPath is null").toAbsolutePath().normalize();
            this.location = location == null ? Location.of(scriptPath, 0, 0) : location;
        }

        /**
         * Create a new builder info instance.
         *
         * @param loader     script loader
         * @param scriptPath script path
         * @return builder info
         */
        public static BuilderInfo of(ScriptLoader loader, Path scriptPath) {
            return new BuilderInfo(loader, scriptPath, null);
        }

        /**
         * Create a new builder info instance.
         *
         * @param loader     script loader
         * @param scriptPath script path
         * @param location   location
         * @return builder info
         */
        public static BuilderInfo of(ScriptLoader loader, Path scriptPath, Location location) {
            return new BuilderInfo(loader, scriptPath, location);
        }

        /**
         * Create a new builder info instance.
         *
         * @param node node
         * @return builder info
         */
        public static BuilderInfo of(Node node) {
            return new BuilderInfo(node.loader, node.scriptPath, node.location);
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

        private final List<Node.Builder<? extends Node, ?>> children = new LinkedList<>();
        private final Map<String, Value> attributes = new HashMap<>();
        private final BuilderInfo info;
        private String value;
        private T instance;

        /**
         * Create a new node builder.
         *
         * @param info builder info
         */
        protected Builder(BuilderInfo info) {
            this.info = requireNonNull(info, "info is null");
        }

        /**
         * Get the builder info.
         *
         * @return builder info
         */
        public BuilderInfo info() {
            return info;
        }

        /**
         * Get the nested builders.
         *
         * @return builders
         */
        public List<Builder<? extends Node, ?>> nestedBuilders() {
            return children;
        }

        /**
         * Get the children.
         *
         * @return children
         */
        public List<Node> children() {
            return children.stream()
                           .map(Node.Builder::build)
                           .collect(Collectors.toUnmodifiableList());
        }

        /**
         * Get the children of a given type.
         *
         * @param clazz type
         * @param <V>   children type
         * @return children
         */
        public <V> List<V> children(Class<V> clazz) {
            return childrenStream(clazz).collect(Collectors.toUnmodifiableList());
        }

        /**
         * Get the children of a given type.
         *
         * @param clazz type
         * @param <V>   children type
         * @return children
         */
        public <V> Stream<V> childrenStream(Class<V> clazz) {
            return children.stream()
                           .map(Node.Builder::build)
                           .filter(clazz::isInstance)
                           .map(clazz::cast);
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
        public String value() {
            return value;
        }

        /**
         * Add attributes.
         *
         * @param attributes attributes
         * @return this builder
         */
        public U attributes(Map<String, Value> attributes) {
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
        public U attribute(String name, Value value) {
            this.attributes.put(name, value);
            return (U) this;
        }

        /**
         * Get an attribute.
         *
         * @param key      attribute key
         * @param required {@code true} if required
         * @return value
         * @throws IllegalStateException if {@code required} is {@code true} and the value is {@code null}
         */
        Value attribute(String key, boolean required) {
            Value value = attributes.get(key);
            if (value == null) {
                if (required) {
                    throw new IllegalStateException(String.format(
                            "Unable to get attribute '%s', location=%s",
                            key, info.location));
                }
                return Value.NULL;
            }
            return value;
        }

        <V> V attribute(String key, GenericType<V> type, V defaultValue) {
            Value value = attributes.get(key);
            if (value == null) {
                return defaultValue;
            }
            return value.as(type);
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
