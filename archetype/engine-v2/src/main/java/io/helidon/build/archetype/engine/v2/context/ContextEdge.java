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

import java.util.List;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;

/**
 * A connection between context nodes that holds a value.
 */
public interface ContextEdge {

    /**
     * Get the value.
     *
     * @return value
     */
    ContextValue value();

    /**
     * Update the value.
     *
     * @param value the new value
     * @param kind  value kind
     * @return created context value
     */
    ContextValue value(Value value, ValueKind kind);

    /**
     * Get the parent node.
     *
     * @return node
     */
    ContextNode node();

    /**
     * Get the nested nodes.
     *
     * @return nested nodes
     */
    List<ContextNode> nestedNodes();

    /**
     * Context edge visitor.
     */
    interface Visitor {

        /**
         * Visit a edge.
         *
         * @param edge edge
         */
        void visit(ContextEdge edge);

        /**
         * Post-visit a edge.
         *
         * @param edge edge
         */
        default void postVisit(ContextEdge edge) {
        }
    }
}
