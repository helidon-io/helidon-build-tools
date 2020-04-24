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

package io.helidon.build.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.fusesource.jansi.AnsiOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility to strip out Ansi escape codes from a string.
 */
public class StripAnsi {
    private static final ByteArrayOutputStream BYTES = new ByteArrayOutputStream();
    private static final AnsiOutputStream STRIP = new AnsiOutputStream(BYTES);

    /**
     * Strips any Ansi escape codes from the given string.
     *
     * @param input The string.
     * @return The stripped string.
     */
    public static synchronized String stripAnsi(String input) {
        BYTES.reset();
        try {
            STRIP.write(input.getBytes(UTF_8));
            return new String(BYTES.toByteArray(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
