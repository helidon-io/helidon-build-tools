/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

import static io.helidon.build.archetype.engine.v2.context.ContextPath.PARENT_REF;
import static io.helidon.build.archetype.engine.v2.context.ContextPath.PATH_SEPARATOR;
import static io.helidon.build.archetype.engine.v2.context.ContextPath.PATH_SEPARATOR_CHAR;
import static io.helidon.build.archetype.engine.v2.context.ContextPath.ROOT_REF;
import static io.helidon.build.archetype.engine.v2.context.ContextPath.ROOT_REF_CHAR;
import static io.helidon.build.common.PropertyEvaluator.evaluate;

/**
 * Main implementation of {@link ContextScope}.
 */
public final class ContextNode implements ContextScope {

    private final ContextNode root;
    private final ContextNode parent0;
    private final ContextNode parent;
    private final ContextEdge edge;
    private final String id;
    private final Function<ContextNode, ContextEdge> factory;
    private Visibility visibility;

    private ContextNode(Function<ContextNode, ContextEdge> factory) {
        this.root = this;
        this.parent = null;
        this.parent0 = null;
        this.factory = Objects.requireNonNull(factory, "factory is null");
        this.edge = NoValueContextEdge.create(this);
        this.id = null;
    }

    private ContextNode(ContextNode parent0,
                        ContextNode parent,
                        Function<ContextNode, ContextEdge> factory,
                        String id,
                        Visibility visibility) {

        this.parent0 = Objects.requireNonNull(parent0, "parent0 is null");
        this.parent = Objects.requireNonNull(parent, "parent is null");
        this.root = Objects.requireNonNull(parent.root, "root is null");
        this.factory = Objects.requireNonNull(parent.factory, "factory is null");
        if (id == null || id.isEmpty() || id.indexOf(PATH_SEPARATOR_CHAR) >= 0) {
            throw new IllegalArgumentException("Invalid id");
        }
        this.id = id;
        this.visibility = visibility;
        this.edge = factory.apply(this);
    }

    /**
     * Create a new context node.
     *
     * @param parent0    true parent node
     * @param parent     parent node
     * @param factory    edge factory
     * @param id         node id
     * @param visibility visibility
     * @return ContextNode
     */
    public static ContextNode create(ContextNode parent0,
                                     ContextNode parent,
                                     Function<ContextNode, ContextEdge> factory,
                                     String id,
                                     Visibility visibility) {

        return new ContextNode(parent0, parent, factory, id, visibility);
    }

    /**
     * Create a new root context node.
     *
     * @param factory edge factory
     * @return ContextNode
     */
    public static ContextNode create(Function<ContextNode, ContextEdge> factory) {
        return new ContextNode(factory);
    }

    /**
     * Create a new root context node with writeable edges.
     *
     * @return ContextNode
     */
    public static ContextNode create() {
        return new ContextNode(WriteableContextEdge::create);
    }

    /**
     * Get the edge.
     *
     * @return edge
     */
    public ContextEdge edge() {
        return edge;
    }

    @Override
    public ContextNode root() {
        return root;
    }

    /**
     * Get the scope identifier.
     *
     * @return scope id
     */
    public String id() {
        return id;
    }

    @Override
    public void visitEdges(ContextEdge.Visitor visitor, boolean visitVariations) {
        ContextNode parent = null;
        Deque<ContextNode> stack = new ArrayDeque<>();
        stack.push(this);
        while (!stack.isEmpty()) {
            ContextNode node = stack.peek();
            List<ContextNode> children = node.edge.children();
            if (children.isEmpty()) {
                visitor.visit(node.edge);
                visitor.postVisit(node.edge);
                stack.pop();
                parent = node.parent0;
            } else {
                if (parent != null && node == parent) {
                    visitor.postVisit(node.edge);
                    stack.pop();
                    parent = node.parent0;
                } else {
                    visitor.visit(node.edge);
                    if (!children.isEmpty()) {
                        ListIterator<ContextNode> it = children.listIterator(children.size());
                        while (it.hasPrevious()) {
                            ContextNode previous = it.previous();
                            stack.push(previous);
                        }
                    } else {
                        parent = node.parent0;
                    }
                    continue;
                }
            }
            if (visitVariations) {
                // add variations post visit
                List<? extends ContextEdge> variations = node.edge.variations();
                if (variations.size() > 1 && node.edge == variations.get(0)) {
                    ListIterator<? extends ContextEdge> it = variations.listIterator(variations.size());
                    while (it.previousIndex() > 0) {
                        ContextEdge previous = it.previous();
                        stack.push(previous.node());
                    }
                }
            }
        }
    }

    @Override
    public ContextNode parent() {
        return parent;
    }

    @Override
    public ContextNode parent0() {
        return parent0;
    }

    @Override
    public Visibility visibility() {
        return visibility;
    }

