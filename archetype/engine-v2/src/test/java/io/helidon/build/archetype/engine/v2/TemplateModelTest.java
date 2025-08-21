/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.TemplateModel.ModelNode;

import org.junit.jupiter.api.Test;

import static io.helidon.build.archetype.engine.v2.Nodes.model;
import static io.helidon.build.archetype.engine.v2.Nodes.modelValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link TemplateModel}.
 */
class TemplateModelTest {

    @Test
    void testResolveWithNotRootContext() {
        Context context = new Context()
                .externalValues(Map.of("bar", "bar"));
        context.pushScope(s -> s.getOrCreate("scope", false, false));

        Node model = model(modelValue("foo", "${bar}"));
        TemplateModel mergedModel = resolveModel(model, context);
        ModelNode node = mergedModel.root().get("foo");

        assertThat(isRootScope(context), is(false));
        assertThat(node, is(instanceOf(TemplateModel.Value.class)));
        assertThat(((TemplateModel.Value) node).value(), is("bar"));
    }

    @Test
    void testEmptyModelValue() {
        Context context = new Context();
        Node model = model(modelValue("foo", null));
        TemplateModel mergedModel = resolveModel(model, context);
        ModelNode node = mergedModel.root().get("foo");
        assertThat(((TemplateModel.Value) node).value(), is(""));

    }

    boolean isRootScope(Context context) {
        Context.Scope scope = context.scope();
        return scope != null && scope.parent() == null;
    }

    static TemplateModel resolveModel(Node scope, Context context) {
        TemplateModel model = new TemplateModel(context);
        ScriptInvoker.invoke(scope, context, new InputResolver.BatchResolver(context), model);
        return model;
    }
}
