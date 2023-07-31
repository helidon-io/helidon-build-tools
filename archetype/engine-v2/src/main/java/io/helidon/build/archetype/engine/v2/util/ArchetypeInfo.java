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

package io.helidon.build.archetype.engine.v2.util;

import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.ast.DeclaredBlock;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Node;

/**
 * Archetype info.
 */
public final class ArchetypeInfo {

    private final Map<Invocation, DeclaredBlock> invocations;
    private final Map<Node, DeclaredBlock> blocks;
    private final Map<DeclaredBlock, Set<Node>> nodes;

    /**
     * Create a new archetype info.
     *
     * @param invocations invocation targets
     * @param blocks      declared blocks
     * @param nodes       nodes
     */
    ArchetypeInfo(Map<Invocation, DeclaredBlock> invocations,
                  Map<Node, DeclaredBlock> blocks,
                  Map<DeclaredBlock, Set<Node>> nodes) {
        this.invocations = invocations;
        this.blocks = blocks;
        this.nodes = nodes;
    }

    /**
     * Get the invocations.
     *
     * @return map of invocation nodes to declared block
     */
    public Map<Invocation, DeclaredBlock> invocations() {
        return invocations;
    }

    /**
     * Get the declared blocks.
     *
     * @return map of nodes to declared block
     */
    public Map<Node, DeclaredBlock> blocks() {
        return blocks;
    }

    /**
     * Get the nodes.
     *
     * @return map of nodes by declared block
     */
    public Map<DeclaredBlock, Set<Node>> nodes() {
        return nodes;
    }
}
