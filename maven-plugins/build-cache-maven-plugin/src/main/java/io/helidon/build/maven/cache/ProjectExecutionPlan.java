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

import java.util.List;
import java.util.stream.Collectors;

import static io.helidon.build.maven.cache.ExecutionStatus.STATE_CACHED;
import static io.helidon.build.maven.cache.ExecutionStatus.STATE_DIFF;
import static io.helidon.build.maven.cache.ExecutionStatus.STATE_NEW;

/**
 * Project execution plan.
 */
final class ProjectExecutionPlan {

    private final List<ExecutionStatus> executionStatuses;
    private final ProjectStateStatus stateStatus;

    /**
     * Create a new plan.
     *
     * @param stateStatus project state status
     * @param executions  life-cycle executions for the project
     */
    ProjectExecutionPlan(ProjectStateStatus stateStatus, List<ExecutionEntry> executions) {
        this.stateStatus = stateStatus;
        ProjectState state = stateStatus.state();
        this.executionStatuses = executions
                .stream()
                .map(exec -> {
                    if (state.hasMatchingExecution(exec)) {
                        ConfigDiffs diffs = state.findMatchingExecution(exec).config().diff(exec.config());
                        if (diffs.hasNext()) {
                            return new ExecutionStatus(STATE_DIFF, exec, diffs);
                        } else {
                            return new ExecutionStatus(STATE_CACHED, exec, null);
                        }
                    }
                    return new ExecutionStatus(STATE_NEW, exec, null);
                }).collect(Collectors.toList());
    }

    /**
     * Get the project state status.
     * @return ProjectStateStatus
     */
    ProjectStateStatus stateStatus() {
        return stateStatus;
    }

    /**
     * Get the executions statuses.
     *
     * @return list of execution status
     */
    List<ExecutionStatus> executionStatuses() {
        return executionStatuses;
    }

    /**
     * Get the life-cycle execution that do have a fully matching record in the state.
     *
     * @return list of execution entry
     */
    List<ExecutionEntry> cachedExecutions() {
        return executionStatuses.stream()
                                .filter(ExecutionStatus::isCached)
                                .map(ExecutionStatus::execution)
                                .collect(Collectors.toList());
    }

    /**
     * Test if the plan has only cached executions.
     *
     * @return {@code true} if all cached, {@code false} otherwise
     */
    boolean allCached() {
        return executionStatuses.stream().allMatch(ExecutionStatus::isCached);
    }

    /**
     * Test if the state is invalid because files have changed.
     *
     * @return {@code true} if the project files have changed.
     */
    boolean hasFileChanges() {
        return stateStatus.code() == ProjectStateStatus.STATE_FILES_CHANGED;
    }

    /**
     * Test if state is invalid because one or more downstream states are invalid.
     *
     * @return {@code true} if  one or more downstream states are invalid.
     */
    public boolean hasInvalidDownstream() {
        return stateStatus.code() == ProjectStateStatus.STATE_INVALID_DOWNSTREAM;
    }
}
