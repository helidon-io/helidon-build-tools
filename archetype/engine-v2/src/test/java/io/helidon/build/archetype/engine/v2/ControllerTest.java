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

package io.helidon.build.archetype.engine.v2;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Step;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextValue;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link Controller}.
 */
class ControllerTest {

    @Test
    void testPresets() {
        Script script = load("controller/presets.xml");
        Context context = Context.create();
        Controller.walk(script, context);

        Value preset1 = context.getValue("preset1");
        assertThat(preset1, is(not(nullValue())));
        assertThat(preset1.type(), is(ValueTypes.BOOLEAN));
        assertThat(preset1.asBoolean(), is(true));

        Value preset2 = context.getValue("preset2");
        assertThat(preset2, is(not(nullValue())));
        assertThat(preset2.type(), is(ValueTypes.STRING));
        assertThat(preset2.asString(), is("text1"));

        Value preset3 = context.getValue("preset3");
        assertThat(preset3, is(not(nullValue())));
        assertThat(preset3.type(), is(ValueTypes.STRING));
        assertThat(preset3.asString(), is("enum1"));

        Value preset4 = context.getValue("preset4");
        assertThat(preset4, is(not(nullValue())));
        assertThat(preset4.type(), is(ValueTypes.STRING_LIST));
        assertThat(preset4.asList(), contains("list1"));
    }

    @Test
    void testConditional() {
        Script script = load("controller/conditional.xml");
        Context context = Context.create();
        context.putValue("doModel", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        context.putValue("doColors", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        context.putValue("doRed", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        context.putValue("doGreen", Value.FALSE, ContextValue.ValueKind.EXTERNAL);
        context.putValue("doBlue", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        context.putValue("doShapes", Value.FALSE, ContextValue.ValueKind.EXTERNAL);

        List<String> values = modelValues(script, context);
        assertThat(values, contains("red", "blue"));
    }

    @Test
    void testExecCwd() {
        Script script = load("controller/exec.xml");
        Context context = Context.builder()
                                 .cwd(script.scriptPath().getParent())
                                 .build();

        List<String> values = modelValues(script, context);
        assertThat(values, contains("red"));
    }

    @Test
    void testSourceCwd() {
        Script script = load("controller/source.xml");
        Context context = Context.builder()
                                 .cwd(script.scriptPath().getParent())
                                 .build();

        List<String> values = modelValues(script, context);
        assertThat(values, contains("green"));
    }

    @Test
    void testInvocationCondition() {
        Script script = load("controller/invocation.xml");
        Context context = Context.builder()
                .cwd(script.scriptPath().getParent())
                .externalValues(Map.of("source", "true", "exec", "true"))
                .build();

        List<String> values = modelValues(script, context);
        assertThat(values, contains("red", "green"));

        context = Context.builder()
                .cwd(script.scriptPath().getParent())
                .externalValues(Map.of("source", "true", "exec", "false"))
                .build();

        values = modelValues(script, context);
        assertThat(values, contains("green"));

        context = Context.builder()
                .cwd(script.scriptPath().getParent())
                .externalValues(Map.of("source", "false", "exec", "true"))
                .build();

        values = modelValues(script, context);
        assertThat(values, contains("red"));

        context = Context.builder()
                .cwd(script.scriptPath().getParent())
                .externalValues(Map.of("source", "false", "exec", "false"))
                .build();

        values = modelValues(script, context);
        assertThat(values.size(), is(0));
    }

    @Test
    void testConditionalScript() {
        Script script = load("controller/conditional-block.xml");
        Context context = createConditionalContext();

        Controller.walk(script, context);

        assertThat(context.getValue("success1"), is(notNullValue()));
        assertThat(context.getValue("success2"), is(notNullValue()));
        assertThat(context.getValue("success3"), is(notNullValue()));
        assertThat(context.getValue("outside1"), is(notNullValue()));
        assertThat(context.getValue("outside2"), is(notNullValue()));
        assertThat(context.getValue("outside3"), is(notNullValue()));
        assertThat(context.getValue("failure1"), is(nullValue()));
        assertThat(context.getValue("failure2"), is(nullValue()));
        assertThat(context.getValue("failure3"), is(nullValue()));
    }

    @Test
    void testConditionModel() {
        Script script = load("controller/conditional-block.xml");
        Context context = createConditionalContext();

        Controller.walk(script, context);

        List<String> values = modelValues(script, context);
        assertThat(values, contains("value1", "value2"));
    }

    @Test
    void testConditionalStep() {
        Script script = load("controller/conditional-block.xml");
        Context context = createConditionalContext();
        List<String> steps = new LinkedList<>();
        Controller.walk(new TerminalInputResolver(new ByteArrayInputStream("yes".getBytes())) {
            @Override
            protected void onVisitStep(Step step, Context context) {
                steps.add(step.name());
                super.onVisitStep(step, context);
            }
        }, script, context);

        assertThat(steps, contains("step1", "step2"));
    }

    @Test
    void testConditionalInput() {
        Script script = load("controller/conditional-inputs.xml");
        Context context = Context.create();
        context.putValue("enum1", Value.create("option1"), ContextValue.ValueKind.EXTERNAL);
        context.putValue("list1", Value.create(List.of("option1", "option3")), ContextValue.ValueKind.EXTERNAL);

        List<String> values = modelValues(script, context);

        assertThat(values, contains("value1", "value2", "value3", "value4", "value5"));
    }

    private static List<String> modelValues(Block block, Context context) {
        List<String> values = new LinkedList<>();
        Controller.walk(new Model.Visitor<>() {
            @Override
            public VisitResult visitValue(Model.Value value, Context arg) {
                values.add(value.value());
                return VisitResult.CONTINUE;
            }
        }, block, context);
        return values;
    }

    private Context createConditionalContext() {
        Context context = Context.create();
        context.putValue("foo", Value.create(true), ContextValue.ValueKind.EXTERNAL);
        context.putValue("bar", Value.create(false), ContextValue.ValueKind.EXTERNAL);
        return context;
    }
}
