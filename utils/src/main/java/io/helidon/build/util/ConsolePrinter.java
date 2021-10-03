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

import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.StyleFunction.Red;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Console printer.
 * The contract of this interface is similar to a {@link java.io.PrintStream} with manual flushing.
 */
public interface ConsolePrinter {

    /**
     * Console printer for {@code stdout}.
     */
    ConsolePrinter STDOUT = new FastPrinter(FileDescriptor.out);

    /**
     * Console printer for {@code stderr}.
     */
    ConsolePrinter STDERR = new FastPrinter(FileDescriptor.err);

    /**
     * Console printer for {@code stdout} colored in red.
     */
    ConsolePrinter RED_STDERR = STDERR.delegate((printer, str) -> printer.print(Red.apply(str)));

    /**
     * Console printer that ignores everything.
     */
    ConsolePrinter DEVNULL = new FunctionalPrinter(s -> {});

    /**
     * Create a new console printer from a function.
     * <b>IMPORTANT:</b> function MUST auto flush.
     *
     * @param consumer function to implement {@link #print(String)}
     * @return created console printer
     */
    static ConsolePrinter create(Consumer<String> consumer) {
        return new FunctionalPrinter(consumer);
    }

    /**
     * Print the given string.
     *
     * @param str the string to print
     */
    void print(String str);

    /**
     * Print an empty line.
     */
    default void println() {
        print(EOL);
    }

    /**
     * Print the given line.
     *
     * @param str line to print
     */
    default void println(String str) {
        print(str + EOL);
    }

    /**
     * Print an empty line and flush.
     */
    default void println2() {
        print(EOL);
        flush();
    }

    /**
     * Print the given line and flush.
     *
     * @param str line to print
     */
    default void println2(String str) {
        print(str + EOL);
        flush();
    }

    /**
     * Print a formatted string.
     *
     * @param format format string
     * @param args   format arguments
     */
    default void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    /**
     * Flush the underlying buffer.
     */
    default void flush() {
    }

    /**
     * Create a new delegate.
     *
     * @param delegateFunc bi-function to handle the delegation to {@link #print(String)}.
     * @return the created delegate
     */
    default ConsolePrinter delegate(BiConsumer<ConsolePrinter, String> delegateFunc) {
        return new DelegatePrinter(this, delegateFunc);
    }

    /**
     * Printer that implements {@link #print(String)} using a {@link Consumer}.
     */
    final class FunctionalPrinter implements ConsolePrinter {

        private final Consumer<String> consumer;

        private FunctionalPrinter(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void print(String str) {
            consumer.accept(str);
        }
    }

    /**
     * Delegating printer that implements {@link #print(String)} using a {@link BiConsumer}.
     */
    class DelegatePrinter implements ConsolePrinter {

        private final ConsolePrinter delegate;
        private final BiConsumer<ConsolePrinter, String> callback;

        private DelegatePrinter(ConsolePrinter delegate, BiConsumer<ConsolePrinter, String> callback) {
            this.delegate = delegate;
            this.callback = callback;
        }

        @Override
        public void print(String str) {
            callback.accept(delegate, str);
        }

        @Override
        public void println(String str) {
            delegate.println(str);
        }

        @Override
        public void printf(String format, Object... args) {
            delegate.printf(format, args);
        }

        @Override
        public void flush() {
            delegate.flush();
        }
    }

    /**
     * Printer implementation for standard descriptors.
     * This implementation does not use the standard {@link java.io.PrintStream} from the {@link System} class and
     * instead writes directly to the given {@link FileDescriptor} using {@code ASCII} encoding.
     */
    final class FastPrinter implements ConsolePrinter {

        private static final int BUFFER_SIZE = 512;

        private final BufferedWriter writer;
        private volatile boolean flush = false;

        private FastPrinter(FileDescriptor desc) {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(desc), US_ASCII);
            this.writer = new BufferedWriter(osw, BUFFER_SIZE);
        }

        @Override
        public void print(String str) {
            try {
                writer.write(str);
                flush = true;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public void flush() {
            if (flush) {
                try {
                    writer.flush();
                    flush = false;
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }
    }
}
