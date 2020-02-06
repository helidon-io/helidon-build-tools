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

/**
 * A receiver of incremental build loop events.
 */
public interface BuildMonitor {

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
     * @param binariesOnly {@code true} if only binaries are being watched and only binary changes were detected.
     */
    void onChanged(boolean binariesOnly);

    /**
     * Called when a build is about to start.
     *
     * @param incremental {@code true} if this is an incremental build, {@code false} if full.
     */
    void onBuildStart(boolean incremental);

    /**
     * Called when a build has failed.
     *
     * @param error The error.
     * @return The number of milliseconds to delay before retrying build.
     */
    long onBuildFail(Throwable error);

    /**
     * Called when a build has succeeded or initial build was not required.
     *
     * @return The number of milliseconds to delay before restarting the build cycle.
     */
    long onReady();

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
