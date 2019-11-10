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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Executes a process and waits for completion, monitoring the output.
 */
public class ProcessMonitor {
    private static final String EOL = System.getProperty("line.separator");
    private static final ExecutorService EXECUTOR = ForkJoinPool.commonPool();
    private final ProcessBuilder builder;
    private final String description;
    private final boolean capturing;
    private final List<String> capturedOutput;
    private final Consumer<String> monitorOut;
    private final Consumer<String> stdOut;
    private final Consumer<String> stdErr;

    /**
     * Returns a new monitor for the given {@link ProcessBuilder}. If {@code logOutput} is {@code false}, output is captured
     * and included in the exception message if the process returns a non-zero exit code.
     *
     * @param description A description of the process.
     * @param builder The builder, which must be ready to start.
     * @param logOutput {@code true} if process output should be logged..
     * included in the exception message if the process returns a non-zero exit code.
     * @return The monitor.
     */
    public static ProcessMonitor newMonitor(String description, ProcessBuilder builder, boolean logOutput) {
        if (logOutput) {
            return newMonitor(description, builder, Log::info, Log::warn);
        } else {
            return newMonitor(description, builder, null, null);
        }
    }

    /**
     * Returns a new monitor for the given {@link ProcessBuilder}. If both {@code stdOut} and {@code stdErr} are
     * {@code null}, output is captured and included in the exception message if the process returns a non-zero exit code.
     *
     * @param description A description of the process.
     * @param builder The builder, which must be ready to start.
     * @param stdOut A consumer for the process output stream. Output is captured if {@code null}.
     * @param stdErr A consumer for the process error stream. Output is captured if {@code null}.
     * @return The monitor.
     */
    public static ProcessMonitor newMonitor(String description,
                                            ProcessBuilder builder,
                                            Consumer<String> stdOut,
                                            Consumer<String> stdErr) {
        return new ProcessMonitor(builder, description, stdOut, stdErr);
    }

    private ProcessMonitor(ProcessBuilder builder, String description, Consumer<String> stdOut, Consumer<String> stdErr) {
        this.builder = requireNonNull(builder);
        this.description = requireNonNull(description);
        if (stdOut == null && stdErr == null) {
            this.monitorOut = Log::info;
            this.capturing = true;
            this.stdOut = null;
            this.stdErr = null;
            this.capturedOutput = new ArrayList<>();
        } else if (stdOut != null && stdErr != null) {
            this.monitorOut = stdOut;
            this.capturing = false;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
            this.capturedOutput = emptyList();
        } else {
            throw new IllegalArgumentException("stdOut and stdErr must both be valid or both be null");
        }
    }

    /**
     * Starts the process and waits for completion.
     *
     * @throws IOException If the process fails.
     */
    public void run() throws IOException, InterruptedException {
        monitorOut.accept(capturing ? description : (description + EOL));
        final Process process = builder.start();
        final Future out = monitor(process.getInputStream(), stdOut);
        final Future err = monitor(process.getErrorStream(), stdErr);
        int exitCode = process.waitFor();
        out.cancel(true);
        err.cancel(true);
        if (exitCode != 0) {
            final StringBuilder message = new StringBuilder();
            message.append(description).append(" failed with exit code ").append(exitCode);
            if (capturing) {
                message.append(EOL);
                capturedOutput.forEach(line -> message.append("    ").append(line).append(EOL));
            }
            throw new IOException(message.toString());
        }
    }

    private static Future monitor(InputStream input, Consumer<String> output) {
        return EXECUTOR.submit(() -> new BufferedReader(new InputStreamReader(input)).lines().forEach(output));
    }
}
