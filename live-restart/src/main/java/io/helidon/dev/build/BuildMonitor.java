/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.util.function.Consumer;

/**
 * A receiver of build loop messages and events.
 */
public interface BuildMonitor {

    /**
     * Returns a consumer for messages written to stdout.
     *
     * @return The consumer.
     */
    default Consumer<String> stdOutConsumer() {
        return System.out::println;
    }

    /**
     * Returns a consumer for messages written to stderr.
     *
     * @return The consumer.
     */
    default Consumer<String> stdErrConsumer() {
        return System.err::println;
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
     * @param binariesOnly {@code true} if only binaries are being watched and only binary changes were detected.
     */
    void onChanged(int cycleNumber, boolean binariesOnly);

    /**
     * Called when a build is about to start.
     *
     * @param incremental {@code true} if this is an incremental build, {@code false} if full.
     */
    void onBuildStart(int cycleNumber, boolean incremental);

    /**
     * Called when a build has failed.
     *
     * @param cycleNumber The cycle number.
     * @param error The error.
     * @return The number of milliseconds to delay before retrying build.
     */
    long onBuildFail(int cycleNumber, Throwable error);

    /**
     * Called when a build has succeeded or when an initial build was not required.
     *
     * @param cycleNumber The cycle number.
     * @param project The project.
     * @return The number of milliseconds to delay before restarting the build cycle.
     */
    long onReady(int cycleNumber, Project project);

    /**
     * Called when a build cycle has completed.
     *
     * @param cycleNumber The cycle number.
     * @return {@code true} to continue to next build cycle, {@code false} to stop.
     */
    boolean onCycleEnd(int cycleNumber);

    /**
     * Called when build loop has stopped.
     */
    void onStopped();
}
