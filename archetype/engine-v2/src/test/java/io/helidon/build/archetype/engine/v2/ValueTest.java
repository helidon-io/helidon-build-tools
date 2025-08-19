/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.NoSuchElementException;

import io.helidon.build.archetype.engine.v2.Value.ValueException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link Value}.
 */
class ValueTest {

    @Test
    void testList() {
        assertThat(Value.of(List.of("foo")), is(not(Value.of("foo"))));
        assertThat(Value.of(List.of()).getList(), is(List.of()));

        ValueException ex;
        ex = assertThrows(ValueException.class, () -> Value.of(List.of()).asInt());
        assertThat(ex.getMessage(), is("Cannot convert a list to an integer"));

        ex = assertThrows(ValueException.class, () -> Value.of(List.of()).asBoolean());
        assertThat(ex.getMessage(), is("Cannot convert a list to a boolean"));

        ex = assertThrows(ValueException.class, () -> Value.of(List.of()).asString());
        assertThat(ex.getMessage(), is("Cannot convert a list to a string"));
    }

    @Test
    void testInt() {
        assertThat(Value.of(0), is(not(Value.of("0"))));

        ValueException ex;
        ex = assertThrows(ValueException.class, () -> Value.of(0).asString());
        assertThat(ex.getMessage(), is("Cannot convert an integer to a string"));

        ex = assertThrows(ValueException.class, () -> Value.of(0).asBoolean());
        assertThat(ex.getMessage(), is("Cannot convert an integer to a boolean"));

        ex = assertThrows(ValueException.class, () -> Value.of(0).asList());
        assertThat(ex.getMessage(), is("Cannot convert an integer to a list"));
    }

    @Test
    void testBoolean() {
        assertThat(Value.FALSE, is(not(Value.of("false"))));

        ValueException ex;
        ex = assertThrows(ValueException.class, Value.FALSE::asString);
        assertThat(ex.getMessage(), is("Cannot convert a boolean to a string"));

        ex = assertThrows(ValueException.class, Value.FALSE::asInt);
        assertThat(ex.getMessage(), is("Cannot convert a boolean to an integer"));

        ex = assertThrows(ValueException.class, Value.FALSE::asList);
        assertThat(ex.getMessage(), is("Cannot convert a boolean to a list"));
    }

    @Test
    void testDynamic() {
        assertThat(Value.dynamic(() -> "none").getList(), is(List.of()));
        assertThat(Value.dynamic(() -> "foo").getList(), is(List.of("foo")));
        assertThat(Value.dynamic(() -> "foo,bar").getList(), is(List.of("foo", "bar")));
        assertThat(Value.dynamic(() -> "true").getBoolean(), is(true));
        assertThat(Value.dynamic(() -> "false").getBoolean(), is(false));
        assertThat(Value.dynamic(() -> "0").getInt(), is(0));

        // Value.equals(DynamicValue, DynamicValue)
        assertThat(Value.isEqual(Value.dynamic(() -> "none"), Value.dynamic(() -> "none")), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "foo"), Value.dynamic(() -> "foo")), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "foo"), Value.dynamic(() -> "bar")), is(false));

        // Value.equals(DynamicValue, Value)
        assertThat(Value.isEqual(Value.dynamic(() -> "none"), Value.of(List.of())), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "foo").asList(), Value.of(List.of("foo"))), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "foo,bar").asList(), Value.of(List.of("foo", "bar"))), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "true").asBoolean(), Value.TRUE), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "false").asBoolean(), Value.FALSE), is(true));
        assertThat(Value.isEqual(Value.dynamic(() -> "0").asInt(), Value.of(0)), is(true));

        // Value.equals(Value, DynamicValue)
        assertThat(Value.isEqual(Value.of(List.of()), Value.dynamic(() -> "none")), is(true));
        assertThat(Value.isEqual(Value.of(List.of("foo")), Value.dynamic(() -> "foo").asList()), is(true));
        assertThat(Value.isEqual(Value.of(List.of("foo", "bar")), Value.dynamic(() -> "foo,bar").asList()), is(true));
        assertThat(Value.isEqual(Value.TRUE, Value.dynamic(() -> "true").asBoolean()), is(true));
        assertThat(Value.isEqual(Value.FALSE, Value.dynamic(() -> "false").asBoolean()), is(true));
        assertThat(Value.isEqual(Value.of(0), Value.dynamic(() -> "0").asInt()), is(true));

        // Dynamic empty()
        assertThat(Value.dynamic(() -> null).isEmpty(), is(true));
        assertThat(Value.dynamic(() -> null).asString().isEmpty(), is(true));
        assertThat(Value.dynamic(() -> null).asList().isEmpty(), is(true));
        assertThat(Value.dynamic(() -> null).asBoolean().isEmpty(), is(true));
        assertThat(Value.dynamic(() -> null).asInt().isEmpty(), is(true));

        ValueException ex;
        ex = assertThrows(ValueException.class, () -> Value.dynamic(() -> "foo").getBoolean());
        assertThat(ex.getMessage(), is("Cannot parse boolean value: foo"));

        ex = assertThrows(ValueException.class, () -> Value.dynamic(() -> "foo").getInt());
        assertThat(ex.getMessage(), is("Cannot parse integer value: foo"));
    }

    @Test
    void testEmpty() {
        assertThrows(NoSuchElementException.class, () -> Value.empty().getString());
        assertThrows(NoSuchElementException.class, () -> Value.empty().getBoolean());
        assertThrows(NoSuchElementException.class, () -> Value.empty().getInt());
        assertThrows(NoSuchElementException.class, () -> Value.empty().getList());
        assertThat(Value.isEqual(Value.empty(), Value.empty()), is(true));
        assertThat(Value.empty().asBoolean().isEmpty(), is(true));
        assertThat(Value.empty().asString().isEmpty(), is(true));
        assertThat(Value.empty().asInt().isEmpty(), is(true));
        assertThat(Value.empty().asList().isEmpty(), is(true));
        assertThat(Value.empty().asBoolean().isPresent(), is(false));
        assertThat(Value.empty().asString().isPresent(), is(false));
        assertThat(Value.empty().asInt().isPresent(), is(false));
        assertThat(Value.empty().asList().isPresent(), is(false));
        assertThat(Value.empty().asBoolean().orElse(false), is(false));
        assertThat(Value.empty().asString().orElse(null), is(nullValue()));
        assertThat(Value.empty().asInt().orElse(0), is(0));
        assertThat(Value.empty().asList().orElse(List.of()), is(List.of()));
    }
}
