/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2;

import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.Expression.FormatException;
import io.helidon.build.archetype.engine.v2.Expression.UnresolvedVariableException;
import io.helidon.build.archetype.engine.v2.Value.ValueException;
import io.helidon.build.common.Lists;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.Maps.mapValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link Expression}.
 */
class ExpressionTest {

    @Test
    void testEvaluateWithVariables() {
        Expression exp;
        Map<String, Value<?>> variables;

        exp = new Expression("${v1} contains ${v2}");
        variables = Map.of(
                "v1", Value.of(List.of("a", "b", "c")),
                "v2", Value.of("b"));
        assertThat(exp.eval(variables::get), is(true));

        exp = new Expression("!(${array} contains 'basic-auth' == false && ${var})");
        variables = Map.of(
                "var", Value.TRUE,
                "array", Value.of(List.of("a", "b", "c")));
        assertThat(exp.eval(variables::get), is(false));

        exp = new Expression("!${var}");
        variables = Map.of("var", Value.TRUE);
        assertThat(exp.eval(variables::get), is(false));

        exp = new Expression("['', 'adc', 'def'] contains ${v1} == ${v4} && ${v2} || !${v3}");
        variables = Map.of(
                "v1", Value.of("abc"),
                "v2", Value.FALSE,
                "v3", Value.TRUE,
                "v4", Value.TRUE);
        assertThat(exp.eval(variables::get), is(false));

        exp = new Expression("${v1} contains ${v2} == ${v3} && ${v4} || ${v5}");
        variables = Map.of(
                "v1", Value.of(List.of("a", "b", "c")),
                "v2", Value.of("c"),
                "v3", Value.TRUE,
                "v4", Value.TRUE,
                "v5", Value.FALSE);
        assertThat(exp.eval(variables::get), is(true));

        exp = new Expression(" ${v1} == ${v1} && ${v2} contains ''");
        variables = Map.of(
                "v1", Value.of("foo"),
                "v2", Value.of(List.of("d", "")));
        assertThat(exp.eval(variables::get), is(true));
    }

    @Test
    void testVariable() {
        Exception e = assertThrows(FormatException.class, () -> new Expression("${varia!ble}"));
        assertThat(e.getMessage(), containsString("Unexpected token: ${varia!ble}"));
    }

    @Test
    void testEvaluate() {
        assertThat(expr("['', 'adc', 'def'] contains 'foo'").eval(), is(false));
        assertThat(expr("!(['', 'adc', 'def'] contains 'foo' == false && false)").eval(), is(true));
        assertThat(expr("!false").eval(), is(true));
        assertThat(expr("['', 'adc', 'def'] contains 'foo' == false && true || !false").eval(), is(true));
        assertThat(expr("['', 'adc', 'def'] contains 'foo' == false && true || !true").eval(), is(true));
        assertThat(expr("['', 'adc', 'def'] contains 'def'").eval(), is(true));
        assertThat(expr("['', 'adc', 'def'] contains 'foo' == true && false").eval(), is(false));
        assertThat(expr("['', 'adc', 'def'] contains 'foo' == false && true").eval(), is(true));
        assertThat(expr(" 'aaa' == 'aaa' && ['', 'adc', 'def'] contains ''").eval(), is(true));
        assertThat(expr("true && \"bar\" == 'foo1' || true").eval(), is(true));
        assertThat(expr("true && \"bar\" == 'foo1' || false").eval(), is(false));
        assertThat(expr("('def' != 'def1') && false == true").eval(), is(false));
        assertThat(expr("('def' != 'def1') && false").eval(), is(false));
        assertThat(expr("('def' != 'def1') && true").eval(), is(true));
        assertThat(expr("'def' != 'def1'").eval(), is(true));
        assertThat(expr("'def' == 'def'").eval(), is(true));
        assertThat(expr("'def' != 'def'").eval(), is(false));
        assertThat(expr("true==((true|| false)&&true)").eval(), is(true));
        assertThat(expr("false==((true|| false)&&true)").eval(), is(false));
        assertThat(expr("false==((true|| false)&&false)").eval(), is(true));
        assertThat(expr("true == 'def'").eval(), is(false));

        Throwable e;

        e = assertThrows(ValueException.class, () -> expr("'true' || 'def'").eval());
        assertThat(e.getMessage(), is("Cannot convert a string to a boolean"));

        e = assertThrows(UnresolvedVariableException.class, () -> expr("true == ${def}").eval());
        assertThat(e.getMessage(), containsString("Unresolved variable"));
    }

