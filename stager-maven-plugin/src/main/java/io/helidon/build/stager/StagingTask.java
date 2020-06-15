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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all tasks.
 */
abstract class StagingTask {

    private final String target;
    private final List<Map<String, List<String>>> iterators;

    StagingTask(List<Map<String, List<String>>> iterators, String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target is required");
        }
        this.target = target;
        this.iterators = iterators == null ? Collections.emptyList() : iterators;
    }

    /**
     * Get the target.
     * @return target, never {@code nul}
     */
    String target() {
        return target;
    }

    /**
     * Get the task iterators.
     * @return task iterators as map, never {@code null}
     */
    List<Map<String, List<String>>> iterators() {
        return iterators;
    }

    /**
     * Execute the task with iterations.
     *
     * @param context   staging context
     * @param dir       stage directory
     * @param variables variables for the current iteration
     * @throws IOException if an IO error occurs
     * @throws IOException if an IO error occurs
     */
    void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        for (Map<String, List<String>> iterator : iterators) {
            Map<String, String> vars = new HashMap<>(variables);
            Map.Entry<String, List<String>>[] entries = iterator.entrySet().toArray(new Map.Entry[iterator.size()]);
            int numIterations = 1;
            for (Map.Entry<String, List<String>> entry : entries) {
                numIterations *= entry.getValue().size();
            }
            int[] indexes = new int[entries.length];
            for (int i = 1; i <= numIterations; i++) {
                int p = 1;
                for (int idx = 0; idx <  entries.length; idx++) {
                    int size = entries[idx].getValue().size();
                    if (indexes[idx] == size) {
                        indexes[idx] = 0;
                    }
                    p *= size;
                    String val;
                    if (i % (numIterations / p) == 0) {
                        val = entries[idx].getValue().get(indexes[idx]++);
                    } else {
                        val = entries[idx].getValue().get(indexes[idx]);
                    }
                    vars.put(entries[idx].getKey(), val);
                }
                doExecute(context, dir, vars);
            }
        }
    }

    /**
     * Execute the task.
     *
     * @param context   staging context
     * @param dir       stage directory
     * @param variables variables for the current iteration
     * @throws IOException if an IO error occurs
     */
    protected abstract void doExecute(StagingContext context, Path dir, Map<String, String> variables)
            throws IOException;

    /**
     * Resolve variables in a given string.
     *
     * @param source    source to be resolved
     * @param variables variables used to perform the resolution
     * @return resolve string
     */
    protected static String resolveVar(String source, Map<String, String> variables) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            source = source.replaceAll("\\{" + variable.getKey() + "\\}", variable.getValue());
        }
        return source;
    }
}
