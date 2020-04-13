/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.helidon.build.util.Log;

import static io.helidon.build.util.Constants.EOL;

/**
 * A build monitor used for testing.
 */
public class TestMonitor implements BuildMonitor {
    // Use copies here in case we use embedded maven and they get reset.
    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;
    private final CountDownLatch stoppedLatch;
    private final List<String> output;
    private final int stopCycle;
    private int lastCycle;
    private boolean started;
    private boolean[] cycleStart;
    private boolean[] changed;
    private boolean[] binariesOnly;
    private boolean[] buildStart;
    private BuildType[] buildType;
    private Throwable[] buildFailed;
    private boolean[] ready;
    private boolean[] cycleEnd;
    private boolean stopped;

    public TestMonitor(int stopCycle) {
        this.stoppedLatch = new CountDownLatch(1);
        this.output = new ArrayList<>();
        this.stopCycle = stopCycle;
        this.cycleStart = new boolean[stopCycle + 1];
        this.changed = new boolean[stopCycle + 1];
        this.binariesOnly = new boolean[stopCycle + 1];
        this.buildStart = new boolean[stopCycle + 1];
        this.buildType = new BuildType[stopCycle + 1];
        this.buildFailed = new Throwable[stopCycle + 1];
        this.ready = new boolean[stopCycle + 1];
        this.cycleEnd = new boolean[stopCycle + 1];
    }

    public Consumer<String> stdOutConsumer() {
        return line -> {
            output.add(line);
            OUT.println(line.charAt(0) == '[' ? "   " + line : line);
        };
    }

    public Consumer<String> stdErrConsumer() {
        return line -> {
            output.add(line);
            ERR.println(line.charAt(0) == '[' ? "   " + line : line);
        };
    }

    @Override
    public void onStarted() {
        log("onStarted");
        started = true;
    }

    @Override
    public void onCycleStart(int cycleNumber) {
        logCycle("onCycleStart", cycleNumber);
        cycleStart[cycleNumber] = true;
    }

    @Override
    public void onChanged(int cycleNumber, boolean binariesOnly) {
        logCycle("onChanged", cycleNumber, "binariesOnly=" + binariesOnly);
        changed[cycleNumber] = true;
        this.binariesOnly[cycleNumber] = binariesOnly;
    }

    @Override
    public void onBuildStart(int cycleNumber, BuildType type) {
        logCycle("onBuildStart", cycleNumber, "type=" + type);
        buildStart[cycleNumber] = true;
        this.buildType[cycleNumber] = type;
    }

    @Override
    public long onBuildFail(int cycleNumber, BuildType type, Throwable error) {
        logCycle("onBuildFail", cycleNumber, error);
        buildFailed[cycleNumber] = error;
        return 0;
    }

    @Override
    public long onReady(int cycleNumber, Project project) {
        logCycle("onReady", cycleNumber);
        ready[cycleNumber] = true;
        return 0;
    }

    @Override
    public boolean onCycleEnd(int cycleNumber) {
        logCycle("onCycleEnd", cycleNumber);
        cycleEnd[cycleNumber] = true;
        return cycleNumber < stopCycle;
    }

    @Override
    public void onStopped() {
        log("onStopped");
        stopped = true;
        stoppedLatch.countDown();
    }

    public List<String> output() {
        return output;
    }

    public String outputAsString() {
        return String.join(EOL, output());
    }

    public int lastCycle() {
        return lastCycle;
    }

    public boolean started() {
        return started;
    }

    public boolean cycleStart(int cycleNumber) {
        return cycleStart[cycleNumber];
    }

    public boolean changed(int cycleNumber) {
        return changed[cycleNumber];
    }

    public boolean binariesOnly(int cycleNumber) {
        return binariesOnly[cycleNumber];
    }

    public boolean buildStart(int cycleNumber) {
        return buildStart[cycleNumber];
    }

    public BuildType buildType(int cycleNumber) {
        return buildType[cycleNumber];
    }

    public Throwable buildFailed(int cycleNumber) {
        return buildFailed[cycleNumber];
    }

    public boolean ready(int cycleNumber) {
        return ready[cycleNumber];
    }

    public boolean cycleEnd(int cycleNumber) {
        return cycleEnd[cycleNumber];
    }

    public boolean stopped() {
        return stopped;
    }

    private void log(String eventName) {
        Log.info("loop: %s", eventName);
    }

    private void logCycle(String eventName, int cycleNumber) {
        logCycle(eventName, cycleNumber, "");
    }

    private void logCycle(String eventName, int cycleNumber, Object message) {
        lastCycle = cycleNumber;
        Log.info("loop %d: %s %s", cycleNumber, eventName, message);
    }
}
