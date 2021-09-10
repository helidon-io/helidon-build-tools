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

package io.helidon.build.archetype.engine.v2.interpreter;

import io.helidon.build.archetype.engine.v2.descriptor.Context;

/**
 * Archetype Context AST node.
 */
public class ContextAST extends ASTNode {

    ContextAST(ASTNode parent, Location location) {
        super(parent, location);
    }

    public ContextAST() {
        super(null, Location.builder().build());
    }

    static ContextAST create(Context contextFrom, ASTNode parent, Location location) {
        ContextAST result = new ContextAST(parent, location);
        result.children().addAll(transformList(
                contextFrom.nodes(),
                n -> ContextNodeASTFactory.create(n, result, location)));
        return result;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
