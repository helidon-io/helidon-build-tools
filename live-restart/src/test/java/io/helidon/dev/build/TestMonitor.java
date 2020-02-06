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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A build monitor used for testing.
 */
public class TestMonitor implements BuildMonitor {
    private final CountDownLatch stoppedLatch;
    private final List<String> output;
    private final int cycleCount;
    private boolean started;
    private boolean cycleStart;
    private int cycleNumber;
    private boolean changed;
    private boolean binariesOnly;
    private boolean buildStart;
    private boolean incremental;
    private Throwable buildFailed;
    private boolean ready;
    private boolean cycleEnd;
    private boolean stopped;

    public TestMonitor(boolean initialClean, int cycleCount) {
        this.stoppedLatch = new CountDownLatch(1);
        this.output = new ArrayList<>();
        this.cycleCount = cycleCount;
    }

    public Consumer<String> stdOutConsumer() {
        return line -> {
            output.add(line);
            System.out.println(line);
        };
    }

    public Consumer<String> stdErrConsumer() {
        return line -> {
            output.add(line);
            System.err.println(line);
        };
    }

    @Override
    public void onStarted() {
        started = true;
    }

    @Override
    public void onCycleStart(int cycleNumber) {
        cycleStart = true;
        this.cycleNumber = cycleNumber;
        changed = false;
        binariesOnly = false;
        buildStart = false;
        incremental = false;
        buildFailed = null;
        ready = false;
        cycleEnd = false;
    }

    @Override
    public void onChanged(boolean binariesOnly) {
        changed = true;
        this.binariesOnly = binariesOnly;
    }

    @Override
    public void onBuildStart(boolean incremental) {
        buildStart = true;
        this.incremental = incremental;
    }

    @Override
    public long onBuildFail(Throwable error) {
        buildFailed = error;
        return 0;
    }

    @Override
    public long onReady() {
        ready = true;
        return 0;
    }

    @Override
    public boolean onCycleEnd(int cycleNumber) {
        return cycleNumber < cycleCount;
    }

    @Override
    public void onStopped() {
        stopped = true;
        stoppedLatch.countDown();
    }

    boolean waitForStopped(long maxWaitSeconds) throws InterruptedException {
        return stoppedLatch.await(maxWaitSeconds, TimeUnit.SECONDS);
    }
}
