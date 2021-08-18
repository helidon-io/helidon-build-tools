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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link Expression}.
 */
class ExpressionTest {

    @Test
    public void testEvaluateWithVariables() {
        String value = "${var1} contains ${var2}";
        Expression expression = Expression.builder().expression(value).build();
        Map<String, String> varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "['a','b','c']");
        varInitializerMap.put("var2", "'b'");
        Object evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(true));

        value = "!(${array} contains 'basic-auth' == false && ${var})";
        expression = Expression.builder().expression(value).build();
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var", "true");
        varInitializerMap.put("array", "['a','b','c']");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(false));

        value = "!${var}";
        expression = Expression.builder().expression(value).build();
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var", "true");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(false));

        value = "['', 'adc', 'def'] contains ${var1} == ${var4} && ${var2} || !${var3}";
        expression = Expression.builder().expression(value).build();
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "'abc'");
        varInitializerMap.put("var4", "true");
        varInitializerMap.put("var2", "false");
        varInitializerMap.put("var3", "true");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(false));

        value = "${var1} contains ${var2} == ${var3} && ${var4} || ${var5}";
        expression = Expression.builder().expression(value).build();
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "['a','b','c']");
        varInitializerMap.put("var2", "'d'");
        varInitializerMap.put("var3", "true");
        varInitializerMap.put("var4", "true");
        varInitializerMap.put("var5", "false");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(false));

        value = " ${var1} == ${var1} && ${var2} contains ''";
        expression = Expression.builder().expression(value).build();
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "'foo'");
        varInitializerMap.put("var2", "['d','']");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat(evaluate, is(true));
    }

    @Test
    public void testVariable() {
        String value = "${variable}";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isVariable(), is(true));
        assertThat(expression.tree().asVariable().name(), is("variable"));

        value = "${variable} == 'some string'";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().asVariable().name(), is("variable"));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("'some string'"));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));

        value = "'some string' != ${variable}";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asVariable().name(), is("variable"));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("'some string'"));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));

        //incorrect variable name
        Exception e = assertThrows(ParserException.class, () -> {
            String stringValue = "${varia!ble}";
            Expression expr = Expression.builder().expression(stringValue).build();
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "unexpected token - ${varia!ble}"));
    }

    @Test
    public void testEvaluate() {
        String value = "['', 'adc', 'def'] contains 'basic-auth'";
        evaluateAndTestLogicalExpression(value, false);

        value = "!(['', 'adc', 'def'] contains 'basic-auth' == false && false)";
        evaluateAndTestLogicalExpression(value, true);

        value = "!false";
        evaluateAndTestLogicalExpression(value, true);

        value = "['', 'adc', 'def'] contains 'basic-auth' == false && true || !false";
        evaluateAndTestLogicalExpression(value, true);

        value = "['', 'adc', 'def'] contains 'basic-auth' == false && true || !true";
        evaluateAndTestLogicalExpression(value, true);

        value = "['', 'adc', 'def'] contains 'def'";
        evaluateAndTestLogicalExpression(value, true);

        value = "['', 'adc', 'def'] contains 'basic-auth' == true && false";
        evaluateAndTestLogicalExpression(value, false);

        value = "['', 'adc', 'def'] contains 'basic-auth' == false && false";
        evaluateAndTestLogicalExpression(value, false);

        value = " 'aaa' == 'aaa' && ['', 'adc', 'def'] contains ''";
        evaluateAndTestLogicalExpression(value, true);

        value = "true && \"bar\" == 'foo1' || true";
        evaluateAndTestLogicalExpression(value, true);

        value = "true && \"bar\" == 'foo1' || false";
        evaluateAndTestLogicalExpression(value, false);

        value = "('def' != 'def1') && false == true";
        evaluateAndTestLogicalExpression(value, false);

        value = "('def' != 'def1') && false";
        evaluateAndTestLogicalExpression(value, false);

        value = "('def' != 'def1') && true";
        evaluateAndTestLogicalExpression(value, true);

        value = "true != true";
        evaluateAndTestLogicalExpression(value, false);

        value = "false != true";
        evaluateAndTestLogicalExpression(value, true);

        value = "false == true";
        evaluateAndTestLogicalExpression(value, false);

        value = "false == false";
        evaluateAndTestLogicalExpression(value, true);

        value = "'def' != 'def1'";
        evaluateAndTestLogicalExpression(value, true);

        value = "'def' == 'def1'";
        evaluateAndTestLogicalExpression(value, false);

        value = "'def' == 'def'";
        evaluateAndTestLogicalExpression(value, true);

        value = "'def' != 'def'";
        evaluateAndTestLogicalExpression(value, false);

        value = "true==((true|| false)&&true)";
        evaluateAndTestLogicalExpression(value, true);

        value = "false==((true|| false)&&true)";
        evaluateAndTestLogicalExpression(value, false);

        value = "false==((true|| false)&&false)";
        evaluateAndTestLogicalExpression(value, true);

        //not equal types of the operands
        Exception e = assertThrows(ParserException.class, () -> {
            String stringValue = "true == 'def'";
            Expression expr = Expression.builder().expression(stringValue).build();
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation '==' cannot be performed on literals. The left literal "));

        //incorrect type of the operands
        e = assertThrows(ParserException.class, () -> {
            String stringValue = "'true' || 'def'";
            Expression expr = Expression.builder().expression(stringValue).build();
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation '||' cannot be performed on literals. The literal "));

        e = assertThrows(ParserException.class, () -> {
            String stringValue = "['', 'adc', 'def'] contains ['', 'adc', 'def']";
            Expression expr = Expression.builder().expression(stringValue).build();
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation 'contains' cannot be performed on literals. The literal "));

        //not initialized variable
        e = assertThrows(IllegalArgumentException.class, () -> {
            String stringValue = "true == ${def}";
            Expression expr = Expression.builder().expression(stringValue).build();
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Variable def must be initialized"));
    }

    private void evaluateAndTestLogicalExpression(String value, boolean expectedResult) {
        Expression expression = Expression.builder().expression(value).build();
        Object evaluate = expression.evaluate();
        assertThat(evaluate, is(expectedResult));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContainsOperator() {
        String value = "['', 'adc', 'def'] contains 'basic-auth'";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asBinaryExpression().left().asLiteral().value(),
                contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.CONTAINS));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("'basic-auth'"));

        Exception e = assertThrows(ParserException.class, () -> {
            String expr = "['', 'adc', 'def'] contains != 'basic-auth'";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unexpected token - !="));

        value = "!(['', 'adc', 'def'] contains 'basic-auth')";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isUnaryExpression(), is(true));
        assertThat(expression.tree().asUnaryExpression().operator(), Matchers.is(Operator.NOT));
        assertThat((List<String>) expression.tree()
                        .asUnaryExpression()
                        .left().asBinaryExpression().left().asLiteral().value(),
                contains("''", "'adc'", "'def'"));
        assertThat(expression.tree()
                        .asUnaryExpression()
                        .left().asBinaryExpression().right().asLiteral().value(),
                is("'basic-auth'"));
        assertThat(expression.tree()
                        .asUnaryExpression()
                        .left().asBinaryExpression().operator(),
                Matchers.is(Operator.CONTAINS));

        e = assertThrows(ParserException.class, () -> {
            String expr = "!['', 'adc', 'def'] contains 'basic-auth'";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Incorrect operand type for the unary logical expression"));

        value = "['', 'adc', 'def'] contains 'basic-auth' == true && false";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat((List<String>) expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asLiteral().value(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asLiteral().value(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.CONTAINS));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asLiteral().value(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression()
                .right().asLiteral().value(), is(false));
        assertThat(expression.tree().asBinaryExpression()
                .operator(), Matchers.is(Operator.AND));

        value = "(['', 'adc', 'def'] contains 'basic-auth') == true && false";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat((List<String>) expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asLiteral().value(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asLiteral().value(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.CONTAINS));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asBinaryExpression().isolated(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asLiteral().value(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression()
                .right().asLiteral().value(), is(false));
        assertThat(expression.tree().asBinaryExpression()
                .operator(), Matchers.is(Operator.AND));


        value = " 'aaa' == 'bbb' && ['', 'adc', 'def'] contains ${basic-auth}";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asLiteral().value(), is("'aaa'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asLiteral().value(), is("'bbb'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.EQUAL));
        assertThat((List<String>) expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .left().asLiteral().value(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .right().asVariable().name(), is("basic-auth"));
        assertThat(expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .operator(), Matchers.is(Operator.CONTAINS));
        assertThat(expression.tree().asBinaryExpression()
                .operator(), Matchers.is(Operator.AND));

        value = " 'aaa' == ${bbb} && (['', 'adc', 'def'] contains 'basic-auth')";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .left().asLiteral().value(), is("'aaa'"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .right().asVariable().name(), is("bbb"));
        assertThat(expression.tree().asBinaryExpression()
                .left().asBinaryExpression()
                .operator(), Matchers.is(Operator.EQUAL));
        assertThat((List<String>) expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .left().asLiteral().value(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .right().asLiteral().value(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .operator(), Matchers.is(Operator.CONTAINS));
        assertThat(expression.tree().asBinaryExpression()
                .right().asBinaryExpression()
                .isolated(), is(true));
        assertThat(expression.tree().asBinaryExpression()
                .operator(), Matchers.is(Operator.AND));
    }

    @Test
    public void testUnaryExpression() {
        String value = "!true";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isUnaryExpression(), is(true));
        assertThat(expression.tree().asUnaryExpression().operator(), Matchers.is(Operator.NOT));
        assertThat(expression.tree().asUnaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asUnaryExpression().left().asLiteral().type(),
                Matchers.is(Literal.Type.BOOLEAN));
        assertThat(expression.tree().asUnaryExpression().left().asLiteral().value(), is(true));

        value = "!('foo' != 'bar')";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isUnaryExpression(), is(true));
        assertThat(expression.tree().asUnaryExpression().operator(), Matchers.is(Operator.NOT));
        assertThat(expression.tree().asUnaryExpression().left().isBinaryExpression(), is(true));
        BinaryExpression left = expression.tree().asUnaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("'foo'"));
        assertThat(left.right().asLiteral().value(), is("'bar'"));
        assertThat(left.operator(), Matchers.is(Operator.NOT_EQUAL));

        value = "'foo1' == 'bar' && !('foo' != ${bar})";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.AND));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("'foo1'"));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("'bar'"));
        assertThat(expression.tree().asBinaryExpression().right().isUnaryExpression(), is(true));
        UnaryExpression right = expression.tree().asBinaryExpression().right().asUnaryExpression();
        assertThat(right.operator(), Matchers.is(Operator.NOT));
        assertThat(right.left().isBinaryExpression(), is(true));
        assertThat(right.left().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(right.left().asBinaryExpression().left().asLiteral().value(), is("'foo'"));
        assertThat(right.left().asBinaryExpression().right().asVariable().name(), is("bar"));


        //incorrect operand type
        Exception e = assertThrows(ParserException.class, () -> {
            String expr = "!'string type'";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Incorrect operand type for the unary logical expression"));
    }

    @Test
    public void testExpressionWithParenthesis() {
        String value = "(\"foo\") != \"bar\"";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));

        value = "((\"foo\")) != \"bar\"";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));

        value = "((\"foo\") != \"bar\")";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));

        value = "\"foo\" != (\"bar\")";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));

        //first operator has higher precedence
        value = "(\"foo\"==\"bar\")|| ${foo1}";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        BinaryExpression left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("\"foo\""));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("\"bar\""));
        assertThat(left.operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(expression.tree().asBinaryExpression().right().isVariable(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asVariable().name(), is("foo1"));

        //first operator has higher precedence
        value = "(\"foo\"==\"bar\"|| true)";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("\"foo\""));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("\"bar\""));
        assertThat(left.operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is(true));

        //first operator has higher precedence
        value = "${foo}==(true|| false)";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isVariable(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().asVariable().name(), is("foo"));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        BinaryExpression right = expression.tree().asBinaryExpression().right().asBinaryExpression();
        assertThat(right.isBinaryExpression(), is(true));
        assertThat(right.left().isLiteral(), is(true));
        assertThat(right.left().asLiteral().value(), is(true));
        assertThat(right.operator(), Matchers.is(Operator.OR));
        assertThat(right.right().isLiteral(), is(true));
        assertThat(right.right().asLiteral().value(), is(false));

        //first operator has higher precedence
        value = "true==((${var}|| false)&&true)";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        assertThat(right.isBinaryExpression(), is(true));
        right = expression.tree().asBinaryExpression().right().asBinaryExpression();
        assertThat(right.operator(), Matchers.is(Operator.AND));
        assertThat(right.left().isBinaryExpression(), is(true));
        assertThat(right.left().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(right.left().asBinaryExpression().left().isVariable(), is(true));
        assertThat(right.left().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(right.left().asBinaryExpression().left().asVariable().name(), is("var"));
        assertThat(right.left().asBinaryExpression().right().asLiteral().value(), is(false));
        assertThat(right.right().isLiteral(), is(true));
        assertThat(right.right().asLiteral().value(), is(true));

        //incorrect parenthesis
        Exception e = assertThrows(ParserException.class, () -> {
            String expr = "\"foo\"==((\"bar\"|| 'foo1')&&true))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = "\"foo\")==((\"bar\"|| 'foo1')&&true))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = "\"foo\"(==((\"bar\"|| 'foo1')&&true))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = ")\"foo\"(==((\"bar\"|| 'foo1')&&true))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLiteralWithParenthesis() throws Exception {
        //boolean literal
        String value = "(true)";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(true));

        //boolean literal and nested parenthesis
        value = "((true))";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(true));

        //variable
        value = "(${var})";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isVariable(), is(true));
        assertThat(expression.tree().asVariable().name(), is("var"));

        //string literal
        value = "(\"value\")";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is("\"value\""));

        //string literal and nested parenthesis
        value = "((\"value\"))";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is("\"value\""));

        //string literal with incorrect parenthesis
        Exception e = assertThrows(ParserException.class, () -> {
            String expr = "((((\"value\"))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //string literal with incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = ")\"value\"(";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //string literal with incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = "(\"value\"()";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //empty array
        value = "([])";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((ArrayList<String>) expression.tree().asLiteral().value()).isEmpty(), is(true));

        //empty array and nested parenthesis
        value = "(([]))";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((ArrayList<String>) expression.tree().asLiteral().value()).isEmpty(), is(true));

        //array with 1 element
        value = "([''])";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().value(), contains("''"));

        //array with 1 element and nested parenthesis
        value = "((['']))";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().value(), contains("''"));

        //array with many elements
        value = "(['', 'adc', 'def'])";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().value(), contains("''", "'adc'", "'def'"));

        //array with many elements and nested parenthesis
        value = "((['', 'adc', 'def']))";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().value(), contains("''", "'adc'", "'def'"));

        //array with many elements and incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = "((['', 'adc', 'def'])))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //array with many elements and incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            String expr = "(((['', 'adc', 'def']))";
            Expression.builder().expression(expr).build();
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

    }

    @Test
    public void testFewOperatorsWithDifferentPrecedenceInOneExpression() {
        /*
         * first operator has higher precedence
         */
        //first case - 3 string literals
        String value = "\"foo\"==\"bar\"|| 'foo1'";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        BinaryExpression left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("\"foo\""));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("\"bar\""));
        assertThat(left.operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("'foo1'"));

        //first case - 4 string literals
        value = "\"foo\"==\"bar\" && 'foo1' || 'bar1'";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isBinaryExpression(), is(true));
        assertThat(left.left().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(left.left().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(left.left().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(left.left().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));
        assertThat(left.left().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("'foo1'"));
        assertThat(left.operator(), Matchers.is(Operator.AND));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("'bar1'"));

        /*
         *  second operator has higher precedence
         */

        //first case - 3 literals
        value = "\"foo\" && \"bar\" == 'foo1'";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.AND));
        assertThat(expression.tree().asBinaryExpression().right().isBinaryExpression(), is(true));
        BinaryExpression right = expression.tree().asBinaryExpression().right().asBinaryExpression();
        assertThat(right.left().isLiteral(), is(true));
        assertThat(right.left().asLiteral().value(), is("\"bar\""));
        assertThat(right.right().isLiteral(), is(true));
        assertThat(right.right().asLiteral().value(), is("'foo1'"));
        assertThat(right.operator(), Matchers.is(Operator.EQUAL));

        //first case - 4 literals
        value = "true && ${bar} == 'foo1' || false";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.OR));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is(true));
        assertThat(left.operator(), Matchers.is(Operator.AND));
        assertThat(left.right().isBinaryExpression(), is(true));
        assertThat(left.right().asBinaryExpression().left().isVariable(), is(true));
        assertThat(left.right().asBinaryExpression().left().asVariable().name(), is("bar"));
        assertThat(left.right().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        assertThat(left.right().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(left.right().asBinaryExpression().right().asLiteral().value(), is("'foo1'"));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is(false));

    }

    @Test
    public void testFewOperatorsWithEqualPrecedenceInOneExpression() {
        //first case - string literals
        String value = "\"foo\"!=\"bar\"=='foo1'";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        BinaryExpression left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("\"foo\""));
        assertThat(left.right().isLiteral(), is(true));
        assertThat(left.right().asLiteral().value(), is("\"bar\""));
        assertThat(left.operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("'foo1'"));

        //second case - different types of the literals
        value = "'foo'!=${var}==true";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isBinaryExpression(), is(true));
        left = expression.tree().asBinaryExpression().left().asBinaryExpression();
        assertThat(left.left().isLiteral(), is(true));
        assertThat(left.left().asLiteral().value(), is("'foo'"));
        assertThat(left.right().isVariable(), is(true));
        assertThat(left.right().asVariable().name(), is("var"));
        assertThat(left.operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.EQUAL));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is(true));

    }

    @Test
    public void testIncorrectOperator() {
        final Exception e = assertThrows(ParserException.class, () -> {
            String value = "\"foo\" !== \"bar\"";
            Expression.builder().expression(value).build();
        });
        assertThat(e.getMessage(), containsString("unexpected token - = \"bar\""));
    }

    @Test
    public void simpleNotEqualStringLiterals() throws Exception {
        String value = "\"foo\" != \"bar\"";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isBinaryExpression(), is(true));
        assertThat(expression.tree().asBinaryExpression().left().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().right().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryExpression().operator(), Matchers.is(Operator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryExpression().left().asLiteral().value(), is("\"foo\""));
        assertThat(expression.tree().asBinaryExpression().right().asLiteral().value(), is("\"bar\""));
    }

    @Test
    public void testStringLiteralWithDoubleQuotes() throws Exception {
        String value = "\"value\"";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(value));
    }

    @Test
    public void testStringLiteralWithSingleQuotes() throws Exception {
        String value = "'value'";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(value));
    }

    @Test
    public void testStringLiteralWithWhitespaces() throws Exception {
        String value = "'value'";
        Expression expression = Expression.builder().expression(" " + value + "   ").build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(value));
    }

    @Test
    public void testBooleanLiteral() throws Exception {
        String value = "true";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().value(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyStringArrayLiteral() throws Exception {
        String value = "[]";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((List<String>) expression.tree().asLiteral().value()).isEmpty(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayWithEmptyLiteral() throws Exception {
        String value = "['']";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().value(), contains("''"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayWithStringLiterals() throws Exception {
        String value = "['', 'adc', 'def']";
        Expression expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().value(), contains("''", "'adc'", "'def'"));

        value = "['','adc','def']";
        expression = Expression.builder().expression(value).build();
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().value(), contains("''", "'adc'", "'def'"));
    }

}