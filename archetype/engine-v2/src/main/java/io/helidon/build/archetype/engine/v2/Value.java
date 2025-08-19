/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.Lists;

/**
 * Pseudo optional value wrapper.
 *
 * @param <T> value type
 */
public interface Value<T> {

    /**
     * Value types.
     */
    enum Type {
        /**
         * String.
         */
        STRING,
        /**
         * Integer.
         */
        INTEGER,
        /**
         * Boolean.
         */
        BOOLEAN,
        /**
         * List.
         */
        LIST,
        /**
         * Dynamic.
         */
        DYNAMIC,
        /**
         * EMPTY.
         */
        EMPTY
    }

    /**
     * True boolean value.
     */
    Value<Boolean> TRUE = new BooleanValue(true);

    /**
     * False boolean value.
     */
    Value<Boolean> FALSE = new BooleanValue(false);

    /**
     * Empty list value.
     */
    Value<List<String>> EMPTY_LIST = new ListValue(List.of());

    /**
     * Empty string value.
     */
    Value<String> EMPTY_STRING = new StringValue("");

    /**
     * Get the value.
     *
     * @return value
     */
    T get();

    /**
     * Get the value type.
     *
     * @return type
     */
    Type type();

    /**
     * If a value is not present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if not present, otherwise {@code false}
     */
    boolean isEmpty();

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if present, otherwise {@code false}
     */
    default boolean isPresent() {
        return !isEmpty();
    }

    /**
     * If empty, return the value produced by the given supplier, otherwise return this instance.
     *
     * @param supplier supplier
     * @return the new value, if empty, otherwise this instance
     */
    default Value<T> or(Supplier<Value<T>> supplier) {
        return isEmpty() ? supplier.get() : this;
    }

    /**
     * If not empty, return a new value mapped by the given function.
     *
     * @param function mapping function
     * @param <R>      new value type
     * @return the new value, if not empty, otherwise this instance
     */
    @SuppressWarnings("unchecked")
    default <R> Value<R> map(Function<T, R> function) {
        if (isEmpty()) {
            return (Value<R>) this;
        }
        R value = function.apply(get());
        if (value instanceof Boolean) {
            return (Value<R>) new BooleanValue((Boolean) value);
        } else if (value instanceof String) {
            return (Value<R>) new DynamicValue(() -> (String) value);
        } else if (value instanceof Integer) {
            return (Value<R>) new IntegerValue((Integer) value);
        } else if (value instanceof List<?>) {
            return (Value<R>) new ListValue((List<String>) value);
        } else {
            throw new ValueException("Unsupported value type: " + value.getClass());
        }
    }

    /**
     * Invoke the given consumer if present.
     *
     * @param consumer consumer
     */
    default void ifPresent(Consumer<? super T> consumer) {
        if (isPresent()) {
            consumer.accept(get());
        }
    }

    /**
     * Get this value as a stream.
     *
     * @return stream
     */
    default Stream<T> stream() {
        return isEmpty() ? Stream.empty() : Stream.of(get());
    }

    /**
     * If not empty, returns this value otherwise returns the {@code other} value.
     *
     * @param other the value to be returned, if empty.
     * @return resolved value
     */
    default T orElse(T other) {
        return isEmpty() ? other : get();
    }

    /**
     * If not empty, returns this value otherwise returns the value produced by the given supplier.
     *
     * @param other the supplier of value to be returned, if empty
     * @return resolved value
     */
    default T orElseGet(Supplier<? extends T> other) {
        return isEmpty() ? other.get() : get();
    }

    /**
     * If empty, throw the exception supplied by the given supplier, otherwise get the value.
     *
     * @param supplier supplier
     * @return value
     */
    default T orElseThrow(Supplier<? extends RuntimeException> supplier) {
        if (isEmpty()) {
            throw supplier.get();
        }
        return get();
    }

    /**
     * Map this value to a boolean.
     *
     * @return boolean
     */
    default Value<Boolean> asBoolean() {
        return empty();
    }

    /**
     * Map this value to a {@code string}.
     *
     * @return string
     */
    Value<String> asString();

    /**
     * Map this value to an int.
     *
     * @return int
     */
    Value<Integer> asInt();

