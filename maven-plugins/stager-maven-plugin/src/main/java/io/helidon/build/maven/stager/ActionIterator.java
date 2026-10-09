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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action iterator.
 */
final class ActionIterator implements Joinable {

    private final Variables variables;

    ActionIterator(Variables variables) {
        this.variables = variables;
    }

    @Override
    public boolean join() {
        return variables.join();
    }

    /**
     * Combine this iterator's variables with the given variables.
     *
     * @param variables variables
     * @return variable combinations
     */
    List<Map<String, String>> forVariables(Map<String, String> variables) {
        List<Map<String, String>> combinations = new ArrayList<>();
        combinations.add(new HashMap<>(variables));
        for (List<Map<String, String>> values : this.variables.values()) {
            List<Map<String, String>> expanded = new ArrayList<>();
            for (Map<String, String> combination : combinations) {
                for (Map<String, String> value : values) {
                    Map<String, String> next = new HashMap<>(combination);
                    next.putAll(value);
                    expanded.add(next);
                }
            }
            combinations = expanded;
        }
        return combinations;
    }
}
