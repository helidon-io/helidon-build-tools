/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all tasks.
 */
abstract class StagingTask implements StagingAction {

    private final String target;
    private final ActionIterators iterators;

    StagingTask(ActionIterators iterators, String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target is required");
        }
        this.target = target;
        this.iterators = iterators;
    }

    /**
     * Get the target.
     *
     * @return target, never {@code nul}
     */
    String target() {
        return target;
    }

    /**
     * Get the task iterators.
     *
     * @return task iterators, may be {@code null}
     */
    ActionIterators iterators() {
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
    @Override
    public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        if (iterators == null || iterators.isEmpty()) {
            context.submit(() -> {
                doExecute(context, dir, variables);
                return CompletableFuture.completedFuture(null);
            });
            return;
        }
        for (ActionIterator iterator : iterators) {
            iterator.baseVariable(variables);
            while (iterator.hasNext()) {
                Map<String, String> wrappedVariables = new HashMap<>(iterator.next());
                context.submit(() -> {
                    doExecute(context, dir, wrappedVariables);
                    return CompletableFuture.completedFuture(null);
                });
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
