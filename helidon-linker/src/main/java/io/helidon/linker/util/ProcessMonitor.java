/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker.util;

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.helidon.linker.util.Constants.EOL;
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
    private final Consumer<String> monitorOut;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;
    private final Predicate<String> filter;
    private final Function<String, String> transform;

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
        this.filter = builder.filter;
        this.transform = builder.transform;
    }

    /**
     * Executes the process and waits for completion.
     *
     * @return This instance.
     * @throws IOException If the process fails.
     * @throws InterruptedException If the a thread is interrupted.
     */
    public ProcessMonitor execute() throws IOException, InterruptedException {
        if (description != null) {
            monitorOut.accept(description);
        }
        final Process process = builder.start();
        final Future<?> out = monitor(process.getInputStream(), filter, transform, capturing ? this::captureStdOut : stdOut);
        final Future<?> err = monitor(process.getErrorStream(), filter, transform, capturing ? this::captureStdErr : stdErr);
        final int exitCode = process.waitFor();
        out.cancel(true);
        err.cancel(true);
        if (exitCode != 0) {
            final StringBuilder message = new StringBuilder();
            message.append(requireNonNullElseGet(description, () -> String.join(" ", builder.command())));
            message.append(" FAILED with exit code ").append(exitCode);
            if (capturing) {
                message.append(EOL);
                capturedOutput.forEach(line -> message.append("    ").append(line).append(EOL));
            }
            throw new IOException(message.toString());
        }
        return this;
    }

    /**
     * Returns the captured output.
     *
     * @return The output. Empty if capture not enabled.
     */
    public List<String> output() {
        return capturedOutput;
    }

    private static void devNull(String line) {
    }

    private void captureStdOut(String line) {
        stdOut.accept(line);
        synchronized (capturedOutput) {
            capturedOutput.add(line);
        }
    }

    private void captureStdErr(String line) {
        stdErr.accept(line);
        synchronized (capturedOutput) {
            capturedOutput.add(line);
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
