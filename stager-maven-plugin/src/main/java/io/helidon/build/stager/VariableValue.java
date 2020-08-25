/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal model for variable value.
 *
 * @see io.helidon.build.stager.VariableValue.SimpleValue
 * @see io.helidon.build.stager.VariableValue.ListValue
 * @param <T> value type
 */
interface VariableValue<T> extends StagingElement {

    String ELEMENT_NAME = "value";

    /**
     * Convert this value to a plain object.
     *
     * @return T
     */
    T unwrap();

    @Override
    default String elementName() {
        return ELEMENT_NAME;
    }

    /**
     * Simple text value.
     */
    class SimpleValue implements VariableValue<String> {

        private final String text;

        SimpleValue(String text) {
            if (text == null || text.isEmpty()) {
                throw new IllegalArgumentException("text is required");
            }
            this.text = text;
        }

        @Override
        public String unwrap() {
            return text;
        }
    }

    /**
     * A value that holds a list of values.
     */
    class ListValue implements VariableValue<List<Object>> {

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
            return value.stream().map(VariableValue::unwrap).collect(Collectors.toList());
        }
    }

    /**
     * A value that holds a list of values.
     */
    class MapValue implements VariableValue<Map<String, Object>> {

        private final Map<String, VariableValue> value;

        MapValue(List<Variable> value) {
            if (this == null) {
                this.value = Map.of();
            } else {
                this.value = new HashMap<>();
                for (Variable variable : value) {
                    this.value.put(variable.name(), variable.value());
                }
            }
        }

        @Override
        public Map<String, Object> unwrap() {
            return value.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrap()));
        }
    }
}
