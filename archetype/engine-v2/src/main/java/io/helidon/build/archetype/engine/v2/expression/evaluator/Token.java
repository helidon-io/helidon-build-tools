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

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Part of an income string representation of the expression that can be transformed in the part of the {@code
 * AbstractSyntaxTree}.
 */
final class Token {

    private final Type type;
    private final String value;

    /**
     * Create a new token.
     *
     * @param type  Type of the token.
     * @param value Value of the token.
     */
    Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Get type of the token.
     *
     * @return Type
     */
    public Type type() {
        return type;
    }

    /**
     * Get value of the token.
     *
     * @return String value.
     */
    public String value() {
        return value;
    }

    /**
     * Supported types of the token.
     */
    public enum Type {
        /**
         * Token that can be skipped (whitespaces for example).
         */
        SKIP {
            @Override
            Pattern pattern() {
                return Pattern.compile("^\\s+");
            }
        },
        /**
         * Array.
         */
        ARRAY {
            @Override
            Pattern pattern() {
                return Pattern.compile("^\\[[^]\\[]*]");
            }
        },
        /**
         * Boolean literal.
         */
        BOOLEAN {
            @Override
            Pattern pattern() {
                return Pattern.compile("^(true|false)");
            }
        },
        /**
         * String literal.
         */
        STRING {
            @Override
            Pattern pattern() {
                return Pattern.compile("^['\"][^'\"]*['\"]");
            }
        },
        /**
         * Variable.
         */
        VARIABLE {
            @Override
            Pattern pattern() {
                return Pattern.compile("^\\$\\{(?<varName>[\\w.-]+)}");
            }
        },
        /**
         * Equality operator.
         */
        EQUALITY_OPERATOR {
            @Override
            Pattern pattern() {
                return Pattern.compile("^(!=|==)");
            }

            @Override
            boolean binaryOperator() {
                return true;
            }
        },
        /**
         * Binary logical operator.
         */
        BINARY_LOGICAL_OPERATOR {
            @Override
            Pattern pattern() {
                return Pattern.compile("^(\\|\\||&&)");
            }

            @Override
            boolean binaryOperator() {
                return true;
            }
        },
        /**
         * Unary logical operator.
         */
        UNARY_LOGICAL_OPERATOR {
            @Override
            Pattern pattern() {
                return Pattern.compile("^[!]");
            }
        },
        /**
         * Contains operator.
         */
        CONTAINS_OPERATOR {
            @Override
            Pattern pattern() {
                return Pattern.compile("^contains");
            }

            @Override
            boolean binaryOperator() {
                return true;
            }
        },
        /**
         * Parenthesis.
         */
        PARENTHESIS {
            @Override
            Pattern pattern() {
                return Pattern.compile("^[()]");
            }
        };

        /**
         * Get {@code Pattern} that represents an regular expression for this type of the token.
         *
         * @return Pattern
         */
        abstract Pattern pattern();

        /**
         * Test if this token represents a binary operator.
         *
         * @return {@code true} if this token represents a binary operator, {@code false} otherwise.
         */
        boolean binaryOperator() {
            return false;
        }

        /**
         * Get type of the token by its value.
         *
         * @param tokenValue Token value.
         * @return Type
         */
        public static Type getByValue(String tokenValue) {
            return Arrays.stream(Type.values())
                    .filter(type -> type.pattern().matcher(tokenValue).find()).findFirst().orElse(null);
        }
    }
}
