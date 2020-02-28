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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElseGet;

/**
 * Executes a process and waits for completion, monitoring the output.
 */
public final class ProcessMonitor {
    private static final int DESTROY_DELAY_SECONDS = 2;
    private static final int DESTROY_RETRIES = 5;
    private static final int DESTROY_FORCE_RETRY = 3;
    private static final ExecutorService EXECUTOR = ForkJoinPool.commonPool();
    private final ProcessBuilder builder;
    private final String description;
    private final boolean capturing;
    private final List<String> capturedOutput;
    private final List<String> capturedStdOut;
    private final List<String> capturedStdErr;
    private final Consumer<String> monitorOut;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;
    private final Predicate<String> filter;
    private final Function<String, String> transform;
    private volatile Process process;
    private volatile Future<?> out;
    private volatile Future<?> err;

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
        this.stdOut = builder.stdOut;
        this.stdErr = builder.stdErr;
        this.capturedOutput = capturing ? new ArrayList<>() : emptyList();
        this.capturedStdOut = capturing ? new ArrayList<>() : emptyList();
        this.capturedStdErr = capturing ? new ArrayList<>() : emptyList();
        this.filter = builder.filter;
        this.transform = builder.transform;
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
        process = builder.start();
        out = monitor(process.getInputStream(), filter, transform, capturing ? this::captureStdOut : stdOut);
        err = monitor(process.getErrorStream(), filter, transform, capturing ? this::captureStdErr : stdErr);
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
     * @throws InterruptedException If the a thread is interrupted.
     */
    @SuppressWarnings("checkstyle:JavadocMethod")
    public ProcessMonitor stop(long timeout, TimeUnit unit) throws ProcessTimeoutException,
                                                                   ProcessFailedException,
                                                                   InterruptedException {
        assertRunning();
        process.destroy();
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
            process.destroy();
        } else {
            process.destroyForcibly();
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
        cancelTasks();
        if (completed) {
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
     * Returns the combined captured output.
     *
     * @return The output. Empty if capture not enabled.
     * @throws IllegalStateException If the process was not started.
     */
    public List<String> output() {
        assertRunning();
        return capturedOutput;
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     * @throws IllegalStateException If the process was not started.
     */
    public List<String> stdOut() {
        assertRunning();
        return capturedStdOut;
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     * @throws IllegalStateException If the process was not started.
     */
    public List<String> stdErr() {
        assertRunning();
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

    private void cancelTasks() {
        if (out != null) {
            out.cancel(true);
            err.cancel(true);
            out = null;
            err = null;
        }
    }

    private void assertRunning() {
        if (process == null) {
            throw new IllegalStateException("not started");
        }
        if (out == null) {
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

    private static Future<?> monitor(InputStream input,
                                     Predicate<String> filter,
                                     Function<String, String> transform,
                                     Consumer<String> output) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        return EXECUTOR.submit(() -> reader.lines().forEach(line -> {
            if (filter.test(line)) {
                output.accept(transform.apply(line));
            }
        }));
    }
}
