/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.cli.tests;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.ProcessMonitor.ProcessFailedException;
import io.helidon.build.common.ProcessMonitor.ProcessTimeoutException;
import io.helidon.build.common.ansi.AnsiTextStyle;
import io.helidon.build.common.logging.LogLevel;

abstract class ProcessInvocation {

    static {
        LogLevel.set(LogLevel.DEBUG);
    }

    protected Path cwd;
    protected Path logDir;
    protected List<String> args = List.of();

    ProcessInvocation args(String... args) {
        this.args = Arrays.asList(args);
        return this;
    }

    ProcessInvocation cwd(Path cwd) {
        this.cwd = cwd;
        return this;
    }

    ProcessInvocation logDir(Path logDir) {
        this.logDir = logDir;
        return this;
    }

    abstract Monitor start();

    static class Recorder {
        private final StringBuffer sb = new StringBuffer();

        void record(String s) {
            sb.append(s);
        }

        String output() {
            return sb.toString();
        }
    }

    static class MonitorException extends RuntimeException {
        private final String output;

        MonitorException(String output, Exception cause) {
            super(cause);
            this.output = AnsiTextStyle.strip(output);
        }

        String output() {
            return output;
        }
    }

    static class Monitor implements AutoCloseable {
        final ProcessMonitor monitor;
        final Recorder recorder;
        final Path cwd;

        Monitor(ProcessMonitor monitor, Recorder recorder, Path cwd) {
            this.monitor = monitor;
            this.recorder = recorder;
            this.cwd = cwd;
        }

        String output() {
            return AnsiTextStyle.strip(recorder.output());
        }

        Path cwd() {
            return cwd;
        }

        @Override
        public void close() {
            monitor.stop();
        }

        boolean isAlive() {
            return monitor.isAlive();
        }

        void await() throws MonitorException {
            try {
                monitor.waitForCompletion(10, TimeUnit.MINUTES);
            } catch (ProcessTimeoutException
                     | ProcessFailedException
                     | InterruptedException e) {
                throw new MonitorException(recorder.output(), e);
            }
        }

        void await(WaitStrategy strategy) {
            strategy.await(this);
        }
    }
}
