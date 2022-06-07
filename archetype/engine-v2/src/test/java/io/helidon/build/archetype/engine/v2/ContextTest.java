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

import java.util.Map;

import io.helidon.build.archetype.engine.v2.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link Context}.
 */
class ContextTest {

    @Test
    void testPopScope() {
        Context context = Context.create();
        ContextScope scope = context.scope();

        scope = scope.getOrCreateScope("foo", false);
        scope.putValue("foo1", Value.create("foo-value1"), ValueKind.EXTERNAL);
        context.pushScope(scope);

        scope = scope.getOrCreateScope("bar", false);
        scope.putValue("bar1", Value.create("bar-value1"), ValueKind.EXTERNAL);
        context.pushScope(scope);

        context.popScope();
        scope = context.scope();
        Value value;

        value = scope.getValue("bar1");
        assertThat(value, is(nullValue()));

        value = scope.getValue("bar.bar1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value1"));

        value = scope.getValue("foo1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value1"));

        value = scope.getValue("~foo1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value1"));

        value = scope.getValue("..foo1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value1"));

        value = scope.getValue("~foo.bar1");
        assertThat(value, is(nullValue()));

        value = scope.getValue("~foo.bar.bar1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value1"));

        value = scope.getValue("..foo.bar.bar1");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value1"));
    }

    @Test
    void testExternalValuesSubstitution() {
        Context context = Context.create(null, Map.of("foo", "foo", "bar", "${foo}"), null);
        assertThat(context.scope().getValue("bar").asString(), is("foo"));
    }
}
