/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * InputStream utilities.
 */
public final class InputStreams {
    private static final byte[] BUFFER = new byte[256 * 1024];

    /**
     * Transfers the contents of the given input stream to the given output stream.
     * This implementation uses a large shared buffer and is therefore single-threaded.
     *
     * @param in The input stream.
     * @param out The output stream.
     * @throws IOException If an error occurs.
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        synchronized (BUFFER) {
            try (InputStream data = in) {
                int bytesRead;
                while ((bytesRead = data.read(BUFFER)) != -1) {
                    out.write(BUFFER, 0, bytesRead);
                }
            }
        }
    }

    /**
     * Reads the contents of the given input stream as a UTF8 string.
     *
     * @param in The input stream.
     * @return The string.
     * @throws IOException If an error occurs.
     */
    public static String toString(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transfer(in, out);
        return toString(out);
    }

    /**
     * Reads the contents of the given output stream as a UTF8 string.
     *
     * @param out The output stream.
     * @return The string.
     */
    public static String toString(ByteArrayOutputStream out) {
        return out.toString(UTF_8);
    }

    /**
     * Wraps the given output stream as a {@code PrintStream} that uses UTF8 encoding.
     *
     * @param out       The stream to wrap.
     * @param autoFlush {@code true} If stream should flush on each line.
     * @return The stream.
     */
    public static PrintStream toPrintStream(OutputStream out, boolean autoFlush) {
        try {
            return new PrintStream(out, autoFlush, UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the contents of the given input stream as a list of UTF8 lines.
     *
     * @param in The input stream.
     * @return The list.
     * @throws IOException If an error occurs.
     */
    public static List<String> toLines(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            List<String> result = new ArrayList<>();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            return result;
        }
    }

    /**
     * Filter the given input stream to normalize new lines.
     *
     * @param in input stream
     * @return InputStream
     */
    public static InputStream normalizeNewLines(InputStream in) {
        Objects.requireNonNull(in);
        return new FilterInputStream(in) {
            private volatile int next = -1;

            @Override
            public int read() throws IOException {
                int i;
                if (next < 0) {
                    i = in.read();
                } else {
                    i = next;
                    next = -1;
                }
                if (i == '\r') {
                    i = in.read();
                    if (i == '\n') {
                        return '\n';
                    }
                    next = i;
                    return '\r';
                }
                return i;
            }
        };
    }

    private InputStreams() {
    }

    private abstract static class FilterInputStream extends InputStream {

        private final InputStream in;

        private FilterInputStream(InputStream in) {
            this.in = Objects.requireNonNull(in);
        }

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public void mark(int limit) {
            in.mark(limit);
        }

        @Override
        public void reset() throws IOException {
            in.reset();
        }

        @Override
        public boolean markSupported() {
            return in.markSupported();
        }
    }
}
