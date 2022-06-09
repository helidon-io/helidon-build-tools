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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.common.GenericType;

/**
 * An implementation of {@link ContextEdge} that contains a value.
 */
public final class WriteableContextEdge implements ContextEdge {

    private ContextValue value;
    private final ContextNode node;
    private final List<ContextNode> children = new ArrayList<>();

    private WriteableContextEdge(ContextNode node) {
        this.node = Objects.requireNonNull(node, "node is null");
    }

    /**
     * Create a new context edge.
     *
     * @param node node
     * @return new instance
     */
    public static WriteableContextEdge create(ContextNode node) {
        return new WriteableContextEdge(node);
    }

    @Override
    public ContextValue value(Value newValue, ValueKind kind) {
        if (value == null || !value.isReadOnly()) {
            value = ContextValue.create(node, newValue, kind);
            return value;
        }
        GenericType<?> type = value.type();
        if (type == null) {
            type = newValue.type();
        }
        if (type == null) {
            type = ValueTypes.STRING;
        }
        Object currentVal = value.as(type);
        Object newVal = newValue.as(type);
        if (!currentVal.equals(newVal)) {
            throw new IllegalStateException(String.format(
                    "Cannot set value, path=%s, current={kind=%s, %s}, new={kind=%s, %s}",
                    node.path(true),
                    value.kind(),
                    value.value(),
                    kind,
                    newValue));
        }
        return value;
    }

    @Override
    public ContextValue value() {
        return value;
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
