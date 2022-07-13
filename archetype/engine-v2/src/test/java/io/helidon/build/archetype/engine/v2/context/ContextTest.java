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
package io.helidon.build.archetype.engine.v2.context;

import java.util.Map;

import io.helidon.build.archetype.engine.v2.context.ContextValue.ValueKind;
import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link Context}.
 */
class ContextTest {

    @Test
    void testPopScope() {
        Context context = Context.create();
        context.pushScope("foo", false)
               .putValue("", Value.create("foo1"), ValueKind.EXTERNAL);
        context.pushScope("bar", false)
               .putValue("", Value.create("bar1"), ValueKind.EXTERNAL);

        context.popScope();
        Value value;

        value = context.scope().getValue("bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar1"));

        value = context.scope().getValue("");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo1"));

        value = context.scope().getValue("~foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo1"));

        value = context.scope().getValue("..foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo1"));

        value = context.scope().getValue("~foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar1"));

        value = context.scope().getValue("~..foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar1"));
    }

    @Test
    void testExternalValuesSubstitution() {
        Context context = Context.builder()
                                 .externalValues(Map.of("foo", "foo", "bar", "${foo}"))
                                 .build();
        assertThat(context.scope().getValue("bar").asString(), is("foo"));
    }
}
