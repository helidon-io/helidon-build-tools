/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.context.Context;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.inputBoolean;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputEnum;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputList;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputOption;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputText;
import static io.helidon.build.archetype.engine.v2.TestHelper.step;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link TerminalInputResolver}.
 */
class TerminalInputResolverTest {

    @Test
    void testBooleanWithEmptyResponse() {
        Block block = step("step", inputBoolean("boolean-input1", true)).build();

        Context context = prompt(block, "");

        Value value = context.getValue("boolean-input1");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.BOOLEAN));
        assertThat(value.asBoolean(), is(Boolean.TRUE));
    }

    @Test
    void testBooleanWithEmptyResponse2() {
        Block block = step("step", inputBoolean("boolean-input2", false)).build();

        Context context = prompt(block, "");
        Value value = context.getValue("boolean-input2");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.BOOLEAN));
        assertThat(value.asBoolean(), is(Boolean.FALSE));
    }

    @Test
    void testInputBoolean() {
        Block block = step("step", inputBoolean("boolean-input3", true)).build();

        Context context = prompt(block, "NO");
        Value value = context.getValue("boolean-input3");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.BOOLEAN));
        assertThat(value.asBoolean(), is(Boolean.FALSE));
    }

    @Test
    void testInputListWithEmptyResponse() {
        Block block = step("step",
                inputList("list-input1", List.of("value1"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"))).build();

        Context context = prompt(block, "");
        Value value = context.getValue("list-input1");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING_LIST));
        assertThat(value.asList(), contains("value1"));
    }

    @Test
    void testInputListWithEmptyResponseMultipleDefault() {
        Block block = step("step",
                inputList("list-input2", List.of("value1", "value2"),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"))).build();

        Context context = prompt(block, "");
        Value value = context.getValue("list-input2");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING_LIST));
        assertThat(value.asList(), contains("value1", "value2"));
    }

    @Test
    void testInputList() {
        Block block = step("step",
                inputList("list-input3", List.of(),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3"))).build();

        Context context = prompt(block, "1 3");
        Value value = context.getValue("list-input3");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING_LIST));
        assertThat(value.asList(), contains("value1", "value3"));
    }

    @Test
    void testInputListResponseDuplicate() {
        Block block = step("step",
                inputList("list-input4", List.of(),
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3"))).build();

        Context context = prompt(block, "1 3 3 1");
        Value value = context.getValue("list-input4");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING_LIST));
        assertThat(value.asList(), contains("value1", "value3"));
    }

    @Test
    void testInputEnumWithEmptyResponse() {
        Block block = step("step",
                inputEnum("enum-input1", "value1",
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"))).build();

        Context context = prompt(block, "");
        Value value = context.getValue("enum-input1");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value1"));
    }

    @Test
    void testInputEnumWithSingleOptionAndDefault() {
        Block block = step("step",
                inputEnum("enum-input1", "value1",
                        inputOption("option1", "value1"))).build();

        Context context = prompt(block, "2");
        Value value = context.getValue("enum-input1");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value1"));
    }

    @Test
    void testInputEnum() {
        Block block = step("step",
                inputEnum("enum-input2", "value3",
                        inputOption("option1", "value1"),
                        inputOption("option2", "value2"),
                        inputOption("option3", "value3"))).build();

        Context context = prompt(block, "2");
        Value value = context.getValue("enum-input2");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value2"));
    }

    @Test
    void testInputTextWithEmptyResponseNoDefault() {
        Block block = step("step", inputText("text-input1", null)).build();

        Context context = prompt(block, "");
        Value value = context.getValue("text-input1");

        assertThat(value, is(not(nullValue())));
        assertThat(value.unwrap(), is(nullValue()));
    }

    @Test
    void testInputTextWithEmptyResult() {
        Block block = step("step", inputText("text-input2", "value1")).build();

        Context context = prompt(block, "");
        Value value = context.getValue("text-input2");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value1"));
    }

    @Test
    void testInputText() {
        Block block = step("step", inputText("text-input3", "value1")).build();

        Context context = prompt(block, "not-value1");
        Value value = context.getValue("text-input3");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("not-value1"));
    }

    @Test
    void testExternalDefault() {
        Block block = step("step", inputText("text-input3", "value1")).build();

        Context context = Context.builder()
                                 .externalDefaults(Map.of("text-input3", "value2"))
                                 .build();

        prompt(block, "", context);
        Value value = context.getValue("text-input3");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value2"));
    }

    @Test
    void testInputEnumWithSingleOptionAndDefault2() {
        Block block = step("step",
                inputEnum("enum-input1", "value1",
                        inputOption("option1", "value1"))).build();

        Context context = Context.builder()
                                 .externalDefaults(Map.of("text-input3", "value2"))
                                 .build();

        List<Step> renderedSteps = new ArrayList<>();
        Controller.walk(new TerminalInputResolver(new ByteArrayInputStream(new byte[0])) {
            @Override
            protected void onVisitStep(Step step, Context context) {
                renderedSteps.add(step);
                super.onVisitStep(step, context);
            }
        }, block, context);

        assertThat(renderedSteps, is(empty()));
    }

    private static Context prompt(Block block, String userInput) {
        return prompt(block, userInput, Context.create());
    }

    private static Context prompt(Block block, String userInput, Context context) {
        byte[] bytes = userInput != null ? userInput.getBytes() : new byte[0];
        Controller.walk(new TerminalInputResolver(new ByteArrayInputStream(bytes)), block, context);
        return context;
    }
}
