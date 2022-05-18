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

import java.util.List;

import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.GenericType;

/**
 * Context value.
 */
public final class ContextValue implements Value {

    private final Value value;
    private final ValueKind kind;

    /**
     * Create a new context value.
     *
     * @param value wrapped value
     * @param kind  value kind
     */
    ContextValue(Value value, ValueKind kind) {
        this.value = value;
        this.kind = kind;
    }

    /**
     * Test if this context value is read-only.
     *
     * @return {@code true} if read-only, {@code false} otherwise
     */
    public boolean isReadOnly() {
        switch (kind) {
            case EXTERNAL:
            case PRESET:
                return true;
            default:
                return false;
        }
    }

    /**
     * Create a new external value.
     *
     * @param value raw value
     * @return ContextValue
     */
    public static ContextValue external(String value) {
        return new ContextValue(DynamicValue.create(value), ValueKind.EXTERNAL);
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

    @Override
    public String toString() {
        return "ContextValue{"
                + "kind=" + kind
                + ", value=" + value
                + '}';
    }

    /**
     * Context value kind.
     */
    public enum ValueKind {
        /**
         * Preset value.
         */
        PRESET,

        /**
         * Default value.
         */
        DEFAULT,

        /**
         * External value.
         */
        EXTERNAL,

        /**
         * User value.
         */
        USER
    }
}
