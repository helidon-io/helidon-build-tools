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

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.load;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link Controller}.
 */
class ControllerTest {

    @Test
    void testPresets() {
        Script script = load("controller/presets.xml");
        Context context = Context.create();
        ContextScope scope = context.scope();
        Controller.walk(script, context);

        Value preset1 = scope.getValue("preset1");
        assertThat(preset1, is(not(nullValue())));
        assertThat(preset1.type(), is(ValueTypes.BOOLEAN));
        assertThat(preset1.asBoolean(), is(true));

        Value preset2 = scope.getValue("preset2");
        assertThat(preset2, is(not(nullValue())));
        assertThat(preset2.type(), is(ValueTypes.STRING));
        assertThat(preset2.asString(), is("text1"));

        Value preset3 = scope.getValue("preset3");
        assertThat(preset3, is(not(nullValue())));
        assertThat(preset3.type(), is(ValueTypes.STRING));
        assertThat(preset3.asString(), is("enum1"));

        Value preset4 = scope.getValue("preset4");
        assertThat(preset4, is(not(nullValue())));
        assertThat(preset4.type(), is(ValueTypes.STRING_LIST));
        assertThat(preset4.asList(), contains("list1"));
    }

    @Test
    void testConditional() {
        Script script = load("controller/conditional.xml");
        Context context = Context.create();
        ContextScope scope = context.scope();
        scope.putValue("do-model", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        scope.putValue("do-colors", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        scope.putValue("do-red", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        scope.putValue("do-green", Value.FALSE, ContextValue.ValueKind.EXTERNAL);
        scope.putValue("do-blue", Value.TRUE, ContextValue.ValueKind.EXTERNAL);
        scope.putValue("do-shapes", Value.FALSE, ContextValue.ValueKind.EXTERNAL);

        List<String> values = modelValues(script, context);
        assertThat(values, contains("red", "blue"));
    }

    @Test
    void testExecCwd() {
        Script script = load("controller/exec.xml");
        Context context = Context.create(script.scriptPath().getParent());

        List<String> values = modelValues(script, context);
        assertThat(values, contains("red"));
    }

    @Test
    void testSourceCwd() {
        Script script = load("controller/source.xml");
        Context context = Context.create(script.scriptPath().getParent());

        List<String> values = modelValues(script, context);
        assertThat(values, contains("green"));
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
}
