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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.helidon.build.common.CurrentThreadExecutorService;

/**
 * Executor's configuration component.
 */
public class ExecutorConfig {

    private String kind;

    private Map<String, String> parameters;

    /**
     * Get configured kind of executor.
     *
     * @return kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Set kind of executor.
     *
     * @param kind of executor
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Get executor parameters.
     *
     * @return parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Set executor parameters.
     *
     * @param parameters to configure executor
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
        ExecutorKind executor = ExecutorKind.parse(kind).orElse(ExecutorKind.DEFAULT);
        switch (executor) {
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

    enum ExecutorKind {
        DEFAULT, CACHED, SINGLE, FIXED, SCHEDULED, SINGLESCHEDULED, VIRTUAL, WORKSTEALINGPOOL;

        public static Optional<ExecutorKind> parse(String kind) {
            if (Objects.isNull(kind)) {
                return Optional.of(DEFAULT);
            }
            return Arrays.stream(ExecutorKind.values())
                    .filter(value -> Objects.equals(value.toString(), kind.toUpperCase()))
                    .findFirst();
        }
    }
}