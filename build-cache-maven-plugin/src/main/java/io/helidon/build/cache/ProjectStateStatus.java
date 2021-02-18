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
package io.helidon.build.cache;

/**
 * Project state status.
 */
final class ProjectStateStatus {

    /**
     * State is valid.
     */
    static final int STATE_VALID = 0;

    /**
     * State is not available.
     */
    static final int STATE_UNAVAILABLE = 1;

    /**
     * State is invalid because of file changes.
     */
    static final int STATE_FILES_CHANGED = 2;

    /**
     * State is invalid because a downstream state is invalid.
     */
    static final int STATE_INVALID_DOWNSTREAM = 3;

    /**
     * Constant status instance for the {@link #STATE_UNAVAILABLE} state.
     */
    static final ProjectStateStatus UNAVAILABLE = new ProjectStateStatus(STATE_UNAVAILABLE, null, null);

    private final int code;
    private final ProjectState state;
    private final ProjectFiles projectFiles;

    ProjectStateStatus(int code, ProjectState state, ProjectFiles projectFiles) {
        this.code = code;
        this.state = state;
        this.projectFiles = projectFiles;
    }

    /**
     * Get the state code.
     *
     * @return code
     */
    int code() {
        return code;
    }

    /**
     * Get the current project files.
     *
     * @return ProjectFiles, may be {@code null}
     */
    ProjectFiles projectFiles() {
        return projectFiles;
    }

    /**
     * Invalidate the state of this status.
     *
     * @return new status
     */
    ProjectStateStatus invalidate() {
        return new ProjectStateStatus(STATE_INVALID_DOWNSTREAM, state, projectFiles);
    }

    /**
     * Get the state from this processing result.
     *
     * @return ProjectState
     * @throws IllegalStateException if the status is not ok
     */
    ProjectState state() throws IllegalStateException {
        return state;
    }
}
