/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.PrintStreams.DEVNULL;
import static java.util.Objects.requireNonNullElseGet;

/**
 * Executes a process and waits for completion, monitoring the output.
 */
public final class ProcessMonitor {

    private static final String EOL = System.getProperty("line.separator");
    private static final int GRACEFUL_STOP_TIMEOUT = 3;
    private static final int FORCEFUL_STOP_TIMEOUT = 2;
    private static final MonitorThread MONITOR_THREAD = new MonitorThread();

    private final ProcessBuilder builder;
    private final String description;
    private final Consumer<String> monitorOut;
    private final ProcessBuilder.Redirect stdIn;
    private final ConsoleRecorder recorder;
    private final boolean capturing;
    private final CompletableFuture<Void> exitFuture;
    private final AtomicBoolean shutdown;
    private final Runnable beforeShutdown;
    private final Runnable afterShutdown;
    private volatile Process process;

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
        private PrintStream stdOut;
        private PrintStream stdErr;
        private Predicate<String> filter = line -> true;
        private Function<String, String> transform = Function.identity();
        private Runnable beforeShutdown = () -> {};
        private Runnable afterShutdown = () -> {};

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
         * Sets whether to capture output.
         *
         * @param capture {@code true} if output should be captured.
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
         * Sets the print stream for process {@code stdout}.
         *
         * @param stdOut The handler.
         * @return This builder.
         */
        public Builder stdOut(PrintStream stdOut) {
            this.stdOut = stdOut;
            this.monitorOut = stdOut::println;
            return this;
        }

