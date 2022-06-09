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

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link ContextEdge} that does not contain a value.
 */
public final class NoValueContextEdge implements ContextEdge {

    private final ContextNode node;
    private final List<ContextNode> children = new ArrayList<>();

    private NoValueContextEdge(ContextNode node) {
        this.node = Objects.requireNonNull(node, "node is null");
    }

    /**
     * Create a new context edge.
     *
     * @param node node
     * @return new instance
     */
    public static NoValueContextEdge create(ContextNode node) {
        return new NoValueContextEdge(node);
    }

    @Override
    public ContextValue value(Value value, ValueKind kind) {
        return null;
    }

    @Override
    public ContextValue value() {
        return null;
    }

    @Override
    public ContextNode node() {
        return node;
    }

    @Override
    public List<ContextNode> nestedNodes() {
        return children;
    }
}