    @Test
    void testContainsOperator() {
        assertThat(expr("['', 'adc', 'def'] contains 'foo'").eval(), is(false));
        assertThat(expr("['', 'adc', 'def'] contains ['', 'adc']").eval(), is(true));
        assertThat(expr("['', 'adc', 'def'] contains ['', 'adc', 'def']").eval(), is(true));
        assertThat(expr("['', 'adc'] contains ['', 'adc', 'def']").eval(), is(false));
        assertThat(expr("['', 'adc'] contains ['', 'adc', 'def']").eval(), is(false));

        FormatException e = assertThrows(FormatException.class, () -> expr("['', 'adc', 'def'] contains != 'foo'").eval());
        assertThat(e.getMessage(), startsWith("Missing operand"));

        assertThat(expr("!(['', 'adc', 'def'] contains 'foo')").eval(), is(true));

        e = assertThrows(FormatException.class, () -> expr("!['', 'adc', 'def'] contains 'basic-auth'"));
        assertThat(e.getMessage(), containsString("Invalid operand"));
    }

    @Test
    void testGreaterThanOperator() {
        assertThat(expr("1 > 0").eval(), is(true));
        assertThat(expr("0 > -1").eval(), is(true));
        assertThat(expr("-1 > 0").eval(), is(false));
        assertThat(expr("0 > -1").literal(), is("0 > -1"));
    }

    @Test
    void testGreaterOrEqualOperator() {
        assertThat(expr("1 >= 0").eval(), is(true));
        assertThat(expr("0 >= -1").eval(), is(true));
        assertThat(expr("-1 >= 0").eval(), is(false));
        assertThat(expr("0 >= -1").literal(), is("0 >= -1"));
    }

    @Test
    void testLowerThanOperator() {
        assertThat(expr("1 < 0").eval(), is(false));
        assertThat(expr("0 < -1").eval(), is(false));
        assertThat(expr("-1 < 0").eval(), is(true));
        assertThat(expr("0 < -1").literal(), is("0 < -1"));
    }

    @Test
    void testLowerOrEqualOperator() {
        assertThat(expr("1 <= 0").eval(), is(false));
        assertThat(expr("0 <= -1").eval(), is(false));
        assertThat(expr("-1 <= 0").eval(), is(true));
        assertThat(expr("0 <= -1").literal(), is("0 <= -1"));
    }

    @Test
    void testAsListOperator() {
        assertThat(expr("['a', 'b'] contains (list) ${v1}").eval(Map.of("v1", Value.dynamic("a,b"))::get), is(true));
    }

    @Test
    void testAsStringOperator() {
        assertThat(expr("(string) ${v1} contains ','").eval(Map.of("v1", Value.dynamic("a,b"))::get), is(true));
    }

    @Test
    void testAsIntOperator() {
        assertThat(expr("(int) ${v1} == 1").eval(Map.of("v1", Value.dynamic("1"))::get), is(true));
    }

    @Test
    void testSizeOfOperator() {
        assertThat(expr("sizeof((list) ${v1}) == 0").eval(Map.of("v1", Value.dynamic("none"))::get), is(true));
        assertThat(expr("sizeof((list) ${v1}) == 1").eval(Map.of("v1", Value.dynamic("a"))::get), is(true));
        assertThat(expr("sizeof((list) ${v1}) == 2").eval(Map.of("v1", Value.dynamic("a,b"))::get), is(true));
        assertThat(expr("sizeof ${v1} == 3").eval(Map.of("v1", Value.dynamic("abc"))::get), is(true));
        assertThat(expr("sizeof ${v1} == sizeof ${v2}").eval(mapValue(Map.of(
                "v1", "abc",
                "v2", "def"), Value::dynamic)::get), is(true));
    }

    @Test
    void testNotOperator() {
        assertThat(expr("!true").eval(), is(false));
        assertThat(expr("!false").eval(), is(true));
        assertThat(expr("!('foo' != 'bar')").eval(), is(false));
        assertThat(expr("'foo1' == 'bar' && !('foo' != 'bar')").eval(), is(false));

        FormatException e = assertThrows(FormatException.class, () -> expr("!'string type'").eval());
        assertThat(e.getMessage(), containsString("Invalid operand"));
    }

