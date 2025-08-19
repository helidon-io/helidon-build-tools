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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import io.helidon.build.common.Maps;

import static java.lang.System.identityHashCode;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * AST node.
 */
public interface Node {

    /**
     * Get the source location.
     *
     * @return location
     */
    Location location();

    /**
     * Get the script.
     *
     * @return script
     */
    Script script();

    /**
     * Set the script.
     *
     * @param script script
     * @return this node
     */
    Node script(Script script);

    /**
     * Get the invocation kind.
     *
     * @return kind
     */
    Kind kind();

    /**
     * Get the attributes.
     *
     * @return attributes map
     */
    Map<String, Value<?>> attributes();

    /**
     * Get an attribute.
     *
     * @param name name
     * @return value, never {@code null}
     */
    default Value<?> attribute(String name) {
        Value<?> value = attributes().get(name);
        if (value == null) {
            return Value.empty(() -> new NoSuchElementException("No such attribute: " + name));
        }
        return value;
    }

    /**
     * Add bulk attributes.
     *
     * @param attributes attributes
     * @return this node
     */
    default Node attributes(Map<String, Value<?>> attributes) {
        attributes().putAll(attributes);
        return this;
    }

    /**
     * Add an attribute.
     *
     * @param name  attribute name
     * @param value attribute value
     * @return this node
     */
    Node attribute(String name, String value);

    /**
     * Add an attribute.
     *
     * @param name  attribute name
     * @param value attribute value
     * @return this node
     */
    Node attribute(String name, List<String> value);

    /**
     * Get the parent.
     *
     * @return parent or {@code null} if root node.
     */
    Node parent();

    /**
     * Unwrap this node.
     *
     * @return Node
     */
    default Node unwrap() {
        if (kind() == Kind.CONDITION) {
            return firstChild().orElseThrow();
        }
        return this;
    }

    /**
     * Wrap this node.
     *
     * @param expr expression
     * @return Node
     */
    default Node wrap(Expression expr) {
        if (expr != Expression.TRUE) {
            Node parent = parent();
            return Nodes.parent(Nodes.condition(expr, this), parent);
        }
        return this;
    }

    /**
     * Set the parent.
     *
     * @param parent parent
     * @return this node
     */
    Node parent(Node parent);

    /**
     * Get the value.
     *
     * @return value
     */
    Value<?> value();

    /**
     * Set the value.
     *
     * @param value value
     * @return this node
     */
    Node value(String value);

    /**
     * Set the value.
     *
     * @param value value
     * @return this node
     */
    Node value(Value<?> value);


    /**
     * Get the node id.
     *
     * @return id
     */
    int id();

    /**
     * Set the node id.
     *
     * @param id id
     */
    void id(int id);

    /**
     * Get the children.
     *
     * @return children
     */
    List<Node> children();

