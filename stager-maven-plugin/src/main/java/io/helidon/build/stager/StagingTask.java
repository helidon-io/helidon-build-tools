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
import java.util.Objects;

/**
 * Base class for all tasks.
 */
abstract class StagingTask {

    private final String target;
    private final List<Map<String, String>> iterators;

    StagingTask(List<Map<String, String>> iterators, String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target is required");
        }
        this.target = target;
        this.iterators = iterators == null ? Collections.emptyList() : iterators;
    }

    String target() {
        return target;
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
        for (Map<String, String> iterator : iterators) {
            Map<String, String> vars = new HashMap<>();
            vars.putAll(variables);
            for (Map.Entry<String, String> variable : iterator.entrySet()) {
                vars.put(variable.getKey(), variable.getValue());
            }
            doExecute(context, dir, variables);
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
            source = source.replaceAll("{" + variable.getKey() + "}", variable.getValue());
        }
        return source;
    }
}
