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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.build.common.Maps;

/**
 * Action iterator.
 */
final class ActionIterator implements Iterator<Map<String, String>>, Joinable {

    private final List<Map<String, String>>[] entries;
    private final int[] indexes;
    private final int maxIterations;
    private int iteration;
    private final Map<String, String> variables;
    private final boolean join;

    @SuppressWarnings("unchecked")
    ActionIterator(Variables variables) {
        this.variables = new HashMap<>();
        this.join = variables.join();
        Map<String, List<Map<String, String>>> iteratorVariables = new HashMap<>();
        for (Variable variable : variables) {
            List<Map<String, String>> values = new LinkedList<>();
            iteratorVariables.put(variable.name(), values);
            if (variable.value() instanceof VariableValue.EmptyValue) {
                continue;
            }
            Object unwrappedValue = variable.value().unwrap();
            if (unwrappedValue instanceof String value) {
                values.add(Map.of(variable.name(), value));
            } else if (unwrappedValue instanceof List<?> list) {
                for (Object value : list) {
                    if (value instanceof String string) {
                        values.add(Map.of(variable.name(), string));
                    } else if (value instanceof Map<?, ?> map) {
                        values.add(stringMap(variable.name(), map));
                    }
                }
            }
        }
        int n = 1;
        for (List<Map<String, String>> values : iteratorVariables.values()) {
            n *= values.size();
        }
        maxIterations = n;
        iteration = 1;
        indexes = new int[iteratorVariables.size()];
        entries = iteratorVariables.values().toArray(List[]::new);
    }

    ActionIterator(ActionIterator it, Map<String, String> variables) {
        entries = it.entries;
        indexes = it.indexes;
        maxIterations = it.maxIterations;
        iteration = it.iteration;
        join = it.join;
        this.variables = Maps.putAll(it.variables, variables);
    }

    @Override
    public boolean join() {
        return join;
    }

    @Override
    public boolean hasNext() {
        return iteration <= maxIterations;
    }

    @Override
    public Map<String, String> next() {
        if (iteration++ > maxIterations) {
            throw new NoSuchElementException();
        }
        Map<String, String> next = new HashMap<>(variables);
        int p = 1;
        for (int idx = 0; idx < entries.length; idx++) {
            int size = entries[idx].size();
            if (indexes[idx] == size) {
                indexes[idx] = 0;
            }
            p *= size;
            Map<String, String> values = entries[idx].get(indexes[idx]);
            if (iteration % (maxIterations / p) == 0) {
                indexes[idx]++;
            }
            next.putAll(values);
        }
        return next;
    }

    /**
     * Make a copy of this iterator that includes the given variables.
     *
     * @param variables variables
     */
    ActionIterator forVariables(Map<String, String> variables) {
        return new ActionIterator(this, variables);
    }

    private static Map<String, String> stringMap(String name, Map<?, ?> map) {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String value)) {
                throw new IllegalArgumentException(
                        "Iterator variable '%s' requires string map entries"
                                .formatted(name));
            }
            values.put(key, value);
        }
        return values;
    }
}
