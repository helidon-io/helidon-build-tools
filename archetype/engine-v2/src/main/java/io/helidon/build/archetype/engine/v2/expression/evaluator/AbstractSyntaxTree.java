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
 * Abstract syntax tree for an expression.
 */
interface AbstractSyntaxTree {

    /**
     * Test if this instance is a literal.
     *
     * @return {@code true} if a literal, {@code false} otherwise
     */
    default boolean isLiteral() {
        return this instanceof Literal;
    }

    /**
     * Get this instance as a {@link Literal}.
     *
     * @return Literal
     */
    default Literal<?> asLiteral() {
        return (Literal<?>) this;
    }

    /**
     * Test if this instance is a binary logical expression.
     *
     * @return {@code true} if a binary logical expression, {@code false} otherwise
     */
    default boolean isBinaryExpression() {
        return this instanceof BinaryExpression;
    }

    /**
     * Get this instance as a {@link BinaryExpression}.
     *
     * @return BinaryLogicalExpression
     */
    default BinaryExpression asBinaryExpression() {
        return (BinaryExpression) this;
    }

    /**
     * Test if this instance is an unary logical expression.
     *
     * @return {@code true} if an unary logical expression, {@code false} otherwise
     */
    default boolean isUnaryExpression() {
        return this instanceof UnaryExpression;
    }

    /**
     * Get this instance as an {@link UnaryExpression}.
     *
     * @return UnaryLogicalExpression
     */
    default UnaryExpression asUnaryExpression() {
        return (UnaryExpression) this;
    }

    /**
     * Get this instance as an {@link ExpressionHandler}.
     *
     * @return ExpressionHandler
     */
    default ExpressionHandler<?> asExpressionHandler() {
        return (ExpressionHandler<?>) this;
    }

    /**
     * Test if this instance is an expression handler.
     *
     * @return {@code true} if an expression handler, {@code false} otherwise
     */
    default boolean isExpressionHandler() {
        return this instanceof ExpressionHandler;
    }

    /**
     * Get this instance as a {@link Variable}.
     *
     * @return Variable
     */
    default Variable asVariable() {
        return (Variable) this;
    }

    /**
     * Test if this instance is a variable.
     *
     * @return {@code true} if a variable, {@code false} otherwise
     */
    default boolean isVariable() {
        return this instanceof Variable;
    }
}
