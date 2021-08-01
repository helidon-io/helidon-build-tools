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

/**
 * Expression for a unary logical operation.
 */
public class UnaryLogicalExpression implements AbstractSyntaxTree, ExpressionHandler<Boolean> {

    private final LogicalOperator operator;
    private final AbstractSyntaxTree left;

    /**
     * Create a new unary logical expression.
     *
     * @param operator operator
     * @param left     operand
     * @throws ParserException if a parsing error occurs
     */
    public UnaryLogicalExpression(LogicalOperator operator, AbstractSyntaxTree left) throws ParserException {
        this.operator = operator;
        if (left.isBinaryLogicalExpression() && !left.asBinaryLogicalExpression().isIsolated()) {
            throw new ParserException("Incorrect operand type for the unary logical expression");
        }
        if (left.isLiteral() && !left.asLiteral().getType().isPermittedForUnaryLogicalExpression()) {
            throw new ParserException("Incorrect operand type for the unary logical expression");
        }
        this.left = left;
    }

    /**
     * Get {@code LogicalOperator} for the current expression.
     *
     * @return LogicalOperator
     */
    public LogicalOperator getOperator() {
        return operator;
    }

    /**
     * Get operand for the the current expression.
     *
     * @return AbstractSyntaxTree
     */
    public AbstractSyntaxTree getLeft() {
        return left;
    }

    @Override
    public Literal<Boolean> evaluate() throws ParserException {
        var leftLiteral = left.isExpressionHandler() ? left.asExpressionHandler().evaluate() : asLiteral(left);
        return new BooleanLiteral(operator.evaluate(leftLiteral));
    }
}
