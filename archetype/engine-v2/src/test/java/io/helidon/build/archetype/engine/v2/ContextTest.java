/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.ast.Value;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link Context}.
 */
class ContextTest {

    @Test
    void testLookup() {
        Context context = Context.create();
        context.push("foo", Value.create("foo-value"));

        Value value;

        value = context.lookup("foo");
        assertThat(value, is(nullValue()));

        value = context.lookup("ROOT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.PARENT.PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));
    }

    @Test
    void testLookupInScope() {
        Context context = Context.create();
        context.push("foo", Value.TRUE);
        context.push("bar", Value.TRUE);
        context.push("color", Value.create("blue"));
        context.pop();
        Value value = context.lookup("color");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("blue"));
    }

    @Test
    void testLookupInternalOnly() {
        Context context = Context.create();
        context.put("foo", Value.create("foo-value"));

        Value value;

        value = context.lookup("foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("ROOT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.PARENT.PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));
    }

    @Test
    void testRelativeLookup() {
        Context context = Context.create();
        context.push("foo", Value.create("foo-value"));
        context.push("bar", Value.create("bar-value"));

        Value value;

        value = context.lookup("bar");
        assertThat(value, is(nullValue()));

        value = context.lookup("ROOT.bar");
        assertThat(value, is(nullValue()));

        value = context.lookup("ROOT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("ROOT.foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));

        value = context.lookup("PARENT.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));

        value = context.lookup("PARENT.PARENT.bar");
        assertThat(value, is(nullValue()));

        value = context.lookup("PARENT.PARENT.PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.PARENT.PARENT.foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));
    }

    @Test
    void testPopInput() {
        Context context = Context.create();
        context.push("foo", Value.create("foo-value"));
        context.push("bar", Value.create("bar-value"));
        context.pop();

        Value value;

        value = context.lookup("bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));

        value = context.lookup("foo");
        assertThat(value, is(nullValue()));

        value = context.lookup("ROOT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("PARENT.foo");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));

        value = context.lookup("foo.bar");
        assertThat(value, is(nullValue()));

        value = context.lookup("ROOT.foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));

        value = context.lookup("PARENT.foo.bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("bar-value"));
    }

    @Test
    void testExternalValuesSubstitution() {
        Context context = Context.create(null, Map.of("foo", "foo", "bar", "${foo}"), null);
        assertThat(context.lookup("bar").asString(), is("foo"));
    }

    @Test
    void testExternalValueAlwaysLowerCase() {
        Context context = Context.create(null, Map.of("foo", "FOO"), null);
        assertThat(context.lookup("foo").asString(), is("foo"));
    }

    @Test
    void testExternalDefaultAlwaysLowerCase() {
        Context context = Context.create(null, null, Map.of("foo", "FOO"));
        assertThat(context.defaultValue("foo").asString(), is("foo"));
    }
}
