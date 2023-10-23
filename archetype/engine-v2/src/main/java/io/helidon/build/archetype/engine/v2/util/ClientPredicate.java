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

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.ConditionBlock;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Variable;

/**
 * Client predicate.
 */
public final class ClientPredicate implements Node.Visitor<Void>, Block.Visitor<Void> {

    private static final ClientPredicate INSTANCE = new ClientPredicate();

    /**
     * Test if the given block should be included for clients.
     *
     * @param node node
     * @return {@code true} if included, {@code false} otherwise
     */
    public static boolean test(Node node) {
        return node.accept(INSTANCE, null) == Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitBlock(Block block, Void arg) {
        if (block.kind().equals(Block.Kind.OUTPUT)) {
            return Node.VisitResult.SKIP_SUBTREE;
        }
        return block.accept((Block.Visitor<Void>) this, arg);
    }

    @Override
    public Node.VisitResult visitVariable(Variable variable, Void arg) {
        if (variable.isTransient()) {
            return Node.VisitResult.TERMINATE;
        }
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitConditionBlock(ConditionBlock condition, Void arg) {
        if (condition.children().isEmpty()) {
            return Node.VisitResult.SKIP_SUBTREE;
        }
        for (Node node : condition.children()) {
            if (node instanceof Block) {
                if (((Block) node).kind() != Block.Kind.OUTPUT) {
                    return Node.VisitResult.CONTINUE;
                }
            }
        }
        return Node.VisitResult.SKIP_SUBTREE;
    }
}