        /**
         * Sets the print stream for process {@code stderr}.
         *
         * @param stdErr The handler.
         * @return This builder.
         */
        public Builder stdErr(PrintStream stdErr) {
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
         * Sets the before shutdown callback.
         *
         * @param beforeShutdown a callback invoked before the process is stopped by the shutdown hook
         * @return This builder.
         */
        public Builder beforeShutdown(Runnable beforeShutdown) {
            this.beforeShutdown = beforeShutdown;
            return this;
        }

        /**
         * Sets the after shutdown callback.
         *
         * @param afterShutdown a callback invoked after the process is stopped by the shutdown hook
         * @return This builder.
         */
        public Builder afterShutdown(Runnable afterShutdown) {
            this.afterShutdown = afterShutdown;
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
            if (stdOut == null) {
                capture = true;
                stdOut = DEVNULL;
                monitorOut = Log::info;
            } else {
                monitorOut = stdOut::println;
            }
            if (stdErr == null) {
                capture = true;
                stdErr = DEVNULL;
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
        this.recorder = new ConsoleRecorder(
                builder.stdOut,
                builder.stdErr,
                builder.filter,
                builder.transform,
                builder.capture);
        this.shutdown = new AtomicBoolean();
        this.beforeShutdown = builder.beforeShutdown;
        this.afterShutdown = builder.afterShutdown;
        this.exitFuture = new CompletableFuture<>();
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
     * @throws InterruptedException If the thread is interrupted.
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
        Log.debug("Executing command: %s", String.join(" ", builder.command()));
        process = builder.start();
        recorder.start(process.getInputStream(), process.getErrorStream());
        Log.debug("Process ID: %d", process.pid());
        MONITOR_THREAD.register(this);
        return this;
    }

    /**
     * Stops the process gracefully.
     *
     * @return This instance.
     * @throws IllegalStateException If the process did not exit after all the attempts
     */
    public ProcessMonitor stop() {
        long pid = process.toHandle().pid();
        process.destroy();
        try {
            try {
                exitFuture.get(GRACEFUL_STOP_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                process.destroyForcibly();
                try {
                    exitFuture.get(FORCEFUL_STOP_TIMEOUT, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    throw new IllegalStateException(String.format(
                            "Failed to stop process %d: %s", pid, "timeout expired"));
                }
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(String.format(
                    "Failed to stop process %d: %s", pid, e.getMessage()));
        } catch (ExecutionException e) {
            throw new IllegalStateException(String.format(
                    "Failed to stop process %d: %s", pid, e.getCause().getMessage()));
        }
        return this;
    }

    /**
     * Waits for the process to complete.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The time unit of the {@code timeout} argument.
     * @return This instance.
     * @throws IllegalStateException   If the process was not started, or if there was an unknown error while waiting
     * @throws ProcessTimeoutException If the process does not complete in the specified time.
     * @throws ProcessFailedException  If the process fails.
     * @throws InterruptedException    If the thread is interrupted.
     */
    public ProcessMonitor waitForCompletion(long timeout, TimeUnit unit)
            throws ProcessTimeoutException, ProcessFailedException, InterruptedException {

        if (process == null) {
            throw new IllegalStateException("not started");
        }
        if (process.isAlive()) {
            Log.debug("Waiting for completion, pid=%d, timeout=%d, unit=%s", process.pid(), timeout, unit);
            try {
                exitFuture.get(timeout, unit);
                try {
                    // ignore exit code if this is a shutdown
                    if (process.exitValue() != 0 && !shutdown.get()) {
                        throw new ProcessFailedException();
                    }
                } catch (IllegalThreadStateException ex) {
                    throw new ProcessFailedException();
                }
            } catch (ExecutionException ex) {
                throw new IllegalStateException(ex);
            } catch (TimeoutException e) {
                throw new ProcessTimeoutException();
            }
        }
        return this;
    }

    /**
     * Tests whether the process is alive.
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
    public String output() {
        return recorder.capturedOutput();
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public String stdOut() {
        return recorder.capturedStdOut();
    }

    /**
     * Returns any captured stderr output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public String stdErr() {
        return recorder.capturedStdErr();
    }

    /**
     * A thread that monitors all started processes.
     * <ul>
     *     <li>It consumes the output of all started processes (one thread handles all processes)</li>
     *     <li>Implements a shutdown hook to stop any running forked process gracefully</li>
     *     <li>Completes {@link #exitFuture} to ensure that the output of forked processes is drained</li>
     *     <li>Implements a backoff to avoid using too much CPU</li>
     * </ul>
     */
    private static final class MonitorThread extends Thread {

        private final List<ProcessMonitor> processes = new ArrayList<>();
        private int backoff = 0;
        private Iterator<ProcessMonitor> iterator;

        private MonitorThread() {
            start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }

        /**
         * Register a new process to be monitored.
         *
         * @param process process to monitor
         */
        void register(ProcessMonitor process) {
            // guard concurrent registration
            synchronized (processes) {
                processes.add(process);
                if (processes.size() == 1) {
                    // unblock the monitor thread
                    LockSupport.unpark(this);
                }
            }
        }

        @Override
        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                if (processes.isEmpty()) {
                    // wait for processes
                    LockSupport.park();
                }

                iterator = processes.iterator();
                boolean ticked = true;
                while (iterator.hasNext()) {
                    if (!tick(iterator.next())) {
                        ticked = false;
                    }
                }

                if (!processes.isEmpty()) {
                    // sleep to avoid consuming cpu
                    backoff = ticked ? 0 : backoff < 5 ? backoff + 1 : backoff;
                    try {
                        //noinspection BusyWait
                        Thread.sleep((50L / processes.size()) * backoff);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }

        private void shutdown() {
            // use a copy since the monitor thread will react to the stop operation
            // and remove processes from the list
            CompletableFuture<Void> exitFuture = CompletableFuture.allOf(
                    new ArrayList<>(processes)
                            .stream()
                            .map(p -> {
                                p.recorder.stop();
                                p.beforeShutdown.run();
                                p.shutdown.set(true);
                                p.process.destroy();
                                return p.exitFuture.thenRun(p.afterShutdown);
                            })
                            .toArray(CompletableFuture[]::new));
            try {
                exitFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // ignored
            }
        }

        private boolean tick(ProcessMonitor process) {
            try {
                return process.recorder.tick();
            } catch (Throwable ex) {
                // pretend no work was done to maybe add a backoff
                return false;
            } finally {
                if (!process.isAlive()) {
                    process.recorder.drain();
                    process.exitFuture.complete(null);
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Process exception.
     */
    public abstract class ProcessException extends Exception {

        private final String reason;

        private ProcessException(String reason) {
            this.reason = reason;
        }

        @Override
        public String getMessage() {
            final StringBuilder message = new StringBuilder()
                    .append(requireNonNullElseGet(description, () -> String.join(" ", builder.command())))
                    .append(" ")
                    .append(reason);
            if (capturing) {
                message.append(EOL);
                for (String line : output().split("\\R")) {
                    message.append("    ").append(line).append(EOL);
                }
            }
            return message.toString();
        }

        /**
         * Returns the process monitor.
         *
         * @return The monitor.
         */
        public ProcessMonitor monitor() {
            return ProcessMonitor.this;
        }
    }

    /**
     * Process timeout exception.
     */
    public final class ProcessTimeoutException extends ProcessException {

        private ProcessTimeoutException() {
            super("timed out");
        }
    }

    /**
     * Process failed exception.
     */
    public final class ProcessFailedException extends ProcessException {

        private ProcessFailedException() {
            super(" failed with exit code " + process.exitValue());
        }
    }
}
