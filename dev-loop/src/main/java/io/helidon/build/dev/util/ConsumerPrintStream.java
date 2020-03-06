/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.dev.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import com.google.common.base.Charsets;

/**
 * A {@code PrintStream} that writes lines to a {@code Consumer<String>}.
 */
public class ConsumerPrintStream extends PrintStream {
    private static final Charset ENCODING = Charsets.UTF_8;
    private static final int LINE_FEED = 10;
    private static final int CARRIAGE_RETURN = 13;
    private static final int NONE = -1;
    private final ByteArrayOutputStream buffer;
    private final Consumer<String> consumer;
    private int last;

    /**
     * Returns a new stream for the given consumer.
     *
     * @param consumer The consumer.
     * @return The stream.
     */
    public static ConsumerPrintStream newStream(Consumer<String> consumer) {
        try {
            return new ConsumerPrintStream(consumer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor.
     *
     * @param consumer The consumer.
     * @throws UnsupportedEncodingException If the ISO_8859_1 encoding is not supported.
     */
    private ConsumerPrintStream(Consumer<String> consumer) throws UnsupportedEncodingException {
        super(new ByteArrayOutputStream(), true, ENCODING.toString());
        this.buffer = (ByteArrayOutputStream) super.out;
        this.consumer = consumer;
        this.last = NONE;
    }

    @Override
    public void write(final int b) {
        if (this.last == CARRIAGE_RETURN && b == LINE_FEED) {
            this.last = NONE;
        } else {
            if (b != LINE_FEED && b != CARRIAGE_RETURN) {
                super.write(b);
            } else {
                try {
                    consumer.accept(buffer.toString());
                } finally {
                    this.buffer.reset();
                }
            }
            this.last = b;
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public void write(byte[] bytes, int offset, int length) {
        if (length >= 0) {
            for (int index = 0; index < length; ++index) {
                this.write(bytes[offset + index]);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException(length);
        }
    }
}
