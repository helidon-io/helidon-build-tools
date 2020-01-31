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
 * A receiver of build messages and status.
 */
public interface BuildMonitor {
    /**
     * Returns a consumer for stdout.
     *
     * @return The consumer.
     */
    Consumer<String> stdOutConsumer();

    /**
     * Returns a consumer for stderr.
     *
     * @return The consumer.
     */
    Consumer<String> stdErrConsumer();

    /**
     * Called when build has started.
     *
     * @return {@code true} if the build should be cleaned prior to first scan.
     */
    boolean onStarted();

    /**
     * Called when build cycle is starting a scan.
     *
     * @return {@code true} if binaries should be watched for changes.
     */
    boolean onCycleStart();

    /**
     * Called when project changes have been detected.
     *
     * @param binariesOnly {@code true} if binaries are being watched and only binary changes were detected.
     */
    void onChanged(boolean binariesOnly);

    /**
     * Called when build is about to start.
     *
     * @param incremental {@code true} if build is increments, {@code false} if full.
     */
    void onBuildStart(boolean incremental);

    /**
     * Called when build has failed.
     *
     * @param error The error.
     * @return The number of milliseconds to delay before retrying build.
     */
    long onBuildFail(Throwable error);

    /**
     * Called when build has succeeded or initial build was not required.
     *
     * @return The number of milliseconds to delay before restarting build cycle.
     */
    long onReady();

    /**
     * Called when build cycle has finished
     *
     * @return {@code true} if should continue, {@code false} if should stop.
     */
    boolean onCycleEnd();

    /**
     * Called when build cycle has stopped.
     */
    void onStopped();
}
