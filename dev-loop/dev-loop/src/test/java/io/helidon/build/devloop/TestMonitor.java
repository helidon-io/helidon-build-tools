/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

import io.helidon.build.common.PrintStreams;

import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;

/**
 * A build monitor used for testing.
 */
public class TestMonitor implements BuildMonitor {

    private static final String EOL = System.lineSeparator();
    private final CountDownLatch stoppedLatch;
    private final StringBuilder output;
    private final int stopCycle;
    private final PrintStream out;
    private final PrintStream err;
    private int lastCycle;
    private boolean started;
    private boolean[] cycleStart;
    private boolean[] changed;
    private ChangeType[] changeType;
    private boolean[] buildStart;
    private boolean[] buildSuccess;
    private BuildType[] buildType;
    private Throwable[] buildFailed;
    private Throwable[] loopFailed;
    private boolean[] ready;
    private boolean[] cycleEnd;
    private boolean stopped;

    public TestMonitor(int stopCycle) {
        this.stoppedLatch = new CountDownLatch(1);
        this.output = new StringBuilder();
        this.stopCycle = stopCycle;
        this.cycleStart = new boolean[stopCycle + 1];
        this.changed = new boolean[stopCycle + 1];
        this.changeType = new ChangeType[stopCycle + 1];
        this.buildStart = new boolean[stopCycle + 1];
        this.buildSuccess = new boolean[stopCycle + 1];
        this.buildType = new BuildType[stopCycle + 1];
        this.buildFailed = new Throwable[stopCycle + 1];
        this.loopFailed = new Throwable[stopCycle + 1];
        this.ready = new boolean[stopCycle + 1];
        this.cycleEnd = new boolean[stopCycle + 1];
        this.out = PrintStreams.apply(STDOUT, s -> {
            output.append(s);
            return s.charAt(0) == '[' ? "   " + s : s;
        });
        this.err = PrintStreams.apply(STDERR, s -> {
            output.append(s);
            return s.charAt(0) == '[' ? "   " + s : s;
        });
    }

    @Override
    public PrintStream stdOut() {
        return out;
    }

    @Override
    public PrintStream stdErr() {
        return err;
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
    public void onChanged(int cycleNumber, ChangeType type) {
        logCycle("onChanged", cycleNumber, "binariesOnly=" + type);
        changed[cycleNumber] = true;
        this.changeType[cycleNumber] = type;
    }

    @Override
    public void onBuildStart(int cycleNumber, BuildType type) {
        logCycle("onBuildStart", cycleNumber, "type=" + type);
        buildStart[cycleNumber] = true;
        this.buildType[cycleNumber] = type;
    }

    @Override
    public void onBuildSuccess(int cycleNumber, BuildType type) {
        logCycle("onBuildSuccess", cycleNumber, "type=" + type);
        buildSuccess[cycleNumber] = true;
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
    public NextAction onCycleEnd(int cycleNumber) {
        logCycle("onCycleEnd", cycleNumber);
        cycleEnd[cycleNumber] = true;
        return cycleNumber < stopCycle ? NextAction.CONTINUE : NextAction.EXIT;
    }

    @Override
    public void onLoopFail(int cycleNumber, Throwable error) {
        logCycle("onLoopFail", cycleNumber, error.getMessage());
        loopFailed[cycleNumber] = error;
    }

    @Override
    public void onStopped() {
        log("onStopped");
        stopped = true;
        stoppedLatch.countDown();
    }

    public String outputAsString() {
        return String.join(EOL, output);
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

    public ChangeType changeType(int cycleNumber) {
        return changeType[cycleNumber];
    }

    public boolean buildStart(int cycleNumber) {
        return buildStart[cycleNumber];
    }

    public boolean buildSuccess(int cycleNumber) {
        return buildSuccess[cycleNumber];
    }

    public BuildType buildType(int cycleNumber) {
        return buildType[cycleNumber];
    }

    public Throwable buildFailed(int cycleNumber) {
        return buildFailed[cycleNumber];
    }

    public Throwable loopFailed(int cycleNumber) {
        return loopFailed[cycleNumber];
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
        out.printf("loop: %s\n", eventName);
    }

    private void logCycle(String eventName, int cycleNumber) {
        logCycle(eventName, cycleNumber, "");
    }

    private void logCycle(String eventName, int cycleNumber, Object message) {
        lastCycle = cycleNumber;
        out.printf("loop %d: %s %s\n", cycleNumber, eventName, message);
    }
}
