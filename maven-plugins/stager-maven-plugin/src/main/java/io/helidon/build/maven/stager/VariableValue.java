/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.common.Strings;

/**
 * Internal model for variable value.
 *
 * @see VariableValue.SimpleValue
 * @see VariableValue.ListValue
 * @see VariableValue.EmptyValue
 */
public interface VariableValue extends StagingElement {

    /**
     * Convert this value to a plain object.
     *
     * @return T
     */
    Object unwrap();

    @Override
    default String elementName() {
        return "value";
    }

    /**
     * Merge a value.
     *
     * @param path  path
     * @param other value to merge
     * @return merged value
     */
    default VariableValue merge(String path, VariableValue other) {
        throw new IllegalArgumentException("Conflicting variable '%s'".formatted(path));
    }

    /**
     * Empty variable value.
     */
    class EmptyValue implements VariableValue {

        private final String name;

        EmptyValue(String name) {
            this.name = Strings.requireValid(name, "name is required");
        }

        @Override
        public Object unwrap() {
            throw new IllegalStateException("Variable '" + name + "' does not have a value");
        }

        @Override
        public VariableValue merge(String path, VariableValue other) {
            if (other instanceof EmptyValue) {
                return this;
            }
            return VariableValue.super.merge(path, other);
        }
    }

    /**
     * Simple text value.
     */
    class SimpleValue implements VariableValue {

        private final String text;

        SimpleValue(String text) {
            this.text = Strings.requireValid(text, "text is required");
        }

        @Override
        public String unwrap() {
            return text;
        }

        @Override
        public VariableValue merge(String path, VariableValue other) {
            if (other instanceof SimpleValue otherSimple
                && text.equals(otherSimple.unwrap())) {
                return this;
            }
            return VariableValue.super.merge(path, other);
        }
    }

    /**
     * A value that holds a list of values.
     */
    class ListValue implements VariableValue {

        private final List<VariableValue> value;

        ListValue(String... values) {
            this.value = new LinkedList<>();
            for (String v : values) {
                this.value.add(new SimpleValue(v));
            }
        }

        ListValue(List<VariableValue> value) {
            this.value = value == null ? List.of() : value;
        }

        @Override
        public List<Object> unwrap() {
            return value.stream()
                    .map(VariableValue::unwrap)
                    .collect(Collectors.toList());
        }

        @Override
        public VariableValue merge(String path, VariableValue other) {
            if (other instanceof ListValue otherList) {
                List<VariableValue> merged = new ArrayList<>(value.size() + otherList.value.size());
                merged.addAll(value);
                merged.addAll(otherList.value);
                return new ListValue(merged);
            }
            return VariableValue.super.merge(path, other);
        }

        List<VariableValue> values() {
            return value;
        }
    }

    /**
     * A value that holds a list of values.
     */
    class MapValue implements VariableValue {

        private final Map<String, VariableValue> value;

        MapValue(List<Variable> value) {
            if (value == null) {
                this.value = Map.of();
            } else {
                this.value = new LinkedHashMap<>();
                for (Variable variable : value) {
                    this.value.put(variable.name(), variable.value());
                }
            }
        }

        MapValue(Map<String, VariableValue> value) {
            this.value = value;
        }

        @Override
        public VariableValue merge(String path, VariableValue other) {
            if (other instanceof MapValue otherMap) {
                Map<String, VariableValue> merged = new LinkedHashMap<>(value);
                otherMap.value.forEach((k, v) -> merged.merge(k, v, (l, r) -> l.merge(path + "." + k, r)));
                return new MapValue(merged);
            }
            return VariableValue.super.merge(path, other);
        }

        @Override
        public Map<String, Object> unwrap() {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, VariableValue> entry : value.entrySet()) {
                result.put(entry.getKey(), entry.getValue().unwrap());
            }
            return result;
        }

        Map<String, VariableValue> values() {
            return value;
        }
    }
}
