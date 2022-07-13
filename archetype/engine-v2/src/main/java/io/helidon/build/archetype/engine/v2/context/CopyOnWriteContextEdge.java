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
package io.helidon.build.archetype.engine.v2.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

/**
 * An implementation of {@link ContextEdge} that creates an alternative tree when writing a value.
 * <p>
 * A context tree must maintain a single edge to be functional (i.e. a single value for a given path).
 * <p>
 * This implementation allows to maintain "variations", where each variation is an edge that uses a modified copy of
 * the original tree that only sees itself as the edge for the parent node.
 *
 * @see #variations
 */
public final class CopyOnWriteContextEdge implements ContextEdge {

    private ContextValue value;
    private final ContextNode scope;
    private final List<CopyOnWriteContextEdge> variations;
    private final List<ContextNode> children = new ArrayList<>();

    private CopyOnWriteContextEdge(ContextNode scope, List<CopyOnWriteContextEdge> variations) {
        this.scope = scope;
        this.variations = variations;
        variations.add(this);
    }

    /**
     * Create a new virtual edge.
     *
     * @param scope scope
     * @return new instance
     */
    public static CopyOnWriteContextEdge create(ContextNode scope) {
        return new CopyOnWriteContextEdge(scope, new ArrayList<>());
    }

    @Override
    public List<ContextNode> children() {
        return children;
    }

    @Override
    public ContextValue value() {
        return value;
    }

    @Override
    public ContextNode node() {
        return scope;
    }

    @Override
    public List<CopyOnWriteContextEdge> variations() {
        return variations;
    }

    @Override
    public ContextValue value(Value value, ValueKind kind) {
        if (this.value == null) {
            this.value = ContextValue.create(scope, value, kind);
            return this.value;
        }
        // deep copy the tree
        ContextValue result = null;
        ContextNode copyRoot = ContextNode.create();
        Deque<ContextNode> stack = new ArrayDeque<>();
        Deque<ContextNode> copyStack = new ArrayDeque<>();
        for (ContextNode scope : scope.root().edge().children()) {
            stack.push(scope);
            copyStack.push(copyRoot);
        }
        while (!stack.isEmpty()) {
            ContextNode scope = stack.pop();
            String id = scope.id();
            ContextScope.Visibility visibility = scope.visibility();
            ContextEdge edge = scope.edge();
            ContextNode copyParent = copyStack.pop();
            ContextEdge copyParentEdge = copyParent.edge();
            ContextNode copyScope;
            if (scope == this.scope) {
                copyScope = ContextNode.create(scope.parent(), copyParent,
                        s -> new CopyOnWriteContextEdge(s, variations), id, visibility);
                result = copyScope.edge().value(value, kind);
            } else {
                copyScope = ContextNode.create(copyParent, copyParent, CopyOnWriteContextEdge::create, id, visibility);
                ContextValue currentValue = edge.value();
                copyScope.edge().value(currentValue.value(), currentValue.kind());
            }
            copyParentEdge.children().add(copyScope);
            for (ContextNode contextScope : edge.children()) {
                copyStack.push(copyScope);
                stack.push(contextScope);
            }
        }
        if (result == null) {
            throw new IllegalStateException("Unable to set value");
        }
        return result;
    }
}
