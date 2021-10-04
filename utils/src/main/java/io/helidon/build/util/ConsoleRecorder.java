/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.helidon.build.util.Constants.EOL;

/**
 * Utility class to process and capture the outputs of a forked process.
 */
final class ConsoleRecorder {

    private final StringBuilder capturedOutput = new StringBuilder();
    private final StringBuilder capturedStdOut = new StringBuilder();
    private final StringBuilder capturedStdErr = new StringBuilder();
    private final Predicate<String> filter;
    private final Function<String, String> transform;
    private final boolean capturing;
    private final ConsolePrinter stdOutPrinter;
    private final ConsolePrinter stdErrPrinter;
    private LineReader stdOutReader;
    private LineReader stdErrReader;

    /**
     * Create a new output forwarder.
     *
     * @param stdOutPrinter console printer for {@code stdout}
     * @param stdErrPrinter console printer for {@code stderr}
     * @param filter        predicate to filter the lines to print
     * @param transform     function to transform the lines to print
     * @param recording     {@code true} if the output should be captured
     */
    ConsoleRecorder(ConsolePrinter stdOutPrinter,
                    ConsolePrinter stdErrPrinter,
                    Predicate<String> filter,
                    Function<String, String> transform,
                    boolean recording) {

        this.filter = filter;
        this.transform = transform;
        this.capturing = recording;
        this.stdOutPrinter = stdOutPrinter.delegate((printer, str) -> print(printer, str, capturedStdOut));
        this.stdErrPrinter = stdErrPrinter.delegate((printer, str) -> print(printer, str, capturedStdErr));
    }

    /**
     * Start forwarding output with the given streams.
     *
     * @param stdOutStream input stream for {@code stdout}
     * @param stdErrStream input stream for {@code stderr}
     * @throws IllegalStateException if the forwarded was already started
     */
    void start(InputStream stdOutStream, InputStream stdErrStream) {
        if (stdOutReader != null || stdErrReader != null) {
            throw new IllegalStateException("Already started");
        }
        stdOutReader = new LineReader(stdOutStream, stdOutPrinter::print, stdOutPrinter::flush);
        stdErrReader = new LineReader(stdErrStream, stdErrPrinter::print, stdErrPrinter::flush);
    }

    /**
     * Stop forwarding output.
     */
    void stop() {
        stdOutReader = null;
        stdErrReader = null;
    }

    /**
     * Process the data available in the underlying buffers.
     *
     * @return {@code true} if data was processed, {@code false} otherwise
     * @throws IOException if an IO error occurs
     */
    boolean tick() throws IOException {
        boolean ticked = false;
        if (stdOutReader != null && stdOutReader.tick()) {
            ticked = true;
        }
        if (stdErrReader != null && stdErrReader.tick()) {
            ticked = true;
        }
        return ticked;
    }

    /**
     * Get the captured output (both {@code stdout}  {@code stderr}).
     *
     * @return output, or empty string if nothing was captured
     */
    String capturedOutput() {
        return capturedOutput.toString();
    }

    /**
     * Get the captured output for {@code stdout}.
     *
     * @return output, or empty string if nothing was captured
     */
    String capturedStdOut() {
        return capturedStdOut.toString();
    }

    /**
     * Get the captured output for {@code stderr}.
     *
     * @return output, or empty string if nothing was captured
     */
    String capturedStdErr() {
        return capturedStdErr.toString();
    }

    private void print(ConsolePrinter printer, String str, StringBuilder capture) {
        String line = str + EOL;
        if (filter.test(str)) {
            printer.print(transform.apply(line));
        }
        if (this.capturing) {
            synchronized (capturedOutput) {
                capturedOutput.append(line);
                capture.append(line);
            }
        }
    }

    /**
     * Consume all that is available in the underlying buffers, including the non terminated lines.
     */
    void drain() {
        if (stdOutReader != null) {
            stdOutReader.drain();
        }
        if (stdErrReader != null) {
            stdErrReader.drain();
        }
    }
}