    @Test
    void testParenthesis() {
        assertThat(expr("(\"foo\") != \"bar\"").eval(), is(true));
        assertThat(expr("((\"foo\")) != \"bar\"").eval(), is(true));

        assertThat(expr("((\"foo\") != \"bar\")").eval(), is(true));
        assertThat(expr("\"foo\" != (\"bar\")").eval(), is(true));
        assertThat(expr("(\"foo\"==\"bar\")|| ${foo1}").eval(s -> Value.TRUE), is(true));
        assertThat(expr("(\"foo\"==\"bar\"|| true)").eval(), is(true));
        assertThat(expr("${foo}==(true|| false)").eval(s -> Value.TRUE), is(true));
        assertThat(expr("true==((${var}|| false)&&true)").eval(s -> Value.TRUE), is(true));

        FormatException e;

        e = assertThrows(FormatException.class, () -> expr("\"foo\"==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> expr("\"foo\")==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> expr("\"foo\"(==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Invalid parenthesis"));

        e = assertThrows(FormatException.class, () -> expr(")\"foo\"(==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));
    }

    @Test
    void testLiteralWithParenthesis() {
        assertThat(expr("(true)").eval(), is(true));
        assertThat(expr("((true))").eval(), is(true));
        assertThat(expr("((${var}))").eval(s -> Value.TRUE), is(true));
        assertThat(expr("(\"value\") == (\"value\")").eval(), is(true));
        assertThat(expr("((\"value\")) == ((\"value\"))").eval(), is(true));
        assertThat(expr("\"(value)\" == \"(value)\"").eval(), is(true));

        FormatException e;

        e = assertThrows(FormatException.class, () -> expr("((((\"value\"))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> expr(")\"value\"(").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> expr("(\"value\"()").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        assertThat(expr("([]) == []").eval(), is(true));
        assertThat(expr("(([])) == []").eval(), is(true));
        assertThat(expr("(['']) == ['']").eval(), is(true));
        assertThat(expr("(([''])) == ['']").eval(), is(true));
        assertThat(expr("(['', 'adc', 'def']) contains 'def'").eval(), is(true));
        assertThat(expr("((['', 'adc', 'def'])) contains 'def'").eval(), is(true));

        e = assertThrows(FormatException.class, () -> expr("((['', 'adc', 'def'])))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> expr("(((['', 'adc', 'def']))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));
    }

    @Test
    void testPrecedence() {
        assertThat(expr("\"foo\"==\"bar\"|| true").eval(), is(true));
        assertThat(expr("\"foo\"==\"bar\" && true || false").eval(), is(false));
        assertThat(expr("true && \"bar\" != 'foo1'").eval(), is(true));
        assertThat(expr("true && ${bar} == 'foo1' || false").eval(s -> Value.of("foo1")), is(true));
    }

    @Test
    void testEqualPrecedence() {
        assertThat(expr("\"foo\"!=\"bar\"==true").eval(), is(true));
        assertThat(expr("'foo'!=${var}==true").eval(s -> Value.of("bar")), is(true));
    }

    @Test
    void testIncorrectOperator() {
        FormatException e = assertThrows(FormatException.class, () -> expr("'foo' !== 'bar'"));
        assertThat(e.getMessage(), startsWith("Unexpected token"));
    }

    @Test
    void simpleNotEqualStringLiterals() {
        assertThat(expr("'foo' != 'bar'").eval(), is(true));
    }

    @Test
    void testStringLiteralWithDoubleQuotes() {
        assertThat(expr("[\"value\"] == \"value\"").eval(), is(false));
    }

    @Test
    void testStringLiteralWithSingleQuotes() {
        assertThat(expr("['value'] == 'value'").eval(), is(false));
    }

    @Test
    void testStringLiteralWithWhitespaces() {
        assertThat(expr("[' value '] != 'value'").eval(), is(true));
    }

    @Test
    void testBooleanLiteral() {
        assertThat(expr("true").eval(), is(true));
        assertThat(expr("false").eval(), is(false));
        assertThat(expr("true == true").eval(), is(true));
        assertThat(expr("false == false").eval(), is(true));
        assertThat(expr("true != false").eval(), is(true));
    }

    @Test
    void testEmptyStringArrayLiteral() {
        assertThat(expr("[] == []").eval(), is(true));
    }

    @Test
    void testArrayWithEmptyLiteral() {
        assertThat(expr("[''] contains ''").eval(), is(true));
    }

    @Test
    void testArrayWithStringLiterals() {
        assertThat(expr("['foo'] contains 'foo'").eval(), is(true));
        assertThat(expr("['foo'] contains 'bar'").eval(), is(false));
    }

    @Test
    void testIntLiteral() {
        assertThat(expr("1 == 1").eval(), is(true));
        assertThat(expr("1 != -1").eval(), is(true));
    }

    @Test
    void testComplex() {
        Expression expr = expr("!(${metrics} || ${tracing} || ${health})"
                               + " || (${metrics} && ${tracing} && ${health})");
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "true",
                "tracing", "true",
                "health", "true"), Value::dynamic)::get), is(true));
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "false",
                "tracing", "true",
                "health", "true"), Value::dynamic)::get), is(false));
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "false",
                "tracing", "false",
                "health", "false"), Value::dynamic)::get), is(true));
    }

    @Test
    void testUnaryPrecededWithLeftParenthesis() {
        assertThat(expr("(!true)").eval(), is(false));
    }

    @Test
    void testMultilineExpression() {
        assertThat(expr("true && \ntrue").eval(), is(true));
    }

    @Test
    void testMultilineStringLiteral() {
        Expression expr = expr("${str} == \"f\no\no\no\"");
        assertThat(expr.eval(mapValue(Map.of("str", "f\no\no\no"), Value::dynamic)::get), is(true));
    }

    @Test
    void testStringContains() {
        assertThat(expr("'foo' contains 'oo'").eval(), is(true));
    }

    @Test
    void testComments() {
        Expression expr = expr("    # the entire line is a comment\n"
                               + "    true && # inline comment\n"
                               + "    !false\n"
                               + "    && ${char} == \"#\"");
        assertThat(expr.eval(mapValue(Map.of("char", "#"), Value::dynamic)::get), is(true));
    }

    @Test
    void testCommentWithStringLiteral() {
        Expression expr = expr("true &&\n"
                               + "\n"
                               + "# 'foo'\n"
                               + "true");
        assertThat(expr.eval(), is(true));
    }

    @Test
    void testAltSymbols() {
        assertThat(expr("NOT false").eval(), is(true));
        assertThat(expr("NOT false").literal(), is("!false"));
        assertThat(expr("true OR false").eval(), is(true));
        assertThat(expr("true OR false").literal(), is("true || false"));
        assertThat(expr("true AND true").eval(), is(true));
        assertThat(expr("true AND false").literal(), is("true && false"));
    }

    @Test
    void testNoneList1() {
        Expression expr = expr("[] == ${v1} || (${v1} contains 'a' || ${v1} contains 'b')");
        assertThat(expr.eval(mapValue(Map.of("v1", "none"), Value::dynamic)::get), is(true));
        assertThat(expr.eval(mapValue(Map.of("v1", "a,b"), Value::dynamic)::get), is(true));
        assertThat(expr.eval(mapValue(Map.of("v1", "c"), Value::dynamic)::get), is(false));
    }

    @Test
    void testNoneList2() {
        Expression expr = expr("${v1} == [] || (${v1} == ['a'] && ${v2} == ['b'])");
        assertThat(expr.eval(mapValue(Map.of("v1", "none", "v2", "b"), Value::dynamic)::get), is(true));
        assertThat(expr.eval(mapValue(Map.of("v1", "a", "v2", "b"), Value::dynamic)::get), is(true));
    }

    @Test
    void testEmptyOperands() {
        Expression expr = expr("[] contains ${v1} || (${v2} && ${v3}) || ${v3} contains ${v5}");
        assertThat(expr.eval(s -> Value.empty()), is(false));
    }

    @Test
    void testPrint() {
        assertThat(expr("true").literal(), is("true"));
        assertThat(expr("false").literal(), is("false"));
        assertThat(expr("!true").literal(), is("!true"));
        assertThat(expr("!false").literal(), is("!false"));
        assertThat(expr("!(!false)").literal(), is("!(!false)"));
        assertThat(expr("''").literal(), is("''"));
        assertThat(expr("'abc'").literal(), is("'abc'"));
        assertThat(expr("${v1} == []").literal(), is("${v1} == []"));
        assertThat(expr("!${v1}").literal(), is("!${v1}"));
        assertThat(expr("${v1} == 'abc'").literal(), is("${v1} == 'abc'"));
        assertThat(expr("${v1} != 'abc'").literal(), is("${v1} != 'abc'"));
        assertThat(expr("${v1} && ${v2}").literal(), is("${v1} && ${v2}"));
        assertThat(expr("${v1} || ${v2}").literal(), is("${v1} || ${v2}"));
        assertThat(expr("(${v1} || ${v2}) && !${v3}").literal(), is("(${v1} || ${v2}) && !${v3}"));
        assertThat(expr("['a', 'b'] contains ${v1}").literal(), is("['a','b'] contains ${v1}"));
        assertThat(expr("(${v1} == ${v2}) || (((${v3} && ${v4}) || ${v5}) && ${v6})").literal(),
                is("${v1} == ${v2} || (${v3} && ${v4} || ${v5}) && ${v6}"));

        assertThat(expr("${v1} && !(${v2} contains 'a')").literal(), is("${v1} && !(${v2} contains 'a')"));
        assertThat(expr("${v1} && ${v2} && ${v2}").literal(), is("${v1} && ${v2} && ${v2}"));

        assertThat(expr("sizeof ((list) ${media}) > 1 && ${app-type} == 'custom'").literal(),
                is("sizeof((list)${media}) > 1 && ${app-type} == 'custom'"));
    }

    @Test
    void testReduceNoVars() {
        assertThat(expr("false || true").reduce(), is(expr("true")));
        assertThat(expr("false || false").reduce(), is(expr("false")));
        assertThat(expr("true && false").reduce(), is(expr("false")));
        assertThat(expr("true && true").reduce(), is(expr("true")));
        assertThat(expr("!false").reduce(), is(expr("true")));
        assertThat(expr("!true").reduce(), is(expr("false")));
        assertThat(expr("'abc' contains 'a'").reduce(), is(expr("true")));
        assertThat(expr("'abc' contains 'd'").reduce(), is(expr("false")));
        assertThat(expr("'abc' == 'def'").reduce(), is(expr("false")));
        assertThat(expr("'abc' != 'def'").reduce(), is(expr("true")));
        assertThat(expr("['a','b'] contains 'a'").reduce(), is(expr("true")));
        assertThat(expr("['a','b'] contains 'c'").reduce(), is(expr("false")));
        assertThat(expr("['a','b'] contains ['a']").reduce(), is(expr("true")));
        assertThat(expr("['a','b'] contains ['a','c']").reduce(), is(expr("false")));
        assertThat(expr("['a','b'] contains ['c']").reduce(), is(expr("false")));
        assertThat(expr("['a','b'] == 'ab'").reduce(), is(expr("false")));
        assertThat(expr("['a','b'] != 'ab'").reduce(), is(expr("true")));
        assertThat(expr("1 > 0").reduce(), is(expr("true")));
        assertThat(expr("1 >= 0").reduce(), is(expr("true")));
        assertThat(expr("1 < 0").reduce(), is(expr("false")));
        assertThat(expr("1 <= 0").reduce(), is(expr("false")));
    }

    @Test
    void testReduceSynthetic() {
        assertThat(expr("${v1} == ${v2}").reduce(), is(expr("${v1} == ${v2}")));
        assertThat(expr("${v1} != ${v2}").reduce(), is(expr("${v1} != ${v2}")));
        assertThat(expr("!(${v1} == ${v2})").reduce(), is(expr("${v1} != ${v2}")));
        assertThat(expr("!(${v1} != ${v2})").reduce(), is(expr("${v1} == ${v2}")));
        assertThat(expr("${v1} || ('abc' contains 'a')").reduce(), is(expr("true")));
        assertThat(expr("${v1} || !('abc' contains 'a')").reduce(), is(expr("${v1}")));
        assertThat(expr("${v1} && !('abc' contains 'a')").reduce(), is(expr("false")));
        assertThat(expr("${v1} && ('abc' contains 'a')").reduce(), is(expr("${v1}")));
        assertThat(expr("('abc' contains 'a') || !${v1}").reduce(), is(expr("true")));
        assertThat(expr("!('abc' contains 'a') || !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("!${v1} && !('abc' contains 'a')").reduce(), is(expr("false")));
        assertThat(expr("!${v1} && ('abc' contains 'a')").reduce(), is(expr("!${v1}")));
        assertThat(expr("!('abc' contains 'a') && !${v1}").reduce(), is(expr("false")));
        assertThat(expr("('abc' contains 'a') && !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("!('abc' contains 'a') || !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("('abc' contains 'a') || (!${v1} && ${v2})").reduce(), is(expr("true")));
        assertThat(expr("!('abc' contains 'a') || (!${v1} && ${v2})").reduce(), is(expr("(!${v1} && ${v2})")));
        assertThat(expr("!('abc' contains 'a') && (!${v1} && ${v2})").reduce(), is(expr("false")));
        assertThat(expr("('abc' contains 'a') && (!${v1} && ${v2})").reduce(), is(expr("(!${v1} && ${v2})")));

        assertThat(expr("${v1} > 0").reduce(), is(expr("${v1} > 0")));
        assertThat(expr("${v1} >= 0").reduce(), is(expr("${v1} >= 0")));
        assertThat(expr("${v1} < 0").reduce(), is(expr("${v1} < 0")));
        assertThat(expr("${v1} <= 0").reduce(), is(expr("${v1} <= 0")));
        assertThat(expr("sizeof(${v1}) == 0").reduce(), is(expr("sizeof(${v1}) == 0")));
        assertThat(expr("sizeof(${v1}) == sizeof(${v2})").reduce(), is(expr("sizeof(${v1}) == sizeof(${v2})")));
        assertThat(expr("sizeof((list) ${v1}) == 0").reduce(), is(expr("sizeof((list) ${v1}) == 0")));
        assertThat(expr("['a', 'b'] contains (list) ${v1}").reduce(), is(expr("['a', 'b'] contains (list) ${v1}")));
        assertThat(expr("(string) ${v1} contains ','").reduce(), is(expr("(string) ${v1} contains ','")));
        assertThat(expr("(int) ${v1} == 1").reduce(), is(expr("(int) ${v1} == 1")));
        assertThat(expr("sizeof((list) ${v1}) == 0").reduce(), is(expr("sizeof((list) ${v1}) == 0")));
    }

    @Test
    void testReduceConstants() {
        assertThat(expr("${v1} || true").reduce(), is(expr("true")));
        assertThat(expr("${v1} || false").reduce(), is(expr("${v1}")));
        assertThat(expr("true || (!${v1} && ${v2})").reduce(), is(expr("true")));
        assertThat(expr("${v1} && false").reduce(), is(expr("false")));
        assertThat(expr("${v1} && true").reduce(), is(expr("${v1}")));
        assertThat(expr("!${v1} || true").reduce(), is(expr("true")));
        assertThat(expr("true || !${v1}").reduce(), is(expr("true")));
        assertThat(expr("false || !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("!${v1} && false").reduce(), is(expr("false")));
        assertThat(expr("!${v1} && true").reduce(), is(expr("!${v1}")));
        assertThat(expr("false && !${v1}").reduce(), is(expr("false")));
        assertThat(expr("true && !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("false || !${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("false || (!${v1} && ${v2})").reduce(), is(expr("(!${v1} && ${v2})")));
        assertThat(expr("false && (!${v1} && ${v2})").reduce(), is(expr("false")));
        assertThat(expr("true && (!${v1} && ${v2})").reduce(), is(expr("(!${v1} && ${v2})")));
        assertThat(expr("${a} && !${a}").reduce(), is(expr("false")));
        assertThat(expr("${a} && !${a} && ${b} && !${b}").reduce(), is(expr("false")));
        assertThat(expr("${a} && ${b} && !(${a} && ${b})").reduce(), is(expr("false")));
    }

    @Test
    void testReduce() {
        assertThat(expr("${v1}").reduce(), is(expr("${v1}")));
        assertThat(expr("!${v1}").reduce(), is(expr("!${v1}")));
        assertThat(expr("!${v1} && ${v2}").reduce(), is(expr("!${v1} && ${v2}")));
        assertThat(expr("${v1} == true").reduce(), is(expr("${v1}")));
        assertThat(expr("${v1} == false").reduce(), is(expr("!${v1}")));
        assertThat(expr("${a} && ${d} || ${a} && ${b} && ${c}").reduce(), is(expr("${a} && ${d} || ${a} && ${b} && ${c}")));
    }

    @Test
    void testReduceTerms4Vars1() {
        assertThat(Expression.reduce(0, 1, 2, 4, 6, 8, 9, 11, 13, 15),
                is(a(i(0, 9), i(0, 6), i(9, 6))));
    }

    @Test
    void testReduceTerms4Vars2() {
        assertThat(Expression.reduce(4, 8, 9, 10, 12, 11, 14, 15),
                is(a(i(4, 8), i(8, 3), i(10, 5))));
    }

    @Test
    void testReduceTerms7Vars() {
        assertThat(Expression.reduce(20, 28, 52, 60), is(a(i(20, 40))));
    }

    @Test
    void testReduceLong() {
        assertThat(expr("${a} == 'def' && ${b} == 'uvw'"
                        + " || ${a} == 'ghi' && ${c} && ${b} == 'uvw'"
                        + " || ${a} == 'def' && ${c} && ${b} == 'xyz'"
                        + " || ${a} == 'def' && ${b} == 'xyz'"
                        + " || ${a} == 'abc' && ${b} == 'xyz'"
                        + " || ${a} == 'abc' && ${b} == 'uvw'"
                        + " || ${a} == 'def' && ${c} && ${b} == 'uvw'"
                        + " || ${a} == 'ghi' && ${b} == 'uvw'"
                        + " || ${a} == 'abc' && ${c} && ${b} == 'uvw'"
                        + " || ${a} == 'abc' && ${c} && ${b} == 'xyz'"
                        + " || ${a} == 'jkl' && ${c} && ${b} == 'xyz'"
                        + " || ${a} == 'jkl' && ${c} && ${b} == 'uvw'").reduce(),
                is(expr("${a} == 'ghi' && ${b} == 'uvw'"
                        + " || ${a} == 'def' && ${b} == 'xyz'"
                        + " || ${a} == 'def' && ${b} == 'uvw'"
                        + " || ${a} == 'abc' && ${b} == 'xyz'"
                        + " || ${a} == 'abc' && ${b} == 'uvw'"
                        + " || ${a} == 'jkl' && ${b} == 'xyz' && ${c}"
                        + " || ${a} == 'jkl' && ${b} == 'uvw' && ${c}")));
    }

    @Test
    void testReduce5Vars() {
        assertThat(expr("${a} || ${b} || ${c} && ${d} && ${e}").reduce(),
                is(expr("${b} || ${a} || ${c} && ${d} && ${e}")));

        assertThat(expr("${a} && ${b} && ${c} || ${a} && ${d} && ${c}").reduce(),
                is(expr("${a} && ${c} && ${d} || ${a} && ${b} && ${c}")));
    }

    @Test
    void testReduceTokensOrder() {
        assertThat(expr("['a','b'] contains ${a}").reduce(), is(expr("['a','b'] contains ${a}")));
    }

    @Test
    void testSort() {
        assertThat(Lists.sorted(List.of(
                        expr("${a} == 'def' && ${b} == 'xyz'"),
                        expr("${a} == 'abc' && ${b} == 'uvw' && ${c} == '1'"),
                        expr("${a} == 'abc' && ${b} == 'uvw'"))),
                is(List.of(
                        expr("${a} == 'abc' && ${b} == 'uvw'"),
                        expr("${a} == 'abc' && ${b} == 'uvw' && ${c} == '1'"),
                        expr("${a} == 'def' && ${b} == 'xyz'"))));
    }

    @Test
    void testSub() {
        assertThat(expr("true").sub(expr("false")),
                is(expr("true")));
        assertThat(expr("true").sub(expr("true")),
                is(expr("true")));
        assertThat(expr("false").sub(expr("true")),
                is(expr("false")));
        assertThat(expr("false").sub(expr("false")),
                is(expr("false")));
        assertThat(expr("true").sub(expr("${a}")),
                is(expr("true")));
        assertThat(expr("false").sub(expr("${a}")),
                is(expr("false")));
        assertThat(expr("${a}").sub(expr("${a}")),
                is(expr("true")));
        assertThat(expr("${a}").sub(expr("${b}")),
                is(expr("${a}")));
        assertThat(expr("${a} && ${b}").sub(expr("${a}")),
                is(expr("${b}")));
        assertThat(expr("${a} && ${b} && ${c}").sub(expr("${a}")),
                is(expr("${b} && ${c}")));
        assertThat(expr("${a} && ${b} && ${c}").sub(expr("${a} && ${b}")),
                is(expr("${c}")));
        assertThat(expr("${a} && !${b}").sub(expr("${a}")),
                is(expr("!${b}")));
        assertThat(expr("${a} && ${b} && !${c}").sub(expr("${a} && ${b}")),
                is(expr("!${c}")));
        assertThat(expr("${a} && ${b} && !${c}").sub(expr("${a} && !${b}")),
                is(expr("${a} && ${b} && !${c}")));
        assertThat(expr("${a} && ${b} && !${c}").sub(expr("${a}")),
                is(expr("${b} && !${c}")));
        assertThat(expr("!${a} || ${b}").sub(expr("${a}")),
                is(expr("!${a} || ${b}")));
        assertThat(expr("${a} || ${b}").sub(expr("${a}")),
                is(expr("${b}")));
        assertThat(expr("!${a} || ${b}").sub(expr("!${a}")),
                is(expr("${b}")));
        assertThat(expr("${a} && ${b} && ${c} && ${d}").sub(expr("${a} && ${c} && ${d}")),
                is(expr("${b}")));
        assertThat(expr("!${a} && ${c} || !${a} && ${b}").sub(expr("${b} || ${c}")),
                is(expr("!${a}")));
        assertThat(expr("${a} && ${b} && ${c} || ${b} && ${c} && ${d}").sub(expr("${a}")),
                is(expr("${b} && ${c}")));
        assertThat(expr("${a} && ${b} && ${c} || ${b} && ${c} && ${d}").sub(expr("${c}")),
                is(expr("${b} && ${d} || ${a} && ${b}")));
        assertThat(expr("${a}").sub(expr("${a} || ${b} || ${c}")),
                is(expr("true")));
    }

    @Test
    void testInline() {
        assertThat(expr("${v1}").inline(s -> Value.TRUE), is(expr("true")));
        assertThat(expr("${v1}").inline(s -> Value.FALSE), is(expr("false")));
        assertThat(expr("${v1} && ${v2}").inline(Map.of("v1", Value.TRUE)::get), is(expr("${v2}")));
        assertThat(expr("${v1} && ${v2}").inline(Map.of("v1", Value.FALSE)::get), is(expr("false")));
        assertThat(expr("${v1} > 0 && ${v2}").inline(Map.of("v1", Value.of(1))::get), is(expr("${v2}")));
        assertThat(expr("${v1} > 0 && ${v2}").inline(Map.of("v1", Value.of(-1))::get), is(expr("false")));
        assertThat(expr("${v1} == 'a' && ${v2}").inline(Map.of("v1", Value.of("a"))::get), is(expr("${v2}")));
        assertThat(expr("${v1} == 'a' && ${v2}").inline(Map.of("v1", Value.of("b"))::get), is(expr("false")));
        assertThat(expr("${v1} == ['a','b'] && ${v2}").inline(Map.of("v1", Value.of(List.of("a", "b")))::get), is(expr("${v2}")));
        assertThat(expr("${v1} == ['a','b'] && ${v2}").inline(Map.of("v1", Value.of(List.of()))::get), is(expr("false")));
        assertThat(expr("${v1} contains ['a','b'] && ${v2}").inline(Map.of("v1", Value.of(List.of("a", "b")))::get),
                is(expr("${v2}")));
        assertThat(expr("${v1} contains ['a'] && ${v2}").inline(Map.of("v1", Value.of(List.of("a", "b")))::get),
                is(expr("${v2}")));
    }

    @Test
    void testTruth() {
        // ${c} and ${d} are exclusives
        assertThat(expr("(${a} && ${c} || ${b} && ${c}) && ${a} && ${d} && (!${c} && ${d} || ${c} && !${d})").reduce(),
                is(expr("false")));
    }

    @Test
    void testSubTruth() {
        // ${c} and ${d} are exclusives
        assertThat(expr("!${a} && !${b} || !${c} && !${d} && (!${d} && ${c} || ${d} && !${c})").reduce()
                        .sub(expr("!${d} && ${c} || ${d} && !${c}")),
                is(expr("!${a} && !${b}")));
    }

    static Expression expr(String expression) {
        return new Expression(expression);
    }

    static int[][] a(int[]... e) {
        return e;
    }

    static int[] i(int... e) {
        return e;
    }
}
