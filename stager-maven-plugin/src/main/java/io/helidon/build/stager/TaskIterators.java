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

/**
 * Task iterators.
 */
final class TaskIterators implements Iterable<Variables> {

    private final List<Variables> variables;

    TaskIterators(List<Variables> variables) {
        this.variables = variables;
    }

    @Override
    public Iterator<Variables> iterator() {
        return null;
    }

    List<Map<String, String>> asList() {
        List<Map<String, String>> list = new LinkedList<>();
        for (Variables variables : variables) {
            Map<String, String> iterator = new HashMap<>();
            list.add(iterator);
            for (Variable variable : variables) {
                VariableValue value = variable.value();
                if (value.isSimple()) {
                    iterator.put(variable.name(), value.asSimple().text());
                } else {
                    throw new IllegalStateException("Iterator only supports list of text values");
                }
            }
        }
        return list;
    }
}
