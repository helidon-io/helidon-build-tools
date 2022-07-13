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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.helidon.build.common.GenericType;
import io.helidon.build.common.Lists;

/**
 * Value.
 * The wrapped value is typed and can only be used as such.
 */
public interface Value {

    /**
     * True boolean value.
     */
    Value TRUE = new TypedValue(true, ValueTypes.BOOLEAN);

    /**
     * False boolean value.
     */
    Value FALSE = new TypedValue(false, ValueTypes.BOOLEAN);

    /**
     * Null string value.
     */
    Value NULL = new NullValue();

    /**
     * Get this value as a boolean.
     *
     * @return boolean
     */
    default Boolean asBoolean() {
        return as(ValueTypes.BOOLEAN);
    }

    /**
     * Get this value as a {@code string}.
     *
     * @return string
     */
    default String asString() {
        return as(ValueTypes.STRING);
    }

    /**
     * Get this value as an int.
     *
     * @return int
     */
    default Integer asInt() {
        return as(ValueTypes.INT);
    }

    /**
     * Get this value as a list.
     *
     * @return list
     */
    default List<String> asList() {
        return as(ValueTypes.STRING_LIST);
    }

    /**
     * Get the text representation of this value.
     *
     * @return value as text
     */
    default String asText() {
        Object o = unwrap();
        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            if (list.isEmpty()) {
                return "none";
            }
            return String.join(",", Lists.map(list, String::valueOf));
        }
        return String.valueOf(o);
    }

    /**
     * Unwrap the value.
     *
     * @return value
     */
    Object unwrap();

    /**
     * Get the value type.
     *
     * @return type
     */
    GenericType<?> type();

    /**
     * Get this value as the given type.
     *
     * @param type type
     * @param <U>  actual type
     * @return instance as the given type
     * @throws ValueTypeException if this instance type does not match the given type
     */
    <U> U as(GenericType<U> type);

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    static Value create(String value) {
        return value == null ? NULL : new TypedValue(value, ValueTypes.STRING);
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    static Value create(List<String> value) {
        return new TypedValue(value, ValueTypes.STRING_LIST);
    }

    /**
     * Create a new value.
     *
     * @param value value
     * @return Value
     */
    static Value create(boolean value) {
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
    static <T> Value create(T value, GenericType<T> type) {
        return new TypedValue(value, type);
    }

    /**
     * Test value equality.
     *
     * @param value1 value1
     * @param value2 value2
     * @return {@code true} if the value are equals, {@code false} otherwise
     */
    static boolean equals(Value value1, Value value2) {
        GenericType<?> type1 = value1.type();
        GenericType<?> type2 = value2.type();
        if ((type1 == null ^ type2 == null) || (type1 != null && type1 == type2)) {
            GenericType<?> type = type1 != null ? type1 : type2;
            return Objects.equals(value1.as(type), value2.as(type));
        }
        return value1.unwrap().equals(value2.unwrap());
    }

    /**
     * Exception raised for unexpected type usages.
     */
    class ValueTypeException extends IllegalStateException {

        /**
         * Create a new value type exception.
         *
         * @param actual   the actual type
         * @param expected the unexpected type
         */
        ValueTypeException(GenericType<?> actual, GenericType<?> expected) {
            super(String.format("Cannot get a value of { %s } as { %s }", actual, expected));
        }

        /**
         * Create a new value type exception.
         *
         * @param message exception message
         */
        protected ValueTypeException(String message) {
            super(message);
        }
    }

    /**
     * Typed value.
     * The wrapped value is typed and can only be used as such.
     */
    class TypedValue implements Value {

        private final Object value;
        private final GenericType<?> type;

        /**
         * Create a new value.
         *
         * @param value value
         * @param type  value type
         */
        protected TypedValue(Object value, GenericType<?> type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public Object unwrap() {
            return value;
        }

        @Override
        public GenericType<?> type() {
            return type;
        }

        @Override
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
            return "TypedValue{"
                    + "value='" + value + "'"
                    + ", type=" + type
                    + '}';
        }
    }

    final class NullValue implements Value {

        private NullValue() {
        }

        @Override
        public Object unwrap() {
            return null;
        }

        @Override
        public GenericType<?> type() {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> U as(GenericType<U> type) {
            if (type.equals(ValueTypes.BOOLEAN)) {
                return (U) Boolean.FALSE;
            } else if (type.equals(ValueTypes.INT)) {
                return (U) Integer.valueOf(0);
            } else if (type.equals(ValueTypes.STRING_LIST)) {
                return (U) Collections.emptyList();
            }
            return null;
        }
    }
}