    /**
     * Map this value to a list.
     *
     * @return list
     */
    Value<List<String>> asList();

    /**
     * Get this value as a boolean.
     *
     * @return boolean
     */
    default boolean getBoolean() {
        return asBoolean().get();
    }

    /**
     * Get this value as a {@code string}.
     *
     * @return string
     */
    default String getString() {
        return asString().get();
    }

    /**
     * Get this value as an int.
     *
     * @return int
     */
    default int getInt() {
        return asInt().get();
    }

    /**
     * Get this value as a list.
     *
     * @return list
     */
    default List<String> getList() {
        return asList().get();
    }

    /**
     * Get the empty value.
     *
     * @param <T> value type
     * @return value
     */
    @SuppressWarnings("unchecked")
    static <T> Value<T> empty() {
        return (Value<T>) EmptyValue.INSTANCE;
    }

    /**
     * Get an empty value with a custom error.
     *
     * @param supplier exception supplier
     * @param <T>      value type
     * @return value
     */
    static <T> Value<T> empty(Supplier<? extends RuntimeException> supplier) {
        return new EmptyValue<>(supplier);
    }

    /**
     * Get a default value.
     *
     * @param type  type
     * @return Value
     */
    static Value<?> typed(Type type) {
        switch (type) {
            case LIST:
                return EMPTY_LIST;
            case BOOLEAN:
                return FALSE;
            case STRING:
                return EMPTY_STRING;
            default:
                throw new IllegalArgumentException("Unsupported value type: " + type);
        }
    }

    /**
     * Create a new typed value.
     *
     * @param value value
     * @param type  type
     * @return Value
     */
    static Value<?> typed(Value<?> value, Type type) {
        switch (type) {
            case LIST:
                return Value.of(value.getList());
            case BOOLEAN:
                return Value.of(value.asBoolean().orElse(null));
            case STRING:
                return Value.of(value.asString().orElse(null));
            default:
                throw new IllegalArgumentException("Unsupported value type: " + type);
        }
    }

    /**
     * Create a new dynamic value.
     *
     * @param value value
     * @return Value
     */
    static Value<String> dynamic(String value) {
        return value == null ? empty() : new DynamicValue(() -> value);
    }

    /**
     * Create a new dynamic value.
     *
     * @param supplier supplier
     * @return Value
     */
    static Value<?> dynamic(Supplier<String> supplier) {
        return supplier == null ? empty() : new DynamicValue(supplier);
    }

    /**
     * Create a new string value.
     *
     * @param value value
     * @return Value
     */
    static Value<String> of(String value) {
        if (value == null) {
            return empty();
        } else if (value.isEmpty()) {
            return EMPTY_STRING;
        } else {
            return new StringValue(value);
        }
    }

    /**
     * Create a new list value.
     *
     * @param value value
     * @return Value
     */
    static Value<List<String>> of(List<String> value) {
        return value == null ? empty() : new ListValue(value);
    }

    /**
     * Create a new boolean value.
     *
     * @param value value
     * @return Value
     */
    static Value<Boolean> of(boolean value) {
        return value ? BooleanValue.TRUE : BooleanValue.FALSE;
    }

    /**
     * Create a new integer value.
     *
     * @param value value
     * @return Value
     */
    static Value<Integer> of(int value) {
        return new IntegerValue(value);
    }

    /**
     * Parse the given string as a list value.
     *
     * @param str string
     * @return list value
     */
    static Value<List<String>> parseList(String str) {
        if (str == null) {
            return empty();
        } else if ("none".equals(str)) {
            return EMPTY_LIST;
        } else {
            return Value.of(List.of(str.split(",")));
        }
    }

    /**
     * Parse the given string as a boolean value.
     *
     * @param str string
     * @return boolean value
     */
    static Value<Boolean> parseBoolean(String str) {
        if (str == null) {
            return empty();
        } else if ("false".equalsIgnoreCase(str)) {
            return FALSE;
        } else if ("true".equalsIgnoreCase(str)) {
            return TRUE;
        } else {
            return empty(() -> new ValueException("Cannot parse boolean value: " + str));
        }
    }

