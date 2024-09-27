/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Value;

/**
 * Process string and if it represents an expression evaluate it and return {@link Value}.
 *
 * @see Expression
 */
public class ValueHandler {

    private static final Pattern VAR_NO_BRACE = Pattern.compile("^\\w+");

    private ValueHandler() {
    }

    /**
     * Process expression.
     *
     * @param expression expression
     * @param resolver   variable resolver
     * @return {@link Value} object
     */
    public static Value process(String expression, Function<String, Value> resolver) {
        if (expression == null) {
            return Value.NULL;
        }
        for (ExpressionType type : ExpressionType.values()) {
            Matcher matcher = type.pattern.matcher(expression);
            if (matcher.find()) {
                String value = matcher.group("expression");
                if (type == ExpressionType.NO_BRACE_VARS) {
                    value = preprocessExpression(value);
                }
                return Expression.parse(value).eval(resolver);
            }

        }
        return Value.create(expression);
    }

    /**
     * Process expression.
     *
     * @param expression expression
     * @return {@link Value} object
     */
    public static Value process(String expression) {
        return process(expression, s -> null);
    }

    private static String preprocessExpression(String expression) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;
        String current = expression.trim();
        while (current.length() > 0) {
            current = current.substring(cursor);
            cursor = 0;
            boolean tokenFound = false;
            for (Token token : Token.values()) {
                Matcher matcher = token.pattern.matcher(current);
                if (matcher.find()) {
                    String value = matcher.group();
                    cursor += value.length();
                    output.append(value);
                    tokenFound = true;
                    break;
                }
            }
            if (tokenFound) {
                continue;
            }
            Matcher matcherVar = VAR_NO_BRACE.matcher(current);
            while (matcherVar.find()) {
                var value = matcherVar.group();
                cursor += value.length();
                output.append("${").append(value).append("}");
                tokenFound = true;
            }
            if (!tokenFound && current.trim().length() > 0) {
                throw new Expression.FormatException("Unexpected token - " + current);
            }
        }
        return output.toString();
    }

    private enum ExpressionType {

        BACKTICK("^`(?<expression>.*)`$"),
        BRACE_VARS("^#\\{(?<expression>.*(\\$\\{)+.*}+.*)}$"),
        NO_BRACE_VARS("^#\\{(?<expression>[^\\$\\{}]*)}$");

        private final Pattern pattern;

        ExpressionType(String regex) {
            this.pattern = Pattern.compile(regex);
        }
    }

    private enum Token {
        SKIP("^\\s+"),
        ARRAY("^\\[[^]\\[]*]"),
        BOOLEAN("^(true|false)\\b"),
        STRING("^['\"][^'\"]*['\"]"),
        VARIABLE("^\\$\\{(?<varName>~?[\\w.-]+)}"),
        EQUALITY_OPERATOR("^(!=|==)"),
        BINARY_LOGICAL_OPERATOR("^(\\|\\||&&)"),
        UNARY_LOGICAL_OPERATOR("^[!]"),
        CONTAINS_OPERATOR("^contains\\b"),
        PARENTHESIS("^[()]"),
        COMMENT("#.*\\R"),
        TERNARY_IF_OPERATOR("^\\?"),
        TERNARY_ELSE_OPERATOR("^:"),
        INTEGER("^[0-9]+");

        private final Pattern pattern;

        Token(String regex) {
            this.pattern = Pattern.compile(regex);
        }
    }
}
