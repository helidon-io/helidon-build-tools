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

package io.helidon.build.archetype.engine.v2.expression.evaluator;

import java.util.Objects;

/**
 * Expression for a binary logical operation.
 */
final class BinaryExpression implements AbstractSyntaxTree, ExpressionHandler<Boolean> {

    private final AbstractSyntaxTree left;
    private final AbstractSyntaxTree right;
    private final Operator operator;
    private boolean isIsolated = false;

    /**
     * Create a new binary logical expression.
     *
     * @param left     left operand.
     * @param right    right operand.
     * @param operator logical operator.
     */
    BinaryExpression(
            AbstractSyntaxTree left, AbstractSyntaxTree right, Operator operator
    ) {
        this.left = left;
        this.right = right;
        Token.Type operatorTokenType = Token.Type.getByValue(operator.operator());
        if (Objects.equals(operatorTokenType, Token.Type.UNARY_LOGICAL_OPERATOR)) {
            throw new ParserException("Unary logical operator cannot be used in binary logical expression");
        }
        this.operator = operator;

    }

    /**
     * Get left operand.
     *
     * @return AbstractSyntaxTree.
     */
    public AbstractSyntaxTree left() {
        return left;
    }

    /**
     * Get right operand.
     *
     * @return AbstractSyntaxTree.
     */
    public AbstractSyntaxTree right() {
        return right;
    }

    /**
     * Get operator.
     *
     * @return LogicalOperator.
     */
    public Operator operator() {
        return operator;
    }

    /**
     * Test if this expression is surrounded by the parentheses.
     *
     * @return {@code true} if this expression is surrounded by the parentheses, {@code false} otherwise.
     */
    public boolean isolated() {
        return isIsolated;
    }

    /**
     * Mark this expression if it is enclosed in parentheses.
     *
     * @param isolated @code true} if this expression is surrounded by the parentheses, {@code false} otherwise.
     */
    public void isolated(boolean isolated) {
        isIsolated = isolated;
    }

    @Override
    public Literal<Boolean> evaluate() {
        Literal<?> leftLiteral = left.isExpressionHandler() ? left.asExpressionHandler().evaluate() : asLiteral(left);
        Literal<?> rightLiteral = right.isExpressionHandler() ? right.asExpressionHandler().evaluate() : asLiteral(right);

        return new BooleanLiteral(operator.evaluate(leftLiteral, rightLiteral));
    }
}
