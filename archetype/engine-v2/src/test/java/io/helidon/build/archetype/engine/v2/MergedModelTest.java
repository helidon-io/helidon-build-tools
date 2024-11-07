/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import java.util.Map;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.context.ContextScope;
import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.TestHelper.model;
import static io.helidon.build.archetype.engine.v2.TestHelper.modelValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link MergedModel}.
 */
class MergedModelTest {

    @Test
    void testResolveWithNotRootContext() {
        Block model = model(modelValue("foo", "${bar}")).build();
        Context context = Context.builder()
                .externalValues(Map.of("bar", "bar"))
                .build();
        context.pushScope("scope", false);
        MergedModel mergedModel = MergedModel.resolveModel(model, context);
        MergedModel.Node node = mergedModel.node().get("foo");

        assertThat(isRootScope(context), is(false));
        assertThat(node, is(instanceOf(MergedModel.Value.class)));
        assertThat(((MergedModel.Value) node).value(), is("bar"));
    }

    private boolean isRootScope(Context context) {
        ContextScope scope = context.scope();
        return scope != null && scope.parent() == null;
    }
}
