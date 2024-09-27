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

import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Expression;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ValueHandler}
 */
public class ValueHandlerTest {

    @Test
    public void testBacktickExpression() {
        Map<String, Value> variables;
        Value result;

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"),
                "var3", Value.create("c"));
        result = ValueHandler.process("`${shape} == 'circle' ? ${var1} contains ${var2} ? 'circle_b' : 'not_circle_b'"
                + " : ${var1} contains ${var3} ? 'circle_c' : 'not_circle_c'`", variables::get);
        assertThat(result.asText(), is("circle_b"));

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        result = ValueHandler.process("`${shape} == 'circle' ? ${var1} contains ${var2} : false`", variables::get);
        assertThat(result.asBoolean(), is(true));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("`${shape} == 'circle' ? 'red' : 'blue'`", variables::get);
        assertThat(result.asString(), is("red"));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("`${shape}`", variables::get);
        assertThat(result.asString(), is("circle"));

        Map<String, Value> varMap = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create("circle"));
        Expression.FormatException e = assertThrows(Expression.FormatException.class,
                () -> ValueHandler.process("`${shape} == var1`", varMap::get));
        assertThat(e.getMessage(), containsString("Unexpected token - var1"));

        e = assertThrows(Expression.FormatException.class,
                () -> ValueHandler.process("`shape`"));
        assertThat(e.getMessage(), containsString("Unexpected token - shape"));
    }

    @Test
    public void testBraceVarExpression() {
        Map<String, Value> variables;
        Value result;

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"),
                "var3", Value.create("c"));
        result = ValueHandler.process("#{${shape} == 'circle' ? ${var1} contains ${var2} ? 'circle_b' : 'not_circle_b'"
                + " : ${var1} contains ${var3} ? 'circle_c' : 'not_circle_c'}", variables::get);
        assertThat(result.asText(), is("circle_b"));

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        result = ValueHandler.process("#{${shape} == 'circle' ? ${var1} contains ${var2} : false}", variables::get);
        assertThat(result.asBoolean(), is(true));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("#{${shape} == 'circle' ? 'red' : 'blue'}", variables::get);
        assertThat(result.asString(), is("red"));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("#{${shape}}", variables::get);
        assertThat(result.asString(), is("circle"));

        Map<String, Value> varMap = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create("circle"));
        Expression.FormatException e = assertThrows(Expression.FormatException.class,
                () -> ValueHandler.process("#{${shape} == var1}", varMap::get));
        assertThat(e.getMessage(), containsString("Unexpected token - var1"));
    }

    @Test
    public void testNoBraceVarExpression() {
        Map<String, Value> variables;
        Value result;

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"),
                "var3", Value.create("c"));
        result = ValueHandler.process("#{shape == 'circle' ? var1 contains var2 ? 'circle_b' : 'not_circle_b' : "
                + "var1 contains var3 ? 'circle_c' : 'not_circle_c'}", variables::get);
        assertThat(result.asText(), is("circle_b"));

        variables = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create(List.of("a", "b", "c")),
                "var2", Value.create("b"));
        result = ValueHandler.process("#{shape == 'circle' ? var1 contains var2 : false}", variables::get);
        assertThat(result.asBoolean(), is(true));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("#{shape == 'circle' ? 'red' : 'blue'}", variables::get);
        assertThat(result.asString(), is("red"));

        variables = Map.of("shape", Value.create("circle"));
        result = ValueHandler.process("#{shape}", variables::get);
        assertThat(result.asString(), is("circle"));

        result = ValueHandler.process("`true`");
        assertThat(result.asBoolean(), is(true));

        result = ValueHandler.process("#{1}");
        assertThat(result.asInt(), is(1));

        result = ValueHandler.process("`['', 'adc', 'def']`");
        assertThat(result.asList(), containsInAnyOrder("", "adc", "def"));

        Map<String, Value> varMap = Map.of(
                "shape", Value.create("circle"),
                "var1", Value.create("circle"));
        Expression.FormatException e = assertThrows(Expression.FormatException.class,
                () -> ValueHandler.process("#{${shape} ^ var1}", varMap::get));
        assertThat(e.getMessage(), containsString("Unexpected token - ^"));
    }

    @Test
    public void testNoExpression() {
        var value = ValueHandler.process("circle");
        assertThat(value.asString(), is("circle"));

        value = ValueHandler.process("true");
        assertThat(value.asString(), is("true"));

        value = ValueHandler.process("1");
        assertThat(value.asString(), is("1"));

        value = ValueHandler.process("['', 'adc', 'def']");
        assertThat(value.asString(), is("['', 'adc', 'def']"));
    }
}