    /**
     * Get an iterable of children.
     *
     * @param predicate predicate
     * @return Iterable
     */
    default Iterable<Node> children(Predicate<Kind> predicate) {
        return () -> new Iterator<>() {
            private int index = 0;
            private Node next;

            @Override
            public boolean hasNext() {
                while (next == null && index < children().size()) {
                    Node child = children().get(index++);
                    if (predicate.test(child.kind())) {
                        next = child;
                    }
                }
                return next != null;
            }

            @Override
            public Node next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Node node = next;
                next = null;
                return node;
            }
        };
    }

    /**
     * Append a child.
     *
     * @param child child
     * @return this node
     */
    default Node append(Node child) {
        children().add(Nodes.parent(child, this));
        return this;
    }

    /**
     * Append a child.
     *
     * @param index index
     * @param child child
     * @return this node
     */
    default Node append(int index, Node child) {
        children().add(index, Nodes.parent(child, this));
        return this;
    }

    /**
     * Get the node index.
     *
     * @return index, or {@code -1} if not found
     */
    default int index() {
        Node parent = parent();
        if (parent != null) {
            List<Node> siblings = parent.children();
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i) == this) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Collect all nodes in depth first order.
     *
     * @return nodes
     */
    default List<Node> collect() {
        return collect(n -> true);
    }

    /**
     * Collect all nodes in depth first order.
     *
     * @param predicate predicate
     * @return nodes
     */
    default List<Node> collect(Predicate<Kind> predicate) {
        List<Node> nodes = new ArrayList<>();
        for (Node node : traverse()) {
            if (predicate.test(node.kind())) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Get the first child.
     *
     * @return Optional
     */
    default Optional<Node> firstChild() {
        List<Node> children = children();
        return children.isEmpty() ? Optional.empty() : Optional.of(children.get(0));
    }

    /**
     * Get the last child.
     *
     * @param predicate predicate
     * @return Optional
     */
    default Optional<Node> lastChild(Predicate<Kind> predicate) {
        List<Node> children = children();
        if (children.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(children.get(children.size() - 1)).filter(n -> predicate.test(n.kind()));
    }

    /**
     * Create a deep copy of this node.
     *
     * @return Node
     */
    default Node deepCopy() {
        return visit(new CopyVisitor()).last;
    }

    /**
     * Create a shallow copy of this node.
     *
     * @return Node
     */
    default Node copy() {
        return Node.builder(kind())
                .parent(parent())
                .script(script())
                .location(location())
                .value(value())
                .expression(expression())
                .attributes(attributes());
    }

    /**
     * Get the expression.
     *
     * @return expression
     */
    Expression expression();

    /**
     * Set the expression.
     *
     * @param expression expression
     * @return this node
     */
    Node expression(Expression expression);

    /**
     * Set the expression.
     *
     * @param expression expression
     * @return this node
     */
    Node expression(String expression);

    /**
     * Remove this node.
     */
    void remove();

    /**
     * Replace this node with the given node.
     *
     * @param node node
     */
    void replace(Node node);

    /**
     * Replace this node with the given nodes.
     *
     * @param nodes nodes
     */
    void replace(List<Node> nodes);

    /**
     * Traverse this node.
     *
     * @param visitor visitor
     * @param <T>     visitor type
     * @return visitor
     */
    default <T extends Visitor> T visit(T visitor) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(this);
        Node parent = parent();
        while (!stack.isEmpty()) {
            Node node = stack.peek();
            if (node == parent) {
                visitor.postVisit(node);
                parent = node.parent();
                stack.pop();
            } else {
                if (visitor.visit(node)) {
                    List<Node> children = node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
                if (parent != node.parent()) {
                    throw new IllegalStateException("Parent mismatch");
                }
                parent = node;
            }
        }
        return visitor;
    }

    /**
     * Traverse this node.
     *
     * @return Iterable
     */
    default Iterable<Node> traverse() {
        return traverse(n -> true);
    }

    /**
     * Traverse this node.
     *
     * @param predicate predicate
     * @return Iterable
     */
    default Iterable<Node> traverse(Predicate<Kind> predicate) {
        Node self = this;
        return () -> {
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(self);
            return new Iterator<>() {
                private Node next;

                @Override
                public boolean hasNext() {
                    while (next == null && !stack.isEmpty()) {
                        Node node = stack.pop();
                        List<Node> children = node.children();
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(children.get(i));
                        }
                        if (predicate.test(node.kind())) {
                            next = node;
                        }
                    }
                    return next != null;
                }

                @Override
                public Node next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    Node node = next;
                    next = null;
                    return node;
                }
            };
        };
    }

    /**
     * Get an ancestor.
     *
     * @param predicate predicate
     * @return Optional
     */
    default Optional<Node> ancestor(Predicate<Kind> predicate) {
        for (Node node : ancestors(predicate)) {
            return Optional.of(node);
        }
        return Optional.empty();
    }

    /**
     * Iterate over the ancestors.
     *
     * @return Iterable
     */
    @SuppressWarnings("unused")
    default Iterable<Node> ancestors() {
        return ancestors(n -> true);
    }

    /**
     * Iterate over the ancestors.
     *
     * @param predicate predicate
     * @return Iterable
     */
    default Iterable<Node> ancestors(Predicate<Kind> predicate) {
        Node self = parent();
        return () -> new Iterator<>() {
            private Node next = self;

            @Override
            public boolean hasNext() {
                while (next != null && !predicate.test(next.kind())) {
                    next = next.parent();
                }
                return next != null;
            }

            @Override
            public Node next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Node node = next;
                next = next.parent();
                return node;
            }
        };
    }

    /**
     * Create a new builder.
     *
     * @param kind kind
     * @return builder
     */
    static Builder builder(Kind kind) {
        return new Builder(kind);
    }

    /**
     * Node kind.
     */
    enum Kind {
        /**
         * Condition.
         */
        CONDITION,

        /**
         * Exec.
         */
        EXEC,

        /**
         * Source.
         */
        SOURCE,

        /**
         * Method call.
         */
        CALL,

        /**
         * Script.
         */
        SCRIPT,

        /**
         * Method.
         */
        METHOD,

        /**
         * Step.
         */
        STEP,

        /**
         * Inputs.
         */
        INPUTS,

        /**
         * Option input.
         */
        INPUT_OPTION,

        /**
         * Text input.
         */
        INPUT_TEXT,

        /**
         * Boolean input.
         */
        INPUT_BOOLEAN,

        /**
         * Enum input.
         */
        INPUT_ENUM,

        /**
         * List input.
         */
        INPUT_LIST,

        /**
         * Presets.
         */
        PRESETS,

        /**
         * Text preset.
         */
        PRESET_TEXT,

        /**
         * Boolean preset.
         */
        PRESET_BOOLEAN,

        /**
         * Enum preset.
         */
        PRESET_ENUM,

        /**
         * List preset.
         */
        PRESET_LIST,

        /**
         * Variables.
         */
        VARIABLES,

        /**
         * Text variable.
         */
        VARIABLE_TEXT,

        /**
         * Boolean variable.
         */
        VARIABLE_BOOLEAN,

        /**
         * Enum variable.
         */
        VARIABLE_ENUM,

        /**
         * List variable.
         */
        VARIABLE_LIST,

        /**
         * Output.
         */
        OUTPUT,

        /**
         * Templates.
         */
        TEMPLATES,

        /**
         * Template.
         */
        TEMPLATE,

        /**
         * Files.
         */
        FILES,

        /**
         * File.
         */
        FILE,

        /**
         * Model.
         */
        MODEL,

        /**
         * List model.
         */
        MODEL_LIST,

        /**
         * Map model.
         */
        MODEL_MAP,

        /**
         * Value model.
         */
        MODEL_VALUE,

        /**
         * Transformation.
         */
        TRANSFORMATION,

        /**
         * Regular expression.
         */
        REGEX,

        /**
         * Validation.
         */
        VALIDATION,

        /**
         * Validations.
         */
        VALIDATIONS,

        /**
         * REPLACE.
         */
        REPLACE,

        /**
         * Includes.
         */
        INCLUDES,

        /**
         * Include.
         */
        INCLUDE,

        /**
         * Excludes.
         */
        EXCLUDES,

        /**
         * Exclude.
         */
        EXCLUDE,

        /**
         * Unknown.
         */
        UNKNOWN;

        /**
         * Get the token.
         *
         * @return token
         */
        public String token() {
            String token = name().toLowerCase();
            int index = token.indexOf('_');
            if (index != -1) {
                return token.substring(index + 1);
            }
            return token;
        }

        /**
         * Test if this kind is an input.
         *
         * @return {@code true} if an input, {@code false} otherwise
         */
        public boolean isInput() {
            switch (this) {
                case INPUT_BOOLEAN:
                case INPUT_TEXT:
                case INPUT_ENUM:
                case INPUT_LIST:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test if this kind is a preset.
         *
         * @return {@code true} if a preset, {@code false} otherwise
         */
        public boolean isPreset() {
            switch (this) {
                case PRESET_BOOLEAN:
                case PRESET_ENUM:
                case PRESET_LIST:
                case PRESET_TEXT:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test if this kind is a variable.
         *
         * @return {@code true} if a variable, {@code false} otherwise
         */
        public boolean isVariable() {
            switch (this) {
                case VARIABLE_BOOLEAN:
                case VARIABLE_ENUM:
                case VARIABLE_LIST:
                case VARIABLE_TEXT:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test if this kind is a model.
         *
         * @return {@code true} if a model, {@code false} otherwise
         */
        public boolean isModel() {
            switch (this) {
                case MODEL_VALUE:
                case MODEL_LIST:
                case MODEL_MAP:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test if this kind is a block.
         *
         * @return {@code true} if a block, {@code false} otherwise
         */
        public boolean isBlock() {
            switch (this) {
                case SCRIPT:
                case STEP:
                case INPUT_BOOLEAN:
                case INPUT_OPTION:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test if this kind is any of the given kinds.
         *
         * @param kinds kinds to tests against
         * @return {@code true} if this kind is any of the given kinds, {@code false} otherwise
         */
        public boolean is(Kind... kinds) {
            for (Kind kind : kinds) {
                if (this.equals(kind)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the value type for a node kind.
         *
         * @return type
         */
        public Value.Type valueType() {
            switch (this) {
                case INPUT_ENUM:
                case INPUT_TEXT:
                case INPUT_OPTION:
                case PRESET_TEXT:
                case PRESET_ENUM:
                case VARIABLE_TEXT:
                case VARIABLE_ENUM:
                    return Value.Type.STRING;
                case INPUT_BOOLEAN:
                case PRESET_BOOLEAN:
                case VARIABLE_BOOLEAN:
                    return Value.Type.BOOLEAN;
                case INPUT_LIST:
                case PRESET_LIST:
                case VARIABLE_LIST:
                    return Value.Type.LIST;
                default:
                    throw new IllegalStateException("Not a valid value type: " + this);
            }
        }
    }

    /**
     * Visitor.
     */
    interface Visitor {

        /**
         * Get a no-op visitor.
         *
         * @return visitor
         */
        static Visitor empty() {
            return node -> true;
        }

        /**
         * Visit a node.
         *
         * @param node node
         * @return {@code true} to keep traversing, {@code false} to stop
         */
        boolean visit(Node node);

        /**
         * Visit a node after traversing the nested nodes.
         *
         * @param node node
         */
        default void postVisit(Node node) {
        }
    }

    /**
     * Source location.
     */
    final class Location {

        /**
         * Unknown location.
         */
        public static final Location UNKNOWN = new Location("[unknown]", 0, 0);

        private final String fileName;
        private final int lineNo;
        private final int colNo;

        /**
         * Create a new instance.
         *
         * @param fileName file name
         * @param lineNo   line number
         * @param colNo    column number
         */
        public Location(String fileName, int lineNo, int colNo) {
            this.fileName = Objects.requireNonNull(fileName, "fileName is null");
            if (lineNo < 0) {
                throw new IllegalArgumentException("Invalid line number: " + lineNo);
            }
            this.lineNo = lineNo;
            if (colNo < 0) {
                throw new IllegalArgumentException("Invalid line character number: " + colNo);
            }
            this.colNo = colNo;
        }

        /**
         * Get the file name.
         *
         * @return file name
         */
        public String fileName() {
            return fileName;
        }

        /**
         * Get the line number.
         *
         * @return line number
         */
        public int lineNumber() {
            return lineNo;
        }

        @Override
        public String toString() {
            return fileName + ":" + lineNo + ":" + colNo;
        }

        /**
         * Make a copy.
         *
         * @return copy
         */
        public Location copy() {
            return new Location(fileName, lineNo, colNo);
        }
    }

    /**
     * Node builder.
     */
    class Builder implements Node {

        private Builder parent;
        private Script script = Script.EMPTY;
        private Location location = Location.UNKNOWN;
        private final Kind kind;
        private final Map<String, Value<?>> attributes = new LinkedHashMap<>();
        private final List<Node> children = new ArrayList<>();
        private Value<?> value = Value.empty();
        private Expression expression = Expression.TRUE;
        private int id = -1;

        /**
         * Create a new builder.
         *
         * @param kind kind
         */
        protected Builder(Kind kind) {
            this.kind = kind;
        }

        @Override
        public Builder parent() {
            return parent;
        }

        @Override
        public Builder parent(Node parent) {
            if (parent instanceof Builder) {
                this.parent = (Builder) parent;
            }
            return this;
        }

        @Override
        public Script script() {
            return script;
        }

        @Override
        public Builder script(Script script) {
            if (script != null) {
                this.script = script;
            }
            return this;
        }

        @Override
        public Location location() {
            return location;
        }

        /**
         * Set the location.
         *
         * @param location location
         * @return this builder
         */
        public Builder location(Location location) {
            if (location != null) {
                this.location = location;
            }
            return this;
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public Map<String, Value<?>> attributes() {
            return attributes;
        }

        @Override
        public Builder attribute(String name, String value) {
            if (value != null && isAttribute(name)) {
                attributes().put(name, Value.dynamic(value));
            }
            return this;
        }

        @Override
        public Builder attribute(String name, List<String> value) {
            if (value != null && isAttribute(name)) {
                attributes().put(name, Value.of(value));
            }
            return this;
        }

        @Override
        public Value<?> value() {
            return value;
        }

        @Override
        public Builder value(String value) {
            if (value != null) {
                this.value = Value.dynamic(value);
            }
            return this;
        }

        @Override
        public Builder value(Value<?> value) {
            if (value != null) {
                this.value = value;
            }
            return this;
        }

        @Override
        public Expression expression() {
            return expression;
        }

        @Override
        public Node expression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public Builder expression(String expression) {
            this.expression = Expression.create(expression);
            return this;
        }

        @Override
        public List<Node> children() {
            return children;
        }

        @Override
        public void remove() {
            if (parent != null) {
                parent.children.remove(index());
                if (parent.kind == Kind.CONDITION && parent.children.isEmpty()) {
                    if (parent.parent != null) {
                        parent.parent.children.remove(parent.index());
                    }
                }
            }
        }

        @Override
        public void replace(Node node) {
            if (parent != null) {
                Nodes.parent(node, parent);
                List<Node> siblings = parent.children();
                int index = index();
                if (index >= 0) {
                    siblings.set(index, node);
                } else {
                    siblings.add(node);
                }
            }
        }

        @Override
        public void replace(List<Node> nodes) {
            if (parent != null) {
                List<Node> siblings = parent.children();
                int index = siblings.indexOf(this);
                if (index >= 0) {
                    siblings.remove(index);
                    siblings.addAll(index, nodes);
                } else {
                    siblings.addAll(nodes);
                }
                for (Node child : nodes) {
                    child.parent(parent);
                    child.script(script);
                }
                nodes.clear();
            }
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public void id(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) {
                return false;
            }
            Node other = (Node) o;
            return kind == other.kind()
                   && Maps.equals(attributes(), other.attributes(), Value::isEqual)
                   && Objects.equals(expression, other.expression())
                   && Value.isEqual(value, other.value());
        }

        @Override
        public int hashCode() {
            return identityHashCode(this);
        }

        @Override
        public String toString() {
            return "Node.Builder{"
                   + "location=" + location
                   + ", kind=" + kind
                   + ", attributes=" + attributes
                   + ", value=" + value
                   + '}';
        }

        /**
         * Create the new instance.
         *
         * @return new instance
         */
        public Node build() {
            return new Node.NodeImpl(null, this);
        }

        private static boolean isAttribute(String name) {
            switch (name) {
                case "if":
                case "value":
                    return false;
                default:
                    return true;
            }
        }
    }

    /**
     * Read-only node.
     */
    class NodeImpl implements Node {

        private final Node parent;
        private final Location location;
        private final Kind kind;
        private final Map<String, Value<?>> attributes;
        private final Expression expression;
        private final Value<?> value;
        private final List<Node> children;
        private final Script script;

        NodeImpl(Node parent, Builder builder) {
            this.parent = parent;
            this.kind = builder.kind();
            this.attributes = unmodifiableMap(builder.attributes());
            this.expression = builder.expression();
            this.value = builder.value();
            this.children = unmodifiableList(builder.children());
            this.location = builder.location;
            this.script = builder.script;
        }

        @Override
        public Script script() {
            return script;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public Node parent() {
            return parent;
        }

        @Override
        public Map<String, Value<?>> attributes() {
            return attributes;
        }

        @Override
        public List<Node> children() {
            return children;
        }

        @Override
        public Value<?> value() {
            return value;
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public Expression expression() {
            return expression;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) {
                return false;
            }
            Node other = (Node) o;
            return kind == other.kind()
                   && Maps.equals(attributes(), other.attributes(), Value::isEqual)
                   && Objects.equals(expression, other.expression())
                   && Value.isEqual(value, other.value());
        }

        @Override
        public int hashCode() {
            return identityHashCode(this);
        }

        @Override
        public String toString() {
            return "NodeImpl{"
                   + "location=" + location
                   + ", kind=" + kind
                   + ", attributes=" + attributes
                   + ", value=" + value
                   + '}';
        }

        @Override
        public Node script(Script script) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node parent(Node parent) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node attribute(String name, String value) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node attribute(String name, List<String> value) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node expression(Expression expression) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node expression(String expression) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node value(String value) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public Node value(Value<?> value) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public void replace(Node node) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public void replace(List<Node> nodes) {
            throw new UnsupportedOperationException("Node is read-only");
        }

        @Override
        public int id() {
            throw new UnsupportedOperationException("id not supported");
        }

        @Override
        public void id(int id) {
            throw new UnsupportedOperationException("Node is read-only");
        }
    }

    /**
     * Copy visitor.
     */
    final class CopyVisitor implements Visitor {
        private Node node;
        private Node last;

        @Override
        public boolean visit(Node n) {
            Node copy = n.copy();
            if (node != null) {
                node.append(copy);
            }
            node = copy;
            return true;
        }

        @Override
        public void postVisit(Node n) {
            if (node != null) {
                last = node;
                node = node.parent();
            }
        }
    }
}
