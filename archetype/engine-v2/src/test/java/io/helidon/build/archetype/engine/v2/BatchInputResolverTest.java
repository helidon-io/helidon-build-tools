/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.context.Context;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.inputEnum;
import static io.helidon.build.archetype.engine.v2.TestHelper.inputOption;
import static io.helidon.build.archetype.engine.v2.TestHelper.step;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link BatchInputResolver}.
 */
class BatchInputResolverTest {

    @Test
    void testInputEnumWithSingleOptionAndDefault() {
        Block block = step("step",
                inputEnum("enum-input1", "value1",
                        inputOption("option1", "value1"))).build();

        Context context = Context.create();
        Controller.walk(new BatchInputResolver(), block, context);
        Value value = context.getValue("enum-input1");

        assertThat(value, is(notNullValue()));
        assertThat(value.type(), is(ValueTypes.STRING));
        assertThat(value.asString(), is("value1"));
    }
}
