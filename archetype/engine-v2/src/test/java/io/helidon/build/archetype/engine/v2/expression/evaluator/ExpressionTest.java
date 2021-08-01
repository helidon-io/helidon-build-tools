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
    public void testEvaluateWithVariables() throws ParserException {
        String value = "${var1} contains ${var2}";
        Expression expression = new Expression(value);
        Map<String, String> varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "['a','b','c']");
        varInitializerMap.put("var2", "'b'");
        Object evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(true));

        value = "!(${array} contains 'basic-auth' == false && ${var})";
        expression = new Expression(value);
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var", "true");
        varInitializerMap.put("array", "['a','b','c']");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(false));

        value = "!${var}";
        expression = new Expression(value);
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var", "true");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(false));

        value = "['', 'adc', 'def'] contains ${var1} == ${var4} && ${var2} || !${var3}";
        expression = new Expression(value);
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "'abc'");
        varInitializerMap.put("var4", "true");
        varInitializerMap.put("var2", "false");
        varInitializerMap.put("var3", "true");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(false));

        value = "${var1} contains ${var2} == ${var3} && ${var4} || ${var5}";
        expression = new Expression(value);
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "['a','b','c']");
        varInitializerMap.put("var2", "'d'");
        varInitializerMap.put("var3", "true");
        varInitializerMap.put("var4", "true");
        varInitializerMap.put("var5", "false");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(false));

        value = " ${var1} == ${var1} && ${var2} contains ''";
        expression = new Expression(value);
        varInitializerMap = new HashMap<>();
        varInitializerMap.put("var1", "'foo'");
        varInitializerMap.put("var2", "['d','']");
        evaluate = expression.evaluate(varInitializerMap);
        assertThat((Boolean) evaluate, is(true));
    }

    @Test
    public void testVariable() throws ParserException {
        String value = "${variable}";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isVariable(), is(true));
        assertThat(expression.tree().asVariable().getName(), is("variable"));

        value = "${variable} == 'some string'";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asVariable().getName(), is("variable"));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'some string'"));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));

        value = "'some string' != ${variable}";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asVariable().getName(), is("variable"));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("'some string'"));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));

        //incorrect variable name
        Exception e = assertThrows(ParserException.class, () -> {
            var stringValue = "${varia!ble}";
            Expression expr = new Expression(stringValue);
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "unexpected token - ${varia!ble}"));
    }

    @Test
    public void testEvaluate() throws ParserException {
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
            var stringValue = "true == 'def'";
            Expression expr = new Expression(stringValue);
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation '==' cannot be performed on literals. The left literal "));

        //incorrect type of the operands
        e = assertThrows(ParserException.class, () -> {
            var stringValue = "'true' || 'def'";
            Expression expr = new Expression(stringValue);
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation '||' cannot be performed on literals. The literal "));

        e = assertThrows(ParserException.class, () -> {
            var stringValue = "['', 'adc', 'def'] contains ['', 'adc', 'def']";
            Expression expr = new Expression(stringValue);
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Operation 'contains' cannot be performed on literals. The literal "));

        //not initialized variable
        e = assertThrows(IllegalArgumentException.class, () -> {
            var stringValue = "true == ${def}";
            Expression expr = new Expression(stringValue);
            expr.evaluate();
        });
        assertThat(e.getMessage(), containsString(
                "Variable def must be initialized"));
    }

    private void evaluateAndTestLogicalExpression(String value, boolean expectedResult) throws ParserException {
        Expression expression = new Expression(value);
        Object evaluate = expression.evaluate();
        assertThat((Boolean) evaluate, is(expectedResult));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testContainsOperator() throws ParserException {
        String value = "['', 'adc', 'def'] contains 'basic-auth'";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(),
                contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.CONTAINS));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'basic-auth'"));

        Exception e = assertThrows(ParserException.class, () -> {
            var expr = "['', 'adc', 'def'] contains != 'basic-auth'";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unexpected token - !="));

        value = "!(['', 'adc', 'def'] contains 'basic-auth')";
        expression = new Expression(value);
        assertThat(expression.tree().isUnaryLogicalExpression(), is(true));
        assertThat(expression.tree().asUnaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT));
        assertThat((List<String>) expression.tree()
                        .asUnaryLogicalExpression()
                        .getLeft().asBinaryLogicalExpression().getLeft().asLiteral().getValue(),
                contains("''", "'adc'", "'def'"));
        assertThat(expression.tree()
                        .asUnaryLogicalExpression()
                        .getLeft().asBinaryLogicalExpression().getRight().asLiteral().getValue(),
                is("'basic-auth'"));
        assertThat(expression.tree()
                        .asUnaryLogicalExpression()
                        .getLeft().asBinaryLogicalExpression().getOperator(),
                Matchers.is(LogicalOperator.CONTAINS));

        e = assertThrows(ParserException.class, () -> {
            var expr = "!['', 'adc', 'def'] contains 'basic-auth'";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Incorrect operand type for the unary logical expression"));

        value = "['', 'adc', 'def'] contains 'basic-auth' == true && false";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat((List<String>) expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.CONTAINS));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is(false));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.AND));

        value = "(['', 'adc', 'def'] contains 'basic-auth') == true && false";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat((List<String>) expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.CONTAINS));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression().isIsolated(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is(false));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.AND));


        value = " 'aaa' == 'bbb' && ['', 'adc', 'def'] contains ${basic-auth}";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), is("'aaa'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is("'bbb'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat((List<String>) expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getRight().asVariable().getName(), is("basic-auth"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.CONTAINS));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.AND));

        value = " 'aaa' == ${bbb} && (['', 'adc', 'def'] contains 'basic-auth')";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), is("'aaa'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getRight().asVariable().getName(), is("bbb"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getLeft().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat((List<String>) expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getLeft().asLiteral().getValue(), contains("''", "'adc'", "'def'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getRight().asLiteral().getValue(), is("'basic-auth'"));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.CONTAINS));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getRight().asBinaryLogicalExpression()
                .isIsolated(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression()
                .getOperator(), Matchers.is(LogicalOperator.AND));
    }

    @Test
    public void testUnaryExpression() throws ParserException {
        String value = "!true";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isUnaryLogicalExpression(), is(true));
        assertThat(expression.tree().asUnaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT));
        assertThat(expression.tree().asUnaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asUnaryLogicalExpression().getLeft().asLiteral().getType(),
                Matchers.is(Literal.Type.BOOLEAN));
        assertThat(expression.tree().asUnaryLogicalExpression().getLeft().asLiteral().getValue(), is(true));

        value = "!('foo' != 'bar')";
        expression = new Expression(value);
        assertThat(expression.tree().isUnaryLogicalExpression(), is(true));
        assertThat(expression.tree().asUnaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT));
        assertThat(expression.tree().asUnaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        var left = expression.tree().asUnaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("'foo'"));
        assertThat(left.getRight().asLiteral().getValue(), is("'bar'"));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));

        value = "'foo1' == 'bar' && !('foo' != ${bar})";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.AND));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("'foo1'"));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("'bar'"));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isUnaryLogicalExpression(), is(true));
        var right = expression.tree().asBinaryLogicalExpression().getRight().asUnaryLogicalExpression();
        assertThat(right.getOperator(), Matchers.is(LogicalOperator.NOT));
        assertThat(right.getLeft().isBinaryLogicalExpression(), is(true));
        assertThat(right.getLeft().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(right.getLeft().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("'foo'"));
        assertThat(right.getLeft().asBinaryLogicalExpression().getRight().asVariable().getName(), is("bar"));


        //incorrect operand type
        Exception e = assertThrows(ParserException.class, () -> {
            var expr = "!'string type'";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Incorrect operand type for the unary logical expression"));
    }

    @Test
    public void testExpressionWithParenthesis() throws ParserException {
        String value = "(\"foo\") != \"bar\"";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));

        value = "((\"foo\")) != \"bar\"";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));

        value = "((\"foo\") != \"bar\")";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));

        value = "\"foo\" != (\"bar\")";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));

        //first operator has higher precedence
        value = "(\"foo\"==\"bar\")|| ${foo1}";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        var left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("\"bar\""));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isVariable(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asVariable().getName(), is("foo1"));

        //first operator has higher precedence
        value = "(\"foo\"==\"bar\"|| true)";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("\"bar\""));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is(true));

        //first operator has higher precedence
        value = "${foo}==(true|| false)";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isVariable(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asVariable().getName(), is("foo"));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        var right = expression.tree().asBinaryLogicalExpression().getRight().asBinaryLogicalExpression();
        assertThat(right.isBinaryLogicalExpression(), is(true));
        assertThat(right.getLeft().isLiteral(), is(true));
        assertThat(right.getLeft().asLiteral().getValue(), is(true));
        assertThat(right.getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(right.getRight().isLiteral(), is(true));
        assertThat(right.getRight().asLiteral().getValue(), is(false));

        //first operator has higher precedence
        value = "true==((${var}|| false)&&true)";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(right.isBinaryLogicalExpression(), is(true));
        right = expression.tree().asBinaryLogicalExpression().getRight().asBinaryLogicalExpression();
        assertThat(right.getOperator(), Matchers.is(LogicalOperator.AND));
        assertThat(right.getLeft().isBinaryLogicalExpression(), is(true));
        assertThat(right.getLeft().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(right.getLeft().asBinaryLogicalExpression().getLeft().isVariable(), is(true));
        assertThat(right.getLeft().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(right.getLeft().asBinaryLogicalExpression().getLeft().asVariable().getName(), is("var"));
        assertThat(right.getLeft().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is(false));
        assertThat(right.getRight().isLiteral(), is(true));
        assertThat(right.getRight().asLiteral().getValue(), is(true));

        //incorrect parenthesis
        Exception e = assertThrows(ParserException.class, () -> {
            var expr = "\"foo\"==((\"bar\"|| 'foo1')&&true))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = "\"foo\")==((\"bar\"|| 'foo1')&&true))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = "\"foo\"(==((\"bar\"|| 'foo1')&&true))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = ")\"foo\"(==((\"bar\"|| 'foo1')&&true))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLiteralWithParenthesis() throws Exception {
        //boolean literal
        String value = "(true)";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(true));

        //boolean literal and nested parenthesis
        value = "((true))";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(true));

        //variable
        value = "(${var})";
        expression = new Expression(value);
        assertThat(expression.tree().isVariable(), is(true));
        assertThat(expression.tree().asVariable().getName(), is("var"));

        //string literal
        value = "(\"value\")";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is("\"value\""));

        //string literal and nested parenthesis
        value = "((\"value\"))";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is("\"value\""));

        //string literal with incorrect parenthesis
        Exception e = assertThrows(ParserException.class, () -> {
            var expr = "((((\"value\"))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //string literal with incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = ")\"value\"(";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //string literal with incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = "(\"value\"()";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unknown AbstractSyntaxTree type"));

        //empty array
        value = "([])";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((ArrayList<String>) expression.tree().asLiteral().getValue()).isEmpty(), is(true));

        //empty array and nested parenthesis
        value = "(([]))";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((ArrayList<String>) expression.tree().asLiteral().getValue()).isEmpty(), is(true));

        //array with 1 element
        value = "([''])";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().getValue(), contains("''"));

        //array with 1 element and nested parenthesis
        value = "((['']))";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().getValue(), contains("''"));

        //array with many elements
        value = "(['', 'adc', 'def'])";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().getValue(), contains("''", "'adc'", "'def'"));

        //array with many elements and nested parenthesis
        value = "((['', 'adc', 'def']))";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((ArrayList<String>) expression.tree().asLiteral().getValue(), contains("''", "'adc'", "'def'"));

        //array with many elements and incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = "((['', 'adc', 'def'])))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

        //array with many elements and incorrect parenthesis
        e = assertThrows(ParserException.class, () -> {
            var expr = "(((['', 'adc', 'def']))";
            new Expression(expr);
        });
        assertThat(e.getMessage(), containsString("Unmatched parenthesis found"));

    }

    @Test
    public void testFewOperatorsWithDifferentPrecedenceInOneExpression() throws ParserException {
        /*
         * first operator has higher precedence
         */
        //first case - 3 string literals
        String value = "\"foo\"==\"bar\"|| 'foo1'";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        var left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("\"bar\""));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'foo1'"));

        //first case - 4 string literals
        value = "\"foo\"==\"bar\" && 'foo1' || 'bar1'";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isBinaryLogicalExpression(), is(true));
        assertThat(left.getLeft().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(left.getLeft().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(left.getLeft().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));
        assertThat(left.getLeft().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("'foo1'"));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.AND));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'bar1'"));

        /*
         *  second operator has higher precedence
         */

        //first case - 3 literals
        value = "\"foo\" && \"bar\" == 'foo1'";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.AND));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isBinaryLogicalExpression(), is(true));
        var right = expression.tree().asBinaryLogicalExpression().getRight().asBinaryLogicalExpression();
        assertThat(right.getLeft().isLiteral(), is(true));
        assertThat(right.getLeft().asLiteral().getValue(), is("\"bar\""));
        assertThat(right.getRight().isLiteral(), is(true));
        assertThat(right.getRight().asLiteral().getValue(), is("'foo1'"));
        assertThat(right.getOperator(), Matchers.is(LogicalOperator.EQUAL));

        //first case - 4 literals
        value = "true && ${bar} == 'foo1' || false";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.OR));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is(true));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.AND));
        assertThat(left.getRight().isBinaryLogicalExpression(), is(true));
        assertThat(left.getRight().asBinaryLogicalExpression().getLeft().isVariable(), is(true));
        assertThat(left.getRight().asBinaryLogicalExpression().getLeft().asVariable().getName(), is("bar"));
        assertThat(left.getRight().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(left.getRight().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(left.getRight().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'foo1'"));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is(false));

    }

    @Test
    public void testFewOperatorsWithEqualPrecedenceInOneExpression() throws ParserException {
        //first case - string literals
        String value = "\"foo\"!=\"bar\"=='foo1'";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        var left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(left.getRight().isLiteral(), is(true));
        assertThat(left.getRight().asLiteral().getValue(), is("\"bar\""));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("'foo1'"));

        //second case - different types of the literals
        value = "'foo'!=${var}==true";
        expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isBinaryLogicalExpression(), is(true));
        left = expression.tree().asBinaryLogicalExpression().getLeft().asBinaryLogicalExpression();
        assertThat(left.getLeft().isLiteral(), is(true));
        assertThat(left.getLeft().asLiteral().getValue(), is("'foo'"));
        assertThat(left.getRight().isVariable(), is(true));
        assertThat(left.getRight().asVariable().getName(), is("var"));
        assertThat(left.getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is(true));

    }

    @Test
    public void testIncorrectOperator() {
        final Exception e = assertThrows(ParserException.class, () -> {
            String value = "\"foo\" !== \"bar\"";
            new Expression(value);
        });
        assertThat(e.getMessage(), containsString("unexpected token - = \"bar\""));
    }

    @Test
    public void simpleNotEqualStringLiterals() throws Exception {
        String value = "\"foo\" != \"bar\"";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isBinaryLogicalExpression(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().isLiteral(), is(true));
        assertThat(expression.tree().asBinaryLogicalExpression().getOperator(), Matchers.is(LogicalOperator.NOT_EQUAL));
        assertThat(expression.tree().asBinaryLogicalExpression().getLeft().asLiteral().getValue(), is("\"foo\""));
        assertThat(expression.tree().asBinaryLogicalExpression().getRight().asLiteral().getValue(), is("\"bar\""));
    }

    @Test
    public void testStringLiteralWithDoubleQuotes() throws Exception {
        String value = "\"value\"";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(value));
    }

    @Test
    public void testStringLiteralWithSingleQuotes() throws Exception {
        String value = "'value'";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(value));
    }

    @Test
    public void testStringLiteralWithWhitespaces() throws Exception {
        String value = "'value'";
        Expression expression = new Expression(" " + value + "   ");
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(value));
    }

    @Test
    public void testBooleanLiteral() throws Exception {
        String value = "true";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(expression.tree().asLiteral().getValue(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyStringArrayLiteral() throws Exception {
        String value = "[]";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat(((List<String>) expression.tree().asLiteral().getValue()).isEmpty(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayWithEmptyLiteral() throws Exception {
        String value = "['']";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().getValue(), contains("''"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayWithStringLiterals() throws Exception {
        String value = "['', 'adc', 'def']";
        Expression expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().getValue(), contains("''", "'adc'", "'def'"));

        value = "['','adc','def']";
        expression = new Expression(value);
        assertThat(expression.tree().isLiteral(), is(true));
        assertThat((List<String>) expression.tree().asLiteral().getValue(), contains("''", "'adc'", "'def'"));
    }

}