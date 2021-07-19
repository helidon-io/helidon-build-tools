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
 * Value of a some type.
 *
 * @param <T> actual type of the value.
 */
abstract class Literal<T> implements AbstractSyntaxTree {

    /**
     * Value of the literal.
     */
    private final T value;

    /**
     * Create a new literal.
     *
     * @param rawExpr Value of the literal
     */
    Literal(T rawExpr) {
        value = rawExpr;
    }

    /**
     * Get value.
     *
     * @return Value
     */
    public T value() {
        return value;
    }

    /**
     * Get type of the literal.
     *
     * @return Type
     */
    abstract Type type();

    /**
     * Supported types of the literals.
     */
    public enum Type {
        /**
         * String.
         */
        STRING,
        /**
         * Boolean.
         */
        BOOLEAN {
            @Override
            boolean permittedForUnaryLogicalExpression() {
                return true;
            }
        },
        /**
         * Array.
         */
        ARRAY;

        /**
         * Test if a literal of this type can be used in an unary logical expression.
         *
         * @return {@code true} if the literal can be used in an unary logical expression, {@code false} otherwise.
         */
        boolean permittedForUnaryLogicalExpression() {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Literal{"
                + "value=" + value + "; "
                + "type=" + type()
                + '}';
    }
}
