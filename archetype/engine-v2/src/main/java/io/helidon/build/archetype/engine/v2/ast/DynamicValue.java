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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.helidon.build.common.GenericType;

import static java.util.stream.Collectors.toList;

/**
 * A value with no predefined type, parsed on the fly.
 */
public final class DynamicValue implements Value {

    private final Supplier<String> supplier;
    private String rawValue;

    private DynamicValue(Supplier<String> supplier) {
        this.supplier = supplier;
    }

    /**
     * Create a new dynamic value.
     *
     * @param value raw value
     * @return Value
     */
    public static Value create(String value) {
        return new DynamicValue(() -> value);
    }

    /**
     * Create a new dynamic value.
     *
     * @param supplier raw value supplier
     * @return Value
     */
    public static Value create(Supplier<String> supplier) {
        return new DynamicValue(supplier);
    }

    @Override
    public Object unwrap() {
        initRawValue();
        return rawValue;
    }

    private void initRawValue() {
        if (rawValue == null) {
            rawValue = supplier.get();
        }
    }

    @Override
    public GenericType<?> type() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U as(GenericType<U> type) {
        Objects.requireNonNull(type, "type is null");
        initRawValue();
        if (type.equals(ValueTypes.BOOLEAN)) {
            return (U) Boolean.valueOf(Input.Boolean.valueOf(rawValue));
        } else if (type.equals(ValueTypes.INT)) {
            return (U) Integer.valueOf(rawValue);
        } else if (type.equals(ValueTypes.STRING_LIST)) {
            if ("none".equals(rawValue)) {
                return (U) List.of();
            }
            return (U) Arrays.stream(rawValue.split(","))
                             .map(String::trim)
                             .filter(s -> !s.isEmpty())
                             .sorted()
                             .collect(toList());
        } else if (type.equals(ValueTypes.STRING)) {
            return (U) rawValue;
        }
        throw new DynamicValueTypeException(type);
    }

    @Override
    public String toString() {
        return "DynamicValue{"
                + "rawValue='" + rawValue + '\''
                + '}';
    }

    private static final class DynamicValueTypeException extends ValueTypeException {

        DynamicValueTypeException(GenericType<?> type) {
            super(String.format("Cannot get a dynamic value as { %s }", type));
        }
    }
}
