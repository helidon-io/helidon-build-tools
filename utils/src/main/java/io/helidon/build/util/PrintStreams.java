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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.helidon.build.util.Constants.EOL;
import static io.helidon.build.util.StyleFunction.Red;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * {@link PrintStreams} utility.
 */
public final class PrintStreams {

    private PrintStreams() {
        // cannot be instantiated
    }

    private static final String FAST_STREAMS_PROP = "io.helidon.build.util.fast.streams";
    private static final boolean FAST_STREAMS = Boolean.parseBoolean(System.getProperty(FAST_STREAMS_PROP, "true"));

    /**
     * {@code stdout}.
     */
    public static final PrintStream STDOUT = FAST_STREAMS ? new FastPrintStream(FileDescriptor.out) : System.out;

    /**
     * {@code stderr}.
     */
    public static final PrintStream STDERR = FAST_STREAMS ? new FastPrintStream(FileDescriptor.err) : System.err;

    /**
     * {@code stdout} colored in red.
     */
    public static final PrintStream RED_STDERR = new TransformedPrintStream(STDERR, Red::apply);

    /**
     * Ignores everything.
     */
    public static final PrintStream DEVNULL = new PrintStream(OutputStream.nullOutputStream());

    /**
     * Create a new delegate print stream with auto-flush.
     *
     * @param delegate print stream
     * @return created print stream
     */
    public static PrintStream autoFlush(PrintStream delegate) {
        return delegate(delegate, (p, s) -> {
            p.print(s);
            p.flush();
        });
    }

    /**
     * Create a new delegate print stream.
     *
     * @param delegate the delegated print stream
     * @param function function used to implement {@link PrintStream#print(String)}.
     * @return created print stream
     */
    public static PrintStream delegate(PrintStream delegate, BiConsumer<PrintStream, String> function) {
        return new FunctionalDelegatePrintStream(delegate, function);
    }

    /**
     * Create a new print stream that observes the invocations to {@link PrintStream#print(String)}.
     *
     * @param delegate the print stream to observe
     * @param consumer function observer function.
     * @return created print stream
     */
    public static PrintStream accept(PrintStream delegate, Consumer<String> consumer) {
        return new ObservedPrintStream(delegate, consumer);
    }

    /**
     * Create a new print stream that applies a transformation.
     *
     * @param delegate underlying delegate
     * @param function function used to apply the transformation.
     * @return created print stream
     */
    public static PrintStream apply(PrintStream delegate, Function<String, String> function) {
        return new TransformedPrintStream(delegate, function);
    }

    /**
     * Base {@link PrintStream} adapter.
     * It is based on {@link OutputStream#nullOutputStream()} and relies on the following abstract methods:
     * <ul>
     *     <li>{@link #print(String)}</li>
     *     <li>{@link #flush()}</li>
     * </ul>
     */
    @SuppressWarnings("NullableProblems")
    public abstract static class PrintStreamAdapter extends PrintStream {

        protected PrintStreamAdapter() {
            super(OutputStream.nullOutputStream());
        }

        @Override
        public abstract void print(String s);

        @Override
        public abstract void flush();

        @Override
        public void println() {
            println(EOL);
        }

        @Override
        public void println(String s) {
            if (s != null) {
                print(s + EOL);
            }
        }

        @Override
        public void print(boolean x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(char x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(int x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(long x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(float x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(double x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(char[] x) {
            print(String.valueOf(x));
        }

        @Override
        public void print(Object x) {
            print(String.valueOf(x));
        }

        @Override
        public void println(boolean x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(char x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(int x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(long x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(float x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(double x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(char[] x) {
            println(String.valueOf(x));
        }

        @Override
        public void println(Object x) {
            println(String.valueOf(x));
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            return format(format, args);
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            return format(l, format, args);
        }

        @Override
        public PrintStream format(String format, Object... args) {
            return super.format(format, args);
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            return super.format(l, format, args);
        }
    }

    private static final class ObservedPrintStream extends PrintStreamAdapter {

        private final PrintStream delegate;
        private final Consumer<String> consumer;

        ObservedPrintStream(PrintStream delegate, Consumer<String> consumer) {
            this.delegate = delegate;
            this.consumer = consumer;
        }

        @Override
        public void print(String s) {
            if (s != null && !s.isEmpty()) {
                delegate.print(s);
                consumer.accept(s);
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }
    }

    private static final class TransformedPrintStream extends PrintStreamAdapter {

        private final PrintStream delegate;
        private final Function<String, String> function;

        TransformedPrintStream(PrintStream delegate, Function<String, String> function) {
            this.delegate = delegate;
            this.function = function;
        }

        @Override
        public void print(String s) {
            if (s != null && !s.isEmpty()) {
                delegate.print(function.apply(s));
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }
    }

    private static final class FunctionalDelegatePrintStream extends PrintStreamAdapter {

        private final PrintStream delegate;
        private final BiConsumer<PrintStream, String> function;

        FunctionalDelegatePrintStream(PrintStream delegate, BiConsumer<PrintStream, String> function) {
            this.delegate = delegate;
            this.function = function;
        }

        @Override
        public void print(String s) {
            if (s != null && !s.isEmpty()) {
                function.accept(delegate, s);
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }
    }

    private static final class FastPrintStream extends PrintStream {

        FastPrintStream(FileDescriptor desc) {
            super(new FileOutputStream(desc), false, US_ASCII);
        }
    }
}
