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
    void testLookup() {
        Context context = Context.create();
        Context.Scope scope = context.newScope("foo", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("foo-value"), ValueKind.EXTERNAL);

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
        Context.Scope scope;
        scope = context.newScope("foo", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.TRUE, ValueKind.EXTERNAL);
        scope = context.newScope("bar", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.TRUE, ValueKind.EXTERNAL);
        scope = context.newScope("color", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("blue"), ValueKind.EXTERNAL);
        context.popScope();
        Value value = context.lookup("color");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("blue"));
    }

    @Test
    void testLookupGlobal() {
        Context context = Context.create();
        Context.Scope scope;
        scope = context.newScope("foo", true);
        context.pushScope(scope);
        context.setValue("bar", Value.create("foo-value"), ValueKind.EXTERNAL);

        Value value;
        value = context.lookup("bar");
        assertThat(value, is(notNullValue()));
        assertThat(value.asString(), is("foo-value"));
    }

    @Test
    void testLookupInternalOnly() {
        Context context = Context.create();
        context.setValue("foo", Value.create("foo-value"), ValueKind.EXTERNAL);

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
        Context.Scope scope;
        scope = context.newScope("foo", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("foo-value"), ValueKind.EXTERNAL);
        scope = context.newScope("bar", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("bar-value"), ValueKind.EXTERNAL);

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
    void testPopScope() {
        Context context = Context.create();
        Context.Scope scope;
        scope = context.newScope("foo", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("foo-value"), ValueKind.EXTERNAL);
        scope = context.newScope("bar", false);
        context.pushScope(scope);
        context.setValue(scope.id(), Value.create("bar-value"), ValueKind.EXTERNAL);
        context.popScope();

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
}
