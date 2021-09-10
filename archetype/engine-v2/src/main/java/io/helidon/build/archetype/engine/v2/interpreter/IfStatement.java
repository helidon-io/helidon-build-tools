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

import io.helidon.build.archetype.engine.v2.expression.evaluator.Expression;

/**
 * {@code If} statement AST node.
 */
public class IfStatement extends ASTNode {

    private final Expression expression;

    IfStatement(String ifExpression, ASTNode parent, Location location) {
        super(parent, location);
        expression = Expression.builder().expression(ifExpression).build();
    }

    /**
     * Return logical expression.
     *
     * @return Expression
     */
    public Expression expression() {
        return expression;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