    /**
     * Parse the given string as an integer value.
     *
     * @param str string
     * @return integer value
     */
    static Value<Integer> parseInt(String str) {
        if (str == null) {
            return empty();
        } else {
            try {
                return Value.of(Integer.parseInt(str));
            } catch (NumberFormatException e) {
                return empty(() -> new ValueException("Cannot parse integer value: " + str));
            }
        }
    }

    /**
     * Convert the given value to a string.
     *
     * @param value value
     * @return string or {@code null} if empty
     */
    static String toString(Value<?> value) {
        if (value != null) {
            switch (value.type()) {
                case DYNAMIC:
                case STRING:
                    return value.getString();
                case INTEGER:
                    return String.valueOf(value.getInt());
                case BOOLEAN:
                    return String.valueOf(value.getBoolean());
                case LIST:
                    return String.join(",", value.getList());
                default:
            }
        }
        return null;
    }

    /**
     * Compare two values.
     *
     * @param v1 value
     * @param v2 value
     * @return {@code 1} if greater, {@code 0} if equal, {@code -1} if less than
     */
    static int compare(Value<?> v1, Value<?> v2) {
        boolean e1 = v1.isEmpty();
        boolean e2 = v2.isEmpty();
        if (e1 || e2) {
            return e1 && e2 ? 0 : e2 ? 1 : -1;
        }
        Type t1 = v1.type();
        Type t2 = v2.type();
        if (t1 == t2) {
            switch (t1) {
                case STRING:
                case DYNAMIC:
                    return v1.getString().compareTo(v2.getString());
                case INTEGER:
                    return Integer.compare(v1.getInt(), v2.getInt());
                case BOOLEAN:
                    return Boolean.compare(v1.getBoolean(), v2.getBoolean());
                case LIST:
                    return Lists.compare(v1.getList(), v2.getList());
                default:
                    throw new IllegalStateException("Unsupported value type: " + t1);
            }
        }
        return Objects.compare(toString(v1), toString(v2), String::compareTo);
    }

    /**
     * Test value equality.
     *
     * @param v1 value
     * @param v2 value
     * @return {@code true} if equal, {@code false} otherwise
     */
    static boolean isEqual(Value<?> v1, Value<?> v2) {
        if (v1 == v2) {
            return true;
        } else if (v1 == null || v2 == null) {
            return false;
        } else {
            switch (v1.type()) {
                case DYNAMIC:
                    switch (v2.type()) {
                        case DYNAMIC:
                        case STRING:
                            return Objects.equals(v1.asString().orElse(null), v2.asString().orElse(null));
                        default:
                            return isEqual(v2, v1);
                    }
                case STRING:
                    switch (v2.type()) {
                        case DYNAMIC:
                        case STRING:
                            return Objects.equals(v1.asString().orElse(null), v2.asString().orElse(null));
                        default:
                            return false;
                    }
                case LIST:
                    switch (v2.type()) {
                        case DYNAMIC:
                        case LIST:
                            List<String> list1 = v1.asList().orElse(null);
                            List<String> list2 = v2.asList().orElse(null);
                            if (list1 == list2) {
                                return true;
                            }
                            return list1 != null
                                   && list2 != null
                                   && list1.size() == list2.size()
                                   && new HashSet<>(list1).containsAll(list2);
                        default:
                            return false;
                    }
                case BOOLEAN:
                    switch (v2.type()) {
                        case DYNAMIC:
                        case BOOLEAN:
                            return Objects.equals(v1.asBoolean().orElse(null), v2.asBoolean().orElse(null));
                        default:
                            return false;
                    }
                case INTEGER:
                    switch (v2.type()) {
                        case DYNAMIC:
                        case INTEGER:
                            return Objects.equals(v1.asInt().orElse(null), v2.asInt().orElse(null));
                        default:
                            return false;
                    }
                default:
                    return Objects.equals(v1, v2);
            }
        }
    }

    /**
     * Value exception.
     */
    final class ValueException extends IllegalStateException {

        /**
         * Create a new value type exception.
         *
         * @param message exception message
         */
        private ValueException(String message) {
            super(message);
        }
    }

    /**
     * A value backed by a supplier that can be mapped to all types.
     */
    final class DynamicValue implements Value<String> {

        private final LazyValue<String> value;

