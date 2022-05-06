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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Action iterator.
 */
final class ActionIterator implements Iterator<Map<String, String>> {

    private final Map.Entry<String, List<String>>[] entries;
    private final int[] indexes;
    private final int maxIterations;
    private int iteration;
    private final HashMap<String, String> baseVariables;

    @SuppressWarnings("unchecked")
    ActionIterator(Variables variables) {
        baseVariables = new HashMap<>();
        Map<String, List<String>> iteratorVariables = new HashMap<>();
        for (Variable variable : variables) {
            List<String> values = new LinkedList<>();
            iteratorVariables.put(variable.name(), values);
            Object unwrappedValue = variable.value().unwrap();
            if (unwrappedValue instanceof String) {
                values.add((String) unwrappedValue);
            } else if (unwrappedValue instanceof List) {
                for (Object o : (List<?>) unwrappedValue) {
                    if (o instanceof String) {
                        values.add((String) o);
                    }
                }
            }
        }
        int n = 1;
        for (List<String> values : iteratorVariables.values()) {
            n *= values.size();
        }
        maxIterations = n;
        iteration = 1;
        indexes = new int[iteratorVariables.size()];
        entries = iteratorVariables.entrySet().toArray(new Map.Entry[0]);
    }

    /**
     * Set the base variables.
     *
     * @param variables base variables
     */
    @SuppressWarnings("UnusedReturnValue")
    ActionIterator baseVariable(Map<String, String> variables) {
        baseVariables.putAll(variables);
        return this;
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
        int p = 1;
        for (int idx = 0; idx < entries.length; idx++) {
            int size = entries[idx].getValue().size();
            if (indexes[idx] == size) {
                indexes[idx] = 0;
            }
            p *= size;
            String val;
            if (iteration % (maxIterations / p) == 0) {
                val = entries[idx].getValue().get(indexes[idx]++);
            } else {
                val = entries[idx].getValue().get(indexes[idx]);
            }
            baseVariables.put(entries[idx].getKey(), val);
        }
        return baseVariables;
    }
}
