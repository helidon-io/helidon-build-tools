/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

/**
 * Recorded execution.
 */
final class RecordedExecution {

    private final ExecutionEntry execution;
    private final ConfigDiffs diffs;

    /**
     * Create a new recorded actual instance.
     *
     * @param actual actual actual entry
     * @param diffs  config diffs
     */
    RecordedExecution(ExecutionEntry actual, ConfigDiffs diffs) {
        this.execution = actual;
        this.diffs = diffs;
    }

    /**
     * Get the execution.
     *
     * @return execution
     */
    ExecutionEntry exec() {
        return execution;
    }

    /**
     * Get the diffs.
     *
     * @return diffs
     */
    ConfigDiffs diffs() {
        return diffs;
    }
}
