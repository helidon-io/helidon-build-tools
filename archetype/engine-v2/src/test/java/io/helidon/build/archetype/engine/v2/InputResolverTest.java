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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.inputEnum;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputList;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputOption;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputText;
import static io.helidon.build.archetype.engine.v2.TestHelper.model;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelList;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelValue;
import static io.helidon.build.archetype.engine.v2.TestHelper.output;
import static io.helidon.build.archetype.engine.v2.TestHelper.step;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link InputResolver}.
 */
public class InputResolverTest {

    @Test
    void testEnumOption() {
        Block block = step("step",
                inputEnum("enum-input", "value3",
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option3", "value3", output(model(modelList("colors", modelValue("blue"))))))).build();

        Context context = Context.create();
        context.setValue("enum-input", Value.create("value2"), ContextValue.ValueKind.EXTERNAL);

        assertThat(resolveInputs(block, context), contains("green"));
    }

    @Test
    void testListOptions() {
        Block block = step("step",
                inputList("list-input", List.of(),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option3", "value3", output(model(modelList("colors", modelValue("blue")))))))
                .build();

        Context context = Context.create();
        context.setValue("list-input", Value.create(List.of("value1", "value3")), ContextValue.ValueKind.EXTERNAL);

        assertThat(resolveInputs(block, context), contains("red", "blue"));
    }

    // TODO test control flow of options
    //      i.e test InputResolver.visitOption

    @Test
    void testDefaultValueSubstitutions() {
        Block block = step("step", inputText("text-input4", "${foo}").attribute("optional", Value.TRUE)).build();
        Context context = Context.create();
        context.setValue("foo", Value.create("bar"), ContextValue.ValueKind.EXTERNAL);
        resolveInputs(block, context, null);

        Value value = context.lookup("text-input4");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("bar"));
    }

    @Test
    void testExternalDefaultValueSubstitutions() {
        Block block = step("step", inputText("text-input5", "foo").attribute("optional", Value.TRUE)).build();
        Context context = Context.create(null, null, Map.of("text-input5", "${foo}"));
        context.setValue("foo", Value.create("bar"), ContextValue.ValueKind.EXTERNAL);
        resolveInputs(block, context, null);

        Value value = context.lookup("text-input5");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("bar"));
    }

    @Test
    void testInvalidEnumExternalValue() {
        Block block = step("step", inputEnum("enum-input", null, inputOption("option1", "value1"))).build();
        Context context = Context.create(null, Map.of("enum-input", ""), null);
        InvocationException ex = assertThrows(InvocationException.class, () -> resolveInputs(block, context, null));
        assertThat(ex.getCause(), is(instanceOf(InvalidInputException.class)));
        assertThat(ex.getCause().getMessage(), is("Invalid input: enum-input=''"));
    }

    @Test
    void testEnumIgnoreCase() {
        Block block = step("step",
                inputEnum("enum-input2", "value1",
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue")))))))
                .build();

        Context context = Context.create();
        context.setValue("enum-input2", Value.create("VALUE2"), ContextValue.ValueKind.EXTERNAL);
        assertThat(resolveInputs(block, context), contains("blue"));
    }

    @Test
    void testEnumDefaultIgnoreCase() {
        Block block = step("step",
                inputEnum("enum-input3", "VALUE2",
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue"))))))
                        .attribute("optional", Value.TRUE))
                .build();

        Context context = Context.create();
        assertThat(resolveInputs(block, context), contains("blue"));
    }

    @Test
    void testListIgnoreCase() {
        Block block = step("step",
                inputList("list-input", List.of("value1"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option1", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option2", "value3", output(model(modelList("colors", modelValue("blue")))))))
                .build();

        Context context = Context.create();
        context.setValue("list-input", Value.create(List.of("VALUE2", "VALUE3")), ContextValue.ValueKind.EXTERNAL);
        assertThat(resolveInputs(block, context), contains("green", "blue"));
    }

    @Test
    void testListDefaultIgnoreCase() {
        Block block = step("step",
                inputList("list-input", List.of("VALUE2,VALUE3"),
                        inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                        inputOption("option1", "value2", output(model(modelList("colors", modelValue("green"))))),
                        inputOption("option2", "value3", output(model(modelList("colors", modelValue("blue"))))))
                        .attribute("optional", Value.TRUE))
                .build();

        Context context = Context.create();
        assertThat(resolveInputs(block, context), contains("green", "blue"));
    }

    @Test
    void testGlobalInputs() {
        Block.Builder nested2 = inputEnum("nested2", "value1",
                inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue"))))));

        Block.Builder nested1 = inputEnum("nested1", "value1",
                inputOption("option1", "value1", output(model(modelList("style", modelValue("plain"))))),
                inputOption("option2", "value2", nested2));

        Block.Builder nestedGlobal = inputEnum("nested-global", "value1",
                inputOption("option1", "value1", nested1))
                .attribute("global", Value.TRUE);

        Block global = step("step",
                inputEnum("global", "value1",
                        inputOption("option1", "value1", nestedGlobal))
                        .attribute("global", Value.TRUE))
                .build();

        Context context = Context.create();
        context.setValue("global", Value.create("value1"), ContextValue.ValueKind.EXTERNAL);
        context.setValue("nested-global", Value.create("value1"), ContextValue.ValueKind.EXTERNAL);
        context.setValue("nested1", Value.create("value2"), ContextValue.ValueKind.EXTERNAL);
        context.setValue("nested1.nested2", Value.create("value2"), ContextValue.ValueKind.EXTERNAL);
        List<String> resolvedInputs = resolveInputs(global, context);
        assertThat(resolvedInputs.size(), is(1));
        assertThat(resolvedInputs, contains("blue"));
    }

    @Test
    void testInvalidGlobalInputs() {
        Block.Builder invalidGlobal = inputEnum("invalid-global", "value1",
                inputOption("option1", "value1", output(model(modelList("colors", modelValue("red"))))),
                inputOption("option2", "value2", output(model(modelList("colors", modelValue("blue"))))))
                .attribute("optional", Value.TRUE)
                .attribute("global", Value.TRUE);

        Block.Builder nested = inputEnum("nested", "value1",
                inputOption("option1", "value1", invalidGlobal));

        Block global = step("step",
                inputEnum("global", "value1",
                        inputOption("option1", "value1", nested))
                        .attribute("global", Value.TRUE))
                .build();

        Context context = Context.create();
        context.setValue("global", Value.create("value1"), ContextValue.ValueKind.EXTERNAL);
        context.setValue("global.nested", Value.create("value1"), ContextValue.ValueKind.EXTERNAL);

        InvocationException ex = assertThrows(InvocationException.class, () -> resolveInputs(global, context, null));
        assertThat(ex.getCause(), is(instanceOf(IllegalStateException.class)));
        assertThat(ex.getCause().getMessage(), endsWith("Parent input is not global"));
    }

    private static void resolveInputs(Block block, Context context, Model.Visitor<Context> modelVisitor) {
        Controller.walk(new BatchInputResolver(), null, modelVisitor, block, context);
    }

    private static List<String> resolveInputs(Block block, Context context) {
        List<String> values = new LinkedList<>();
        resolveInputs(block, context, new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Context arg) {
                values.add(value.value());
                return VisitResult.CONTINUE;
            }
        });
        return values;
    }
}
