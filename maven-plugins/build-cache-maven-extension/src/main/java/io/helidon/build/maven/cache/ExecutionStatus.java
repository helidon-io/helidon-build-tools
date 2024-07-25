/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.util.List;

/**
 * Execution status.
 */
public final class ExecutionStatus {

    static final int STATE_CACHED = 0;
    static final int STATE_NEW = 1;
    static final int STATE_DIFF = 2;

    private final int code;
    private final ExecutionEntry execution;
    private final List<ConfigDiff> diffs;

    /**
     * Create a new execution status.
     *
     * @param code      execution state ; {@link #STATE_CACHED}, {@link #STATE_DIFF}, {@link #STATE_NEW}
     * @param execution execution entry
     * @param diffs     config diff if the status is {@link #STATE_DIFF}
     */
    ExecutionStatus(int code, ExecutionEntry execution, List<ConfigDiff> diffs) {
        if (code > 2) {
            throw new IllegalArgumentException("Invalid state: " + code);
        }
        this.code = code;
        this.execution = execution;
        this.diffs = diffs;
    }

    /**
     * Indicate if the execution state is {@link #STATE_NEW}.
     *
     * @return {@code true} if the state is {@link #STATE_NEW}, {@code false} otherwise
     */
    public boolean isNew() {
        return code == STATE_NEW;
    }

    /**
     * Indicate if the execution state is {@link #STATE_DIFF}.
     *
     * @return {@code true} if the state is {@link #STATE_DIFF}, {@code false} otherwise
     */
    public boolean isDiff() {
        return code == STATE_DIFF;
    }

    /**
     * Indicate if the execution state is {@link #STATE_CACHED}.
     *
     * @return {@code true} if the state is {@link #STATE_CACHED}, {@code false} otherwise
     */
    public boolean isCached() {
        return code == STATE_CACHED;
    }

    /**
     * Get the diffs if the execution state is {@link #STATE_DIFF}.
     *
     * @return diffs
     */
    public List<ConfigDiff> diffs() {
        return diffs;
    }

    /**
     * Get the execution.
     *
     * @return execution, never {@code null}
     */
    public ExecutionEntry execution() {
        return execution;
    }

    private static final String[] STATES_DESC = new String[]{"Cached", "New  ", "Diff  "};

    @Override
    public String toString() {
        return STATES_DESC[code] + " | " + execution.name();
    }
}
