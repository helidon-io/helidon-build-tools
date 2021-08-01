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
public enum LogicalOperator implements Operator {

    /**
     * Equality operator.
     */
    EQUAL(8, "==") {
        Boolean evaluate(Literal<?> left, Literal<?> right) throws ParserException {
            checkLiteralTypesEquality(left, right);
            return left.getValue().equals(right.getValue());
        }
    },
    /**
     * Inequality operator.
     */
    NOT_EQUAL(8, "!=") {
        @Override
        Boolean evaluate(Literal<?> left, Literal<?> right) throws ParserException {
            checkLiteralTypesEquality(left, right);
            return !left.getValue().equals(right.getValue());
        }
    },
    /**
     * Logical AND operator.
     */
    AND(4, "&&") {
        @Override
        Boolean evaluate(Literal<?> left, Literal<?> right) throws ParserException {
            checkLiteralTypesEquality(left, right);
            checkLiteralTypeEquality(left, Literal.Type.BOOLEAN);
            return Boolean.logicalAnd(
                    (Boolean) left.getValue(),
                    (Boolean) right.getValue()
            );
        }
    },
    /**
     * Logical OR operator.
     */
    OR(3, "||") {
        @Override
        Boolean evaluate(Literal<?> left, Literal<?> right) throws ParserException {
            checkLiteralTypesEquality(left, right);
            checkLiteralTypeEquality(left, Literal.Type.BOOLEAN);
            return Boolean.logicalOr(
                    (Boolean) left.getValue(),
                    (Boolean) right.getValue()
            );
        }
    },
    /**
     * {@code contains} operator.
     */
    CONTAINS(9, "contains") {
        @Override
        @SuppressWarnings("unchecked")
        Boolean evaluate(Literal<?> left, Literal<?> right) throws ParserException {
            checkLiteralTypeEquality(left, Literal.Type.ARRAY);
            checkLiteralTypeEquality(right, Literal.Type.STRING);
            return ((List<String>) left.getValue()).contains(right.getValue().toString());
        }
    },
    /**
     * Logical NOT operator.
     */
    NOT(13, "!") {
        @Override
        Boolean evaluate(Literal<?> left) throws ParserException {
            if (!left.getType().isPermittedForUnaryLogicalExpression()) {
                throw new ParserException(String.format(
                        "Operation '%s' cannot be performed on literals. "
                                + "The literal %s must have the type %s.",
                        "!",
                        left,
                        left.getType()
                ));
            }
            return !(Boolean) left.getValue();
        }
    };

    /**
     * Compare type of the literal with the given type and throw {@code ParserException} if they are not equal.
     *
     * @param left Literal
     * @param type Literal.Type for comparison
     * @throws ParserException if types are not equal
     */
    void checkLiteralTypeEquality(Literal<?> left, Literal.Type type) throws ParserException {
        if (!left.getType().equals(type)) {
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
     * @throws ParserException if types are not equal
     */
    void checkLiteralTypesEquality(Literal<?> left, Literal<?> right) throws ParserException {
        if (!left.getType().equals(right.getType())) {
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
     * Operators with higher precedence are evaluated before operators with relatively lower precedence.
     */
    private final int precedence;
    /**
     * String representation of the operator.
     */
    private final String operator;

    LogicalOperator(int precedence, String operator) {
        this.precedence = precedence;
        this.operator = operator;
    }

    private static final Map<String, LogicalOperator> OPERATOR_MAP;

    static {
        OPERATOR_MAP = new HashMap<>();
        for (LogicalOperator op : LogicalOperator.values()) {
            OPERATOR_MAP.put(op.operator, op);
        }
    }

    /**
     * Get {@code LogicalOperator} by its string representation.
     *
     * @param operator string representation of the logical operator. E.g. {@code "!="}.
     * @return LogicalOperator
     */
    public static LogicalOperator find(String operator) {
        return OPERATOR_MAP.get(operator);
    }

    @Override
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    /**
     * Evaluate expression for the literals (use for binary logical operations).
     *
     * @param leftLiteral  Literal
     * @param rightLiteral Literal
     * @return result of the evaluated expression
     * @throws ParserException if a parsing error occurs
     */
    Boolean evaluate(Literal<?> leftLiteral, Literal<?> rightLiteral) throws ParserException {
        throw new UnsupportedOperationException("Operation with two operands is not supported.");
    }

    /**
     * Evaluate expression for the literal (use for unary logical operations).
     *
     * @param leftLiteral Literal
     * @return result of the evaluated expression
     * @throws ParserException if a parsing error occurs
     */
    Boolean evaluate(Literal<?> leftLiteral) throws ParserException {
        throw new UnsupportedOperationException("Operation with one operand is not supported.");
    }
}