    private void copy(ContextNode target) {
        Deque<ContextNode> stack = new ArrayDeque<>();
        Deque<ContextNode> copyStack = new ArrayDeque<>();
        ContextValue value = edge.value();
        if (value != null) {
            target.edge.value(value.value(), value.kind());
        }
        for (ContextNode child : edge.children()) {
            stack.push(child);
            copyStack.push(target);
        }
        while (!stack.isEmpty()) {
            ContextNode src = stack.pop();
            ContextNode parent = copyStack.pop();
            ContextEdge parentEdge = parent.edge();
            ContextNode copy;
            copy = new ContextNode(parent, parent, src.factory, src.id, src.visibility);
            value = src.edge.value();
            if (value != null) {
                copy.edge.value(value.value(), value.kind());
            }
            parentEdge.children().add(copy);
            for (ContextNode contextScope : src.edge.children()) {
                copyStack.push(copy);
                stack.push(contextScope);
            }
        }
    }

    private void updateVisibility(Visibility visibility) {
        if (this.visibility != visibility && visibility != Visibility.UNSET) {
            if (this.visibility == Visibility.UNSET) {
                this.visibility = visibility;
            } else {
                throw new IllegalStateException(String.format(
                        "Visibility mismatch, id=%s, current=%s, requested=%s",
                        id, this.visibility, visibility));
            }
        }
    }

    @Override
    public ContextNode getOrCreate(String path, Visibility visibility) {
        if (path.indexOf(PATH_SEPARATOR_CHAR) >= 0 || path.indexOf(ROOT_REF_CHAR) >= 0) {
            throw new IllegalArgumentException("Invalid id");
        }
        ContextNode node = find(path);
        if (node != null) {
            node.updateVisibility(visibility);
            return node;
        }
        node = new ContextNode(this, this, factory, path, visibility);
        if (this != root) {
            if (visibility == Visibility.GLOBAL || this.visibility == Visibility.GLOBAL) {
                ContextNode existing = root.find(path);
                if (existing != null) {
                    node.updateVisibility(existing.visibility);
                    existing.copy(node);
                    existing.parent.edge.children().remove(existing);
                }
            }
        }
        edge.children().add(node);
        return node;
    }

    @Override
    public void clear() {
        edge.clear();
    }

    @Override
    public ContextValue putValue(String path, Value value, ValueKind kind) {
        String[] segments = ContextPath.parse(path);
        ContextNode node = resolve(segments, (s, sid) -> s.getOrCreate(sid, Visibility.UNSET));
        if (node == null) {
            throw new IllegalStateException("Unresolved node: " + path);
        }
        return node.edge.value(value, kind);
    }

    @Override
    public ContextValue getValue(String path) {
        ContextNode node = resolve(path);
        if (node != null) {
            return node.edge.value();
        }
        return null;
    }

    @Override
    public ContextNode resolve(String path) {
        String[] segments = ContextPath.parse(path);
        return resolve(segments, ContextNode::find);
    }

    @Override
    public String path(boolean internal) {
        StringBuilder resolved = new StringBuilder();
        ContextNode node = this;
        while (node.parent != null) {
            if (resolved.length() == 0) {
                resolved.append(node.id);
            } else {
                resolved.insert(0, node.id + PATH_SEPARATOR);
            }
            if (!internal
                    && (node.visibility == Visibility.GLOBAL
                    || node.parent.visibility == Visibility.GLOBAL)) {
                break;
            }
            node = node.parent;
        }
        return resolved.toString();
    }

    @Override
    public String interpolate(String value) {
        if (value == null) {
            return null;
        }
        String input = null;
        String output = value;
        while (!output.equals(input)) {
            input = output;
            output = evaluate(output, var -> {
                Value val = getValue(var);
                if (val == null) {
                    throw new IllegalArgumentException("Unresolved variable: " + var);
                }
                return String.valueOf(val.unwrap());
            });
        }
        return output;
    }

    @Override
    public String toString() {
        return "ContextNode{"
                + "id='" + id + '\''
                + ", visibility=" + visibility
                + '}';
    }

    private ContextNode resolve(String[] segments, BiFunction<ContextNode, String, ContextNode> fn) {
        ContextNode node = this;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i == 0 && ROOT_REF.equals(segment)) {
                node = root;
            } else if (PARENT_REF.equals(segment)) {
                node = i == 0 ? parent : node.parent;
                if (node == null) {
                    //it was a root node
                    node = root;
                }
            } else {
                node = fn.apply(node, segment);
                if (node == null) {
                    if (this != root) {
                        return root.resolve(segments, fn);
                    }
                    return null;
                }
            }
        }
        return node;
    }

    private ContextNode find(String id) {
        if (id.equals(this.id)) {
            return this;
        }
        Deque<ContextNode> stack = new ArrayDeque<>(edge.children());
        while (!stack.isEmpty()) {
            ContextNode node = stack.pop();
            if (node.id.equals(id)) {
                return node;
            }
            if (node.visibility == Visibility.GLOBAL) {
                stack.addAll(node.edge().children());
            }
        }
        return null;
    }
}