        private DynamicValue(Supplier<String> supplier) {
            this.value = new LazyValue<>(supplier);
        }

        @Override
        public String toString() {
            return String.valueOf(value.get());
        }

        @Override
        public boolean isEmpty() {
            return value.get() == null;
        }

        @Override
        public Type type() {
            return Type.DYNAMIC;
        }

        @Override
        public String get() {
            String str = value.get();
            if (str == null) {
                throw new NoSuchElementException();
            }
            return str;
        }

        @Override
        public Value<String> asString() {
            return this;
        }

        @Override
        public Value<List<String>> asList() {
            return parseList(value.get());
        }

        @Override
        public Value<Boolean> asBoolean() {
            return parseBoolean(value.get());
        }

        @Override
        public Value<Integer> asInt() {
            return parseInt(value.get());
        }
    }

    /**
     * String value.
     */
    final class StringValue implements Value<String> {

        private final String value;

        private StringValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Type type() {
            return Type.STRING;
        }

        @Override
        public String get() {
            return value;
        }

        @Override
        public Value<String> asString() {
            return this;
        }

        @Override
        public Value<Boolean> asBoolean() {
            throw new ValueException("Cannot convert a string to a boolean");
        }

        @Override
        public Value<List<String>> asList() {
            throw new ValueException("Cannot convert a string to a list");
        }

        @Override
        public Value<Integer> asInt() {
            throw new ValueException("Cannot convert a string to an integer");
        }
    }

    /**
     * List value.
     */
    final class ListValue implements Value<List<String>> {

        private final List<String> value;

        private ListValue(List<String> value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Type type() {
            return Type.LIST;
        }

        @Override
        public List<String> get() {
            return value;
        }

        @Override
        public Value<List<String>> asList() {
            return this;
        }

        @Override
        public Value<Boolean> asBoolean() {
            throw new ValueException("Cannot convert a list to a boolean");
        }

        @Override
        public Value<String> asString() {
            throw new ValueException("Cannot convert a list to a string");
        }

        @Override
        public Value<Integer> asInt() {
            throw new ValueException("Cannot convert a list to an integer");
        }
    }

    /**
     * Boolean value.
     */
    final class BooleanValue implements Value<Boolean> {

        private final boolean value;

        private BooleanValue(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Type type() {
            return Type.BOOLEAN;
        }

        @Override
        public Boolean get() {
            return value;
        }

        @Override
        public Value<Boolean> asBoolean() {
            return this;
        }

        @Override
        public Value<String> asString() {
            throw new ValueException("Cannot convert a boolean to a string");
        }

        @Override
        public Value<Integer> asInt() {
            throw new ValueException("Cannot convert a boolean to an integer");
        }

        @Override
        public Value<List<String>> asList() {
            throw new ValueException("Cannot convert a boolean to a list");
        }
    }

    /**
     * Int value.
     */
    final class IntegerValue implements Value<Integer> {

        private final int value;

        private IntegerValue(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Type type() {
            return Type.INTEGER;
        }

        @Override
        public Integer get() {
            return value;
        }

        @Override
        public Value<Integer> asInt() {
            return this;
        }

        @Override
        public Value<Boolean> asBoolean() {
            throw new ValueException("Cannot convert an integer to a boolean");
        }

        @Override
        public Value<String> asString() {
            throw new ValueException("Cannot convert an integer to a string");
        }

        @Override
        public Value<List<String>> asList() {
            throw new ValueException("Cannot convert an integer to a list");
        }
    }

    /**
     * Empty value.
     *
     * @param <T> value type
     */
    final class EmptyValue<T> implements Value<T> {

        private static final Value<?> INSTANCE = new EmptyValue<>(NoSuchElementException::new);
        private final Supplier<? extends RuntimeException> supplier;

        private EmptyValue(Supplier<? extends RuntimeException> supplier) {
            this.supplier = supplier;
        }

        @Override
        public String toString() {
            return "null";
        }

        @Override
        public Type type() {
            return Type.EMPTY;
        }

        @Override
        public T get() {
            throw supplier.get();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Value<String> asString() {
            return (Value<String>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Value<Integer> asInt() {
            return (Value<Integer>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Value<List<String>> asList() {
            return (Value<List<String>>) this;
        }
    }
}
