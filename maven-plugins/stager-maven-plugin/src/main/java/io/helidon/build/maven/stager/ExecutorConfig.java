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
package io.helidon.build.maven.stager;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.helidon.build.common.CurrentThreadExecutorService;

/**
 * Executor service configuration.
 */
@SuppressWarnings("unused")
public class ExecutorConfig {

    private ExecutorKind kind = ExecutorKind.DEFAULT;
    private Map<String, String> parameters;

    /**
     * Create a new executor config.
     *
     * @param kind       kind
     * @param parameters parameters
     */
    ExecutorConfig(ExecutorKind kind, Map<String, String> parameters) {
        this.kind = kind;
        this.parameters = parameters;
    }

    /**
     * Create a new executor config.
     */
    public ExecutorConfig() {
    }

    /**
     * Get the executor kind.
     *
     * @return kind
     */
    public ExecutorKind getKind() {
        return kind;
    }

    /**
     * Set the executor kind.
     *
     * @param kind kind
     */
    public void setKind(ExecutorKind kind) {
        this.kind = kind;
    }

    /**
     * Get the executor parameters.
     *
     * @return parameters map
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Set the executor parameters.
     *
     * @param parameters parameters map
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Select the configured ExecutorService.
     *
     * @return ExecutorService
     */
    public ExecutorService select() {
        switch (kind) {
            case DEFAULT:
                return new CurrentThreadExecutorService();
            case CACHED:
                return Executors.newCachedThreadPool();
            case SINGLE:
                return Executors.newSingleThreadExecutor();
            case FIXED:
                return Executors.newFixedThreadPool(getParameter("nThreads", 5));
            case SCHEDULED:
                return Executors.newScheduledThreadPool(getParameter("corePoolSize", 5));
            case SINGLESCHEDULED:
                return Executors.newSingleThreadScheduledExecutor();
            case WORKSTEALINGPOOL:
                int parallelism = getParameter("parallelism", -1);
                return parallelism == -1
                        ? Executors.newWorkStealingPool()
                        : Executors.newWorkStealingPool(parallelism);
            default:
                throw new IllegalArgumentException("Executor kind must be one of " + Arrays.toString(ExecutorKind.values()));
        }
    }

    private int getParameter(String key, int defaultValue) {
        if (Objects.isNull(parameters)) {
            return defaultValue;
        }
        return parameters.get(key) != null ? Integer.parseInt(parameters.get(key)) : defaultValue;
    }

    /**
     * Executor kind.
     */
    enum ExecutorKind {
        /**
         * Uses the current thread (no parallelism).
         */
        DEFAULT,

        /**
         * Uses {@link Executors#newCachedThreadPool}.
         */
        CACHED,

        /**
         * Uses {@link Executors#newSingleThreadExecutor}.
         */
        SINGLE,

        /**
         * Uses {@link Executors#newFixedThreadPool(int)}.
         * Number of threads is configured via parameter {@code nThreads}. Default value is {@code 5}.
         */
        FIXED,

        /**
         * Uses {@link Executors#newScheduledThreadPool(int)}.
         * The number of threads to keep in the pool is configured via parameter {@code corePoolSize}. Default value is {@code 5}.
         */
        SCHEDULED,

        /**
         * Uses {@link Executors#newSingleThreadScheduledExecutor}.
         */
        SINGLESCHEDULED,

        /**
         * Not implemented yet.
         */
        VIRTUAL,

        /**
         * Uses {@link Executors#newWorkStealingPool(int)}.
         * The targeted parallelism level can be configured via {@code parallelism}. Default is {@code -1} which means
         * max parallelism (using {@link Executors#newWorkStealingPool}).
         */
        WORKSTEALINGPOOL
    }
}
