/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Expression.FormatException;
import io.helidon.build.archetype.engine.v2.ast.Expression.UnresolvedVariableException;
import io.helidon.build.archetype.engine.v2.ast.Value.ValueTypeException;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.ast.Expression.parse;
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
    public void testTernaryExpression() {
        Expression exp;
        Map<String, Value> variables;

        exp = Expression.parse("'circle'");
        assertThat(exp.eval().asText(), is("circle"));

        exp = Expression.parse("${shape} == 'circle' ? 'red' : 'blue'");
        variables = Map.of("shape", Value.create("circle"));
        assertThat(exp.eval(variables::get).asText(), is("red"));

        exp = Expression.parse("${shape} != 'circle' ? 'red' : 'blue'");
        variables = Map.of("shape", Value.create("circle"));
        assertThat(exp.eval(variables::get).asText(), is("blue"));

        exp = Expression.parse("false ? 'red' : 'blue'");
        assertThat(exp.eval(variables::get).asText(), is("blue"));

        exp = Expression.parse("${shape} == 'circle' ? ${var1} contains ${var2} : false");
        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        assertThat(exp.eval(variables::get).asBoolean(), is(true));

        exp = Expression.parse("${shape} == 'circle' ? ${var1} contains ${var2} ? 'circle_b' : 'not_circle_b' : ${var1} "
                + "contains ${var3} ? 'circle_c' : 'not_circle_c'");
        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"),
                "var3", Value.create("c"));
        assertThat(exp.eval(variables::get).asText(), is("circle_b"));

        exp = Expression.parse("${shape} == 'circle' ? (${var1} contains ${var2} ? 'circle_b' : 'not_circle_b') : (${var1} "
                + "contains ${var3} ? 'circle_c' : 'not_circle_c')");
        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"),
                "var3", Value.create("c"));
        assertThat(exp.eval(variables::get).asText(), is("circle_b"));

        exp = Expression.parse("${shape} != 'circle' ? ${var1} contains ${var2} : false");
        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        assertThat(exp.eval(variables::get).asBoolean(), is(false));

        exp = Expression.parse("${var1} contains ${var2} == ${var3} && ${var4} || ${var5} ? ['', 'adc', 'def'] contains 'foo' "
                + "== false && true || !false : ['', 'adc', 'def'] contains 'foo' == true && false");
        variables = Map.of(
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("c"),
                "var3", Value.TRUE,
                "var4", Value.TRUE,
                "var5", Value.FALSE);
        assertThat(exp.eval(variables::get).asBoolean(), is(true));
    }

    @Test
    void testEvaluateWithVariables() {
        Expression exp;
        Map<String, Value> variables;

        exp = Expression.parse("${var1} contains ${var2}");
        variables = Map.of(
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        assertThat(exp.eval(variables::get).asBoolean(), is(true));

        exp = Expression.parse("!(${array} contains 'basic-auth' == false && ${var})");
        variables = Map.of(
                "var", Value.TRUE,
                "array", Value.create(List.of("a", "b", "c")));
        assertThat(exp.eval(variables::get).asBoolean(), is(false));

        exp = Expression.parse("!${var}");
        variables = Map.of("var", Value.TRUE);
        assertThat(exp.eval(variables::get).asBoolean(), is(false));

        exp = Expression.parse("['', 'adc', 'def'] contains ${var1} == ${var4} && ${var2} || !${var3}");
        variables = Map.of(
                "var1", Value.create("abc"),
                "var2", Value.FALSE,
                "var3", Value.TRUE,
                "var4", Value.TRUE);
        assertThat(exp.eval(variables::get).asBoolean(), is(false));

        exp = Expression.parse("${var1} contains ${var2} == ${var3} && ${var4} || ${var5}");
        variables = Map.of(
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("c"),
                "var3", Value.TRUE,
                "var4", Value.TRUE,
                "var5", Value.FALSE);
        assertThat(exp.eval(variables::get).asBoolean(), is(true));

        exp = Expression.parse(" ${var1} == ${var1} && ${var2} contains ''");
        variables = Map.of(
                "var1", Value.create("foo"),
                "var2", Value.create(List.of("d", "")));
        assertThat(exp.eval(variables::get).asBoolean(), is(true));
    }

    @Test
    void testVariable() {
        //noinspection SpellCheckingInspection
        Exception e = assertThrows(FormatException.class, () -> Expression.parse("${varia!ble}"));
        //noinspection SpellCheckingInspection
        assertThat(e.getMessage(), containsString("Unexpected token - ${varia!ble}"));
    }

    @Test
    void testEvaluate() {
        assertThat(parse("['', 'adc', 'def'] contains 'foo'").eval().asBoolean(), is(false));
        assertThat(parse("!(['', 'adc', 'def'] contains 'foo' == false && false)").eval().asBoolean(), is(true));
        assertThat(parse("!false").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc', 'def'] contains 'foo' == false && true || !false").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc', 'def'] contains 'foo' == false && true || !true").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc', 'def'] contains 'def'").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc', 'def'] contains 'foo' == true && false").eval().asBoolean(), is(false));
        assertThat(parse("['', 'adc', 'def'] contains 'foo' == false && true").eval().asBoolean(), is(true));
        assertThat(parse(" 'aaa' == 'aaa' && ['', 'adc', 'def'] contains ''").eval().asBoolean(), is(true));
        assertThat(parse("true && \"bar\" == 'foo1' || true").eval().asBoolean(), is(true));
        assertThat(parse("true && \"bar\" == 'foo1' || false").eval().asBoolean(), is(false));
        assertThat(parse("('def' != 'def1') && false == true").eval().asBoolean(), is(false));
        assertThat(parse("('def' != 'def1') && false").eval().asBoolean(), is(false));
        assertThat(parse("('def' != 'def1') && true").eval().asBoolean(), is(true));
        assertThat(parse("'def' != 'def1'").eval().asBoolean(), is(true));
        assertThat(parse("'def' == 'def'").eval().asBoolean(), is(true));
        assertThat(parse("'def' != 'def'").eval().asBoolean(), is(false));
        assertThat(parse("true==((true|| false)&&true)").eval().asBoolean(), is(true));
        assertThat(parse("false==((true|| false)&&true)").eval().asBoolean(), is(false));
        assertThat(parse("false==((true|| false)&&false)").eval().asBoolean(), is(true));
        assertThat(parse("true == 'def'").eval().asBoolean(), is(false));

        Throwable e;

        e = assertThrows(ValueTypeException.class, () -> parse("'true' || 'def'").eval());
        assertThat(e.getMessage(), startsWith("Cannot get a value of"));

        assertThat(e.getMessage(), startsWith("Cannot get a value of"));

        e = assertThrows(UnresolvedVariableException.class, () -> parse("true == ${def}").eval());
        assertThat(e.getMessage(), containsString("Unresolved variable"));
    }

    @Test
    void testContainsOperator() {
        assertThat(parse("['', 'adc', 'def'] contains 'foo'").eval().asBoolean(), is(false));
        assertThat(parse("['', 'adc', 'def'] contains ['', 'adc']").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc', 'def'] contains ['', 'adc', 'def']").eval().asBoolean(), is(true));
        assertThat(parse("['', 'adc'] contains ['', 'adc', 'def']").eval().asBoolean(), is(false));
        assertThat(parse("['', 'adc'] contains ['', 'adc', 'def']").eval().asBoolean(), is(false));

        FormatException e = assertThrows(FormatException.class, () -> parse("['', 'adc', 'def'] contains != 'foo'").eval());
        assertThat(e.getMessage(), startsWith("Missing operand"));

        assertThat(parse("!(['', 'adc', 'def'] contains 'foo')").eval().asBoolean(), is(true));

        e = assertThrows(FormatException.class, () -> parse("!['', 'adc', 'def'] contains 'basic-auth'"));
        assertThat(e.getMessage(), containsString("Invalid operand"));
    }

    @Test
    void testUnaryLogicalExpression() {
        assertThat(parse("!true").eval().asBoolean(), is(false));
        assertThat(parse("!false").eval().asBoolean(), is(true));
        assertThat(parse("!('foo' != 'bar')").eval().asBoolean(), is(false));
        assertThat(parse("'foo1' == 'bar' && !('foo' != 'bar')").eval().asBoolean(), is(false));

        FormatException e = assertThrows(FormatException.class, () -> parse("!'string type'").eval());
        assertThat(e.getMessage(), containsString("Invalid operand"));
    }

    @Test
    void testExpressionWithParenthesis() {
        assertThat(parse("(\"foo\") != \"bar\"").eval().asBoolean(), is(true));
        assertThat(parse("((\"foo\")) != \"bar\"").eval().asBoolean(), is(true));

        assertThat(parse("((\"foo\") != \"bar\")").eval().asBoolean(), is(true));
        assertThat(parse("\"foo\" != (\"bar\")").eval().asBoolean(), is(true));
        assertThat(parse("(\"foo\"==\"bar\")|| ${foo1}").eval(s -> Value.TRUE).asBoolean(), is(true));
        assertThat(parse("(\"foo\"==\"bar\"|| true)").eval().asBoolean(), is(true));
        assertThat(parse("${foo}==(true|| false)").eval(s -> Value.TRUE).asBoolean(), is(true));
        assertThat(parse("true==((${var}|| false)&&true)").eval(s -> Value.TRUE).asBoolean(), is(true));

        FormatException e;

        e = assertThrows(FormatException.class, () -> parse("\"foo\"==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> parse("\"foo\")==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> parse("\"foo\"(==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Invalid parenthesis"));

        e = assertThrows(FormatException.class, () -> parse(")\"foo\"(==((\"bar\"|| 'foo1')&&true))"));
        assertThat(e.getMessage(), containsString("Unmatched parenthesis"));
    }

    @Test
    void testLiteralWithParenthesis() {
        assertThat(parse("(true)").eval().asBoolean(), is(true));
        assertThat(parse("((true))").eval().asBoolean(), is(true));
        assertThat(parse("((${var}))").eval(s -> Value.TRUE).asBoolean(), is(true));
        assertThat(parse("(\"value\") == (\"value\")").eval().asBoolean(), is(true));
        assertThat(parse("((\"value\")) == ((\"value\"))").eval().asBoolean(), is(true));
        assertThat(parse("\"(value)\" == \"(value)\"").eval().asBoolean(), is(true));

        FormatException e;

        e = assertThrows(FormatException.class, () -> parse("((((\"value\"))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> parse(")\"value\"(").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> parse("(\"value\"()").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        assertThat(parse("([]) == []").eval().asBoolean(), is(true));
        assertThat(parse("(([])) == []").eval().asBoolean(), is(true));
        assertThat(parse("(['']) == ['']").eval().asBoolean(), is(true));
        assertThat(parse("(([''])) == ['']").eval().asBoolean(), is(true));
        assertThat(parse("(['', 'adc', 'def']) contains 'def'").eval().asBoolean(), is(true));
        assertThat(parse("((['', 'adc', 'def'])) contains 'def'").eval().asBoolean(), is(true));

        e = assertThrows(FormatException.class, () -> parse("((['', 'adc', 'def'])))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));

        e = assertThrows(FormatException.class, () -> parse("(((['', 'adc', 'def']))").eval());
        assertThat(e.getMessage(), startsWith("Unmatched parenthesis"));
    }

    @Test
    void testPrecedence() {
        assertThat(parse("\"foo\"==\"bar\"|| true").eval().asBoolean(), is(true));
        assertThat(parse("\"foo\"==\"bar\" && true || false").eval().asBoolean(), is(false));
        assertThat(parse("true && \"bar\" != 'foo1'").eval().asBoolean(), is(true));
        assertThat(parse("true && ${bar} == 'foo1' || false").eval(s -> Value.create("foo1")).asBoolean(), is(true));
    }

    @Test
    void testEqualPrecedence() {
        assertThat(parse("\"foo\"!=\"bar\"==true").eval().asBoolean(), is(true));
        assertThat(parse("'foo'!=${var}==true").eval(s -> Value.create("bar")).asBoolean(), is(true));
    }

    @Test
    void testIncorrectOperator() {
        FormatException e = assertThrows(FormatException.class, () -> parse("'foo' !== 'bar'"));
        assertThat(e.getMessage(), startsWith("Unexpected token"));
    }

    @Test
    public void simpleNotEqualStringLiterals() {
        assertThat(parse("'foo' != 'bar'").eval().asBoolean(), is(true));
    }

    @Test
    void testStringLiteralWithDoubleQuotes() {
        assertThat(parse("[\"value\"] == \"value\"").eval().asBoolean(), is(false));
    }

    @Test
    void testStringLiteralWithSingleQuotes() {
        assertThat(parse("['value'] == 'value'").eval().asBoolean(), is(false));
    }

    @Test
    void testStringLiteralWithWhitespaces() {
        assertThat(parse("[' value '] != 'value'").eval().asBoolean(), is(true));
    }

    @Test
    void testBooleanLiteral() {
        assertThat(parse("true").eval().asBoolean(), is(true));
        assertThat(parse("false").eval().asBoolean(), is(false));
        assertThat(parse("true == true").eval().asBoolean(), is(true));
        assertThat(parse("false == false").eval().asBoolean(), is(true));
        assertThat(parse("true != false").eval().asBoolean(), is(true));
    }

    @Test
    void testEmptyStringArrayLiteral() {
        assertThat(parse("[] == []").eval().asBoolean(), is(true));
    }

    @Test
    void testArrayWithEmptyLiteral() {
        assertThat(parse("[''] contains ''").eval().asBoolean(), is(true));
    }

    @Test
    void testArrayWithStringLiterals() {
        assertThat(parse("['foo'] contains 'foo'").eval().asBoolean(), is(true));
        assertThat(parse("['foo'] contains 'bar'").eval().asBoolean(), is(false));
    }

    @Test
    void testComplex() {
        Expression expr = parse("!(${metrics} || ${tracing} || ${health}) || (${metrics} && ${tracing} && ${health})");
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "true",
                "tracing", "true",
                "health", "true"), DynamicValue::create)::get).asBoolean(), is(true));
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "false",
                "tracing", "true",
                "health", "true"), DynamicValue::create)::get).asBoolean(), is(false));
        assertThat(expr.eval(mapValue(Map.of(
                "metrics", "false",
                "tracing", "false",
                "health", "false"), DynamicValue::create)::get).asBoolean(), is(true));
    }

    @Test
    void testUnaryPrecededWithLeftParenthesis() {
        assertThat(parse("(!true)").eval().asBoolean(), is(false));
    }

    @Test
    void testMultilineExpression() {
        assertThat(parse("true && \ntrue").eval().asBoolean(), is(true));
    }

    @Test
    void testMultilineStringLiteral() {
        Expression expr = parse("${str} == \"f\no\no\no\"");
        assertThat(expr.eval(mapValue(Map.of("str", "f\no\no\no"), DynamicValue::create)::get).asBoolean(), is(true));
    }

    @Test
    void testStringContains() {
        assertThat(parse("'foo' contains 'oo'").eval().asBoolean(), is(true));
    }

    @Test
    void testComments() {
        Expression expr = parse(""
                + "    # the entire line is a comment\n"
                + "    true && # inline comment\n"
                + "    !false\n"
                + "    && ${char} == \"#\"");
        assertThat(expr.eval(mapValue(Map.of("char", "#"), DynamicValue::create)::get).asBoolean(), is(true));
    }

    @Test
    void testCommentWithStringLiteral() {
        Expression expr = parse(""
                + "true &&\n"
                + "\n"
                + "# 'foo'\n"
                + "true");
        assertThat(expr.eval().asBoolean(), is(true));
    }

    @Test
    void testNoneList1() {
        Expression expr = parse("${list} == [] || (${list} contains 'foo' || ${list} contains 'bar')");
        assertThat(expr.eval(mapValue(Map.of("list", "none"), DynamicValue::create)::get).asBoolean(), is(true));
        assertThat(expr.eval(mapValue(Map.of("list", "foo,bar"), DynamicValue::create)::get).asBoolean(), is(true));
        assertThat(expr.eval(mapValue(Map.of("list", "bob"), DynamicValue::create)::get).asBoolean(), is(false));
    }

    @Test
    void testNoneList2() {
        Expression expr = parse("${list1} == [] || (${list1} == ['foo'] && ${list2} == ['bar'])");
        assertThat(expr.eval(mapValue(Map.of("list1", "none", "list2", "bar"), DynamicValue::create)::get).asBoolean(), is(true));
        assertThat(expr.eval(mapValue(Map.of("list1", "foo", "list2", "bar"), DynamicValue::create)::get).asBoolean(), is(true));
    }
}
