/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.util;

import java.util.List;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.GenericType;

/**
 * {@link Value} delegate implementation.
 */
public abstract class ValueDelegate implements Value {

    private final Value value;

    /**
     * Create a new value delegate.
     *
     * @param value delegated value, must be non {@code null}
     */
    protected ValueDelegate(Value value) {
        this.value = Objects.requireNonNull(value, "value is null");
    }

    /**
     * Get the wrapped value.
     *
     * @return Value
     */
    public Value value() {
        return value;
    }

    @Override
    public Boolean asBoolean() {
        return value.asBoolean();
    }

    @Override
    public String asString() {
        return value.asString();
    }

    @Override
    public Integer asInt() {
        return value.asInt();
    }

    @Override
    public List<String> asList() {
        return value.asList();
    }

    @Override
    public Object unwrap() {
        return value.unwrap();
    }

    @Override
    public GenericType<?> type() {
        return value.type();
    }

    @Override
    public <U> U as(GenericType<U> type) {
        return value.as(type);
    }
}
