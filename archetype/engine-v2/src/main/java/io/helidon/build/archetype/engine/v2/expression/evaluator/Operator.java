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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logical operator.
 */
enum Operator {

    /**
     * Equality operator.
     */
    EQUAL(8, "==") {
        @Override
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(2, literals);
            checkLiteralTypesEquality(literals[0], literals[1]);
            return literals[0].value().equals(literals[1].value());
        }
    },
    /**
     * Inequality operator.
     */
    NOT_EQUAL(8, "!=") {
        @Override
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(2, literals);
            checkLiteralTypesEquality(literals[0], literals[1]);
            return !literals[0].value().equals(literals[1].value());
        }
    },
    /**
     * Logical AND operator.
     */
    AND(4, "&&") {
        @Override
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(2, literals);
            checkLiteralTypesEquality(literals[0], literals[1]);
            checkLiteralTypeEquality(literals[0], Literal.Type.BOOLEAN);
            return Boolean.logicalAnd(
                    (Boolean) literals[0].value(),
                    (Boolean) literals[1].value()
            );
        }
    },
    /**
     * Logical OR operator.
     */
    OR(3, "||") {
        @Override
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(2, literals);
            checkLiteralTypesEquality(literals[0], literals[1]);
            checkLiteralTypeEquality(literals[0], Literal.Type.BOOLEAN);
            return Boolean.logicalOr(
                    (Boolean) literals[0].value(),
                    (Boolean) literals[1].value()
            );
        }
    },
    /**
     * {@code contains} operator.
     */
    CONTAINS(9, "contains") {
        @Override
        @SuppressWarnings("unchecked")
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(2, literals);
            checkLiteralTypeEquality(literals[0], Literal.Type.ARRAY);
            checkLiteralTypeEquality(literals[1], Literal.Type.STRING);
            return ((List<String>) literals[0].value()).contains(literals[1].value().toString());
        }
    },
    /**
     * Logical NOT operator.
     */
    NOT(13, "!") {
        @Override
        Boolean evaluate(Literal<?>... literals) {
            checkLiteralsCount(1, literals);
            if (!literals[0].type().permittedForUnaryLogicalExpression()) {
                throw new ParserException(String.format(
                        "Operation '%s' cannot be performed on literals. "
                                + "The literal %s must have the type %s.",
                        "!",
                        literals[0],
                        literals[0].type()
                ));
            }
            return !(Boolean) literals[0].value();
        }
    };

    /**
     * Compare type of the literal with the given type and throw {@code ParserException} if they are not equal.
     *
     * @param left Literal
     * @param type Literal.Type for comparison
     */
    void checkLiteralTypeEquality(Literal<?> left, Literal.Type type) {
        if (!left.type().equals(type)) {
            throw new ParserException(String.format(
                    "Operation '%s' cannot be performed on literals. "
                            + "The literal %s must have the type %s.",
                    operator,
                    left,
                    type
            ));
        }
    }

    /**
     * Compare types of the literals and throw {@code ParserException} if they are not equal.
     *
     * @param left  Literal
     * @param right Literal
     */
    void checkLiteralTypesEquality(Literal<?> left, Literal<?> right) {
        if (!left.type().equals(right.type())) {
            throw new ParserException(String.format(
                    "Operation '%s' cannot be performed on literals. "
                            + "The left literal %s and the right literal %s must be of the same type.",
                    operator,
                    left,
                    right
            ));
        }
    }

    /**
     * Check literals count.
     *
     * @param expectedCount expected count
     * @param literals      literals
     */
    void checkLiteralsCount(int expectedCount, Literal<?>... literals) {
        if (literals.length != expectedCount) {
            throw new UnsupportedOperationException(String.format("Operation %s with %d operand is not supported.",
                    operator,
                    literals.length));
        }
    }

    /**
     * Operators with higher precedence are evaluated before operators with relatively lower precedence.
     */
    private final int priority;
    /**
     * String representation of the operator.
     */
    private final String operator;

    Operator(int priority, String operator) {
        this.priority = priority;
        this.operator = operator;
    }

    private static final Map<String, Operator> OPERATOR_MAP;

    static {
        OPERATOR_MAP = new HashMap<>();
        for (Operator op : Operator.values()) {
            OPERATOR_MAP.put(op.operator, op);
        }
    }

    /**
     * Get {@code LogicalOperator} by its string representation.
     *
     * @param operator string representation of the logical operator. E.g. {@code "!="}.
     * @return LogicalOperator
     */
    public static Operator find(String operator) {
        return OPERATOR_MAP.get(operator);
    }

    /**
     * Get priority of the operator.
     * Operators with higher priority are evaluated before operators with relatively lower priority.
     *
     * @return priority for the current Operator.
     */
    public int priority() {
        return priority;
    }

    /**
     * Get string representation of the operator.
     *
     * @return string representation of the operator. E.g. {@code "!="}
     */
    public String operator() {
        return operator;
    }

    /**
     * Evaluate expression for the literals.
     *
     * @param literals operands
     * @return Boolean
     */
    abstract Boolean evaluate(Literal<?>... literals);
}
