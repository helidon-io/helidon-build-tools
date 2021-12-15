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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.List;
import java.util.Objects;

import io.helidon.build.common.GenericType;

/**
 * Value.
 * The wrapped value is typed and can only be used as such.
 */
public class Value {

    /**
     * True boolean value.
     */
    public static final Value TRUE = new Value(true, ValueTypes.BOOLEAN);

    /**
     * False boolean value.
     */
    public static final Value FALSE = new Value(false, ValueTypes.BOOLEAN);

    /**
     * Null string value.
     */
    public static final Value NULL = new Value(null, ValueTypes.STRING);

    private final Object value;
    private final GenericType<?> type;

    /**
     * Create a new value.
     *
     * @param value value
     * @param type  value type
     */
    protected Value(Object value, GenericType<?> type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Unwrap the value.
     *
     * @return value
     */
    public Object unwrap() {
        return value;
    }

    /**
     * Get the value type.
     *
     * @return type
     */
    public GenericType<?> type() {
        return type;
    }

    /**
     * Get this value as a {@code string}.
     *
     * @return string
     */
    public String asString() {
        return as(ValueTypes.STRING);
    }

    /**
     * Get this value as a boolean.
     *
     * @return boolean
     */
    public Boolean asBoolean() {
        return as(ValueTypes.BOOLEAN);
    }

    /**
     * Get this value as an int.
     *
     * @return int
     */
    public Integer asInt() {
        return as(ValueTypes.INT);
    }

    /**
     * Get this value as a list.
     *
     * @return list
     */
    public List<String> asList() {
        return as(ValueTypes.STRING_LIST);
    }

    /**
     * Get this value as the given type.
     *
     * @param type type
     * @param <U>  actual type
     * @return instance as the given type
     * @throws ValueTypeException if this instance type does not match the given type
     */
    @SuppressWarnings("unchecked")
    public <U> U as(GenericType<U> type) {
        Objects.requireNonNull(type, "type is null");
        if (!this.type.equals(type)) {
            throw new ValueTypeException(this.type, type);
        }
        return (U) value;
    }

    @Override
    public String toString() {
        return "Value{ " + value + " }";
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    public static Value create(String value) {
        return new Value(value, ValueTypes.STRING);
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    public static Value create(List<String> value) {
        return new Value(value, ValueTypes.STRING_LIST);
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    public static Value create(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @param type  value type
     * @param <T>   value type
     * @return Value
     */
    public static <T> Value create(T value, GenericType<T> type) {
        return new Value(value, type);
    }

    /**
     * Exception raised for unexpected type usages.
     */
    public static final class ValueTypeException extends IllegalStateException {

        /**
         * Create a new value type exception.
         *
         * @param actual   the actual type
         * @param expected the unexpected type
         */
        ValueTypeException(GenericType<?> actual, GenericType<?> expected) {
            super(String.format("Cannot get a value of { %s } as { %s }", actual, expected));
        }
    }
}
