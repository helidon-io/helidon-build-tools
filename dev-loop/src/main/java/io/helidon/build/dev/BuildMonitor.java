/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.dev;

import java.io.PrintStream;

import io.helidon.build.util.PrintStreams;

/**
 * A receiver of build loop messages and events.
 */
public interface BuildMonitor {

    /**
     * Returns a printer for messages written to stdout.
     *
     * @return The print stream
     */
    default PrintStream stdOut() {
        return PrintStreams.STDOUT;
    }

    /**
     * Returns a {@link PrintStream} for messages written to stderr.
     *
     * @return The print stream
     */
    default PrintStream stdErr() {
        return PrintStreams.STDERR;
    }

    /**
     * Called when the build loop has started.
     */
    void onStarted();

    /**
     * Called when a new build cycle is starting.
     *
     * @param cycleNumber The cycle number.
     */
    void onCycleStart(int cycleNumber);

    /**
     * Called when project changes have been detected.
     *
     * @param cycleNumber The cycle number.
     * @param type The change type.
     */
    void onChanged(int cycleNumber, ChangeType type);

    /**
     * Called when a build is about to start.
     *
     * @param cycleNumber The cycle number.
     * @param type The build type.
     */
    void onBuildStart(int cycleNumber, BuildType type);

    /**
     * Called when a build has succeeded.
     *
     * @param cycleNumber The cycle number.
     * @param type The build type.
     */
    void onBuildSuccess(int cycleNumber, BuildType type);

    /**
     * Called when a build has failed.
     *
     * @param cycleNumber The cycle number.
     * @param type The build type.
     * @param error The error.
     * @return The number of milliseconds to delay before retrying build.
     */
    long onBuildFail(int cycleNumber, BuildType type, Throwable error);

    /**
     * Called when a build has succeeded or when an initial build was not required.
     *
     * @param cycleNumber The cycle number.
     * @param project The project.
     * @return The number of milliseconds to delay before restarting the build cycle.
     */
    long onReady(int cycleNumber, Project project);

    /**
     * The action to take on the next cycle.
     */
    enum NextAction {
        /**
         * Continue.
         */
        CONTINUE,
        /**
         * Wait for a change.
         */
        WAIT_FOR_CHANGE,

        /**
         * Exit.
         */
        EXIT
    }

    /**
     * Called when a build cycle has completed.
     *
     * @param cycleNumber The cycle number.
     * @return {@code true} to continue to next build cycle, {@code false} to stop.
     */
    NextAction onCycleEnd(int cycleNumber);

    /**
     * Called when build loop has failed.
     *
     * @param cycleNumber The cycle number.
     * @param error The error.
     */
    void onLoopFail(int cycleNumber, Throwable error);

    /**
     * Called when build loop has stopped.
     */
    void onStopped();
}
