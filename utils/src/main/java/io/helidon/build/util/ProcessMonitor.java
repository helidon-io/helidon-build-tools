/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElseGet;

/**
 * Executes a process and waits for completion, monitoring the output.
 */
public final class ProcessMonitor {
    private static final ExecutorService EXECUTOR = ForkJoinPool.commonPool();
    private final ProcessBuilder builder;
    private final String description;
    private final boolean capturing;
    private final List<String> capturedOutput;
    private final List<String> capturedStdOut;
    private final List<String> capturedStdErr;
    private final Consumer<String> monitorOut;
    private final ProcessBuilder.Redirect stdIn;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;
    private final Predicate<String> filter;
    private final Function<String, String> transform;
    private final AtomicBoolean running;
    private volatile Process process;
    private volatile MonitorTask out;
    private volatile MonitorTask err;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link ProcessMonitor}.
     */
    public static final class Builder {
        private ProcessBuilder builder;
        private String description;
        private boolean capture;
        private Consumer<String> monitorOut;
        private ProcessBuilder.Redirect stdIn;
        private Consumer<String> stdOut;
        private Consumer<String> stdErr;
        private Predicate<String> filter;
        private Function<String, String> transform;

        private Builder() {
        }

        /**
         * Sets the process builder.
         *
         * @param processBuilder The process builder.
         * @return This builder.
         */
        public Builder processBuilder(ProcessBuilder processBuilder) {
            this.builder = processBuilder;
            return this;
        }

        /**
         * Sets the process description.
         *
         * @param description The description.
         * @return This builder.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets whether or not to capture output.
         *
         * @param capture {@code true} if output should be capturee.
         * @return This builder.
         */
        public Builder capture(boolean capture) {
            this.capture = capture;
            return this;
        }

        /**
         * Sets the input for process.
         *
         * @param stdIn The input.
         * @return This builder.
         */
        public Builder stdIn(File stdIn) {
            if (stdIn != null) {
                return stdIn(ProcessBuilder.Redirect.from(stdIn));
            } else {
                return this;
            }
        }

        /**
         * Sets the input for process.
         *
         * @param stdIn The input.
         * @return This builder.
         */
        public Builder stdIn(ProcessBuilder.Redirect stdIn) {
            this.stdIn = stdIn;
            return this;
        }

        /**
         * Sets the consumer for process {@code stdout} stream.
         *
         * @param stdOut The description.
         * @return This builder.
         */
        public Builder stdOut(Consumer<String> stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        /**
         * Sets the consumer for process {@code stderr} stream.
         *
         * @param stdErr The description.
         * @return This builder.
         */
        public Builder stdErr(Consumer<String> stdErr) {
            this.stdErr = stdErr;
            return this;
        }

        /**
         * Sets a filter for all process output.
         *
         * @param filter The filter.
         * @return This builder.
         */
        public Builder filter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets a transformer for all process output.
         *
         * @param transform The transformer.
         * @return This builder.
         */
        public Builder transform(Function<String, String> transform) {
            this.transform = transform;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return The instance.
         */
        public ProcessMonitor build() {
            if (builder == null) {
                throw new IllegalStateException("processBuilder required");
            }
            monitorOut = stdOut;
            if (stdOut == null) {
                capture = true;
                stdOut = ProcessMonitor::devNull;
                monitorOut = Log::info;
            }
            if (stdErr == null) {
                capture = true;
                stdErr = ProcessMonitor::devNull;
            }
            if (filter == null) {
                filter = line -> true;
            }
            if (transform == null) {
                transform = Function.identity();
            }
            return new ProcessMonitor(this);
        }
    }

    private ProcessMonitor(Builder builder) {
        this.builder = builder.builder;
        this.description = builder.description;
        this.capturing = builder.capture;
        this.monitorOut = builder.monitorOut;
        this.stdIn = builder.stdIn;
        this.stdOut = builder.stdOut;
        this.stdErr = builder.stdErr;
        this.capturedOutput = capturing ? new ArrayList<>() : emptyList();
        this.capturedStdOut = capturing ? new ArrayList<>() : emptyList();
        this.capturedStdErr = capturing ? new ArrayList<>() : emptyList();
        this.filter = builder.filter;
        this.transform = builder.transform;
        this.running = new AtomicBoolean();
    }

    /**
     * Starts the process and waits for completion.
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the {@code timeout} argument.
     * @return This instance.
     * @throws IOException If an I/O error occurs.
     * @throws ProcessTimeoutException If the process does not complete in the specified time.
     * @throws ProcessFailedException If the process fails.
     * @throws InterruptedException If the a thread is interrupted.
     */
    @SuppressWarnings({"checkstyle:JavadocMethod", "checkstyle:ThrowsCount"})
    public ProcessMonitor execute(long timeout, TimeUnit unit) throws IOException,
            ProcessTimeoutException,
            ProcessFailedException,
            InterruptedException {
        return start().waitForCompletion(timeout, unit);
    }

    /**
     * Starts the process.
     *
     * @return This instance.
     * @throws IllegalStateException If the process was already started.
     * @throws IOException If an I/O error occurs.
     */
    public ProcessMonitor start() throws IOException {
        if (process != null) {
            throw new IllegalStateException("already started");
        }
        if (description != null) {
            monitorOut.accept(description);
        }
        if (stdIn != null) {
            builder.redirectInput(stdIn);
        }
        process = builder.start();
        running.set(true);
        out = monitor(process.getInputStream(), filter, transform, capturing ? this::captureStdOut : stdOut, running);
        err = monitor(process.getErrorStream(), filter, transform, capturing ? this::captureStdErr : stdErr, running);
        return this;
    }

    /**
     * Returns the process handle.
     *
     * @return The handle.
     * @throws IllegalStateException If the process was not started.
     */
    public ProcessHandle toHandle() {
        return process().toHandle();
    }

    /**
     * Stops the process and waits for it to exit.
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the {@code timeout} argument.
     * @return This instance.
     * @throws IllegalStateException If the process was not started or has already been completed.
     * @throws ProcessTimeoutException If the process does not complete in the specified time.
     * @throws ProcessFailedException If the process fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    @SuppressWarnings("checkstyle:JavadocMethod")
    public ProcessMonitor stop(long timeout, TimeUnit unit) throws ProcessTimeoutException,
            ProcessFailedException,
            InterruptedException {
        assertRunning();
        process.destroy();
        running.set(false);
        return waitForCompletion(timeout, unit);
    }

    /**
     * Stops the process and does not wait for it to exit.
     *
     * @param force {@code true} if the process should not be allowed to exit normally.
     * @return This instance.
     */
    public ProcessMonitor destroy(boolean force) {
        assertRunning();
        if (force) {
            process.destroyForcibly();
        } else {
            process.destroy();
        }
        cancelTasks();
        return this;
    }

    /**
     * Waits for the process to complete. If the process does not complete in the given time {@code destroy(false)} is called
     * and a {@link ProcessTimeoutException} thrown.
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the {@code timeout} argument.
     * @return This instance.
     * @throws IllegalStateException If the process was not started or has already been completed.
     * @throws ProcessTimeoutException If the process does not complete in the specified time.
     * @throws ProcessFailedException If the process fails.
     * @throws InterruptedException If the a thread is interrupted.
     */
    @SuppressWarnings("checkstyle:JavadocMethod")
    public ProcessMonitor waitForCompletion(long timeout, TimeUnit unit) throws ProcessTimeoutException,
            ProcessFailedException,
            InterruptedException {
        assertRunning();
        final boolean completed = process.waitFor(timeout, unit);
        if (completed) {
            stopTasks();
            if (process.exitValue() != 0) {
                throw new ProcessFailedException(this);
            }
            return this;
        } else {
            destroy(false);
            throw new ProcessTimeoutException(this);
        }
    }

    /**
     * Tests whether or not the proces is alive.
     *
     * @return {@code true} if the process is alive.
     */
    public boolean isAlive() {
        Process process = this.process;
        if (process == null) {
            return false;
        } else {
            return process.isAlive();
        }
    }

    /**
     * Returns the combined captured output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public List<String> output() {
        return capturedOutput;
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public List<String> stdOut() {
        return capturedStdOut;
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public List<String> stdErr() {
        return capturedStdErr;
    }

    /**
     * Process exception.
     */
    public static class ProcessException extends Exception {
        private final ProcessMonitor monitor;
        private final boolean timeout;

        private ProcessException(ProcessMonitor monitor, boolean timeout) {
            this.monitor = monitor;
            this.timeout = timeout;
        }

        /**
         * Returns the process monitor.
         *
         * @return The monitor.
         */
        public ProcessMonitor monitor() {
            return monitor;
        }

        @Override
        public String getMessage() {
            return monitor().toErrorMessage(timeout);
        }
    }

    /**
     * Process timeout exception.
     */
    public static final class ProcessTimeoutException extends ProcessException {
        private ProcessTimeoutException(ProcessMonitor monitor) {
            super(monitor, true);
        }
    }

    /**
     * Process failed exception.
     */
    public static final class ProcessFailedException extends ProcessException {
        private int exitCode;

        private ProcessFailedException(ProcessMonitor monitor) {
            super(monitor, false);
            this.exitCode = monitor.process.exitValue();
        }

        /**
         * Returns the exit code.
         *
         * @return The code.
         */
        public int exitCode() {
            return exitCode;
        }
    }

    private Process process() {
        assertRunning();
        return process;
    }

    private void stopTasks() {
        if (out != null) {
            running.set(false);
            out.join();
            err.join();
            out = null;
            err = null;
        }
    }

    private void cancelTasks() {
        if (out != null) {
            out.cancel();
            err.cancel();
            out = null;
            err = null;
        }
    }

    private Process assertStarted() {
        final Process process = this.process;
        if (process == null) {
            throw new IllegalStateException("not started");
        }
        return process;
    }

    private void assertRunning() {
        final Process process = assertStarted();
        if (!process.isAlive()) {
            throw new IllegalStateException("already completed");
        }
    }

    private String toErrorMessage(boolean timeout) {
        final StringBuilder message = new StringBuilder();
        message.append(describe());
        if (timeout) {
            message.append(" timed out");
        } else {
            message.append(" failed with exit code ").append(process.exitValue());
        }
        if (capturing) {
            message.append(Constants.EOL);
            capturedOutput.forEach(line -> message.append("    ").append(line).append(Constants.EOL));
        }
        return message.toString();
    }

    private String describe() {
        return requireNonNullElseGet(description, () -> String.join(" ", builder.command()));
    }

    private static void devNull(String line) {
    }

    private void captureStdOut(String line) {
        stdOut.accept(line);
        synchronized (capturedOutput) {
            capturedOutput.add(line);
            capturedStdOut.add(line);
        }
    }

    private void captureStdErr(String line) {
        stdErr.accept(line);
        synchronized (capturedOutput) {
            capturedOutput.add(line);
            capturedStdErr.add(line);
        }
    }

    private static final class MonitorTask {

        private final MonitorReader reader;
        private final Future<?> task;

        MonitorTask(MonitorReader reader, Future<?> task) {
            this.reader = reader;
            this.task = task;
        }

        void join() {
            try {
                task.get();
            } catch (Exception ignore) {
            } finally {
                close();
            }
        }

        void cancel() {
            try {
                task.cancel(true);
            } catch (Exception ignore) {
            } finally {
                close();
            }
        }

        private void close() {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    private static final class MonitorReader extends BufferedReader {

        private final Predicate<String> filter;
        private final Function<String, String> transform;
        private final Consumer<String> output;

        MonitorReader(InputStream input,
                      Predicate<String> filter,
                      Function<String, String> transform,
                      Consumer<String> output) {

            super(new InputStreamReader(input, StandardCharsets.UTF_8));
            this.filter = filter;
            this.transform = transform;
            this.output = output;
        }

        void consumeLines() {
            lines().forEach(line -> {
                if (filter.test(line)) {
                    output.accept(transform.apply(line));
                }
            });
        }

        @Override
        public void close() throws IOException {
            try {
                consumeLines();
            } catch (Exception ignore) {
            } finally {
                super.close();
            }
        }
    }

    private static MonitorTask monitor(InputStream input,
                                       Predicate<String> filter,
                                       Function<String, String> transform,
                                       Consumer<String> output,
                                       AtomicBoolean running) {

        final MonitorReader reader = new MonitorReader(input, filter, transform, output);
        Future<?> task = EXECUTOR.submit(() -> {
            while (running.get()) {
                reader.consumeLines();
            }
        });
        return new MonitorTask(reader, task);
    }
}
